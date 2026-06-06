package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.dimension.MoonChunkGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 月球核心貫穿機制：空心月球，掉到核心觸發重力翻轉 + 座標映射到「另一面」。
 *
 * 重力狀態機（per player）：
 *   NORMAL(-Y) → 掉到 Y<CORE_TRIGGER 且下墜 → TRANSIT(+Y) + XZ 傳送到 (D-x,-z)
 *   TRANSIT(+Y) → 升到 Y>SURFACE_EXIT → 轉回 NORMAL(-Y)，落在另一面地表
 *
 * 座標映射：(x,z) ↔ (D-x, -z)，D=100000，可逆，大偏移避免小座標破功。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class MoonCoreTraversalHandler {

    private static final int   COORD_OFFSET   = 100000; // D
    private static final double CORE_TRIGGER  = -38.0;  // 掉到此高度觸發翻轉+傳送
    private static final double FLIP_ACCEL    = 0.08;    // TRANSIT 每 tick 向上加速度
    private static final double MOON_GRAVITY  = 0.16;    // 月球地表重力係數（地球=1）

    // 處於 TRANSIT（+Y 重力上升中）的玩家
    private static final Map<UUID, Boolean> inTransit = new HashMap<>();

    /** 該玩家是否在核心過渡中（相機翻轉用）。單人時 client/server 共用此靜態 map。 */
    public static boolean isInTransit(UUID id) {
        return inTransit.getOrDefault(id, false);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean onMoon = player.level().dimension().equals(ModDimensions.MOON);
        UUID id = player.getUUID();
        boolean serverSide = !player.level().isClientSide;

        if (!onMoon) {
            inTransit.remove(id);
            return;
        }

        boolean transit = inTransit.getOrDefault(id, false);
        Vec3 v = player.getDeltaMovement();

        if (transit) {
            // TRANSIT：往上加速（+Y），免 fall damage（兩端都套，client 才有體感）
            player.setDeltaMovement(v.x, v.y + FLIP_ACCEL, v.z);
            player.fallDistance = 0;
            player.resetFallDistance();
            // 升到中空頂（地殼底）→ 結束過渡，直接彈到地表正常站立（不用挖整片地殼）
            if (player.getY() >= MoonChunkGenerator.CRUST_BOTTOM - 2) {
                inTransit.put(id, false);
                if (serverSide && player.level() instanceof ServerLevel sl) {
                    int sx = Mth.floor(player.getX());
                    int sz = Mth.floor(player.getZ());
                    int surfaceY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, sx, sz);
                    player.teleportTo(player.getX(), surfaceY + 1.0, player.getZ());
                }
                player.setDeltaMovement(0, 0, 0); // 停下，正常（低）重力接管
            }
        } else {
            // NORMAL：月球低重力（飄飄感）——兩端都套，本地玩家 client 端才感覺得到
            // 跳躍上升段也減速 → 跳更高、滯空久、慢慢落；站地面不補避免被頂
            // 飛行（creative/spectator）時不補，否則會一直往上飄
            if (!player.onGround() && !player.getAbilities().flying && Math.abs(v.y) > 1e-4) {
                player.setDeltaMovement(v.x, v.y + 0.08 * (1.0 - MOON_GRAVITY), v.z);
            }
            // 核心傳送：只在 server 端觸發（避免 client 重複傳送/desync）
            if (serverSide && player.getY() < CORE_TRIGGER && v.y < -0.05) {
                triggerCorePassage(player);
            }
        }
    }

    private static void triggerCorePassage(Player player) {
        double nx = COORD_OFFSET - player.getX();
        double nz = -player.getZ();
        // 傳到另一面對應的中空深處（同 Y，在空氣裡）→ +Y 重力把玩家帶上去 → 彈出地表
        player.teleportTo(nx, player.getY(), nz);
        // 進入 TRANSIT：重力翻轉成 +Y
        inTransit.put(player.getUUID(), true);
        // 給向上初速，開始上升
        player.setDeltaMovement(player.getDeltaMovement().x, 0.3, player.getDeltaMovement().z);
        player.fallDistance = 0;
        player.resetFallDistance();
    }

    // TRANSIT 中完全免 fall damage
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.level().dimension().equals(ModDimensions.MOON)) return;
        // 月球低重力：摔落傷害大幅降低
        event.setDistance(event.getDistance() * (float) MOON_GRAVITY);
    }
}
