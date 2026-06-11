package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.space.ship.ShipContraption;
import com.github.nalamodikk.space.ship.ShipMeshHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 飛船靜態方塊的「烤一次」快取：把所有 MODEL 方塊用假世界 tesselate 進 VertexBuffer 一次（每個
 * chunk render layer 一個 VBO），之後每幀只用實體變換矩陣畫，不再每幀重建。走 vanilla chunk
 * shader（RenderType.setupRenderState + drawWithShader），AMD 安全。
 *
 * 光照在烤的當下寫進頂點（來自 ShipRenderWorld，含方塊自身發光），飛船無動態光，靜態即可。
 * BER 方塊（箱子）不在此，仍每幀由 dispatcher 畫。
 */
public class ShipMeshCache implements AutoCloseable, ShipMeshHandle {

    // 編輯後不立刻重烤：標記 dirty，停手這麼久才重烤一次(debounce)。
    private static final long REBAKE_DELAY_MS = 400;

    private final Map<RenderType, VertexBuffer> buffers = new HashMap<>();
    private boolean built = false;
    private boolean dirty = false;
    private long dirtyAtMs = 0;
    // 背景執行緒烤的結果：tesselate(慢)在 worker 跑，烤好才在 render thread 上傳 GL → 組裝/編輯不卡主執行緒。
    private CompletableFuture<BakeResult> pending;
    // 烤 mesh 時順便算好的真實光照，給 BER/船上實體渲染共用(別重算)。volatile:worker 算、render thread 讀。
    private volatile ShipLight shipLight;

    /** 一層的烤好結果：MeshData 參考著 bytes 的記憶體，上傳前 bytes 不能 close。 */
    private record LayerMesh(RenderType layer, MeshData mesh, ByteBufferBuilder bytes) {}
    /** 一次背景烤的完整結果：各層 mesh + 算好的光。 */
    private record BakeResult(List<LayerMesh> layers, ShipLight light) {}

    /** 給渲染器查船上某 local 位置的真實光(BER/實體用)。烤好前可能為 null。 */
    @org.jetbrains.annotations.Nullable public ShipLight getShipLight() { return shipLight; }

    /** 編輯時呼叫：不砍 VBO，只標記稍後重烤(debounce)。 */
    @Override
    public void markDirty() {
        dirty = true;
        dirtyAtMs = System.currentTimeMillis();
    }

    /** @return true 若這次剛把新 VBO 上傳完(供渲染器清掉「每幀先畫的剛放方塊」，接棒給 VBO 不重畫)。 */
    public boolean buildIfNeeded(ShipContraption c, Level level) {
        boolean uploaded = false;
        // 1. 背景烤好了 → 在 render thread 上傳並換掉舊 VBO
        if (pending != null && pending.isDone()) {
            uploadPending();
            uploaded = true;
        }
        // 2. 初次組裝：開始背景烤(期間 buffers 空 = 靜態方塊短暫不顯示，BER 方塊照畫)
        if (!built && pending == null) {
            startBake(c, level);
        }
        // 3. 編輯後 debounce 到 → 背景重烤(舊 VBO 續用，烤好才換 → 不閃不卡)
        if (built && dirty && pending == null && System.currentTimeMillis() - dirtyAtMs >= REBAKE_DELAY_MS) {
            startBake(c, level);
            dirty = false;
        }
        return uploaded;
    }

    /** render thread：快照方塊(避免 worker 讀 live contraption 併發)後丟背景烤。 */
    private void startBake(ShipContraption c, Level level) {
        Map<BlockPos, BlockState> snapshot = new HashMap<>(c.getBlocks().size());
        for (var e : c.getBlocks().entrySet()) snapshot.put(e.getKey(), e.getValue().state());
        pending = CompletableFuture.supplyAsync(() -> bakeOffThread(snapshot, level), Util.backgroundExecutor());
    }

    /** worker thread：純 tesselate(讀不可變快照 + baked model，皆 thread-safe)。不碰 GL。 */
    private static BakeResult bakeOffThread(Map<BlockPos, BlockState> snapshot, Level level) {
        ShipRenderWorld world = new ShipRenderWorld(level, snapshot);
        BlockRenderDispatcher brd = Minecraft.getInstance().getBlockRenderer();
        ModelBlockRenderer mr = brd.getModelRenderer();
        RandomSource random = RandomSource.create();
        List<LayerMesh> out = new ArrayList<>();

        for (RenderType layer : RenderType.chunkBufferLayers()) {
            ByteBufferBuilder bytes = new ByteBufferBuilder(4096);
            BufferBuilder bb = new BufferBuilder(bytes, layer.mode(), layer.format());
            PoseStack ps = new PoseStack();
            boolean any = false;

            for (var entry : snapshot.entrySet()) {
                BlockPos local = entry.getKey();
                BlockState state = entry.getValue();
                if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;
                BakedModel model = brd.getBlockModel(state);
                ModelData md = model.getModelData(world, local, state, ModelData.EMPTY);
                if (!model.getRenderTypes(state, random, md).contains(layer)) continue;
                ps.pushPose();
                ps.translate(local.getX(), local.getY(), local.getZ());
                mr.tesselateBlock(world, model, state, local, ps, bb, true, random,
                        state.getSeed(local), OverlayTexture.NO_OVERLAY, md, layer);
                ps.popPose();
                any = true;
            }

            MeshData mesh = any ? bb.build() : null;
            if (mesh != null) out.add(new LayerMesh(layer, mesh, bytes));
            else bytes.close();
        }
        return new BakeResult(out, world.light());
    }

    /** render thread：把背景烤好的 mesh 上傳成 VBO，換掉舊的(舊 VBO 撐到這刻才關 → 重烤期間不閃)。 */
    private void uploadPending() {
        BakeResult result;
        try {
            result = pending.join();
        } catch (Exception ex) {
            pending = null;
            built = true; // 失敗就維持舊 buffers，別卡死重試
            return;
        }
        pending = null;
        shipLight = result.light(); // 換 mesh 的同時換上對應的光,給 BER/實體查
        for (VertexBuffer vb : buffers.values()) vb.close();
        buffers.clear();
        for (LayerMesh lm : result.layers()) {
            VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vb.bind();
            vb.upload(lm.mesh()); // 上傳並消耗 mesh
            VertexBuffer.unbind();
            buffers.put(lm.layer(), vb);
            lm.bytes().close(); // mesh 已上傳，釋放 native buffer
        }
        built = true;
    }

    /**
     * 每幀畫。model-view = 相機視角矩陣(RenderSystem) × 實體位移(pose)。
     * 烤好的頂點是 contraption local 空間，pose 只含「實體相對相機的位移」(不含相機旋轉)，
     * 相機旋轉在全域 modelview。漏乘相機矩陣會讓船黏著畫面跟玩家跑，所以這裡要相乘。
     */
    public void draw(PoseStack pose) {
        if (buffers.isEmpty()) return;
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        for (Map.Entry<RenderType, VertexBuffer> e : buffers.entrySet()) {
            RenderType layer = e.getKey();
            VertexBuffer vb = e.getValue();
            layer.setupRenderState();
            vb.bind();
            vb.drawWithShader(modelView, projection, RenderSystem.getShader());
            VertexBuffer.unbind();
            layer.clearRenderState();
        }
    }

    @Override
    public void close() {
        // 還在背景烤就把結果收掉，否則 MeshData/ByteBufferBuilder 的 native 記憶體會漏
        if (pending != null) {
            try {
                for (LayerMesh lm : pending.join().layers()) { lm.mesh().close(); lm.bytes().close(); }
            } catch (Exception ignored) {}
            pending = null;
        }
        for (VertexBuffer vb : buffers.values()) vb.close();
        buffers.clear();
    }
}
