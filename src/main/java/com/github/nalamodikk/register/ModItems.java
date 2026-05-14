package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.NaraWatchItem;
import com.github.nalamodikk.common.item.UpgradeItem;
import com.github.nalamodikk.common.item.research.AspectTokenItem;
import com.github.nalamodikk.common.item.research.CompletedResearchItem;
import com.github.nalamodikk.common.item.research.InkQuillItem;
import com.github.nalamodikk.common.item.research.ResearchNoteItem;
import com.github.nalamodikk.common.item.debug.ManaDebugToolItem;
import com.github.nalamodikk.common.item.tool.AdvancedTechWandItem;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.item.tool.ManaPickaxeItem;
import com.github.nalamodikk.common.item.tool.ModToolTiers;
import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    private static final int UPGRADE_STACK_SIZE = 16;
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KoniavacraftMod.MOD_ID);
    /***
     * 素材類
     */
    public static final DeferredItem<Item>  MANA_DUST = ITEMS.register("mana_dust",() ->  new Item(new Item.Properties()));
    public static final DeferredItem<Item>  CORRUPTED_MANA_DUST = ITEMS.register("corrupted_mana_dust",() ->  new Item(new Item.Properties()));
    public static final DeferredItem<Item>  MANA_INGOT = ITEMS.register("mana_ingot",() ->  new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_MANA_DUST = ITEMS.register("raw_mana_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CONDENSED_MANA_DUST = ITEMS.register("condensed_mana_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_CRYSTAL_FRAGMENT = ITEMS.register("mana_crystal_fragment", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> REFINED_MANA_DUST = ITEMS.register("refined_mana_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_CRYSTAL = ITEMS.register("mana_crystal", () -> new Item(new Item.Properties()));
    /***
     * 工具
     */
    public static final DeferredItem<Item>  MANA_DEBUG_TOOL = ITEMS.register("mana_debug_tool",() ->  new ManaDebugToolItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item>  BASIC_TECH_WAND = ITEMS.register("basic_tech_wand",() ->  new BasicTechWandItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item>  ADVANCED_TECH_WAND = ITEMS.register("advanced_tech_wand", () -> new AdvancedTechWandItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ManaPickaxeItem> MANA_PICKAXE = ITEMS.register("mana_pickaxe",
            () -> new ManaPickaxeItem(ModToolTiers.MANA, new Item.Properties().stacksTo(1)));

    /**
     * 升級物品
     */
    public static final DeferredItem<Item> SPEED_UPGRADE = ITEMS.register("speed_upgrade", () -> new UpgradeItem(UpgradeType.SPEED, new Item.Properties().stacksTo(UPGRADE_STACK_SIZE)));
    public static final DeferredItem<Item> EFFICIENCY_UPGRADE = ITEMS.register("efficiency_upgrade", () -> new UpgradeItem(UpgradeType.EFFICIENCY, new Item.Properties().stacksTo(UPGRADE_STACK_SIZE)));
    
    // Mana Generator 升級物品
    public static final DeferredItem<Item> ACCELERATED_PROCESSING_UPGRADE = ITEMS.register("accelerated_processing_upgrade", () -> new UpgradeItem(UpgradeType.ACCELERATED_PROCESSING, new Item.Properties().stacksTo(UPGRADE_STACK_SIZE)));
    public static final DeferredItem<Item> EXPANDED_FUEL_CHAMBER_UPGRADE = ITEMS.register("expanded_fuel_chamber_upgrade", () -> new UpgradeItem(UpgradeType.EXPANDED_FUEL_CHAMBER, new Item.Properties().stacksTo(UPGRADE_STACK_SIZE)));
    public static final DeferredItem<Item> CATALYTIC_CONVERTER_UPGRADE = ITEMS.register("catalytic_converter_upgrade", () -> new UpgradeItem(UpgradeType.CATALYTIC_CONVERTER, new Item.Properties().stacksTo(UPGRADE_STACK_SIZE)));
    public static final DeferredItem<Item> DIAGNOSTIC_DISPLAY_UPGRADE = ITEMS.register("diagnostic_display_upgrade", () -> new UpgradeItem(UpgradeType.DIAGNOSTIC_DISPLAY, new Item.Properties().stacksTo(UPGRADE_STACK_SIZE)));


    // 研究系統
    public static final DeferredItem<ResearchNoteItem> RESEARCH_NOTE =
            ITEMS.register("research_note", () -> new ResearchNoteItem(
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<InkQuillItem> INK_QUILL =
            ITEMS.register("ink_quill", () -> new InkQuillItem(
                    new Item.Properties().stacksTo(1).durability(350)));

    public static final DeferredItem<NaraWatchItem> NARA_WATCH =
            ITEMS.register("nara_watch", () -> new NaraWatchItem(
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<CompletedResearchItem> COMPLETED_RESEARCH =
            ITEMS.register("completed_research", () -> new CompletedResearchItem(
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<AspectTokenItem> ASPECT_TOKEN =
            ITEMS.register("aspect_token", () -> new AspectTokenItem(
                    new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
