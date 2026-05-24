package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.NaraWatchItem;
import com.github.nalamodikk.common.item.UpgradeItem;
import com.github.nalamodikk.common.item.research.AspectTokenItem;
import com.github.nalamodikk.common.item.research.CompletedResearchItem;
import com.github.nalamodikk.common.item.research.InkQuillItem;
import com.github.nalamodikk.common.item.research.ResearchNoteItem;
import com.github.nalamodikk.common.item.ConsensusGlassesItem;
import com.github.nalamodikk.common.item.DevRenderTestItem;
import com.github.nalamodikk.common.item.DevRenderTestItem2;
import com.github.nalamodikk.common.item.DevRenderTestItem3;
import com.github.nalamodikk.common.item.DevRenderTestItem4;
import com.github.nalamodikk.common.item.SourceTomeItem;
import com.github.nalamodikk.common.item.debug.ManaDebugToolItem;
import com.github.nalamodikk.common.item.tool.AdvancedTechWandItem;
import com.github.nalamodikk.common.item.tool.BasicTechWandItem;
import com.github.nalamodikk.common.item.tool.StructureBuildWandItem;
import com.github.nalamodikk.common.item.tool.ManaPickaxeItem;
import com.github.nalamodikk.common.item.tool.ModToolTiers;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.common.item.wand.core.WandCoreBehavior;
import com.github.nalamodikk.common.item.wand.core.WandCoreItem;
import com.github.nalamodikk.common.item.wand.upgrade.WandUpgradeBehavior;
import com.github.nalamodikk.common.item.wand.upgrade.WandUpgradeItem;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
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

    // ── T1 電路板中間材料 ─────────────────────────────────────────────────────
    public static final DeferredItem<Item> MANA_SUBSTRATE    = ITEMS.register("mana_substrate",    () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_WIRE         = ITEMS.register("mana_wire",         () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_ADHESIVE     = ITEMS.register("mana_adhesive",     () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_WAFER        = ITEMS.register("mana_wafer",        () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BASIC_MANA_CIRCUIT = ITEMS.register("basic_mana_circuit", () -> new Item(new Item.Properties()));

    // ── 浮游砲中間材料 ─────────────────────────────────────────────────────
    public static final DeferredItem<Item> MANA_BARREL =
            ITEMS.register("mana_barrel", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PRECISION_MANA_CIRCUIT =
            ITEMS.register("precision_mana_circuit", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HIGH_DENSITY_MANA_CORE =
            ITEMS.register("high_density_mana_core", () -> new Item(new Item.Properties()));

    /***
     * 武器
     */
    public static final DeferredItem<FloatingTurretItem> FLOATING_TURRET =
            ITEMS.register("floating_turret", () -> new FloatingTurretItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .durability(500)
                            .component(ModDataComponents.MANA_STORED, 0)
                            .component(ModDataComponents.MAX_MANA,  FloatingTurretItem.DEFAULT_MAX_MANA)));

    /***
     * 工具
     */
    public static final DeferredItem<Item>  MANA_DEBUG_TOOL = ITEMS.register("mana_debug_tool",() ->  new ManaDebugToolItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item>  BASIC_TECH_WAND = ITEMS.register("basic_tech_wand",() ->  new BasicTechWandItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item>  RITUAL_WAND = ITEMS.register("ritual_wand", () -> new com.github.nalamodikk.common.item.tool.RitualWandItem(new Item.Properties().stacksTo(1).durability(256)));
    public static final DeferredItem<Item>  ADVANCED_TECH_WAND = ITEMS.register("advanced_tech_wand", () -> new AdvancedTechWandItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item>  STRUCTURE_BUILD_WAND = ITEMS.register("structure_build_wand", () -> new StructureBuildWandItem(new Item.Properties().stacksTo(1)));

    // ── 模組化魔杖系統 ────────────────────────────────────────────────────────
    public static final DeferredItem<Item> BLANK_CORE =
            ITEMS.registerSimpleItem("blank_core", new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> BASIC_UPGRADE_CASING =
            ITEMS.registerSimpleItem("basic_upgrade_casing", new Item.Properties().stacksTo(16));

    public static final DeferredItem<WandRodItem> WAND_ROD =
            ITEMS.register("wand_rod", () -> new WandRodItem(1, 4, new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.MANA_STORED, 0)
                    .component(ModDataComponents.MAX_MANA, 8000)));
    public static final DeferredItem<WandRodItem> WAND_ROD_ADVANCED =
            ITEMS.register("wand_rod_advanced", () -> new WandRodItem(3, 6, new Item.Properties().stacksTo(1)
                    .component(ModDataComponents.MANA_STORED, 0)
                    .component(ModDataComponents.MAX_MANA, 8000)));

    public static final DeferredItem<WandCoreItem> FORMATION_CORE =
            ITEMS.register("formation_core", () -> new WandCoreItem(WandCoreBehavior.FORMATION, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<WandCoreItem> ACTIVATION_CORE =
            ITEMS.register("activation_core", () -> new WandCoreItem(WandCoreBehavior.ACTIVATION, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<WandCoreItem> IO_CORE =
            ITEMS.register("io_core", () -> new WandCoreItem(WandCoreBehavior.IO, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<WandCoreItem> ROTATION_CORE =
            ITEMS.register("rotation_core", () -> new WandCoreItem(WandCoreBehavior.ROTATION, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<WandCoreItem> RITUAL_CORE =
            ITEMS.register("ritual_core", () -> new WandCoreItem(WandCoreBehavior.RITUAL, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<WandCoreItem> STRUCTURE_BUILD_CORE =
            ITEMS.register("structure_build_core", () -> new WandCoreItem(WandCoreBehavior.STRUCTURE_BUILD, new Item.Properties().stacksTo(1)));

    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_CAPACITY_MK0 =
            ITEMS.register("wand_upgrade_capacity_mk0", () -> new WandUpgradeItem(WandUpgradeBehavior.CAPACITY, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_CAPACITY_MK1 =
            ITEMS.register("wand_upgrade_capacity_mk1", () -> new WandUpgradeItem(WandUpgradeBehavior.CAPACITY, 1, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_CAPACITY_MK2 =
            ITEMS.register("wand_upgrade_capacity_mk2", () -> new WandUpgradeItem(WandUpgradeBehavior.CAPACITY, 2, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_CAPACITY_MK3 =
            ITEMS.register("wand_upgrade_capacity_mk3", () -> new WandUpgradeItem(WandUpgradeBehavior.CAPACITY, 3, new Item.Properties().stacksTo(4)));

    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_EFFICIENCY_MK0 =
            ITEMS.register("wand_upgrade_efficiency_mk0", () -> new WandUpgradeItem(WandUpgradeBehavior.EFFICIENCY, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_EFFICIENCY_MK1 =
            ITEMS.register("wand_upgrade_efficiency_mk1", () -> new WandUpgradeItem(WandUpgradeBehavior.EFFICIENCY, 1, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_EFFICIENCY_MK2 =
            ITEMS.register("wand_upgrade_efficiency_mk2", () -> new WandUpgradeItem(WandUpgradeBehavior.EFFICIENCY, 2, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_EFFICIENCY_MK3 =
            ITEMS.register("wand_upgrade_efficiency_mk3", () -> new WandUpgradeItem(WandUpgradeBehavior.EFFICIENCY, 3, new Item.Properties().stacksTo(4)));

    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_RANGE_MK0 =
            ITEMS.register("wand_upgrade_range_mk0", () -> new WandUpgradeItem(WandUpgradeBehavior.RANGE, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_RANGE_MK1 =
            ITEMS.register("wand_upgrade_range_mk1", () -> new WandUpgradeItem(WandUpgradeBehavior.RANGE, 1, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_RANGE_MK2 =
            ITEMS.register("wand_upgrade_range_mk2", () -> new WandUpgradeItem(WandUpgradeBehavior.RANGE, 2, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_RANGE_MK3 =
            ITEMS.register("wand_upgrade_range_mk3", () -> new WandUpgradeItem(WandUpgradeBehavior.RANGE, 3, new Item.Properties().stacksTo(4)));

    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_COOLDOWN_MK0 =
            ITEMS.register("wand_upgrade_cooldown_mk0", () -> new WandUpgradeItem(WandUpgradeBehavior.COOLDOWN, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_COOLDOWN_MK1 =
            ITEMS.register("wand_upgrade_cooldown_mk1", () -> new WandUpgradeItem(WandUpgradeBehavior.COOLDOWN, 1, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_COOLDOWN_MK2 =
            ITEMS.register("wand_upgrade_cooldown_mk2", () -> new WandUpgradeItem(WandUpgradeBehavior.COOLDOWN, 2, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<WandUpgradeItem> WAND_UPGRADE_COOLDOWN_MK3 =
            ITEMS.register("wand_upgrade_cooldown_mk3", () -> new WandUpgradeItem(WandUpgradeBehavior.COOLDOWN, 3, new Item.Properties().stacksTo(4)));
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
    public static final DeferredItem<UpgradeItem> MANA_OUTPUT_UPGRADE_MK0 = ITEMS.register("mana_output_upgrade_mk0", () -> new UpgradeItem(UpgradeType.MANA_OUTPUT, 0, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<UpgradeItem> MANA_OUTPUT_UPGRADE_MK1 = ITEMS.register("mana_output_upgrade_mk1", () -> new UpgradeItem(UpgradeType.MANA_OUTPUT, 1, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<UpgradeItem> MANA_OUTPUT_UPGRADE_MK2 = ITEMS.register("mana_output_upgrade_mk2", () -> new UpgradeItem(UpgradeType.MANA_OUTPUT, 2, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<UpgradeItem> MANA_OUTPUT_UPGRADE_MK3 = ITEMS.register("mana_output_upgrade_mk3", () -> new UpgradeItem(UpgradeType.MANA_OUTPUT, 3, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<UpgradeItem> ENERGY_OUTPUT_UPGRADE_MK0 = ITEMS.register("energy_output_upgrade_mk0", () -> new UpgradeItem(UpgradeType.ENERGY_OUTPUT, 0, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<UpgradeItem> ENERGY_OUTPUT_UPGRADE_MK1 = ITEMS.register("energy_output_upgrade_mk1", () -> new UpgradeItem(UpgradeType.ENERGY_OUTPUT, 1, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<UpgradeItem> ENERGY_OUTPUT_UPGRADE_MK2 = ITEMS.register("energy_output_upgrade_mk2", () -> new UpgradeItem(UpgradeType.ENERGY_OUTPUT, 2, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<UpgradeItem> ENERGY_OUTPUT_UPGRADE_MK3 = ITEMS.register("energy_output_upgrade_mk3", () -> new UpgradeItem(UpgradeType.ENERGY_OUTPUT, 3, new Item.Properties().stacksTo(1)));


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

    // 特殊物品
    public static final DeferredItem<ConsensusGlassesItem> CONSENSUS_GLASSES =
            ITEMS.register("consensus_glasses", () -> new ConsensusGlassesItem(
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<SourceTomeItem> SOURCE_TOME =
            ITEMS.register("source_tome", () -> new SourceTomeItem(
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<DevRenderTestItem> DEV_RENDER_TEST_1 =
            ITEMS.register("dev_render_test_1", () -> new DevRenderTestItem(
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<DevRenderTestItem2> DEV_RENDER_TEST_2 =
            ITEMS.register("dev_render_test_2", () -> new DevRenderTestItem2(
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<DevRenderTestItem3> DEV_RENDER_TEST_3 =
            ITEMS.register("dev_render_test_3", () -> new DevRenderTestItem3(
                    new Item.Properties().stacksTo(1)));

    public static final DeferredItem<DevRenderTestItem4> DEV_RENDER_TEST_4 =
            ITEMS.register("dev_render_test_4", () -> new DevRenderTestItem4(
                    new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
