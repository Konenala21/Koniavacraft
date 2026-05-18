package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.renderer.altar.AltarCameraController;
import com.github.nalamodikk.client.renderer.altar.AltarUpgradeAnimManager;
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
    }

    @SubscribeEvent
    public static void onClientLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
        NaraTutorialFlow.resetSessionFlags();
        AltarUpgradeAnimManager.clear();
        AltarCameraController.reset();
    }
}
