package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.space.orbit.PlanetDef;
import com.github.nalamodikk.space.orbit.StarSystem;
import com.github.nalamodikk.space.orbit.StarSystemRegistry;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3f;
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

        // 太空維度旁觀者模式：飛行加速（真實尺度太大，原速跑不動）
        var abilities = player.getAbilities();
        float want = (inSpace && player.isSpectator()) ? 0.8f : 0.05f;
        if (Math.abs(abilities.getFlyingSpeed() - want) > 1e-4f) {
            abilities.setFlyingSpeed(want);
            if (!player.level().isClientSide) player.onUpdateAbilities();
        }
    }

    // 進入太空維度時，傳送到地球軌道附近 (1500, 64, 0)
    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.getTo().equals(ModDimensions.SPACE)) return;
        if (event.getFrom().equals(ModDimensions.SPACE)) return;
        Player player = event.getEntity();
        long tick = player.level().getGameTime();

        // 依來源維度找對應星球，從那顆星球旁邊出現
        for (StarSystem sys : StarSystemRegistry.getActive()) {
            for (PlanetDef p : sys.planets()) {
                if (p.dimension().equals(event.getFrom())) {
                    Vector3f pos = planetWorldPos(sys, p, tick);
                    // 偏移到星球外側（不卡進星球），近一點更有「剛離開」的震撼感
                    float off = p.physicalRadius() * 1.6f + 25f;
                    player.teleportTo(pos.x + off, pos.y, pos.z);
                    return;
                }
            }
        }
        // 找不到對應星球（如從地獄來）→ 預設地球軌道
        player.teleportTo(3000.0, 64.0, 0.0);
    }

    /** 算星球當前世界位置（含繞父行星的衛星鏈）。 */
    private static Vector3f planetWorldPos(StarSystem sys, PlanetDef planet, long tick) {
        Vector3f starPos = sys.worldPos();
        if (planet.parentId().isEmpty()) {
            return planet.worldPositionAt(tick, starPos);
        }
        PlanetDef parent = sys.planets().stream()
                .filter(pp -> pp.id().equals(planet.parentId())).findFirst().orElse(null);
        Vector3f parentPos = parent != null ? parent.worldPositionAt(tick, starPos) : starPos;
        return planet.worldPositionAt(tick, parentPos);
    }

    // 太空 + 月球維度禁止天氣（月球無大氣不該下雪/雨）
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        boolean space = level.dimension().equals(ModDimensions.SPACE);
        boolean moon  = level.dimension().equals(ModDimensions.MOON);
        if (!space && !moon) return;
        // 鎖死晴天：連 rainLevel/thunderLevel 的淡入淡出也抓(isRaining 為 false 但 level 還在 fade 時，
        // 移動快會在區塊邊界閃下雨)。一有任何天氣強度就重設長晴天時間，從根本不讓它下。
        if (level.isRaining() || level.isThundering()
                || level.getRainLevel(1.0f) > 0.0f || level.getThunderLevel(1.0f) > 0.0f) {
            level.setWeatherParameters(1_000_000, 0, false, false);
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
