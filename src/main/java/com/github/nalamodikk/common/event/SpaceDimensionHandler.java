package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.dimension.ModDimensions;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class SpaceDimensionHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean inSpace = player.level().dimension().equals(ModDimensions.SPACE);
        if (player.isNoGravity() != inSpace) {
            player.setNoGravity(inSpace);
        }
    }

    // 進入太空維度時，傳送到地球軌道附近 (500, 64, 0)
    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.getTo().equals(ModDimensions.SPACE)) return;
        Player player = event.getEntity();
        // 只有從非太空維度進入才重設位置
        if (!event.getFrom().equals(ModDimensions.SPACE)) {
            player.teleportTo(1500.0, 64.0, 0.0);
        }
    }

    // 太空維度禁止天氣（不能下雨）
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(ModDimensions.SPACE)) return;
        if (level.isRaining() || level.isThundering()) {
            level.setWeatherParameters(6000, 0, false, false);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!event.getEntity().level().dimension().equals(ModDimensions.SPACE)) return;
        if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            event.setNewDamage(0);
        }
    }
}
