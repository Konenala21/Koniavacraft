package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.dimension.ModDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 月球低重力手感：實心月球，沒有空心/核心傳送（試過另一面傳送，體驗差，已砍）。
 * 只保留月球該有的低重力飄飄感 + 摔落減傷。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class MoonCoreTraversalHandler {

    private static final double MOON_GRAVITY = 0.16; // 月球地表重力係數（地球=1）

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().dimension().equals(ModDimensions.MOON)) return;

        Vec3 v = player.getDeltaMovement();
        // 月球低重力（飄飄感）——兩端都套，本地玩家 client 端才感覺得到
        // 跳躍上升段也減速 → 跳更高、滯空久、慢慢落；站地面不補避免被頂
        // 飛行（creative/spectator）時不補，否則會一直往上飄
        if (!player.onGround() && !player.getAbilities().flying && Math.abs(v.y) > 1e-4) {
            player.setDeltaMovement(v.x, v.y + 0.08 * (1.0 - MOON_GRAVITY), v.z);
        }
    }

    // 月球低重力：摔落傷害大幅降低
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.level().dimension().equals(ModDimensions.MOON)) return;
        event.setDistance(event.getDistance() * (float) MOON_GRAVITY);
    }
}
