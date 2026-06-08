package com.github.nalamodikk.common.datagen;


import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, KoniavacraftMod.MOD_ID, helper);
    }

    private static final String[] CORE_NAMES = {
            "formation_core", "activation_core", "io_core", "rotation_core", "ritual_core",
            "structure_build_core", "spell_core", "blank_core"
    };

    private static final String[] UPGRADE_NAMES = {
            "wand_upgrade_capacity_mk0",  "wand_upgrade_capacity_mk1",  "wand_upgrade_capacity_mk2",  "wand_upgrade_capacity_mk3",
            "wand_upgrade_efficiency_mk0","wand_upgrade_efficiency_mk1","wand_upgrade_efficiency_mk2","wand_upgrade_efficiency_mk3",
            "wand_upgrade_range_mk0",     "wand_upgrade_range_mk1",     "wand_upgrade_range_mk2",     "wand_upgrade_range_mk3",
            "wand_upgrade_cooldown_mk0",  "wand_upgrade_cooldown_mk1",  "wand_upgrade_cooldown_mk2",  "wand_upgrade_cooldown_mk3",
            "boots_upgrade_dash_distance_mk0", "boots_upgrade_dash_distance_mk1", "boots_upgrade_dash_distance_mk2", "boots_upgrade_dash_distance_mk3",
            "boots_upgrade_mana_efficiency_mk0","boots_upgrade_mana_efficiency_mk1","boots_upgrade_mana_efficiency_mk2","boots_upgrade_mana_efficiency_mk3",
            "helmet_upgrade_night_vision",
            "leggings_upgrade_multi_jump_mk0",  "leggings_upgrade_multi_jump_mk1",  "leggings_upgrade_multi_jump_mk2",  "leggings_upgrade_multi_jump_mk3",
            "armor_upgrade_capacity_mk0",       "armor_upgrade_capacity_mk1",       "armor_upgrade_capacity_mk2",       "armor_upgrade_capacity_mk3",
            "armor_upgrade_defense_mk0",        "armor_upgrade_defense_mk1",        "armor_upgrade_defense_mk2",        "armor_upgrade_defense_mk3",
            "turret_upgrade_capacity_mk0",      "turret_upgrade_capacity_mk1",      "turret_upgrade_capacity_mk2",      "turret_upgrade_capacity_mk3",
            "turret_upgrade_healing_mk0",       "turret_upgrade_healing_mk1",       "turret_upgrade_healing_mk2",       "turret_upgrade_healing_mk3",
            "turret_upgrade_health_mk0",        "turret_upgrade_health_mk1",        "turret_upgrade_health_mk2",
            "turret_upgrade_defense_mk0",       "turret_upgrade_defense_mk1",
            "turret_upgrade_auto_aim",          "turret_upgrade_no_block_damage",   "turret_upgrade_player_lock",
            "turret_upgrade_protect_mk0",       "turret_upgrade_protect_mk1",
            "turret_upgrade_slow",              "turret_upgrade_root",              "turret_upgrade_levitate",
            "chestplate_upgrade_shield_reduction_mk0", "chestplate_upgrade_shield_reduction_mk1",
            "chestplate_upgrade_shield_reduction_mk2", "chestplate_upgrade_shield_reduction_mk3",
            "chestplate_upgrade_shield_absorb_mk0",    "chestplate_upgrade_shield_absorb_mk1",
            "chestplate_upgrade_shield_absorb_mk2",    "chestplate_upgrade_shield_absorb_mk3"
    };

    @Override
    protected void registerModels() {
        // 核心插件：parent 指向自訂 wand_core 模型
        for (String name : CORE_NAMES) {
            withExistingParent(name, modLoc("item/wand_core"));
        }

        // 升級物品：parent 指向 wand_upgrade 模型
        for (String name : UPGRADE_NAMES) {
            withExistingParent(name, modLoc("item/wand_upgrade"));
        }
        // 研究筆記：用蕎麥麵的自訂貼圖（item/generated + layer0）
        withExistingParent(ModItems.RESEARCH_NOTE.getId().getPath(),
                ResourceLocation.parse("item/generated"))
                .texture("layer0", modLoc("item/research_note"));
        // 訓練假人放置物：使用自訂貼圖
        withExistingParent(ModItems.TRAINING_DUMMY.getId().getPath(),
                ResourceLocation.parse("item/generated"))
                .texture("layer0", modLoc("item/training_dummy"));
        // 完成研究卷軸：使用自訂貼圖
        withExistingParent(ModItems.COMPLETED_RESEARCH.getId().getPath(),
                ResourceLocation.parse("item/generated"))
                .texture("layer0", modLoc("item/completed_research"));
        // 魔力水晶：使用自訂貼圖
        withExistingParent(ModItems.MANA_CRYSTAL.getId().getPath(),
                ResourceLocation.parse("item/generated"))
                .texture("layer0", modLoc("item/mana_crystal"));
        // 研究台：BlockItem 但需要手動指定（createManaModel 不生成物品模型）
        withExistingParent("research_table", modLoc("block/research_table"));
        // 魔力壓板機
        withExistingParent("mana_plate_press", modLoc("block/mana_plate_press"));

        // 魔力發電機輸出升級
        for (int mk = 0; mk <= 3; mk++) {
            withExistingParent("mana_output_upgrade_mk" + mk, ResourceLocation.parse("item/generated"))
                    .texture("layer0", modLoc("item/mana_output_upgrade_mk" + mk));
        }

        // 能量輸出升級
        for (int mk = 0; mk <= 3; mk++) {
            withExistingParent("energy_output_upgrade_mk" + mk, ResourceLocation.parse("item/generated"))
                    .texture("layer0", modLoc("item/energy_output_upgrade_mk" + mk));
        }

        ModItems.ITEMS.getEntries().forEach(item -> {
            Item instance = item.get();
            String name = item.getId().getPath();

            // ❌ 跳過 BlockItem（例如 mana_block）
            if (instance instanceof BlockItem) {
                return;
            }

            // ❌ 跳過已在上方明確處理的物品
            if (name.equals("research_note") || name.equals("nara_watch")
                    || name.equals("completed_research") || name.equals("mana_crystal")
                    || name.equals("basic_upgrade_casing")) {
                return;
            }

            // ❌ 跳過核心插件與升級物品（已用 wand_core parent 處理）
            for (String coreName : CORE_NAMES) { if (name.equals(coreName)) return; }
            for (String upgName : UPGRADE_NAMES) { if (name.equals(upgName)) return; }
            if (name.startsWith("mana_output_upgrade_mk")) return;
            if (name.startsWith("energy_output_upgrade_mk")) return;

            // ❌ 若對應貼圖不存在，也跳過（避免崩潰）
            ResourceLocation texture = modLoc("item/" + name);
            if (!existingFileHelper.exists(texture, TEXTURE)) {
                LOGGER.warn("Skipping item model for '{}': missing texture", name);
                return;
            }

            // ✅ 自動判斷工具或普通物品
            if (instance instanceof TieredItem || instance instanceof SwordItem) {
                handheldItem(instance);
            } else {
                basicItem(instance);
            }
        });
    }

}
