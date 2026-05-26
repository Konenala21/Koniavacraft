package com.github.nalamodikk.register.client;

import com.github.nalamodikk.common.item.equipment.armor.ChestplateUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.HelmetUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.LeggingsUpgradeItem;
import com.github.nalamodikk.common.item.equipment.boots.BootsUpgradeItem;
import com.github.nalamodikk.common.item.wand.core.WandCoreItem;
import com.github.nalamodikk.common.item.wand.upgrade.WandUpgradeItem;
import com.github.nalamodikk.register.ModItems;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public class ModColorHandlers {

    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // 核心插件：依 behavior 染色
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0 && stack.getItem() instanceof WandCoreItem core) {
                        return core.getBehavior().getColor();
                    }
                    return 0xFFFFFF;
                },
                ModItems.FORMATION_CORE.get(),
                ModItems.ACTIVATION_CORE.get(),
                ModItems.IO_CORE.get(),
                ModItems.ROTATION_CORE.get(),
                ModItems.RITUAL_CORE.get()
        );

        // 升級物品：依 behavior 染色（Mk0-Mk3 全部）
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0 && stack.getItem() instanceof WandUpgradeItem upg) {
                        return upg.getBehavior().getColor();
                    }
                    return 0xFFFFFF;
                },
                ModItems.WAND_UPGRADE_CAPACITY_MK0.get(),
                ModItems.WAND_UPGRADE_CAPACITY_MK1.get(),
                ModItems.WAND_UPGRADE_CAPACITY_MK2.get(),
                ModItems.WAND_UPGRADE_CAPACITY_MK3.get(),
                ModItems.WAND_UPGRADE_EFFICIENCY_MK0.get(),
                ModItems.WAND_UPGRADE_EFFICIENCY_MK1.get(),
                ModItems.WAND_UPGRADE_EFFICIENCY_MK2.get(),
                ModItems.WAND_UPGRADE_EFFICIENCY_MK3.get(),
                ModItems.WAND_UPGRADE_RANGE_MK0.get(),
                ModItems.WAND_UPGRADE_RANGE_MK1.get(),
                ModItems.WAND_UPGRADE_RANGE_MK2.get(),
                ModItems.WAND_UPGRADE_RANGE_MK3.get(),
                ModItems.WAND_UPGRADE_COOLDOWN_MK0.get(),
                ModItems.WAND_UPGRADE_COOLDOWN_MK1.get(),
                ModItems.WAND_UPGRADE_COOLDOWN_MK2.get(),
                ModItems.WAND_UPGRADE_COOLDOWN_MK3.get()
        );

        // 頭盔升級物品：依 behavior 染色
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0 && stack.getItem() instanceof HelmetUpgradeItem upg) {
                        return upg.getBehavior().getColor();
                    }
                    return 0xFFFFFF;
                },
                ModItems.HELMET_UPGRADE_CAPACITY_MK0.get(),
                ModItems.HELMET_UPGRADE_CAPACITY_MK1.get(),
                ModItems.HELMET_UPGRADE_CAPACITY_MK2.get(),
                ModItems.HELMET_UPGRADE_CAPACITY_MK3.get(),
                ModItems.HELMET_UPGRADE_ARMOR_MK0.get(),
                ModItems.HELMET_UPGRADE_ARMOR_MK1.get(),
                ModItems.HELMET_UPGRADE_ARMOR_MK2.get(),
                ModItems.HELMET_UPGRADE_ARMOR_MK3.get()
        );

        // 胸甲升級物品：依 behavior 染色
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0 && stack.getItem() instanceof ChestplateUpgradeItem upg) {
                        return upg.getBehavior().getColor();
                    }
                    return 0xFFFFFF;
                },
                ModItems.CHESTPLATE_UPGRADE_CAPACITY_MK0.get(),
                ModItems.CHESTPLATE_UPGRADE_CAPACITY_MK1.get(),
                ModItems.CHESTPLATE_UPGRADE_CAPACITY_MK2.get(),
                ModItems.CHESTPLATE_UPGRADE_CAPACITY_MK3.get(),
                ModItems.CHESTPLATE_UPGRADE_ARMOR_MK0.get(),
                ModItems.CHESTPLATE_UPGRADE_ARMOR_MK1.get(),
                ModItems.CHESTPLATE_UPGRADE_ARMOR_MK2.get(),
                ModItems.CHESTPLATE_UPGRADE_ARMOR_MK3.get()
        );

        // 護腿升級物品：依 behavior 染色
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0 && stack.getItem() instanceof LeggingsUpgradeItem upg) {
                        return upg.getBehavior().getColor();
                    }
                    return 0xFFFFFF;
                },
                ModItems.LEGGINGS_UPGRADE_CAPACITY_MK0.get(),
                ModItems.LEGGINGS_UPGRADE_CAPACITY_MK1.get(),
                ModItems.LEGGINGS_UPGRADE_CAPACITY_MK2.get(),
                ModItems.LEGGINGS_UPGRADE_CAPACITY_MK3.get(),
                ModItems.LEGGINGS_UPGRADE_ARMOR_MK0.get(),
                ModItems.LEGGINGS_UPGRADE_ARMOR_MK1.get(),
                ModItems.LEGGINGS_UPGRADE_ARMOR_MK2.get(),
                ModItems.LEGGINGS_UPGRADE_ARMOR_MK3.get()
        );

        // 靴子升級物品：依 behavior 染色
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0 && stack.getItem() instanceof BootsUpgradeItem upg) {
                        return upg.getBehavior().getColor();
                    }
                    return 0xFFFFFF;
                },
                ModItems.BOOTS_UPGRADE_ARMOR_MK0.get(),
                ModItems.BOOTS_UPGRADE_ARMOR_MK1.get(),
                ModItems.BOOTS_UPGRADE_ARMOR_MK2.get(),
                ModItems.BOOTS_UPGRADE_ARMOR_MK3.get(),
                ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK0.get(),
                ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK1.get(),
                ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK2.get(),
                ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK3.get(),
                ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK0.get(),
                ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK1.get(),
                ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK2.get(),
                ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK3.get(),
                ModItems.BOOTS_UPGRADE_CAPACITY_MK0.get(),
                ModItems.BOOTS_UPGRADE_CAPACITY_MK1.get(),
                ModItems.BOOTS_UPGRADE_CAPACITY_MK2.get(),
                ModItems.BOOTS_UPGRADE_CAPACITY_MK3.get()
        );
    }
}
