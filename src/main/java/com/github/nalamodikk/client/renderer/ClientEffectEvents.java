package com.github.nalamodikk.client.renderer;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.renderer.dimension.SpacePlanetManager;
import com.github.nalamodikk.client.renderer.dimension.SpaceSkyRenderer;
import com.github.nalamodikk.common.item.DevRenderTestItem;
import com.github.nalamodikk.common.item.DevRenderTestItem2;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEffectEvents {

    public enum DevMode2 {
        FOURIER("Fourier Epicycles"),
        MAGIC_CIRCLE("Magic Circle"),
        FUSION("Fusion: Circle + Fourier + Orbital"),
        CHAOS("CHAOS: New Effects Only"),
        POLAR_ROSE("Polar Rose r=cos(k*t)"),
        TORUS_KNOT("Torus Knot (2,3)"),
        EPICYCLOID("Epicycloid"),
        MOBIUS("Mobius Strip"),
        FLOW_FIELD("Flow Field"),
        KOCH("Koch Snowflake"),
        LORENZ("Lorenz Attractor"),
        BEZIER("Bezier Curve"),
        N_BODY("N-Body (3-body)");

        public final String label;
        DevMode2(String label) { this.label = label; }
    }

    public static DevMode2 currentMode2 = DevMode2.FOURIER;

    private ClientEffectEvents() {}

    // 換世界時清除 static shader 狀態，避免舊 GL 資源殘留
    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SpaceSkyRenderer.reload();
        SpacePlanetManager.reload();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        SpaceSkyRenderer.onRenderLevel(event);
        SpacePlanetManager.onRenderLevel(event);
        ManaStrikeShaderRenderer.onRenderLevel(event);
        ShockwaveRenderer.onRenderLevel(event);
        OrbitalTestShaderRenderer.onRenderLevel(event);
        FourierCurveRenderer.onRenderLevel(event);
        MagicCircleRenderer.onRenderLevel(event);
        RippleRenderer.onRenderLevel(event);
        SpiralCurveRenderer.onRenderLevel(event);
        LissajousRenderer.onRenderLevel(event);
        FresnelSphereRenderer.onRenderLevel(event);
        PolarRoseRenderer.onRenderLevel(event);
        TorusKnotRenderer.onRenderLevel(event);
        EpicycloidRenderer.onRenderLevel(event);
        MobiusStripRenderer.onRenderLevel(event);
        FlowFieldRenderer.onRenderLevel(event);
        KochSnowflakeRenderer.onRenderLevel(event);
        LorenzAttractorRenderer.onRenderLevel(event);
        BezierCurveRenderer.onRenderLevel(event);
        NBodyRenderer.onRenderLevel(event);
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isShiftKeyDown()) return;

        if (mc.player.getMainHandItem().getItem() instanceof DevRenderTestItem) {
            event.setCanceled(true);
            int delta = event.getScrollDeltaY() > 0 ? 1 : -1;
            OrbitalTestShaderRenderer.cycleMode(delta);
            mc.player.displayClientMessage(
                Component.literal("[Dev1] " + OrbitalTestShaderRenderer.currentMode.name()
                    + " (" + (OrbitalTestShaderRenderer.currentMode.ordinal() + 1)
                    + "/" + OrbitalTestShaderRenderer.Mode.values().length + ")"),
                true
            );
            return;
        }

        if (mc.player.getMainHandItem().getItem() instanceof DevRenderTestItem2) {
            event.setCanceled(true);
            int delta = event.getScrollDeltaY() > 0 ? 1 : -1;
            DevMode2[] modes = DevMode2.values();
            currentMode2 = modes[Math.floorMod(currentMode2.ordinal() + delta, modes.length)];
            mc.player.displayClientMessage(
                Component.literal("[Dev2] " + currentMode2.label
                    + " (" + (currentMode2.ordinal() + 1) + "/" + modes.length + ")"),
                true
            );
        }
    }
}
