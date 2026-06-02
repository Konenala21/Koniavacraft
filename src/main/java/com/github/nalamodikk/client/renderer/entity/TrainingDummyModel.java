package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.model.bedrock.BedrockAnimData;
import com.github.nalamodikk.client.model.bedrock.BedrockAnimPlayer;
import com.github.nalamodikk.client.model.bedrock.BedrockGeoParser;
import com.github.nalamodikk.common.entity.TrainingDummyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

public class TrainingDummyModel<T extends TrainingDummyEntity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "training_dummy"), "main");

    private static final String ANIM_IDLE = "animation.test_cactus.idle_loop";
    private static final String ANIM_HIT  = "animation.test_cactus.hit";

    private static final BedrockAnimData ANIM_DATA = BedrockAnimData.parse(
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "training_dummy.animation.json"));

    private final ModelPart body;
    private final BedrockAnimPlayer animPlayer;

    public TrainingDummyModel(ModelPart root) {
        this.body       = root.getChild("body");
        this.animPlayer = new BedrockAnimPlayer(root);
    }

    public static LayerDefinition createBodyLayer() {
        return BedrockGeoParser.parse(
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "training_dummy.geo.json"));
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float timeSec = ageInTicks / 20f;

        // hit animation takes priority; falls back to idle when done
        float hitSec = entity.getHitAnimTime() / 20f;
        if (hitSec > 0f) {
            animPlayer.apply(ANIM_HIT, hitSec, ANIM_DATA);
        } else {
            animPlayer.apply(ANIM_IDLE, timeSec, ANIM_DATA);
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
