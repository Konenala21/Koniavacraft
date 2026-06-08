package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipContraption;
import com.github.nalamodikk.space.ship.ShipEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.Map;

/**
 * 飛船渲染器（M2.5）：逐方塊用 ModelBlockRenderer.tesselateBlock 搭配假世界 ShipRenderWorld 畫。
 * 比 renderSingleBlock 多了：鄰面剔除（內部面不畫，省三角形）、AO、以及把方塊自身發光值算進光照
 * （修復發光方塊變實體後不亮）。仍是每幀重建（未做 VBO 烤一次）；超大船的烤一次優化之後再做。
 *
 * 對齊：ShipEntity 落在核心方塊角落，translate(localPos) 即與原方塊位置一致。
 * 限制：非 MODEL 渲染的方塊（箱子等需要 BER）暫不畫，資料仍保留在 contraption 裡。
 */
public class ShipEntityRenderer extends EntityRenderer<ShipEntity> {

    private final BlockRenderDispatcher blockRenderer;
    private final BlockEntityRenderDispatcher beRenderer;
    private final RandomSource random = RandomSource.create();

    public ShipEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.blockRenderer = ctx.getBlockRenderDispatcher();
        this.beRenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher();
    }

    @Override
    public void render(ShipEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        ShipContraption c = entity.getContraption();
        if (c != null) {
            // 實體原點在船中心，繞原點旋轉，再 translate(-centerOffset) 把方塊 local 座標對齊到中心。
            // 這樣 translate(local) 後的世界位置與 rotatedWorldCorner(local) 一致（碰撞與渲染同步）。
            var co = entity.centerOffset();
            pose.pushPose();
            pose.mulPose(entity.orientation(partialTick)); // 完整 3D 姿勢(yaw+pitch+roll)；pitch=roll=0 時即純 yaw
            pose.translate(-co.x, -co.y, -co.z);

            // 靜態方塊：優先用烤好的 VBO（每幀只變換）；烤失敗則退回每幀 tesselate
            boolean drewStatic = false;
            if (!entity.isMeshFailed()) {
                ShipMeshCache cache = (ShipMeshCache) entity.getMeshCache();
                try {
                    if (cache == null) {
                        cache = new ShipMeshCache();
                        entity.setMeshCache(cache);
                    }
                    if (cache.buildIfNeeded(c, entity.level())) entity.clearPendingVisualBlocks(); // 烤好接棒
                    cache.draw(pose);
                    // 剛放、還沒進 VBO 的方塊：每幀先畫(放下去立刻可見，不先透明)
                    if (!entity.getPendingVisualBlocks().isEmpty()) {
                        renderBlocksPerFrame(entity, c, entity.getPendingVisualBlocks().keySet(), pose, buffers);
                    }
                    drewStatic = true;
                } catch (Exception ex) {
                    KoniavacraftMod.LOGGER.error("[Ship] VBO bake failed, falling back to per-frame", ex);
                    if (cache != null) try { cache.close(); } catch (Exception ignored) {}
                    entity.setMeshCache(null);
                    entity.markMeshFailed();
                }
            }
            if (!drewStatic) {
                renderStaticPerFrame(entity, c, pose, buffers);
            }

            // BER 方塊（箱子等）：用快取的臨時 BlockEntity 每幀畫。
            // 不用 dispatcher.render()，它內部 shouldRender() 拿 BE 的 local pos（靠近原點）跟相機算距離，
            // 船離原點遠就被距離剔除 → 透明。改直接拿 renderer 用明確光照 render，跳過 shouldRender。
            for (Map.Entry<BlockPos, BlockEntity> e : entity.getRenderBlockEntities().entrySet()) {
                BlockPos local = e.getKey();
                BlockEntity be = e.getValue();
                BlockEntityRenderer<BlockEntity> r = beRenderer.getRenderer(be);
                if (r == null) continue;
                pose.pushPose();
                pose.translate(local.getX(), local.getY(), local.getZ());
                r.render(be, partialTick, pose, buffers, packedLight, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            pose.popPose(); // 結束整艘船的 yaw 旋轉
        }
        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    /** 退回方案：每幀逐方塊 tesselate（VBO 烤失敗時用）。 */
    private void renderStaticPerFrame(ShipEntity entity, ShipContraption c, PoseStack pose,
                                      MultiBufferSource buffers) {
        renderBlocksPerFrame(entity, c, c.getBlocks().keySet(), pose, buffers);
    }

    /** 逐方塊 tesselate 指定的一組 local pos（VBO 退回 / 剛放方塊每幀先畫共用）。 */
    private void renderBlocksPerFrame(ShipEntity entity, ShipContraption c, java.util.Set<BlockPos> positions,
                                      PoseStack pose, MultiBufferSource buffers) {
        ShipRenderWorld world = new ShipRenderWorld(entity.level(), c);
        ModelBlockRenderer modelRenderer = blockRenderer.getModelRenderer();
        for (BlockPos local : positions) {
            StructureBlockInfo info = c.getBlocks().get(local);
            if (info == null) continue;
            BlockState state = info.state();
            if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;
            BakedModel model = blockRenderer.getBlockModel(state);
            long seed = state.getSeed(local);
            ModelData modelData = model.getModelData(world, local, state, ModelData.EMPTY);
            pose.pushPose();
            pose.translate(local.getX(), local.getY(), local.getZ());
            for (RenderType rt : model.getRenderTypes(state, random, modelData)) {
                modelRenderer.tesselateBlock(world, model, state, local, pose, buffers.getBuffer(rt),
                        true, random, seed, OverlayTexture.NO_OVERLAY, modelData, rt);
            }
            pose.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ShipEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
