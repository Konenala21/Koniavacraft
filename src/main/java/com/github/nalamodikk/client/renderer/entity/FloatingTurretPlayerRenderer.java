package com.github.nalamodikk.client.renderer.entity;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.FloatingTurretEntity;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import com.github.nalamodikk.register.ModMobEffects;
import com.github.nalamodikk.register.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.List;

/**
 * Renders floating turrets attached to the LOCAL player via RenderPlayerEvent.
 * Avoids entity position sync lag by computing position from the player's
 * own interpolated yaw/pitch — zero jitter.
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class FloatingTurretPlayerRenderer {

    private static final float SCALE_THIRD_PERSON = 0.35F;
    private static final float SCALE_SLOT         = 0.35F;
    private static final float ORBIT_RADIUS        = 1.5F;
    private static final float ORBIT_HEIGHT        = 1.0F;

    // Per-hand smoothed charge ratio — persists between frames for release animation
    private static float smoothedChargeMain = 0F;
    private static float smoothedChargeOff  = 0F;
    private static int   lastDecayTickMain  = -1;
    private static int   lastDecayTickOff   = -1;

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = event.getEntity();
        if (!player.getUUID().equals(mc.player.getUUID())) return; // only local player

        float partialTick          = event.getPartialTick();
        PoseStack poseStack        = event.getPoseStack();
        MultiBufferSource buffers  = event.getMultiBufferSource();
        int packedLight            = event.getPackedLight();
        boolean firstPerson        = mc.options.getCameraType() == CameraType.FIRST_PERSON;

        // Hand mode: first-person is handled by FloatingTurretRenderer (entity renderer + delta correction)
        if (!firstPerson) {
            renderHandTurret(player, poseStack, buffers, packedLight, partialTick, true);
            renderHandTurret(player, poseStack, buffers, packedLight, partialTick, false);
        }
        // Slot mode: always rendered here (no entity sync dependency)
        renderSlotTurrets(player, poseStack, buffers, packedLight, partialTick);
    }

    // ── Hand mode ──────────────────────────────────────────────────────────

    // Third-person only (first-person is handled by FloatingTurretRenderer via entity delta correction)
    private static void renderHandTurret(Player player, PoseStack ps,
                                          MultiBufferSource buffers, int light,
                                          float pt, boolean isMainHand) {
        ItemStack handItem = isMainHand ? player.getMainHandItem() : player.getOffhandItem();
        if (handItem.isEmpty() || !(handItem.getItem() instanceof FloatingTurretItem)) return;

        boolean isLeftHanded = player.getMainArm() == HumanoidArm.LEFT;
        double mainHandSide  = isLeftHanded ? -1.0 : 1.0;
        double side          = isMainHand ? mainHandSide : -mainHandSide;

        float interpYaw = Mth.lerp(pt, player.yRotO, player.getYRot());
        float yawRad    = interpYaw * (float) (Math.PI / 180.0);
        double rightX   = -Math.cos(yawRad);
        double rightZ   = -Math.sin(yawRad);
        double forwardX = -Math.sin(yawRad);
        double forwardZ =  Math.cos(yawRad);

        // 蓄力狀態：兩砲向前靠攏
        boolean isCharging = player.isUsingItem()
                && player.getUseItem().getItem() instanceof FloatingTurretItem;
        float chargeRatio = isCharging
                ? Mth.clamp(1.0F - player.getUseItemRemainingTicks() / (float) FloatingTurretItem.MAX_CHARGE_TICKS, 0F, 1F)
                : 0F;

        // Smooth the charge ratio so turrets glide back quickly instead of snapping
        int currentTick = player.tickCount;
        if (isCharging) {
            if (isMainHand) smoothedChargeMain = chargeRatio;
            else smoothedChargeOff = chargeRatio;
        } else {
            if (isMainHand && currentTick != lastDecayTickMain) {
                smoothedChargeMain *= 0.45F;
                if (smoothedChargeMain < 0.002F) smoothedChargeMain = 0F;
                lastDecayTickMain = currentTick;
            } else if (!isMainHand && currentTick != lastDecayTickOff) {
                smoothedChargeOff *= 0.45F;
                if (smoothedChargeOff < 0.002F) smoothedChargeOff = 0F;
                lastDecayTickOff = currentTick;
            }
        }
        float smoothed = isMainHand ? smoothedChargeMain : smoothedChargeOff;

        // 閒置浮動（蓄力時停止浮動，平滑插值回復）
        double bob = (1.0 - smoothed) * Math.sin((player.tickCount + pt) * 0.08) * 0.08;

        double sideMult = 1.0 - smoothed * 0.6;
        double fwdBoost = smoothed * 0.4;

        double offsetX = rightX * side * 1.8 * sideMult + forwardX * (1.5 + fwdBoost);
        double offsetY = player.getEyeHeight() + 0.8 + bob;
        double offsetZ = rightZ * side * 1.8 * sideMult + forwardZ * (1.5 + fwdBoost);

        ps.pushPose();
        ps.translate(offsetX, offsetY, offsetZ);

        // 充能光球：billboard 貼鏡頭，隨蓄力比例成長
        if (isCharging && chargeRatio > 0) {
            ps.pushPose();
            ps.translate(forwardX * 0.15, 0, forwardZ * 0.15); // 稍微往砲口偏
            ps.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            renderChargingOrb(ps, buffers, chargeRatio);
            ps.popPose();
        }

        ps.scale(SCALE_THIRD_PERSON, SCALE_THIRD_PERSON, SCALE_THIRD_PERSON);
        ps.mulPose(Axis.YP.rotationDegrees(-interpYaw + 270F));

        float interpPitch = Mth.lerp(pt, player.xRotO, player.getXRot());
        ps.mulPose(Axis.ZP.rotationDegrees(-interpPitch));
        ps.translate(2.8125, -0.6756, 1.0);

        renderModel(player, ps, buffers, light);
        ps.popPose();
    }

    // ── Equipment slot mode ─────────────────────────────────────────────────

    private static void renderSlotTurrets(Player player, PoseStack ps,
                                           MultiBufferSource buffers, int light, float pt) {
        if (!player.hasEffect(ModMobEffects.COMBAT_STATE)) return;

        // Discover which slots (0/1) actually have live entities on the client
        AABB searchBox = player.getBoundingBox().inflate(3.0);
        List<FloatingTurretEntity> turrets = player.level().getEntitiesOfClass(
                FloatingTurretEntity.class, searchBox,
                e -> e.getSlotIndex() < 2
                        && e.getOwnerUUID().map(id -> id.equals(player.getUUID())).orElse(false));

        if (turrets.isEmpty()) return;

        float interpYaw  = Mth.lerp(pt, player.yRotO, player.getYRot());
        float yawRad     = interpYaw * (float) (Math.PI / 180.0);
        float behindAngle = (float) Math.atan2(-Math.cos(yawRad), Math.sin(yawRad));
        float spread     = (float) (Math.PI / 5); // 36° spread
        float bobPhase   = (player.tickCount + pt) * 0.08f;

        for (FloatingTurretEntity turret : turrets) {
            int slotIdx   = turret.getSlotIndex();
            float angle   = behindAngle + (slotIdx == 0 ? -spread : spread);
            float bob     = (float) (Math.sin(bobPhase) * 0.2);

            double offsetX = Math.cos(angle) * ORBIT_RADIUS;
            double offsetY = ORBIT_HEIGHT + bob;
            double offsetZ = Math.sin(angle) * ORBIT_RADIUS;

            ps.pushPose();
            ps.translate(offsetX, offsetY, offsetZ);
            ps.scale(SCALE_SLOT, SCALE_SLOT, SCALE_SLOT);

            // Face forward + spin animation
            ps.mulPose(Axis.YP.rotationDegrees(-interpYaw + 270F));
            float spin = (player.tickCount + pt) * 2.0F;
            ps.mulPose(Axis.XP.rotationDegrees(spin));
            ps.translate(2.8125, -0.6756, 1.0);

            renderModel(player, ps, buffers, light);
            ps.popPose();
        }
    }

    // ── Shared helpers ──────────────────────────────────────────────────────

    // Package-private: also used by FloatingTurretRenderer for non-local players
    static void renderChargingOrb(PoseStack ps, MultiBufferSource buffers, float ratio) {
        Matrix4f pose = ps.last().pose();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        float s = 0.05F + ratio * 0.13F;
        // 外層光暈
        addOrbQuad(vc, pose, s * 3.0F,  30, 100, 255,  35);
        // 中層
        addOrbQuad(vc, pose, s * 1.8F,  80, 170, 255,  90);
        // 亮核心
        addOrbQuad(vc, pose, s,         200, 230, 255, 220);
        // 白芯（滿蓄才出現）
        if (ratio > 0.85F) {
            float t = (ratio - 0.85F) / 0.15F;
            addOrbQuad(vc, pose, s * 0.5F, 255, 255, 255, (int)(180 * t));
        }
    }

    private static void addOrbQuad(VertexConsumer vc, Matrix4f pose,
                                    float size, int r, int g, int b, int a) {
        vc.addVertex(pose, -size, -size, 0).setColor(r, g, b, a);
        vc.addVertex(pose,  size, -size, 0).setColor(r, g, b, a);
        vc.addVertex(pose,  size,  size, 0).setColor(r, g, b, a);
        vc.addVertex(pose, -size,  size, 0).setColor(r, g, b, a);
    }

    private static void renderModel(Player player, PoseStack ps,
                                     MultiBufferSource buffers, int light) {
        ItemStack renderStack = new ItemStack(ModItems.FLOATING_TURRET.get());
        Minecraft.getInstance().getItemRenderer().renderStatic(
                renderStack, ItemDisplayContext.NONE,
                light, OverlayTexture.NO_OVERLAY,
                ps, buffers, player.level(), player.getId());
    }

}
