package com.github.nalamodikk.common.item.equipment.boots;

import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.common.item.equipment.armor.ArmorCapacityUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.ArmorDefenseUpgradeItem;
import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.register.ModMobEffects;
import com.github.nalamodikk.register.client.ModKeyMappings;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ManaSprintBootsItem extends ManaArmorItem {

    public static final int BASE_ARMOR = 2;
    public static final int BASE_MAX_MANA = 6000;
    public static final int BASE_DASH_COST = 10;
    public static final int BASE_DASH_COOLDOWN = 20;
    public static final int MIN_DASH_COST = 5;
    public static final double BASE_DASH_DISTANCE = 3.0;
    public static final int MAX_UPGRADE_SLOTS = 5;

    public ManaSprintBootsItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.BOOTS, BASE_MAX_MANA, BASE_ARMOR, MAX_UPGRADE_SLOTS, properties);
    }

    // ── ManaArmorItem abstract impl ──────────────────────────────────────────

    @Override
    public void recalculateMaxMana(ItemStack boots) {
        int max = BASE_MAX_MANA;
        for (ItemStack upg : getData(boots).upgrades().values()) {
            if (upg.getItem() instanceof ArmorCapacityUpgradeItem acu) {
                max += acu.getBonus();
            }
        }
        boots.set(ModDataComponents.MAX_MANA, max);
        int stored = getMana(boots);
        if (stored > max) boots.set(ModDataComponents.MANA_STORED, max);
    }

    @Override
    public boolean isValidUpgradeItem(ItemStack stack) {
        return stack.getItem() instanceof BootsUpgradeItem
                || stack.getItem() instanceof ArmorCapacityUpgradeItem
                || stack.getItem() instanceof ArmorDefenseUpgradeItem;
    }

    @Override
    public String getUpgradeBehaviorKey(ItemStack upgradeStack) {
        if (upgradeStack.getItem() instanceof ArmorCapacityUpgradeItem) return "CAPACITY";
        if (upgradeStack.getItem() instanceof ArmorDefenseUpgradeItem) return "DEFENSE";
        if (upgradeStack.getItem() instanceof BootsUpgradeItem bu) return "boots_" + bu.getBehavior().name();
        return "";
    }

    @Override
    protected int getArmorBonus(ItemStack stack) {
        int bonus = 0;
        for (ItemStack upg : getData(stack).upgrades().values()) {
            if (upg.getItem() instanceof ArmorDefenseUpgradeItem adu) {
                bonus += adu.getBonus();
            }
        }
        return bonus;
    }

    // ── Dash stat helpers ────────────────────────────────────────────────────

    public static int getDashCost(ItemStack stack) {
        int reduction = 0;
        for (ItemStack upg : getData(stack).upgrades().values()) {
            if (upg.getItem() instanceof BootsUpgradeItem bu && bu.getBehavior() == BootsUpgradeBehavior.MANA_EFFICIENCY) {
                reduction += bu.getBehavior().getBonusForMk(bu.getMk());
            }
        }
        return Math.max(MIN_DASH_COST, BASE_DASH_COST - reduction);
    }

    public static double getDashDistance(ItemStack stack) {
        double bonus = 0;
        for (ItemStack upg : getData(stack).upgrades().values()) {
            if (upg.getItem() instanceof BootsUpgradeItem bu && bu.getBehavior() == BootsUpgradeBehavior.DASH_DISTANCE) {
                bonus += bu.getBehavior().getBonusForMk(bu.getMk());
            }
        }
        return (BASE_DASH_DISTANCE + bonus) / 5.0;
    }

    // ── Server-side dash ─────────────────────────────────────────────────────

    public static boolean performDash(ServerPlayer player, ItemStack boots) {
        if (player.level().dimension().equals(ModDimensions.VOID_MIRROR)) {
            player.displayClientMessage(
                    Component.translatable("message.koniava.void_mirror.ability_disabled"), true);
            return false;
        }
        if (player.hasEffect(ModMobEffects.SPRINT_COOLDOWN)) return false;
        if (player.hasEffect(ModMobEffects.ROOT)) return false;
        if (player.isInWater() || player.isInLava()) {
            player.displayClientMessage(
                    Component.translatable("message.koniava.boots.in_liquid"), true
            );
            return false;
        }

        int cost = getDashCost(boots);
        if (getMana(boots) < cost) {
            player.displayClientMessage(
                    Component.translatable("message.koniava.boots.not_enough_mana"), true
            );
            return false;
        }

        double dist = getDashDistance(boots);
        var look = player.getLookAngle().normalize().scale(dist);
        double vy = Math.max(player.getDeltaMovement().y, 0.3);
        player.setDeltaMovement(look.x, vy, look.z);
        player.hurtMarked = true;

        setMana(boots, getMana(boots) - cost);
        player.addEffect(new MobEffectInstance(
                ModMobEffects.SPRINT_COOLDOWN,
                BASE_DASH_COOLDOWN, 0, false, true, true
        ));
        return true;
    }

    // ── Tooltip ──────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.koniava.mana_sprint_boots.mana",
                getMana(stack), getMaxMana(stack)));
        int count = (int) getData(stack).upgrades().values().stream().filter(s -> !s.isEmpty()).count();
        if (count > 0) {
            lines.add(Component.translatable("tooltip.koniava.mana_sprint_boots.upgrades",
                    count, MAX_UPGRADE_SLOTS));
        }
        lines.add(Component.translatable("tooltip.koniava.mana_sprint_boots.open_upgrade",
                ModKeyMappings.OPEN_UPGRADE_GUI.getTranslatedKeyMessage()));
    }
}
