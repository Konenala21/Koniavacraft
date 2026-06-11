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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 飛船靜態方塊的「烤一次」快取，**分塊版**：把方塊依 16³ section 各自烤進 VBO（每 section × 每 render layer 一個）。
 * 編輯一塊只重烤它所在的 section（+ 邊界鄰居 section，因為剔除會變），不再整艘重烤 → 超大船編輯也不卡。
 * 走 vanilla chunk shader（setupRenderState + drawWithShader），AMD 安全。光照烤當下寫進頂點(無動態光)。
 * BER 方塊(箱子)不在此，仍每幀由 dispatcher 畫。
 */
public class ShipMeshCache implements AutoCloseable, ShipMeshHandle {

    private static final long REBAKE_DELAY_MS = 400; // 停手這麼久才重烤(debounce)
    private static final int SECTION = 16;            // section 邊長

    // 每個 section 一組「layer → VBO」。section key = 打包的 section 座標(local>>4)。
    private final Map<Long, Map<RenderType, VertexBuffer>> sectionBuffers = new HashMap<>();
    private final Set<Long> dirtySections = new HashSet<>();
    private boolean built = false;
    private long dirtyAtMs = 0;
    // 背景烤的結果：section → 各 layer mesh。tesselate(慢)在 worker，上傳 GL 在 render thread。
    private CompletableFuture<Map<Long, List<LayerMesh>>> pending;

    /** 一個 section 一層的烤好結果：MeshData 參考 bytes 記憶體，上傳前 bytes 不能 close。 */
    private record LayerMesh(RenderType layer, MeshData mesh, ByteBufferBuilder bytes) {}

    private static long sectionKey(BlockPos p) {
        return BlockPos.asLong(p.getX() >> 4, p.getY() >> 4, p.getZ() >> 4);
    }

    /** 編輯時呼叫：標記這格 + 6 面鄰居所在的 section 要重烤(鄰居因剔除可能跨 section)。 */
    @Override
    public void markDirty(BlockPos local) {
        dirtySections.add(sectionKey(local));
        dirtySections.add(sectionKey(local.above()));
        dirtySections.add(sectionKey(local.below()));
        dirtySections.add(sectionKey(local.north()));
        dirtySections.add(sectionKey(local.south()));
        dirtySections.add(sectionKey(local.east()));
        dirtySections.add(sectionKey(local.west()));
        dirtyAtMs = System.currentTimeMillis();
    }

    /** @return true 若這次剛上傳新 VBO(供渲染器清掉「每幀先畫的剛放方塊」)。 */
    public boolean buildIfNeeded(ShipContraption c, Level level) {
        boolean uploaded = false;
        if (pending != null && pending.isDone()) {
            uploadPending();
            uploaded = true;
        }
        if (!built && pending == null) {
            startBake(c, level, null); // 初次:全部 section
        }
        if (built && !dirtySections.isEmpty() && pending == null
                && System.currentTimeMillis() - dirtyAtMs >= REBAKE_DELAY_MS) {
            Set<Long> toBake = new HashSet<>(dirtySections);
            dirtySections.clear();
            startBake(c, level, toBake);
        }
        return uploaded;
    }

    /** render thread：快照方塊 + 依 section 分組(要烤的 section)後丟背景烤。 */
    private void startBake(ShipContraption c, Level level, Set<Long> sections) {
        Map<BlockPos, BlockState> snapshot = new HashMap<>(c.getBlocks().size());
        for (var e : c.getBlocks().entrySet()) snapshot.put(e.getKey(), e.getValue().state());

        Map<Long, List<BlockPos>> sectionBlocks = new HashMap<>();
        if (sections == null) {
            for (BlockPos p : snapshot.keySet()) sectionBlocks.computeIfAbsent(sectionKey(p), k -> new ArrayList<>()).add(p);
        } else {
            for (long sec : sections) sectionBlocks.put(sec, new ArrayList<>()); // 含可能變空的 section
            for (BlockPos p : snapshot.keySet()) {
                List<BlockPos> l = sectionBlocks.get(sectionKey(p));
                if (l != null) l.add(p);
            }
        }
        pending = CompletableFuture.supplyAsync(() -> bakeOffThread(snapshot, sectionBlocks, level), Util.backgroundExecutor());
    }

    /** worker thread：純 tesselate。world 用整艘 snapshot → section 邊界剔除正確;只烤指定 section 的方塊。 */
    private static Map<Long, List<LayerMesh>> bakeOffThread(Map<BlockPos, BlockState> snapshot,
                                                            Map<Long, List<BlockPos>> sectionBlocks, Level level) {
        ShipRenderWorld world = new ShipRenderWorld(level, snapshot);
        BlockRenderDispatcher brd = Minecraft.getInstance().getBlockRenderer();
        ModelBlockRenderer mr = brd.getModelRenderer();
        RandomSource random = RandomSource.create();
        Map<Long, List<LayerMesh>> out = new HashMap<>();

        for (var se : sectionBlocks.entrySet()) {
            List<BlockPos> locals = se.getValue();
            List<LayerMesh> layers = new ArrayList<>();
            for (RenderType layer : RenderType.chunkBufferLayers()) {
                ByteBufferBuilder bytes = new ByteBufferBuilder(4096);
                BufferBuilder bb = new BufferBuilder(bytes, layer.mode(), layer.format());
                PoseStack ps = new PoseStack();
                boolean any = false;
                for (BlockPos local : locals) {
                    BlockState state = snapshot.get(local);
                    if (state == null || state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;
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
                if (mesh != null) layers.add(new LayerMesh(layer, mesh, bytes));
                else bytes.close();
            }
            out.put(se.getKey(), layers); // 空 list = 該 section 沒可畫方塊 → 上傳時清掉
        }
        return out;
    }

    /** render thread：把背景烤好的 section mesh 上傳成 VBO，換掉那些 section 的舊 VBO。 */
    private void uploadPending() {
        Map<Long, List<LayerMesh>> result;
        try {
            result = pending.join();
        } catch (Exception ex) {
            pending = null;
            built = true;
            return;
        }
        pending = null;
        for (var se : result.entrySet()) {
            long sec = se.getKey();
            Map<RenderType, VertexBuffer> old = sectionBuffers.remove(sec);
            if (old != null) for (VertexBuffer vb : old.values()) vb.close();
            List<LayerMesh> layers = se.getValue();
            if (layers.isEmpty()) continue; // section 變空
            Map<RenderType, VertexBuffer> map = new HashMap<>();
            for (LayerMesh lm : layers) {
                VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
                vb.bind();
                vb.upload(lm.mesh());
                VertexBuffer.unbind();
                map.put(lm.layer(), vb);
                lm.bytes().close();
            }
            sectionBuffers.put(sec, map);
        }
        built = true;
    }

    /**
     * 每幀畫。按 layer 分組(每 layer setupRenderState 一次,畫全部 section 的該 layer VBO)以減少狀態切換。
     * 烤好的頂點在 contraption local 空間，pose 只含實體相對相機位移，相機旋轉在全域 modelview。
     */
    public void draw(PoseStack pose) {
        if (sectionBuffers.isEmpty()) return;
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        for (RenderType layer : RenderType.chunkBufferLayers()) {
            boolean setup = false;
            for (Map<RenderType, VertexBuffer> secMap : sectionBuffers.values()) {
                VertexBuffer vb = secMap.get(layer);
                if (vb == null) continue;
                if (!setup) { layer.setupRenderState(); setup = true; }
                vb.bind();
                vb.drawWithShader(modelView, projection, RenderSystem.getShader());
            }
            if (setup) {
                VertexBuffer.unbind();
                layer.clearRenderState();
            }
        }
    }

    @Override
    public void close() {
        if (pending != null) {
            try {
                for (List<LayerMesh> layers : pending.join().values())
                    for (LayerMesh lm : layers) { lm.mesh().close(); lm.bytes().close(); }
            } catch (Exception ignored) {}
            pending = null;
        }
        for (Map<RenderType, VertexBuffer> secMap : sectionBuffers.values())
            for (VertexBuffer vb : secMap.values()) vb.close();
        sectionBuffers.clear();
    }
}
