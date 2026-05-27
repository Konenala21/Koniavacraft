package com.github.nalamodikk.common.item.equipment.boots;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.armor.ArmorCapacityUpgradeItem;
import com.github.nalamodikk.common.item.upgrade.EquipmentUpgradeData;
import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.register.ModMobEffects;
import com.github.nalamodikk.register.client.ModKeyMappings;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

public class ManaSprintBootsItem extends ArmorItem {

    public static final int BASE_ARMOR = 2;
    public static final int BASE_MAX_MANA = 6000;
    public static final int BASE_DASH_COST = 10;
    public static final int BASE_DASH_COOLDOWN = 20;
    public static final int MIN_DASH_COST = 5;
    public static final double BASE_DASH_DISTANCE = 3.0;
    public static final int MAX_UPGRADE_SLOTS = 5;

    public ManaSprintBootsItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.BOOTS, properties.stacksTo(1));
    }

    // ── Mana helpers ─────────────────────────────────────────────────────────

    public static int getMana(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
    }

    public static void setMana(ItemStack stack, int mana) {
        stack.set(ModDataComponents.MANA_STORED, Mth.clamp(mana, 0, getMaxMana(stack)));
    }

    public static int getMaxMana(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MAX_MANA, BASE_MAX_MANA);
    }

    public static void recalculateMaxMana(ItemStack boots) {
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

    // ── Upgrade data helpers ─────────────────────────────────────────────────

    public static EquipmentUpgradeData getData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.EQUIPMENT_UPGRADE_DATA, EquipmentUpgradeData.empty());
    }

    public static void setData(ItemStack stack, EquipmentUpgradeData data) {
        stack.set(ModDataComponents.EQUIPMENT_UPGRADE_DATA, data);
    }

    public int getMaxUpgradeSlots() { return MAX_UPGRADE_SLOTS; }

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
        // BASE_DASH_DISTANCE and bonuses are in blocks; divide by 5 to convert to velocity units
        return (BASE_DASH_DISTANCE + bonus) / 5.0;
    }

    // ── Server-side dash ─────────────────────────────────────────────────────

    public static boolean performDash(ServerPlayer player, ItemStack boots) {
        if (player.hasEffect(ModMobEffects.SPRINT_COOLDOWN)) return false;
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

    // ── Dynamic armor attribute ──────────────────────────────────────────────

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        int armorValue = BASE_ARMOR;
        for (ItemStack upg : getData(stack).upgrades().values()) {
            if (upg.getItem() instanceof BootsUpgradeItem bu && bu.getBehavior() == BootsUpgradeBehavior.ARMOR) {
                armorValue += bu.getBehavior().getBonusForMk(bu.getMk());
            }
        }
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ARMOR,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_boots_armor"),
                                armorValue,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.FEET
                )
                .build();
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
