package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.common.entity.FloatingTurretEntity;
import com.github.nalamodikk.register.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class FloatingTurretRenderer extends EntityRenderer<FloatingTurretEntity> {

    private static final float SCALE_ENTITY     = 0.35F;
    private static final float SCALE_FIRST_PERSON = 0.10F;

    public FloatingTurretRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(FloatingTurretEntity entity, Frustum frustum, double x, double y, double z) {
        // 手持模式的本地玩家：不做 frustum culling，避免視角轉動時 ghost 消失
        Minecraft mc = Minecraft.getInstance();
        if (entity.getSlotIndex() >= 2 && mc.player != null && entity.getOwnerUUID().isPresent()
                && entity.getOwnerUUID().get().equals(mc.player.getUUID())) {
            return true;
        }
        return super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(FloatingTurretEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        int slotIdx = entity.getSlotIndex();
        if (slotIdx < 2 && !entity.isInCombat()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;

        boolean ownerIsLocal = localPlayer != null && entity.getOwnerUUID().isPresent()
                && entity.getOwnerUUID().get().equals(localPlayer.getUUID());
        boolean isFirstPerson = mc.options.getCameraType() == CameraType.FIRST_PERSON;

        poseStack.pushPose();

        if (slotIdx >= 2 && ownerIsLocal) {
            float yawRad = localPlayer.getYRot() * (float) (Math.PI / 180.0);
            double rightX   = -Math.cos(yawRad);
            double rightZ   = -Math.sin(yawRad);
            double forwardX = -Math.sin(yawRad);
            double forwardZ =  Math.cos(yawRad);

            // 慣用手設定：左撇子的主手方向相反
            boolean isLeftHanded = localPlayer.getMainArm() == HumanoidArm.LEFT;
            double mainHandSide = isLeftHanded ? -1.0 : 1.0;
            double side = (slotIdx == 2) ? mainHandSide : -mainHandSide;

            double targetX, targetY, targetZ;
            double lerpX, lerpY, lerpZ;

            if (isFirstPerson) {
                // 第一人稱：用 player lerped position 當基準，避免衝刺時 entity sync 延遲導致暴衝
                // 加入 pitch 讓砲跟著視角仰角移動，和原版持握物品行為一致
                float pitch = Mth.lerp(partialTick, localPlayer.xRotO, localPlayer.getXRot());
                float pitchRad = pitch * (float) (Math.PI / 180.0);
                double lookX = forwardX * Math.cos(pitchRad);
                double lookY = -Math.sin(pitchRad);
                double lookZ = forwardZ * Math.cos(pitchRad);

                double playerLerpX = Mth.lerp(partialTick, localPlayer.xo, localPlayer.getX());
                double playerLerpY = Mth.lerp(partialTick, localPlayer.yo, localPlayer.getY());
                double playerLerpZ = Mth.lerp(partialTick, localPlayer.zo, localPlayer.getZ());

                targetX = playerLerpX + rightX * side * 0.70 + lookX * 0.35;
                targetY = playerLerpY + localPlayer.getEyeHeight() - 0.10 + lookY * 0.35;
                targetZ = playerLerpZ + rightZ * side * 0.70 + lookZ * 0.35;
            } else {
                // 第三人稱：側邊 1.8 + 前方 1.5
                targetX = localPlayer.getX() + rightX * side * 1.8 + forwardX * 1.5;
                targetY = localPlayer.getEyeY() + 0.8;
                targetZ = localPlayer.getZ() + rightZ * side * 1.8 + forwardZ * 1.5;
            }

            lerpX = Mth.lerp(partialTick, entity.xo, entity.getX());
            lerpY = Mth.lerp(partialTick, entity.yo, entity.getY());
            lerpZ = Mth.lerp(partialTick, entity.zo, entity.getZ());
            poseStack.translate(targetX - lerpX, targetY - lerpY, targetZ - lerpZ);
        }

        float scale = (slotIdx >= 2 && ownerIsLocal && isFirstPerson) ? SCALE_FIRST_PERSON : SCALE_ENTITY;
        poseStack.scale(scale, scale, scale);

        if (slotIdx >= 2 && ownerIsLocal && isFirstPerson) {
            // 第一人稱：yaw + pitch 同步，讓砲管完整追蹤視角
            // model +X = 砲管方向；Ry 讓砲管對準水平朝向，Rz(-pitch) 讓砲管跟著仰角傾斜
            float yawOffset = -localPlayer.getYRot() + 270F;
            poseStack.mulPose(Axis.YP.rotationDegrees(yawOffset));
            float interpPitch = Mth.lerp(partialTick, localPlayer.xRotO, localPlayer.getXRot());
            poseStack.mulPose(Axis.ZP.rotationDegrees(-interpPitch));
        } else {
            float yawOffset = ownerIsLocal ? -localPlayer.getYRot() + 270F : 270F;
            poseStack.mulPose(Axis.YP.rotationDegrees(yawOffset));
            if (slotIdx >= 2 && ownerIsLocal) {
                // 手持第三人稱：跟第一人稱一樣加仰角旋轉
                float interpPitch = Mth.lerp(partialTick, localPlayer.xRotO, localPlayer.getXRot());
                poseStack.mulPose(Axis.ZP.rotationDegrees(-interpPitch));
            } else {
                // 裝備槽模式：保留自旋動畫
                float spin = (entity.tickCount + partialTick) * 2.0F;
                poseStack.mulPose(Axis.XP.rotationDegrees(spin));
            }
        }

        // OBJ 幾何中心在 (-2.8125, 0.6756, -0.0843)，translate 讓模型繞自身中心旋轉
        // PoseStack 後加 = 先作用到頂點，等同在旋轉前把模型中心移到原點
        poseStack.translate(2.8125, -0.6756, 0.0843);

        ItemStack stack = new ItemStack(ModItems.FLOATING_TURRET.get());
        boolean isGhost = slotIdx >= 2 && ownerIsLocal && isFirstPerson;
        int light = isGhost ? LightTexture.FULL_BRIGHT : packedLight;
        MultiBufferSource renderSource = isGhost ? ghostSource(bufferSource, 100) : bufferSource;

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.NONE,
                light,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                renderSource,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FloatingTurretEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }

    private static MultiBufferSource ghostSource(MultiBufferSource original, int alpha) {
        VertexConsumer buf = original.getBuffer(RenderType.translucent());
        return ignored -> new GhostConsumer(buf, alpha);
    }

    private static final class GhostConsumer implements VertexConsumer {
        private final VertexConsumer d;
        private final int alpha;

        GhostConsumer(VertexConsumer d, int alpha) { this.d = d; this.alpha = alpha; }

        @Override public VertexConsumer addVertex(float x, float y, float z) { d.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { d.setColor(r, g, b, Math.min(a, alpha)); return this; }
        @Override public VertexConsumer setUv(float u, float v) { d.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { d.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { d.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float nx, float ny, float nz) { d.setNormal(nx, ny, nz); return this; }
    }
}
