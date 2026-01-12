package com.github.nalamodikk.particle.manager;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.style.ParticleGroupStyle;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 粒子樣式管理器
 * 管理所有活動的粒子組樣式
 */
public class ParticleStyleManager {
    private static final ParticleStyleManager INSTANCE = new ParticleStyleManager();

    /** 存儲所有活動的樣式 */
    private final Map<UUID, ParticleGroupStyle> activeStyles = new ConcurrentHashMap<>();

    /** 每個世界的樣式列表 */
    private final Map<Level, Set<UUID>> worldStyles = new ConcurrentHashMap<>();

    private ParticleStyleManager() {}

    public static ParticleStyleManager getInstance() {
        return INSTANCE;
    }

    /**
     * 在世界中生成粒子樣式
     * @param world 世界
     * @param pos 位置
     * @param style 樣式
     */
    public static void spawnStyle(ServerLevel world, Vec3 pos, ParticleGroupStyle style) {
        getInstance().spawn(world, pos, style);
    }

    /**
     * 生成粒子樣式
     * @param world 世界
     * @param pos 位置
     * @param style 樣式
     */
    public void spawn(Level world, Vec3 pos, ParticleGroupStyle style) {
        if (activeStyles.containsKey(style.getUuid())) {
            KoniavacraftMod.LOGGER.warn("Style {} already active, skipping spawn", style.getUuid());
            return;
        }

        style.display(world, pos);
        activeStyles.put(style.getUuid(), style);

        worldStyles.computeIfAbsent(world, k -> ConcurrentHashMap.newKeySet()).add(style.getUuid());

        KoniavacraftMod.LOGGER.debug("Spawned particle style {} at {}", style.getUuid(), pos);

        // TODO: 發送網絡包到可見玩家
        if (world instanceof ServerLevel serverLevel) {
            List<UUID> visiblePlayers = filterVisiblePlayer(style);
            KoniavacraftMod.LOGGER.debug("Found {} visible players for style {}",
                    visiblePlayers.size(), style.getUuid());
            // Send packet to visible players
        }
    }

    /**
     * 移除粒子樣式
     * @param uuid 樣式 UUID
     */
    public void remove(UUID uuid) {
        ParticleGroupStyle style = activeStyles.remove(uuid);
        if (style != null) {
            style.remove();

            Level world = style.getWorld();
            if (world != null) {
                Set<UUID> styles = worldStyles.get(world);
                if (styles != null) {
                    styles.remove(uuid);
                }
            }

            KoniavacraftMod.LOGGER.debug("Removed particle style {}", uuid);
        }
    }

    /**
     * 獲取粒子樣式
     * @param uuid 樣式 UUID
     * @return 樣式，如果不存在則返回 null
     */
    public ParticleGroupStyle getStyle(UUID uuid) {
        return activeStyles.get(uuid);
    }

    /**
     * 檢查樣式是否活動
     * @param uuid 樣式 UUID
     * @return 是否活動
     */
    public boolean isActive(UUID uuid) {
        return activeStyles.containsKey(uuid);
    }

    /**
     * Tick 所有活動的樣式
     * @param world 世界
     */
    public void tick(Level world) {
        Set<UUID> styles = worldStyles.get(world);
        if (styles == null || styles.isEmpty()) {
            return;
        }

        List<UUID> toRemove = new ArrayList<>();

        for (UUID uuid : styles) {
            ParticleGroupStyle style = activeStyles.get(uuid);
            if (style == null || !style.isValid()) {
                toRemove.add(uuid);
                continue;
            }

            try {
                style.tick();
            } catch (Exception e) {
                KoniavacraftMod.LOGGER.error("Error ticking particle style {}", uuid, e);
                toRemove.add(uuid);
            }
        }

        // 移除無效的樣式
        for (UUID uuid : toRemove) {
            remove(uuid);
        }
    }

    /**
     * 篩選可以看到樣式的玩家
     * @param style 樣式
     * @return 玩家 UUID 列表
     */
    public static List<UUID> filterVisiblePlayer(ParticleGroupStyle style) {
        return getInstance().getVisiblePlayers(style);
    }

    /**
     * 獲取可見玩家列表
     * @param style 樣式
     * @return 玩家 UUID 列表
     */
    public List<UUID> getVisiblePlayers(ParticleGroupStyle style) {
        Level world = style.getWorld();
        if (!(world instanceof ServerLevel serverLevel)) {
            return Collections.emptyList();
        }

        Vec3 pos = style.getPos();
        double visibleRange = style.getVisibleRange();
        double visibleRangeSq = visibleRange * visibleRange;

        List<UUID> visiblePlayers = new ArrayList<>();

        for (ServerPlayer player : serverLevel.players()) {
            Vec3 playerPos = player.position();
            double distanceSq = playerPos.distanceToSqr(pos);

            if (distanceSq <= visibleRangeSq) {
                visiblePlayers.add(player.getUUID());
            }
        }

        return visiblePlayers;
    }

    /**
     * 清除世界中的所有樣式
     * @param world 世界
     */
    public void clearWorld(Level world) {
        Set<UUID> styles = worldStyles.remove(world);
        if (styles != null) {
            for (UUID uuid : styles) {
                ParticleGroupStyle style = activeStyles.remove(uuid);
                if (style != null) {
                    style.remove();
                }
            }
        }
        KoniavacraftMod.LOGGER.debug("Cleared all styles from world");
    }

    /**
     * 清除所有樣式
     */
    public void clearAll() {
        for (ParticleGroupStyle style : activeStyles.values()) {
            style.remove();
        }
        activeStyles.clear();
        worldStyles.clear();
        KoniavacraftMod.LOGGER.info("Cleared all particle styles");
    }

    /**
     * 獲取統計信息
     */
    public String getStats() {
        return String.format("Active styles: %d, Worlds: %d",
                activeStyles.size(), worldStyles.size());
    }
}
