package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.FloatingTurretEntity;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import com.github.nalamodikk.register.ModDataAttachments;
import com.github.nalamodikk.register.ModEntities;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class FloatingTurretEventHandler {

    private static final int SYNC_INTERVAL = 20;
    private static final int TURRET_SLOT_START = 8;
    private static final int TURRET_SLOT_COUNT = 2;
    public static final int HAND_MAIN_SLOT  = 2;
    public static final int HAND_OFF_SLOT   = 3;
    public static final int MAX_TURRETS_PER_PLAYER = TURRET_SLOT_COUNT + 2;

    // Registry: ownerUUID → (slotIndex → entity); avoids spatial BBox search
    private static final Map<UUID, Map<Integer, FloatingTurretEntity>> TURRET_REGISTRY = new HashMap<>();

    public static void registerTurret(UUID ownerUUID, int slotIndex, FloatingTurretEntity entity) {
        TURRET_REGISTRY.computeIfAbsent(ownerUUID, k -> new HashMap<>()).put(slotIndex, entity);
    }

    @Nullable
    public static FloatingTurretEntity getTurretEntity(UUID ownerUUID, int slotIndex) {
        Map<Integer, FloatingTurretEntity> slots = TURRET_REGISTRY.get(ownerUUID);
        if (slots == null) return null;
        FloatingTurretEntity entity = slots.get(slotIndex);
        if (entity != null && entity.isAlive() && !entity.isRemoved()) return entity;
        if (entity != null) unregisterTurret(ownerUUID, slotIndex);
        return null;
    }

    public static void unregisterTurret(UUID ownerUUID, int slotIndex) {
        Map<Integer, FloatingTurretEntity> slots = TURRET_REGISTRY.get(ownerUUID);
        if (slots == null) return;
        slots.remove(slotIndex);
        if (slots.isEmpty()) TURRET_REGISTRY.remove(ownerUUID);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % SYNC_INTERVAL != 0) return;

        ServerLevel level = player.serverLevel();

        // 旁觀者模式：清除所有浮游砲 entity，不生成新的
        if (player.isSpectator()) {
            for (int i = 0; i < TURRET_SLOT_COUNT; i++) {
                FloatingTurretEntity e = findTurretEntity(level, player, i);
                if (e != null) e.discard();
            }
            FloatingTurretEntity m = findTurretEntity(level, player, HAND_MAIN_SLOT);
            if (m != null) m.discard();
            FloatingTurretEntity o = findTurretEntity(level, player, HAND_OFF_SLOT);
            if (o != null) o.discard();
            return;
        }

        // ── 裝備槽模式（slot 0, 1）──
        NonNullList<ItemStack> equipment = player.getData(ModDataAttachments.EXTRA_EQUIPMENT.get());
        for (int i = 0; i < TURRET_SLOT_COUNT; i++) {
            int dataIdx = TURRET_SLOT_START + i;
            if (dataIdx >= equipment.size()) break;

            ItemStack stack = equipment.get(dataIdx);
            FloatingTurretEntity existing = findTurretEntity(level, player, i);

            if (!stack.isEmpty() && stack.getItem() instanceof FloatingTurretItem) {
                if (existing == null) spawnTurretEntity(level, player, i);
            } else {
                if (existing != null) existing.discard();
            }
        }

        // ── 手持模式（slot 2 = 主手, 3 = 副手）──
        syncHandEntity(level, player, player.getMainHandItem(), HAND_MAIN_SLOT);
        syncHandEntity(level, player, player.getOffhandItem(), HAND_OFF_SLOT);
    }

    private static void syncHandEntity(ServerLevel level, ServerPlayer player,
                                       ItemStack handItem, int slotIndex) {
        FloatingTurretEntity existing = findTurretEntity(level, player, slotIndex);
        if (!handItem.isEmpty() && handItem.getItem() instanceof FloatingTurretItem) {
            if (existing == null) spawnTurretEntity(level, player, slotIndex);
        } else {
            if (existing != null) existing.discard();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        TURRET_REGISTRY.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.setData(ModDataAttachments.LAST_COMBAT_TIME.get(), player.level().getGameTime());
    }

    // 玩家攻擊實體：更新戰鬥時間
    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        player.setData(ModDataAttachments.LAST_COMBAT_TIME.get(), player.level().getGameTime());
    }

    @Nullable
    private static FloatingTurretEntity findTurretEntity(ServerLevel level, Player player, int slotIndex) {
        Map<Integer, FloatingTurretEntity> slots = TURRET_REGISTRY.get(player.getUUID());
        if (slots == null) return null;
        FloatingTurretEntity entity = slots.get(slotIndex);
        if (entity != null && entity.isAlive() && !entity.isRemoved()) return entity;
        if (entity != null) unregisterTurret(player.getUUID(), slotIndex);
        return null;
    }

    private static void spawnTurretEntity(ServerLevel level, ServerPlayer player, int slotIndex) {
        Map<Integer, FloatingTurretEntity> slots = TURRET_REGISTRY.get(player.getUUID());
        int currentCount = slots != null
                ? (int) slots.values().stream().filter(e -> e.isAlive() && !e.isRemoved()).count()
                : 0;
        if (currentCount >= MAX_TURRETS_PER_PLAYER) return;

        FloatingTurretEntity entity = ModEntities.FLOATING_TURRET.get().create(level);
        if (entity == null) return;

        entity.setOwnerUUID(player.getUUID());
        entity.setSlotIndex(slotIndex);

        float phase = slotIndex == 0 ? 0.0F : (float) Math.PI;
        entity.setPos(
                player.getX() + Math.cos(phase) * 1.5,
                player.getY() + 1.0,
                player.getZ() + Math.sin(phase) * 1.5
        );

        level.addFreshEntity(entity);
        registerTurret(player.getUUID(), slotIndex, entity);
    }
}
