package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.space.ship.ShipContraption;
import com.github.nalamodikk.space.ship.ShipEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
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
    private final RandomSource random = RandomSource.create();

    public ShipEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.blockRenderer = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(ShipEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        ShipContraption c = entity.getContraption();
        if (c != null) {
            ShipRenderWorld world = new ShipRenderWorld(entity.level(), c);
            ModelBlockRenderer modelRenderer = blockRenderer.getModelRenderer();
            for (Map.Entry<BlockPos, StructureBlockInfo> e : c.getBlocks().entrySet()) {
                BlockPos local = e.getKey();
                BlockState state = e.getValue().state();
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
        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ShipEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
