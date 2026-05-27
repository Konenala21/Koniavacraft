package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.common.item.equipment.armor.LeggingsUpgradeBehavior;
import com.github.nalamodikk.common.item.equipment.armor.LeggingsUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyLeggingsItem;
import com.github.nalamodikk.dimension.ModDimensions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class LeggingsDoubleJumpHandler {

    public static final int BASE_JUMP_MANA_COST = 300;
    private static final double BASE_JUMP_VELOCITY = 0.40;
    private static final int COOLDOWN_TICKS = 30; // 1.5s

    private static final Map<UUID, Integer> extraJumpsUsed = new HashMap<>();
    private static final Map<UUID, Long> cooldownEndTick = new HashMap<>();

    public static void handleDoubleJump(ServerPlayer player) {
        if (player.getAbilities().flying || player.isSpectator()) return;
        if (player.level().dimension().equals(ModDimensions.VOID_MIRROR)) {
            player.displayClientMessage(
                    Component.translatable("message.koniava.void_mirror.ability_disabled"), true);
            return;
        }

        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!(leggings.getItem() instanceof ManaAlloyLeggingsItem)) return;

        UUID id = player.getUUID();
        long currentTick = player.level().getGameTime();
        if (cooldownEndTick.getOrDefault(id, 0L) > currentTick) return;

        int maxExtra = ManaAlloyLeggingsItem.getMaxExtraJumps(leggings);
        int used = extraJumpsUsed.getOrDefault(id, 0);
        if (used >= maxExtra) return;

        LeggingsUpgradeItem multiJumpUpgrade = findMultiJumpUpgrade(leggings);
        if (multiJumpUpgrade != null) {
            performUpgradeJump(player, leggings, multiJumpUpgrade, id, used);
        } else {
            performBaseJump(player, leggings, id, used);
        }
    }

    private static void performUpgradeJump(ServerPlayer player, ItemStack leggings,
                                            LeggingsUpgradeItem upgrade, UUID id, int used) {
        LeggingsUpgradeBehavior behavior = upgrade.getBehavior();
        int mk = upgrade.getMk();
        int manaCost = behavior.getJumpManaCost(mk);
        int mana = ManaArmorItem.getMana(leggings);

        if (mana < manaCost) {
            player.displayClientMessage(
                    Component.translatable("message.koniava.leggings.not_enough_mana"), true);
            return;
        }

        double velocity = behavior.getJumpVelocity(mk);
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, velocity, motion.z);
        player.hurtMarked = true;

        ManaArmorItem.setMana(leggings, mana - manaCost);
        extraJumpsUsed.put(id, used + 1);
    }

    private static void performBaseJump(ServerPlayer player, ItemStack leggings, UUID id, int used) {
        int mana = ManaArmorItem.getMana(leggings);
        if (mana < BASE_JUMP_MANA_COST) {
            player.displayClientMessage(
                    Component.translatable("message.koniava.leggings.not_enough_mana"), true);
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, BASE_JUMP_VELOCITY, motion.z);
        player.hurtMarked = true;

        ManaArmorItem.setMana(leggings, mana - BASE_JUMP_MANA_COST);
        extraJumpsUsed.put(id, used + 1);
    }

    public static LeggingsUpgradeItem findMultiJumpUpgrade(ItemStack leggings) {
        for (ItemStack upg : ManaAlloyLeggingsItem.getData(leggings).upgrades().values()) {
            if (upg.getItem() instanceof LeggingsUpgradeItem lu
                    && lu.getBehavior() == LeggingsUpgradeBehavior.MULTI_JUMP) {
                return lu;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();

        if (player.onGround() || player.isInWater() || player.isInLava()) {
            int used = extraJumpsUsed.getOrDefault(id, 0);
            if (used > 0) {
                ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
                if (leggings.getItem() instanceof ManaAlloyLeggingsItem
                        && findMultiJumpUpgrade(leggings) != null) {
                    cooldownEndTick.put(id, player.level().getGameTime() + COOLDOWN_TICKS);
                }
            }
            extraJumpsUsed.remove(id);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!(leggings.getItem() instanceof ManaAlloyLeggingsItem)) return;
        if (findMultiJumpUpgrade(leggings) != null) {
            event.setDamageMultiplier(0f);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        extraJumpsUsed.remove(id);
        cooldownEndTick.remove(id);
    }
}
