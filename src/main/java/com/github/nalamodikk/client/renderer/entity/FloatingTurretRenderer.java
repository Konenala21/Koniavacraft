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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Rendering split:
 *   - Local player, hand entity, first-person  → rendered here with delta correction (proven to work)
 *   - Local player, hand entity, third-person  → skip here; FloatingTurretPlayerRenderer handles it
 *   - Local player, slot entity               → skip here; FloatingTurretPlayerRenderer handles it
 *   - Other players' entities                 → rendered here normally
 */
public class FloatingTurretRenderer extends EntityRenderer<FloatingTurretEntity> {

    private static final float SCALE_ENTITY      = 0.35F;
    private static final float SCALE_FIRST_PERSON = 0.10F;

    public FloatingTurretRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(FloatingTurretEntity entity, Frustum frustum, double x, double y, double z) {
        // First-person local hand entity: bypass frustum so it stays visible when camera enters it
        Minecraft mc = Minecraft.getInstance();
        if (isLocalHandFirstPerson(mc, entity)) return true;
        return super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(FloatingTurretEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;
        int slotIdx = entity.getSlotIndex();

        boolean ownerIsLocal = localPlayer != null && entity.getOwnerUUID()
                .map(id -> id.equals(localPlayer.getUUID())).orElse(false);

        if (ownerIsLocal) {
            // Slot entities: always handled by FloatingTurretPlayerRenderer
            if (slotIdx < 2) return;

            // Hand entity in third-person: handled by FloatingTurretPlayerRenderer
            boolean isFirstPerson = mc.options.getCameraType() == CameraType.FIRST_PERSON;
            if (!isFirstPerson) return;

            // Hand entity in first-person: render here with delta correction (no jitter at close range)
            renderHandFirstPerson(entity, localPlayer, partialTick, poseStack, bufferSource, packedLight, slotIdx);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }

        // Non-local player: render slot entities if in combat, hand entities always
        if (slotIdx < 2 && !entity.isInCombat()) return;

        // Look up the owner in the client level to get correct yaw/pitch
        Player owner = findOwner(entity);
        float ownerYaw   = owner != null ? Mth.lerp(partialTick, owner.yRotO, owner.getYRot()) : entityYaw;
        float ownerPitch = owner != null ? Mth.lerp(partialTick, owner.xRotO, owner.getXRot()) : 0F;

        poseStack.pushPose();
        poseStack.scale(SCALE_ENTITY, SCALE_ENTITY, SCALE_ENTITY);
        poseStack.mulPose(Axis.YP.rotationDegrees(270F - ownerYaw));
        if (slotIdx < 2) {
            float spin = (entity.tickCount + partialTick) * 2.0F;
            poseStack.mulPose(Axis.XP.rotationDegrees(spin));
        } else {
            // Hand entity: track owner's pitch so barrel faces their look direction
            poseStack.mulPose(Axis.ZP.rotationDegrees(-ownerPitch));
        }
        poseStack.translate(2.8125, -0.6756, 0.0843);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(ModItems.FLOATING_TURRET.get()),
                ItemDisplayContext.NONE,
                packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, entity.level(), entity.getId());
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void renderHandFirstPerson(FloatingTurretEntity entity, LocalPlayer player,
                                        float partialTick, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int slotIdx) {
        float yawRad = player.getYRot() * (float) (Math.PI / 180.0);
        double rightX   = -Math.cos(yawRad);
        double rightZ   = -Math.sin(yawRad);
        double forwardX = -Math.sin(yawRad);
        double forwardZ =  Math.cos(yawRad);

        boolean isLeftHanded = player.getMainArm() == HumanoidArm.LEFT;
        double mainHandSide = isLeftHanded ? -1.0 : 1.0;
        double side = (slotIdx == 2) ? mainHandSide : -mainHandSide;

        float pitch    = Mth.lerp(partialTick, player.xRotO, player.getXRot());
        float pitchRad = pitch * (float) (Math.PI / 180.0);
        double lookX = forwardX * Math.cos(pitchRad);
        double lookY = -Math.sin(pitchRad);
        double lookZ = forwardZ * Math.cos(pitchRad);

        double playerLerpX = Mth.lerp(partialTick, player.xo, player.getX());
        double playerLerpY = Mth.lerp(partialTick, player.yo, player.getY());
        double playerLerpZ = Mth.lerp(partialTick, player.zo, player.getZ());

        double targetX = playerLerpX + rightX * side * 0.70 + lookX * 0.35;
        double targetY = playerLerpY + player.getEyeHeight() - 0.10 + lookY * 0.35;
        double targetZ = playerLerpZ + rightZ * side * 0.70 + lookZ * 0.35;

        double lerpX = Mth.lerp(partialTick, entity.xo, entity.getX());
        double lerpY = Mth.lerp(partialTick, entity.yo, entity.getY());
        double lerpZ = Mth.lerp(partialTick, entity.zo, entity.getZ());

        poseStack.pushPose();
        poseStack.translate(targetX - lerpX, targetY - lerpY, targetZ - lerpZ);
        poseStack.scale(SCALE_FIRST_PERSON, SCALE_FIRST_PERSON, SCALE_FIRST_PERSON);

        float yawOffset = -player.getYRot() + 270F;
        poseStack.mulPose(Axis.YP.rotationDegrees(yawOffset));

        poseStack.mulPose(Axis.ZP.rotationDegrees(-pitch));
        poseStack.translate(2.8125, -0.6756, 0.0843);

        MultiBufferSource ghostSrc = ghostSource(bufferSource, 100);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(ModItems.FLOATING_TURRET.get()),
                ItemDisplayContext.NONE,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                poseStack, ghostSrc, entity.level(), entity.getId());
        poseStack.popPose();
    }

    @Nullable
    private static Player findOwner(FloatingTurretEntity entity) {
        return entity.getOwnerUUID().map(uuid -> {
            for (Player p : entity.level().players()) {
                if (p.getUUID().equals(uuid)) return p;
            }
            return null;
        }).orElse(null);
    }

    private static boolean isLocalHandFirstPerson(Minecraft mc, FloatingTurretEntity entity) {
        return entity.getSlotIndex() >= 2
                && mc.player != null
                && entity.getOwnerUUID().map(id -> id.equals(mc.player.getUUID())).orElse(false)
                && mc.options.getCameraType() == CameraType.FIRST_PERSON;
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
        @Override public VertexConsumer setColor(int r, int g, int b, int a)  { d.setColor(r, g, b, Math.min(a, alpha)); return this; }
        @Override public VertexConsumer setUv(float u, float v)               { d.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v)                  { d.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v)                  { d.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float nx, float ny, float nz){ d.setNormal(nx, ny, nz); return this; }
    }
}
