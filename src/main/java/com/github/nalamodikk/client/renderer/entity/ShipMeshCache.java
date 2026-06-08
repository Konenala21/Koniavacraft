package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.space.ship.ShipContraption;
import com.github.nalamodikk.space.ship.ShipMeshHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
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

import java.util.HashMap;
import java.util.Map;

/**
 * 飛船靜態方塊的「烤一次」快取：把所有 MODEL 方塊用假世界 tesselate 進 VertexBuffer 一次（每個
 * chunk render layer 一個 VBO），之後每幀只用實體變換矩陣畫，不再每幀重建。走 vanilla chunk
 * shader（RenderType.setupRenderState + drawWithShader），AMD 安全。
 *
 * 光照在烤的當下寫進頂點（來自 ShipRenderWorld，含方塊自身發光），飛船無動態光，靜態即可。
 * BER 方塊（箱子）不在此，仍每幀由 dispatcher 畫。
 */
public class ShipMeshCache implements AutoCloseable, ShipMeshHandle {

    // 編輯後不立刻重烤(大船烤整艘會卡)：標記 dirty，停手這麼久才重烤一次。期間沿用舊 VBO(視覺暫時舊)。
    private static final long REBAKE_DELAY_MS = 400;

    private final Map<RenderType, VertexBuffer> buffers = new HashMap<>();
    private boolean built = false;
    private boolean dirty = false;
    private long dirtyAtMs = 0;

    /** 編輯時呼叫：不砍 VBO，只標記稍後重烤(debounce)。 */
    @Override
    public void markDirty() {
        dirty = true;
        dirtyAtMs = System.currentTimeMillis();
    }

    public void buildIfNeeded(ShipContraption c, Level level) {
        if (!built) {
            bake(c, level);
            built = true;
            return;
        }
        // dirty 後等 debounce：連續編輯不會每次重烤，停手才烤一次(只一個卡頓而非每方塊一個)。
        if (dirty && System.currentTimeMillis() - dirtyAtMs >= REBAKE_DELAY_MS) {
            for (VertexBuffer vb : buffers.values()) vb.close();
            buffers.clear();
            bake(c, level);
            dirty = false;
        }
    }

    private void bake(ShipContraption c, Level level) {
        ShipRenderWorld world = new ShipRenderWorld(level, c);
        BlockRenderDispatcher brd = Minecraft.getInstance().getBlockRenderer();
        ModelBlockRenderer mr = brd.getModelRenderer();
        RandomSource random = RandomSource.create();

        for (RenderType layer : RenderType.chunkBufferLayers()) {
            ByteBufferBuilder bytes = new ByteBufferBuilder(4096);
            BufferBuilder bb = new BufferBuilder(bytes, layer.mode(), layer.format());
            PoseStack ps = new PoseStack();
            boolean any = false;

            for (var entry : c.getBlocks().entrySet()) {
                BlockPos local = entry.getKey();
                BlockState state = entry.getValue().state();
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
            if (mesh != null) {
                VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
                vb.bind();
                vb.upload(mesh);
                VertexBuffer.unbind();
                buffers.put(layer, vb);
            }
            bytes.close();
        }
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
        for (VertexBuffer vb : buffers.values()) vb.close();
        buffers.clear();
    }
}
