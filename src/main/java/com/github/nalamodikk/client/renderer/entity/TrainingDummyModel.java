package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.TrainingDummyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * 訓練假人模型：抽象仙人掌（中央柱 + 兩側手臂 + 頂端小塊）。先用方塊堆疊的 placeholder，視覺之後再調。
 */
public class TrainingDummyModel<T extends TrainingDummyEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "training_dummy"), "main");

    private final ModelPart main;

    public TrainingDummyModel(ModelPart root) {
        this.main = root.getChild("main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("main",
                CubeListBuilder.create()
                        // 中央柱
                        .texOffs(0, 0).addBox(-4.0F, -28.0F, -4.0F, 8.0F, 28.0F, 8.0F, new CubeDeformation(0.0F))
                        // 頂端小塊
                        .texOffs(0, 0).addBox(-3.0F, -31.0F, -3.0F, 6.0F, 3.0F, 6.0F, new CubeDeformation(0.0F))
                        // 左臂
                        .texOffs(0, 0).addBox(-7.0F, -20.0F, -2.0F, 3.0F, 9.0F, 4.0F, new CubeDeformation(0.0F))
                        // 右臂
                        .texOffs(0, 0).addBox(4.0F, -23.0F, -2.0F, 3.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        // 不動的假人，無動畫
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        main.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
