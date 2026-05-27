package com.github.nalamodikk.register.client;

import com.github.nalamodikk.common.item.equipment.armor.ArmorCapacityUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.ArmorDefenseUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.HelmetUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.LeggingsUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.ChestplateUpgradeItem;
import com.github.nalamodikk.common.item.equipment.boots.BootsUpgradeItem;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeItem;
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
                ModItems.HELMET_UPGRADE_NIGHT_VISION.get()
        );

        // 護腿升級物品：依 behavior 染色
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0 && stack.getItem() instanceof LeggingsUpgradeItem upg) {
                        return upg.getBehavior().getColor();
                    }
                    return 0xFFFFFF;
                },
                ModItems.LEGGINGS_UPGRADE_MULTI_JUMP_MK0.get(),
                ModItems.LEGGINGS_UPGRADE_MULTI_JUMP_MK1.get(),
                ModItems.LEGGINGS_UPGRADE_MULTI_JUMP_MK2.get(),
                ModItems.LEGGINGS_UPGRADE_MULTI_JUMP_MK3.get()
        );

        // 通用容量升級：固定顏色（與各部位 CAPACITY 行為一致）
        event.register(
                (stack, tintIndex) -> tintIndex == 0 ? ArmorCapacityUpgradeItem.COLOR : 0xFFFFFF,
                ModItems.ARMOR_UPGRADE_CAPACITY_MK0.get(),
                ModItems.ARMOR_UPGRADE_CAPACITY_MK1.get(),
                ModItems.ARMOR_UPGRADE_CAPACITY_MK2.get(),
                ModItems.ARMOR_UPGRADE_CAPACITY_MK3.get()
        );

        // 通用防禦升級：固定顏色
        event.register(
                (stack, tintIndex) -> tintIndex == 0 ? ArmorDefenseUpgradeItem.COLOR : 0xFFFFFF,
                ModItems.ARMOR_UPGRADE_DEFENSE_MK0.get(),
                ModItems.ARMOR_UPGRADE_DEFENSE_MK1.get(),
                ModItems.ARMOR_UPGRADE_DEFENSE_MK2.get(),
                ModItems.ARMOR_UPGRADE_DEFENSE_MK3.get()
        );

        // 胸甲護盾升級物品：依 behavior 染色
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0 && stack.getItem() instanceof ChestplateUpgradeItem upg) {
                        return upg.getBehavior().getColor();
                    }
                    return 0xFFFFFF;
                },
                ModItems.CHESTPLATE_UPGRADE_SHIELD_REDUCTION_MK0.get(),
                ModItems.CHESTPLATE_UPGRADE_SHIELD_REDUCTION_MK1.get(),
                ModItems.CHESTPLATE_UPGRADE_SHIELD_REDUCTION_MK2.get(),
                ModItems.CHESTPLATE_UPGRADE_SHIELD_REDUCTION_MK3.get(),
                ModItems.CHESTPLATE_UPGRADE_SHIELD_ABSORB_MK0.get(),
                ModItems.CHESTPLATE_UPGRADE_SHIELD_ABSORB_MK1.get(),
                ModItems.CHESTPLATE_UPGRADE_SHIELD_ABSORB_MK2.get(),
                ModItems.CHESTPLATE_UPGRADE_SHIELD_ABSORB_MK3.get()
        );

        // 浮游砲升級物品：依 behavior 染色
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0 && stack.getItem() instanceof TurretUpgradeItem upg) {
                        return upg.getBehavior().getColor();
                    }
                    return 0xFFFFFF;
                },
                ModItems.TURRET_UPGRADE_CAPACITY_MK0.get(),
                ModItems.TURRET_UPGRADE_CAPACITY_MK1.get(),
                ModItems.TURRET_UPGRADE_CAPACITY_MK2.get(),
                ModItems.TURRET_UPGRADE_CAPACITY_MK3.get(),
                ModItems.TURRET_UPGRADE_HEALING_MK0.get(),
                ModItems.TURRET_UPGRADE_HEALING_MK1.get(),
                ModItems.TURRET_UPGRADE_HEALING_MK2.get(),
                ModItems.TURRET_UPGRADE_HEALING_MK3.get(),
                ModItems.TURRET_UPGRADE_HEALTH_MK0.get(),
                ModItems.TURRET_UPGRADE_HEALTH_MK1.get(),
                ModItems.TURRET_UPGRADE_HEALTH_MK2.get(),
                ModItems.TURRET_UPGRADE_DEFENSE_MK0.get(),
                ModItems.TURRET_UPGRADE_DEFENSE_MK1.get(),
                ModItems.TURRET_UPGRADE_AUTO_AIM.get(),
                ModItems.TURRET_UPGRADE_NO_BLOCK_DAMAGE.get(),
                ModItems.TURRET_UPGRADE_PLAYER_LOCK.get(),
                ModItems.TURRET_UPGRADE_PROTECT_MK0.get(),
                ModItems.TURRET_UPGRADE_PROTECT_MK1.get(),
                ModItems.TURRET_UPGRADE_SLOW.get(),
                ModItems.TURRET_UPGRADE_ROOT.get(),
                ModItems.TURRET_UPGRADE_LEVITATE.get()
        );

        // 靴子升級物品：依 behavior 染色
        event.register(
                (stack, tintIndex) -> {
                    if (tintIndex == 0 && stack.getItem() instanceof BootsUpgradeItem upg) {
                        return upg.getBehavior().getColor();
                    }
                    return 0xFFFFFF;
                },
                ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK0.get(),
                ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK1.get(),
                ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK2.get(),
                ModItems.BOOTS_UPGRADE_DASH_DISTANCE_MK3.get(),
                ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK0.get(),
                ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK1.get(),
                ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK2.get(),
                ModItems.BOOTS_UPGRADE_MANA_EFFICIENCY_MK3.get()
        );
    }
}
