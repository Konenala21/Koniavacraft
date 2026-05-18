package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.DevRenderTestItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEffectEvents {

    private ClientEffectEvents() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        ManaStrikeShaderRenderer.onRenderLevel(event);
        OrbitalTestShaderRenderer.onRenderLevel(event);
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isShiftKeyDown()) return;
        if (!(mc.player.getMainHandItem().getItem() instanceof DevRenderTestItem)) return;

        event.setCanceled(true);
        int delta = event.getScrollDeltaY() > 0 ? 1 : -1;
        OrbitalTestShaderRenderer.cycleMode(delta);
        mc.player.displayClientMessage(
            Component.literal("[DevTest] " + OrbitalTestShaderRenderer.currentMode.name()
                + " (" + (OrbitalTestShaderRenderer.currentMode.ordinal() + 1)
                + "/" + OrbitalTestShaderRenderer.Mode.values().length + ")"),
            true
        );
    }
}
