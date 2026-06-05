package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.dimension.ModDimensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
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

    // 進入太空維度時，傳送到地球軌道附近 (1500, 64, 0)
    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.getTo().equals(ModDimensions.SPACE)) return;
        Player player = event.getEntity();
        if (!event.getFrom().equals(ModDimensions.SPACE)) {
            player.teleportTo(1500.0, 64.0, 0.0);
        }
    }

    // 太空 + 月球維度禁止天氣（月球無大氣不該下雪/雨）
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        boolean space = level.dimension().equals(ModDimensions.SPACE);
        boolean moon  = level.dimension().equals(ModDimensions.MOON);
        if (!space && !moon) return;
        if (level.isRaining() || level.isThundering()) {
            level.setWeatherParameters(6000, 0, false, false);
        }
    }

    // 太空維度禁止自然生怪（敵對 + 生物，保持空曠）
    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.PositionCheck event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(ModDimensions.SPACE)) return;
        MobCategory cat = event.getEntity().getType().getCategory();
        // 只擋自然生成的怪物/生物，不擋玩家召喚或其他來源
        if (cat == MobCategory.MONSTER || cat == MobCategory.CREATURE
                || cat == MobCategory.AMBIENT || cat == MobCategory.WATER_CREATURE
                || cat == MobCategory.WATER_AMBIENT) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    // 虛空傷害由 SpaceVoidDamageMixin 在 LivingEntity.hurt() 層取消（含動畫）
}
