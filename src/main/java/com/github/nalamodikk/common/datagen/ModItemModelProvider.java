package com.github.nalamodikk.common.datagen;


import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, KoniavacraftMod.MOD_ID, helper);
    }

    private static final String[] CORE_NAMES = {
            "formation_core", "activation_core", "io_core", "rotation_core", "ritual_core",
            "structure_build_core", "blank_core"
    };

    private static final String[] UPGRADE_NAMES = {
            "wand_upgrade_capacity_mk0",  "wand_upgrade_capacity_mk1",  "wand_upgrade_capacity_mk2",  "wand_upgrade_capacity_mk3",
            "wand_upgrade_efficiency_mk0","wand_upgrade_efficiency_mk1","wand_upgrade_efficiency_mk2","wand_upgrade_efficiency_mk3",
            "wand_upgrade_range_mk0",     "wand_upgrade_range_mk1",     "wand_upgrade_range_mk2",     "wand_upgrade_range_mk3",
            "wand_upgrade_cooldown_mk0",  "wand_upgrade_cooldown_mk1",  "wand_upgrade_cooldown_mk2",  "wand_upgrade_cooldown_mk3"
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
        // 研究系統物品：借用 vanilla 貼圖
        withExistingParent(ModItems.RESEARCH_NOTE.getId().getPath(),
                ResourceLocation.withDefaultNamespace("item/paper"));
        // 娜拉全息手錶：貼圖未完成前借用 vanilla compass
        withExistingParent(ModItems.NARA_WATCH.getId().getPath(),
                ResourceLocation.withDefaultNamespace("item/compass"));
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



    private ItemModelBuilder saplingItem(DeferredBlock<Block> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID,"block/" + item.getId().getPath()));
    }

    private ItemModelBuilder handheldItem(DeferredItem<?> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID,"item/" + item.getId().getPath()));
    }
}
