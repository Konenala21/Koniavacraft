package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.space.ship.ShipContraption;
import com.github.nalamodikk.space.ship.ShipEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import java.util.Map;

/**
 * 飛船渲染器（M2b naive 版）：逐方塊用 renderSingleBlock 畫。
 * renderSingleBlock 不做鄰面剔除/AO、用統一光照，最簡單但夠用先看到船。
 * 之後 M2.5 換「烤 buffer」（需要假世界算面剔除/AO/正確光照）優化。
 *
 * 對齊：ShipEntity 落在核心方塊角落，translate(localPos) 即與原方塊位置一致。
 * 限制：非 MODEL 渲染的方塊（箱子等需要 BER）暫不畫，資料仍保留在 contraption 裡。
 */
public class ShipEntityRenderer extends EntityRenderer<ShipEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public ShipEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.blockRenderer = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(ShipEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        ShipContraption c = entity.getContraption();
        if (c != null) {
            for (Map.Entry<BlockPos, StructureBlockInfo> e : c.getBlocks().entrySet()) {
                BlockPos local = e.getKey();
                BlockState state = e.getValue().state();
                if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) continue;
                pose.pushPose();
                pose.translate(local.getX(), local.getY(), local.getZ());
                blockRenderer.renderSingleBlock(state, pose, buffers, packedLight, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }
        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ShipEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
