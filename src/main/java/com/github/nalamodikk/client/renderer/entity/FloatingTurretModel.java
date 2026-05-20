package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.FloatingTurretEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class FloatingTurretModel<T extends FloatingTurretEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "floating_turret"), "main");

    private final ModelPart innerBarrel;  // 內砲彈
    private final ModelPart outerFrame;   // 外框
    private final ModelPart outerShell;   // 外殼

    public FloatingTurretModel(ModelPart root) {
        this.innerBarrel = root.getChild("inner_barrel");
        this.outerFrame  = root.getChild("outer_frame");
        this.outerShell  = root.getChild("outer_shell");
    }

    // BlockBench exported geometry (128x128 texture)
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition innerBarrel = partdefinition.addOrReplaceChild("inner_barrel",
                CubeListBuilder.create()
                        .texOffs(39, 13).addBox(-14.8F, -2.98F,  1.32F,  31.82F, 2.96F, 2.96F, new CubeDeformation(0.0F))
                        .texOffs(39, 13).addBox(-14.8F, -8.604F, 6.944F, 31.82F, 2.96F, 2.96F, new CubeDeformation(0.0F))
                        .texOffs(39, 13).addBox(-14.8F, -5.57F,  4.132F, 31.82F, 2.96F, 2.96F, new CubeDeformation(0.0F))
                        .texOffs(39, 13).addBox(-14.8F, -2.906F, 6.944F, 31.82F, 2.96F, 2.96F, new CubeDeformation(0.0F))
                        .texOffs(39, 13).addBox(-14.8F, -8.604F, 1.246F, 31.82F, 2.96F, 2.96F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.0F, 17.5F, -6.8F));

        PartDefinition outerFrame = partdefinition.addOrReplaceChild("outer_frame",
                CubeListBuilder.create()
                        .texOffs(34, 0) .addBox(-20.0F, -3.5F,  -1.9F,  43.0F, 2.0F,  13.0F, new CubeDeformation(0.0F))
                        .texOffs(66, 16).addBox( 22.0F, -5.5F,  -1.9F,   3.0F, 2.0F,  13.0F, new CubeDeformation(0.0F))
                        .texOffs(66, 16).addBox( 22.0F, -7.5F,  -2.9F,   3.0F, 2.0F,  15.0F, new CubeDeformation(0.0F))
                        .texOffs(66, 16).addBox( 22.0F, -9.5F,  -3.9F,   3.0F, 2.0F,  17.0F, new CubeDeformation(0.0F))
                        .texOffs(66, 16).addBox( 22.0F, -11.5F, -3.9F,   3.0F, 2.0F,  17.0F, new CubeDeformation(0.0F))
                        .texOffs(66, 16).addBox( 22.0F, -13.5F, -3.9F,   3.0F, 2.0F,  17.0F, new CubeDeformation(0.0F))
                        .texOffs(66, 16).addBox( 22.0F, -15.5F, -3.9F,   3.0F, 2.0F,  17.0F, new CubeDeformation(0.0F))
                        .texOffs(66, 16).addBox( 22.0F, -16.5F, -3.4F,   3.0F, 1.0F,  15.9F, new CubeDeformation(0.0F))
                        .texOffs(66, 16).addBox( 22.0F, -17.5F, -1.9F,   3.0F, 1.0F,  13.4F, new CubeDeformation(0.0F))
                        .texOffs(66, 16).addBox( 22.0F, -18.5F, -0.9F,   3.0F, 1.0F,  11.4F, new CubeDeformation(0.0F))
                        .texOffs(66, 16).addBox( 22.0F, -19.5F,  0.1F,   3.0F, 1.0F,   9.4F, new CubeDeformation(0.0F))
                        .texOffs(34, 0) .addBox(-20.0F, -20.5F, -1.9F,  43.0F, 2.0F,  13.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 13).addBox(-20.0F, -5.5F,  -2.9F,  43.0F, 2.0F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 13).addBox(-20.0F, -5.5F,  10.1F,  43.0F, 2.0F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 13).addBox(-20.0F, -18.5F, 10.1F,  43.0F, 2.0F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 13).addBox(-20.0F, -7.5F,  -4.9F,  43.0F, 2.0F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 13).addBox(-20.0F, -18.5F, -2.9F,  43.0F, 2.0F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 13).addBox(-20.0F, -16.5F, -4.9F,  43.0F, 2.0F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 41).addBox( 20.0F, -14.5F, -4.9F,   3.0F, 7.0F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 41).addBox(-20.0F, -14.5F, -4.9F,   3.0F, 7.0F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 41).addBox(  0.0F, -14.5F, -4.9F,   3.0F, 7.0F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 41).addBox(-20.0F, -14.5F, 12.1F,   3.0F, 7.0F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 41).addBox( 20.0F, -14.5F, 12.1F,   3.0F, 7.0F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 41).addBox(  0.0F, -14.5F, 12.1F,   3.0F, 7.0F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 13).addBox(-20.0F, -7.5F,  12.1F,  43.0F, 2.0F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 13).addBox(-20.0F, -16.5F, 12.1F,  43.0F, 2.0F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 13).addBox(-20.0F, -16.9F, 12.1F,  43.0F, 0.4F,   2.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 13).addBox(-20.0F, -16.9F, -4.9F,  43.0F, 0.4F,   2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.0F, 23.6F, -6.0F));

        PartDefinition outerShell = partdefinition.addOrReplaceChild("outer_shell",
                CubeListBuilder.create()
                        .texOffs(39, 13).addBox(-14.8F, -30.204F,  6.044F, 31.82F,  2.96F,  2.96F, new CubeDeformation(0.0F))
                        .texOffs(16, 13).addBox( -8.164F, -5.3015F, 6.5855F, 18.4556F, 9.0F, 1.7168F, new CubeDeformation(0.0F))
                        .texOffs(3, 13) .addBox( -8.164F, -28.5183F, -24.4145F, 18.4556F, 11.2168F, 1.7168F, new CubeDeformation(0.0F))
                        .texOffs(3, 13) .addBox( -6.164F, -19.5183F, -30.0F, 14.4556F, 2.2168F, 14.7168F, new CubeDeformation(0.0F))
                        .texOffs(3, 13) .addBox( -6.164F, -6.5183F,  -30.0F, 14.4556F, 2.2168F, 14.7168F, new CubeDeformation(0.0F))
                        .texOffs(16, 13).addBox( -8.164F, -5.3015F, -24.4145F, 18.4556F, 9.0F, 1.7168F, new CubeDeformation(0.0F))
                        .texOffs(3, 13) .addBox( -8.164F, -28.5183F, 6.5855F, 18.4556F, 11.2168F, 1.7168F, new CubeDeformation(0.0F))
                        .texOffs(3, 13) .addBox( -6.164F, -19.5183F, 1.5855F, 14.4556F, 2.2168F, 13.7168F, new CubeDeformation(0.0F))
                        .texOffs(3, 18) .addBox( -6.164F, -32.0F, -15.6985F, 14.4556F, 14.7168F, 2.2168F, new CubeDeformation(0.0F))
                        .texOffs(3, 18) .addBox( -6.164F, -32.0F, -2.6985F, 14.4556F, 14.7168F, 2.2168F, new CubeDeformation(0.0F))
                        .texOffs(3, 18) .addBox( -6.164F, -4.4145F, -15.6985F, 14.4556F, 13.7168F, 2.2168F, new CubeDeformation(0.0F))
                        .texOffs(3, 18) .addBox( -6.164F, -4.4145F, -2.6985F, 14.4556F, 13.7168F, 2.2168F, new CubeDeformation(0.0F))
                        .texOffs(3, 13) .addBox( -6.164F, -6.5183F, 1.5855F, 14.4556F, 2.2168F, 13.7168F, new CubeDeformation(0.0F))
                        .texOffs(3, 13) .addBox( -6.164F, 5.4817F, -22.1145F, 14.4556F, 2.2168F, 7.7168F, new CubeDeformation(0.0F))
                        .texOffs(3, 13) .addBox( -6.164F, -31.5183F, -0.1145F, 14.4556F, 2.2168F, 6.7168F, new CubeDeformation(0.0F))
                        .texOffs(3, 13) .addBox( -6.164F, -31.5183F, -22.1145F, 14.4556F, 2.2168F, 7.7168F, new CubeDeformation(0.0F))
                        .texOffs(3, 13) .addBox( -6.164F, 5.4817F, -0.1145F, 14.4556F, 2.2168F, 6.7168F, new CubeDeformation(0.0F))
                        .texOffs(39, 13).addBox(-14.8F, 3.494F,    6.044F, 31.82F,  2.96F,  2.96F, new CubeDeformation(0.0F))
                        .texOffs(39, 13).addBox(-14.8F, -18.58F,  -30.0F,  31.82F, 11.96F,  2.96F, new CubeDeformation(0.0F))
                        .texOffs(39, 13).addBox(-14.8F, -17.506F, 10.044F, 31.82F, 10.96F,  2.96F, new CubeDeformation(0.0F))
                        .texOffs(39, 13).addBox(-14.8F, 3.42F,   -24.58F,  31.82F,  2.96F,  2.96F, new CubeDeformation(0.0F))
                        .texOffs(39, 13).addBox(-14.8F, 7.42F,   -14.58F,  31.82F,  2.96F, 14.96F, new CubeDeformation(0.0F))
                        .texOffs(39, 13).addBox(-14.8F, -32.0F,  -15.08F,  31.82F,  2.96F, 14.96F, new CubeDeformation(0.0F))
                        .texOffs(39, 13).addBox(-14.8F, -30.204F, -24.654F, 31.82F,  2.96F,  2.96F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.0F, 24.0F, 6.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 內砲彈繞 X 軸緩慢旋轉（表示待機充能）
        this.innerBarrel.xRot = ageInTicks * 0.06F;
        // 外殼輕微上下搖擺
        this.outerShell.yRot = (float) Math.sin(ageInTicks * 0.03F) * 0.08F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay, int color) {
        this.innerBarrel.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.outerFrame .render(poseStack, buffer, packedLight, packedOverlay, color);
        this.outerShell .render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
