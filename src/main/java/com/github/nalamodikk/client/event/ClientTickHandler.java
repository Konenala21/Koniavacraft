package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.renderer.altar.AltarCameraController;
import com.github.nalamodikk.client.renderer.altar.AltarFadeRenderer;
import com.github.nalamodikk.client.renderer.altar.AltarUpgradeAnimManager;
import com.github.nalamodikk.client.renderer.altar.AltarExplosionManager;
import com.github.nalamodikk.client.renderer.altar.AltarExplosionRenderer;
import com.github.nalamodikk.register.client.ModKeyMappings;
import net.minecraft.client.Minecraft;
import com.github.nalamodikk.client.renderer.FourierCurveRenderer;
import com.github.nalamodikk.client.renderer.FresnelSphereRenderer;
import com.github.nalamodikk.client.renderer.LissajousRenderer;
import com.github.nalamodikk.client.renderer.MagicCircleRenderer;
import com.github.nalamodikk.client.renderer.RippleRenderer;
import com.github.nalamodikk.client.renderer.SpiralCurveRenderer;
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

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        AltarUpgradeAnimManager.clientTick();
        AltarExplosionManager.clientTick();

        if (ModKeyMappings.SKIP_ALTAR_ANIM.consumeClick() && AltarUpgradeAnimManager.hasAnyActive()) {
            AltarUpgradeAnimManager.skipAll();
            AltarCameraController.reset();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) mc.player.setXRot(0f);
        }
    }

    @SubscribeEvent
    public static void onClientLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
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
    }
}
