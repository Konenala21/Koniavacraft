package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.common.entity.PlayerCloneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.UUID;

public class PlayerCloneRenderer extends HumanoidMobRenderer<PlayerCloneEntity, PlayerModel<PlayerCloneEntity>> {

    public PlayerCloneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
        this.addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerCloneEntity entity) {
        UUID id = entity.getSourceUUID().orElse(null);
        Minecraft mc = Minecraft.getInstance();
        if (id != null && mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(id);
            if (info != null) {
                return info.getSkin().texture();
            }
        }
        return DefaultPlayerSkin.get(id != null ? id : entity.getUUID()).texture();
    }

    @Override
    public void render(PlayerCloneEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // 死亡演出階段化視覺：搖晃 / 旋轉 / 縮放
        int phase = entity.getDeathPhase();
        if (phase > 0 && phase < 5) {
            poseStack.pushPose();
            applyDeathPhaseTransform(entity, phase, partialTicks, poseStack);
            renderInner(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            poseStack.popPose();
            return;
        }
        // Phase 5 = boss 視覺消失（remove() 將在 phase 5 結束時觸發）
        if (phase == 5) return;
        renderInner(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderInner(PlayerCloneEntity entity, float entityYaw, float partialTicks,
                              PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.isArmored()) {
            // 變身期間本體保持正常大小，移到機甲頭部位置渲染（像駕駛艙裡的本體）
            poseStack.pushPose();
            poseStack.translate(0.0, PlayerCloneEntity.ARMORED_BODY_OFFSET_Y, 0.0);
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            poseStack.popPose();
        } else {
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        }
    }

    // 死亡階段的 PoseStack 變換（搖晃、旋轉、縮放等視覺）
    private void applyDeathPhaseTransform(PlayerCloneEntity entity, int phase,
                                          float partialTicks, PoseStack pose) {
        float t = entity.deathTime + partialTicks;
        switch (phase) {
            case 1 -> {
                // Stagger：高頻小幅抖動，後傾倒姿
                float jitter = (float) Math.sin(t * 3.0) * 0.04f;
                pose.translate(jitter, 0, 0);
                pose.mulPose(Axis.XP.rotationDegrees(-(t / PlayerCloneEntity.DEATH_PHASE_STAGGER_END) * 8f));
            }
            case 2 -> {
                // Glow Up：緩慢 Y 軸自轉 + 略微上浮
                float p = (t - PlayerCloneEntity.DEATH_PHASE_STAGGER_END)
                        / (PlayerCloneEntity.DEATH_PHASE_GLOW_END - PlayerCloneEntity.DEATH_PHASE_STAGGER_END);
                p = Mth.clamp(p, 0f, 1f);
                pose.translate(0, p * 0.5f, 0);                          // 浮起 0.5 格
                pose.mulPose(Axis.YP.rotationDegrees(p * p * 180f));     // 二次加速旋轉，最後 180°
                pose.mulPose(Axis.XP.rotationDegrees(-8f - p * 4f));     // 後仰加深
            }
            case 3 -> {
                // Crack：加速旋轉 + 抖動加大（裂痕能量震顫感）
                float p = (t - PlayerCloneEntity.DEATH_PHASE_GLOW_END)
                        / (PlayerCloneEntity.DEATH_PHASE_CRACK_END - PlayerCloneEntity.DEATH_PHASE_GLOW_END);
                p = Mth.clamp(p, 0f, 1f);
                float shake = (float) Math.sin(t * 6.0) * (0.05f + p * 0.10f);
                pose.translate(shake, 0.5f + p * 0.3f, shake * 0.5f);
                pose.mulPose(Axis.YP.rotationDegrees(180f + p * 360f));  // 累積到 540°
                pose.mulPose(Axis.XP.rotationDegrees(-12f - p * 6f));
                float pulse = 1f + (float) Math.sin(t * 4.0) * 0.06f * p;
                pose.scale(pulse, pulse, pulse);
            }
            case 4 -> {
                // Shatter：高速旋轉 + scale 急速縮小 + 大幅抖動
                float p = (t - PlayerCloneEntity.DEATH_PHASE_CRACK_END)
                        / (PlayerCloneEntity.DEATH_PHASE_SHATTER_END - PlayerCloneEntity.DEATH_PHASE_CRACK_END);
                p = Mth.clamp(p, 0f, 1f);
                float shake = (float) Math.sin(t * 9.0) * 0.15f * (1f - p);
                pose.translate(shake, 0.8f + p * 0.2f, shake * 0.7f);
                pose.mulPose(Axis.YP.rotationDegrees(540f + p * 720f));  // 累積到 1260°
                pose.mulPose(Axis.XP.rotationDegrees(-18f));
                float shrink = Mth.lerp(p, 1f, 0.15f);                   // 1 → 0.15
                pose.scale(shrink, shrink, shrink);
            }
        }
    }
}
