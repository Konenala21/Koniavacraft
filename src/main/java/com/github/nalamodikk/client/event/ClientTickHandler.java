package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.client.renderer.altar.AltarCameraController;
import com.github.nalamodikk.client.renderer.altar.AltarFadeRenderer;
import com.github.nalamodikk.client.renderer.altar.AltarUpgradeAnimManager;
import com.github.nalamodikk.client.renderer.altar.AltarExplosionManager;
import com.github.nalamodikk.client.renderer.altar.AltarExplosionRenderer;
import com.github.nalamodikk.client.screen.armor.UnifiedArmorUpgradeScreen;
import com.github.nalamodikk.client.screen.turret.TurretUpgradeScreen;
import com.github.nalamodikk.client.screen.wand.WandUpgradeScreen;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyHelmetItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyLeggingsItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaConcentrationHelper;
import com.github.nalamodikk.common.item.equipment.boots.ManaSprintBootsItem;
import com.github.nalamodikk.common.network.packet.server.armor.DoubleJumpPacket;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.common.event.HelmetNightVisionHandler;
import com.github.nalamodikk.common.network.packet.server.armor.ToggleNightVisionPacket;
import com.github.nalamodikk.common.network.packet.server.boots.DashPacket;
import net.minecraft.world.entity.EquipmentSlot;
import com.github.nalamodikk.register.client.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import com.github.nalamodikk.common.entity.PlayerCloneEntity;
import net.minecraft.world.InteractionHand;
import com.github.nalamodikk.client.renderer.FourierCurveRenderer;
import com.github.nalamodikk.client.renderer.FresnelSphereRenderer;
import com.github.nalamodikk.client.renderer.LissajousRenderer;
import com.github.nalamodikk.client.renderer.MagicCircleRenderer;
import com.github.nalamodikk.client.renderer.RippleRenderer;
import com.github.nalamodikk.client.renderer.SpiralCurveRenderer;
import com.github.nalamodikk.client.renderer.DamageNumberRenderer;
import com.github.nalamodikk.client.renderer.entity.FloatingTurretPlayerRenderer;
import com.github.nalamodikk.client.renderer.armor.ManaShieldEffectManager;
import com.github.nalamodikk.client.renderer.armor.ManaShieldEffectRenderer;
import com.github.nalamodikk.client.renderer.turret.TurretHitEffectManager;
import com.github.nalamodikk.client.renderer.turret.TurretHitEffectRenderer;
import com.github.nalamodikk.client.renderer.ManaStrikeShaderRenderer;
import com.github.nalamodikk.client.renderer.OrbitalTestShaderRenderer;
import com.github.nalamodikk.research.client.ClientResearchCache;
import com.github.nalamodikk.narasystem.nara.hud.NaraFirstLoginFlow;
import com.github.nalamodikk.narasystem.nara.hud.NaraTutorialFlow;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ClientTickHandler {

    private static boolean jumpWasDown = false;
    private static int airTicks = 0;
    private static boolean greyEffectApplied = false;
    private static float greySaturation = 0.12f; // 鏡中世界動態飽和度（平時低、boss 危險時回色），平滑趨近目標

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        AltarUpgradeAnimManager.clientTick();
        AltarExplosionManager.clientTick();

        Minecraft mcTick = Minecraft.getInstance();
        if (mcTick.player != null) ManaConcentrationHelper.clientTick(mcTick.player);

        updateVoidMirrorEffect(mcTick);

        if (ModKeyMappings.OPEN_UPGRADE_GUI.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null) {
                boolean opened = false;
                for (InteractionHand hand : InteractionHand.values()) {
                    if (mc.player.getItemInHand(hand).getItem() instanceof WandRodItem) {
                        mc.setScreen(new WandUpgradeScreen(hand));
                        opened = true;
                        break;
                    }
                }
                if (!opened) {
                    for (InteractionHand hand : InteractionHand.values()) {
                        if (mc.player.getItemInHand(hand).getItem() instanceof FloatingTurretItem) {
                            mc.setScreen(new TurretUpgradeScreen(hand));
                            opened = true;
                            break;
                        }
                    }
                }
                if (!opened) {
                    for (EquipmentSlot slot : new EquipmentSlot[]{
                            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                        var item = mc.player.getItemBySlot(slot).getItem();
                        if (item instanceof ManaArmorItem || item instanceof ManaSprintBootsItem) {
                            mc.setScreen(new UnifiedArmorUpgradeScreen(slot));
                            break;
                        }
                    }
                }
            }
        }

        if (ModKeyMappings.DASH.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null
                    && mc.player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof ManaSprintBootsItem
                    && !mc.player.hasEffect(com.github.nalamodikk.register.ModMobEffects.SPRINT_COOLDOWN)) {
                if (mc.player.isInWater() || mc.player.isInLava()) {
                    mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("message.koniava.boots.in_liquid"), true);
                } else {
                    DashPacket.send();
                }
            }
        }

        // 二段跳偵測
        if (mcTick.player != null && mcTick.screen == null) {
            if (mcTick.player.getAbilities().flying) {
                airTicks = 0;
            } else {
                boolean onGround = mcTick.player.onGround();
                boolean jumpDown = mcTick.options.keyJump.isDown();
                boolean justPressed = jumpDown && !jumpWasDown;
                if (onGround) airTicks = 0; else airTicks++;
                if (justPressed && airTicks >= 3
                        && mcTick.player.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof ManaAlloyLeggingsItem) {
                    DoubleJumpPacket.send();
                }
                jumpWasDown = jumpDown;
            }
        }

        if (ModKeyMappings.TOGGLE_HELMET.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null) {
                var helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
                if (helmet.getItem() instanceof ManaAlloyHelmetItem
                        && HelmetNightVisionHandler.hasNightVisionUpgrade(helmet)) {
                    ToggleNightVisionPacket.send();
                }
            }
        }

        if (ModKeyMappings.SKIP_ALTAR_ANIM.consumeClick() && AltarUpgradeAnimManager.hasAnyActive()) {
            AltarUpgradeAnimManager.skipAll();
            AltarCameraController.reset();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.setXRot(0f);
        }
    }

    private static void updateVoidMirrorEffect(Minecraft mc) {
        boolean inVoidMirror = mc.player != null
                && mc.player.level().dimension().equals(ModDimensions.VOID_MIRROR);
        if (inVoidMirror) {
            if (!greyEffectApplied || mc.gameRenderer.currentEffect() == null) {
                mc.gameRenderer.loadEffect(KoniavacraftMod.rl("shaders/post/grey.json"));
                greyEffectApplied = true;
            }
            // 動態回色：平時低飽和，boss 預兆/鏡反/變身時畫面回色當警示（平滑過渡）
            float target = computeVoidMirrorSaturation(mc);
            greySaturation += (target - greySaturation) * 0.08F;
            PostChain chain = mc.gameRenderer.currentEffect();
            if (chain != null) chain.setUniform("Saturation", greySaturation);
        } else if (greyEffectApplied) {
            mc.gameRenderer.shutdownEffect();
            greyEffectApplied = false;
            greySaturation = 0.12F;
        }
    }

    // 依附近 boss 狀態決定畫面目標飽和度：危險時刻回色當警示
    private static float computeVoidMirrorSaturation(Minecraft mc) {
        float base = 0.12F;
        if (mc.player == null || mc.level == null) return base;
        for (PlayerCloneEntity boss : mc.level.getEntitiesOfClass(PlayerCloneEntity.class,
                mc.player.getBoundingBox().inflate(48.0))) {
            if (boss.getTelegraphSkill() != 0 || boss.isReflecting()) return 0.75F; // 預兆/鏡反：強回色警示
            if (boss.isArmored()) return 0.40F;                                      // 變身期間：中等回色
        }
        return base;
    }

    @SubscribeEvent
    public static void onClientRespawn(ClientPlayerNetworkEvent.Clone event) {
        jumpWasDown = false;
        airTicks = 0;
    }

    @SubscribeEvent
    public static void onClientLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ManaConcentrationHelper.reset();
        jumpWasDown = false;
        airTicks = 0;
        if (greyEffectApplied) {
            Minecraft.getInstance().gameRenderer.shutdownEffect();
            greyEffectApplied = false;
        }
        NaraTutorialFlow.resetSessionFlags();
        NaraFirstLoginFlow.resetIgnoreCount();
        AltarUpgradeAnimManager.clear();
        AltarExplosionManager.clear();
        AltarExplosionRenderer.release();
        AltarCameraController.reset();
        ManaStrikeShaderRenderer.release();
        OrbitalTestShaderRenderer.release();
        FourierCurveRenderer.release();
        MagicCircleRenderer.release();
        RippleRenderer.release();
        SpiralCurveRenderer.release();
        LissajousRenderer.release();
        FresnelSphereRenderer.release();
        AltarFadeRenderer.release();
        ClientResearchCache.clear();
        DamageNumberRenderer.clear();
        TurretHitEffectManager.clear();
        TurretHitEffectRenderer.release();
        ManaShieldEffectManager.clear();
        ManaShieldEffectRenderer.release();
        FloatingTurretPlayerRenderer.reset();
    }
}
