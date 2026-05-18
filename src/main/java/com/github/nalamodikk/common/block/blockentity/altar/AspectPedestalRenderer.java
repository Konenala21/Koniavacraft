package com.github.nalamodikk.common.block.blockentity.altar;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import org.joml.Quaternionf;

public class AspectPedestalRenderer implements BlockEntityRenderer<AspectPedestalBlockEntity>,
        IBlockEntityRendererExtension<AspectPedestalBlockEntity> {

    public AspectPedestalRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(AspectPedestalBlockEntity pedestal, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack item = pedestal.getHeldItem();
        if (item.isEmpty()) return;

        Level level = pedestal.getLevel();
        if (level == null) return;

        float time = level.getGameTime() + partialTick;
        float bob = (float) Math.sin(time * 0.05f) * 0.06f;
        float rot = (time * 2.0f) % 360f;

        poseStack.pushPose();
        poseStack.translate(0.5, 1.45 + bob, 0.5);
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(rot)));
        poseStack.scale(1.0f, 1.0f, 1.0f);

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(item, level, null, 0);
        itemRenderer.render(item, ItemDisplayContext.GROUND, false, poseStack,
                bufferSource, packedLight, packedOverlay, model);

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(AspectPedestalBlockEntity pedestal) {
        BlockPos pos = pedestal.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
    }
}
