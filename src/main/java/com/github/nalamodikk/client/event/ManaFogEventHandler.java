package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.ManaFogTracker;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ManaFogEventHandler {

    private static final double FOG_MAX_DIST_SQ = 48.0 * 48.0;
    private static final double FOG_MIN_DIST_SQ = 32.0 * 32.0;
    private static final int SCAN_INTERVAL = 40;
    private static int scanTimer = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (++scanTimer < SCAN_INTERVAL) return;
        scanTimer = 0;
        ManaFogTracker.setNearestFog(findNearestFogBlock(mc.level, mc.player));
    }

    @Nullable
    private static BlockPos findNearestFogBlock(Level level, Player player) {
        BlockPos origin = player.blockPosition();
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = -48; x <= 48; x += 2) {
            for (int z = -48; z <= 48; z += 2) {
                for (int y = -8; y <= 8; y++) {
                    BlockPos check = origin.offset(x, y, z);
                    if (level.getBlockState(check).is(ModBlocks.MANA_FOG_BLOCK.get())) {
                        double dist = player.distanceToSqr(
                                check.getX() + 0.5, check.getY() + 0.5, check.getZ() + 0.5);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = check;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        BlockPos fogPos = ManaFogTracker.getNearestFog();
        if (fogPos == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double distSq = mc.player.distanceToSqr(
                fogPos.getX() + 0.5, fogPos.getY() + 0.5, fogPos.getZ() + 0.5);
        if (distSq > FOG_MAX_DIST_SQ || distSq < FOG_MIN_DIST_SQ) return;

        float t = (float) ((FOG_MAX_DIST_SQ - distSq) / (FOG_MAX_DIST_SQ - FOG_MIN_DIST_SQ));
        event.setFarPlaneDistance(Mth.lerp(t, event.getFarPlaneDistance(), 16f));
        event.setNearPlaneDistance(Mth.lerp(t, event.getNearPlaneDistance(), 2f));
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        BlockPos fogPos = ManaFogTracker.getNearestFog();
        if (fogPos == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double distSq = mc.player.distanceToSqr(
                fogPos.getX() + 0.5, fogPos.getY() + 0.5, fogPos.getZ() + 0.5);
        if (distSq > FOG_MAX_DIST_SQ || distSq < FOG_MIN_DIST_SQ) return;

        float t = (float) ((FOG_MAX_DIST_SQ - distSq) / (FOG_MAX_DIST_SQ - FOG_MIN_DIST_SQ));
        event.setRed(Mth.lerp(t, event.getRed(), 0.07f));
        event.setGreen(Mth.lerp(t, event.getGreen(), 0.02f));
        event.setBlue(Mth.lerp(t, event.getBlue(), 0.15f));
    }
}
