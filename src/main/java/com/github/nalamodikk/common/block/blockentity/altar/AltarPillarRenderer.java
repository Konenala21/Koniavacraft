package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.register.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class AltarPillarRenderer implements BlockEntityRenderer<AltarPillarBlockEntity> {

    public AltarPillarRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(AltarPillarBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // 只有底段（TOP=false）負責渲染整根柱子
        if (be.getBlockState().getValue(AltarPillarBlock.TOP)) return;

        Level level = be.getLevel();
        if (level == null) return;

        int rotation = be.getRotation();
        BlockState state = ModBlocks.ALTAR_PILLAR.get().defaultBlockState()
                .setValue(AltarPillarBlock.TOP, false);

        // 直接從 BakedModel 取 quad，不走 ItemRenderer（避免 display transform 干擾）
        var modelShaper = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
        var bakedModel = modelShaper.getBlockModel(state);

        RandomSource random = RandomSource.create(42L);
        List<BakedQuad> quads = new ArrayList<>();
        // 取所有面（含方向性剔除面）
        quads.addAll(bakedModel.getQuads(state, null, random, ModelData.EMPTY, null));
        for (Direction dir : Direction.values()) {
            quads.addAll(bakedModel.getQuads(state, dir, random, ModelData.EMPTY, null));
        }

        if (quads.isEmpty()) return;

        // entityCutoutNoCull + block atlas = BER 正確渲染方式，不透視地板
        var consumer = bufferSource.getBuffer(
                RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));

        poseStack.pushPose();
        // 置中在方塊 XZ，Y+1 讓 OBJ 底部（y=-1.0）對齊方塊底面（render y=0）
        poseStack.translate(0.5, 1.0, 0.5);
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-rotation)));

        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(AltarPillarBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1,
                        pos.getX() + 2, pos.getY() + 3, pos.getZ() + 2);
    }
}
