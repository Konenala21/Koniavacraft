package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.NaraWatchItem;
import com.github.nalamodikk.common.item.TrainingDummyItem;
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
import com.github.nalamodikk.common.item.equipment.boots.BootsUpgradeBehavior;
import com.github.nalamodikk.common.item.equipment.boots.BootsUpgradeItem;
import com.github.nalamodikk.common.item.equipment.boots.ManaSprintBootsItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyHelmetItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyChestplateItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyLeggingsItem;
import com.github.nalamodikk.common.item.equipment.armor.HelmetUpgradeBehavior;
import com.github.nalamodikk.common.item.equipment.armor.HelmetUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.ChestplateUpgradeBehavior;
import com.github.nalamodikk.common.item.equipment.armor.ChestplateUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.ArmorCapacityUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.ArmorDefenseUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.LeggingsUpgradeBehavior;
import com.github.nalamodikk.common.item.equipment.armor.LeggingsUpgradeItem;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeBehavior;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeItem;
import com.github.nalamodikk.common.utils.upgrade.UpgradeType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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
    // 鏡中世界 boss 擊敗紀念物（混入獎勵寶箱，純收藏 + 未來內容預留鑰匙）
    public static final DeferredItem<Item>  MIRROR_CORE_SHARD = ITEMS.register("mirror_core_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    // 訓練假人放置物：右鍵生成一隻測傷害用的假人
    public static final DeferredItem<Item> TRAINING_DUMMY = ITEMS.register("training_dummy",
            () -> new TrainingDummyItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> RAW_MANA_DUST = ITEMS.register("raw_mana_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CONDENSED_MANA_DUST = ITEMS.register("condensed_mana_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_CRYSTAL_FRAGMENT = ITEMS.register("mana_crystal_fragment", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> REFINED_MANA_DUST = ITEMS.register("refined_mana_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_CRYSTAL = ITEMS.register("mana_crystal", () -> new Item(new Item.Properties()));

    // ── 感知系中間材料 ────────────────────────────────────────────────────────
    public static final DeferredItem<Item> MANA_EYE =
            ITEMS.register("mana_eye", () -> new Item(new Item.Properties()));

    // ── 魔力合金套裝底殼（激活前中間品）────────────────────────────────────────
    public static final DeferredItem<Item> MANA_ALLOY_HELMET_BASE =
            ITEMS.register("mana_alloy_helmet_base", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_ALLOY_CHESTPLATE_BASE =
            ITEMS.register("mana_alloy_chestplate_base", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_ALLOY_LEGGINGS_BASE =
            ITEMS.register("mana_alloy_leggings_base", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_SPRINT_BOOTS_BASE =
            ITEMS.registerSimpleItem("mana_sprint_boots_base", new Item.Properties().stacksTo(1));


    // ── 魔力合金套裝中間材料 ─────────────────────────────────────────────────────
    public static final DeferredItem<Item> MANA_IRON =
            ITEMS.register("mana_iron", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_CRYSTAL_ALLOY_DUST =
            ITEMS.register("mana_crystal_alloy_dust", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_ALLOY_INGOT =
            ITEMS.register("mana_alloy_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MANA_REINFORCED_PLATE =
            ITEMS.register("mana_reinforced_plate", () -> new Item(new Item.Properties()));

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

    // 浮游砲升級插件（USB 形狀）
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_CAPACITY_MK0 =
            ITEMS.register("turret_upgrade_capacity_mk0", () -> new TurretUpgradeItem(TurretUpgradeBehavior.CAPACITY, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_CAPACITY_MK1 =
            ITEMS.register("turret_upgrade_capacity_mk1", () -> new TurretUpgradeItem(TurretUpgradeBehavior.CAPACITY, 1, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_CAPACITY_MK2 =
            ITEMS.register("turret_upgrade_capacity_mk2", () -> new TurretUpgradeItem(TurretUpgradeBehavior.CAPACITY, 2, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_CAPACITY_MK3 =
            ITEMS.register("turret_upgrade_capacity_mk3", () -> new TurretUpgradeItem(TurretUpgradeBehavior.CAPACITY, 3, new Item.Properties().stacksTo(4)));

    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_HEALING_MK0 =
            ITEMS.register("turret_upgrade_healing_mk0", () -> new TurretUpgradeItem(TurretUpgradeBehavior.HEALING, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_HEALING_MK1 =
            ITEMS.register("turret_upgrade_healing_mk1", () -> new TurretUpgradeItem(TurretUpgradeBehavior.HEALING, 1, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_HEALING_MK2 =
            ITEMS.register("turret_upgrade_healing_mk2", () -> new TurretUpgradeItem(TurretUpgradeBehavior.HEALING, 2, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_HEALING_MK3 =
            ITEMS.register("turret_upgrade_healing_mk3", () -> new TurretUpgradeItem(TurretUpgradeBehavior.HEALING, 3, new Item.Properties().stacksTo(4)));

    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_HEALTH_MK0 =
            ITEMS.register("turret_upgrade_health_mk0", () -> new TurretUpgradeItem(TurretUpgradeBehavior.HEALTH, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_HEALTH_MK1 =
            ITEMS.register("turret_upgrade_health_mk1", () -> new TurretUpgradeItem(TurretUpgradeBehavior.HEALTH, 1, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_HEALTH_MK2 =
            ITEMS.register("turret_upgrade_health_mk2", () -> new TurretUpgradeItem(TurretUpgradeBehavior.HEALTH, 2, new Item.Properties().stacksTo(4)));

    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_DEFENSE_MK0 =
            ITEMS.register("turret_upgrade_defense_mk0", () -> new TurretUpgradeItem(TurretUpgradeBehavior.DEFENSE, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_DEFENSE_MK1 =
            ITEMS.register("turret_upgrade_defense_mk1", () -> new TurretUpgradeItem(TurretUpgradeBehavior.DEFENSE, 1, new Item.Properties().stacksTo(4)));

    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_AUTO_AIM =
            ITEMS.register("turret_upgrade_auto_aim", () -> new TurretUpgradeItem(TurretUpgradeBehavior.AUTO_AIM, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_NO_BLOCK_DAMAGE =
            ITEMS.register("turret_upgrade_no_block_damage", () -> new TurretUpgradeItem(TurretUpgradeBehavior.NO_BLOCK_DAMAGE, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_PLAYER_LOCK =
            ITEMS.register("turret_upgrade_player_lock", () -> new TurretUpgradeItem(TurretUpgradeBehavior.PLAYER_LOCK, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_PROTECT_MK0 =
            ITEMS.register("turret_upgrade_protect_mk0", () -> new TurretUpgradeItem(TurretUpgradeBehavior.PROTECT, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_PROTECT_MK1 =
            ITEMS.register("turret_upgrade_protect_mk1", () -> new TurretUpgradeItem(TurretUpgradeBehavior.PROTECT, 1, new Item.Properties().stacksTo(4)));

    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_SLOW =
            ITEMS.register("turret_upgrade_slow", () -> new TurretUpgradeItem(TurretUpgradeBehavior.SLOW, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_ROOT =
            ITEMS.register("turret_upgrade_root", () -> new TurretUpgradeItem(TurretUpgradeBehavior.ROOT, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<TurretUpgradeItem> TURRET_UPGRADE_LEVITATE =
            ITEMS.register("turret_upgrade_levitate", () -> new TurretUpgradeItem(TurretUpgradeBehavior.LEVITATE, 0, new Item.Properties().stacksTo(4)));

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


    // 衝刺距離升級
    public static final DeferredItem<BootsUpgradeItem> BOOTS_UPGRADE_DASH_DISTANCE_MK0 =
            ITEMS.register("boots_upgrade_dash_distance_mk0", () -> new BootsUpgradeItem(BootsUpgradeBehavior.DASH_DISTANCE, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<BootsUpgradeItem> BOOTS_UPGRADE_DASH_DISTANCE_MK1 =
            ITEMS.register("boots_upgrade_dash_distance_mk1", () -> new BootsUpgradeItem(BootsUpgradeBehavior.DASH_DISTANCE, 1, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<BootsUpgradeItem> BOOTS_UPGRADE_DASH_DISTANCE_MK2 =
            ITEMS.register("boots_upgrade_dash_distance_mk2", () -> new BootsUpgradeItem(BootsUpgradeBehavior.DASH_DISTANCE, 2, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<BootsUpgradeItem> BOOTS_UPGRADE_DASH_DISTANCE_MK3 =
            ITEMS.register("boots_upgrade_dash_distance_mk3", () -> new BootsUpgradeItem(BootsUpgradeBehavior.DASH_DISTANCE, 3, new Item.Properties().stacksTo(4)));

    // 魔力效率升級
    public static final DeferredItem<BootsUpgradeItem> BOOTS_UPGRADE_MANA_EFFICIENCY_MK0 =
            ITEMS.register("boots_upgrade_mana_efficiency_mk0", () -> new BootsUpgradeItem(BootsUpgradeBehavior.MANA_EFFICIENCY, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<BootsUpgradeItem> BOOTS_UPGRADE_MANA_EFFICIENCY_MK1 =
            ITEMS.register("boots_upgrade_mana_efficiency_mk1", () -> new BootsUpgradeItem(BootsUpgradeBehavior.MANA_EFFICIENCY, 1, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<BootsUpgradeItem> BOOTS_UPGRADE_MANA_EFFICIENCY_MK2 =
            ITEMS.register("boots_upgrade_mana_efficiency_mk2", () -> new BootsUpgradeItem(BootsUpgradeBehavior.MANA_EFFICIENCY, 2, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<BootsUpgradeItem> BOOTS_UPGRADE_MANA_EFFICIENCY_MK3 =
            ITEMS.register("boots_upgrade_mana_efficiency_mk3", () -> new BootsUpgradeItem(BootsUpgradeBehavior.MANA_EFFICIENCY, 3, new Item.Properties().stacksTo(4)));


    // ── 魔力合金裝備套組 ────────────────────────────────────────────────────────

    public static final DeferredItem<ManaAlloyHelmetItem> MANA_ALLOY_HELMET =
            ITEMS.register("mana_alloy_helmet", () -> new ManaAlloyHelmetItem(
                    ModArmorMaterials.MANA_ALLOY,
                    new Item.Properties()
                            .durability(363)
                            .component(ModDataComponents.MANA_STORED, 0)
                            .component(ModDataComponents.MAX_MANA, ManaAlloyHelmetItem.BASE_MAX_MANA)));

    public static final DeferredItem<ManaAlloyChestplateItem> MANA_ALLOY_CHESTPLATE =
            ITEMS.register("mana_alloy_chestplate", () -> new ManaAlloyChestplateItem(
                    ModArmorMaterials.MANA_ALLOY,
                    new Item.Properties()
                            .durability(528)
                            .component(ModDataComponents.MANA_STORED, 0)
                            .component(ModDataComponents.MAX_MANA, ManaAlloyChestplateItem.BASE_MAX_MANA)));

    public static final DeferredItem<ManaAlloyLeggingsItem> MANA_ALLOY_LEGGINGS =
            ITEMS.register("mana_alloy_leggings", () -> new ManaAlloyLeggingsItem(
                    ModArmorMaterials.MANA_ALLOY,
                    new Item.Properties()
                            .durability(495)
                            .component(ModDataComponents.MANA_STORED, 0)
                            .component(ModDataComponents.MAX_MANA, ManaAlloyLeggingsItem.BASE_MAX_MANA)));

    public static final DeferredItem<ManaSprintBootsItem> MANA_SPRINT_BOOTS =
            ITEMS.register("mana_sprint_boots", () -> new ManaSprintBootsItem(
                    ModArmorMaterials.MANA_ALLOY,
                    new Item.Properties()
                            .durability(429)
                            .component(ModDataComponents.MANA_STORED, 0)
                            .component(ModDataComponents.MAX_MANA, ManaSprintBootsItem.BASE_MAX_MANA)));


    // 通用魔力容量升級（適用於所有魔力盔甲）
    public static final DeferredItem<ArmorCapacityUpgradeItem> ARMOR_UPGRADE_CAPACITY_MK0 =
            ITEMS.register("armor_upgrade_capacity_mk0", () -> new ArmorCapacityUpgradeItem(0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<ArmorCapacityUpgradeItem> ARMOR_UPGRADE_CAPACITY_MK1 =
            ITEMS.register("armor_upgrade_capacity_mk1", () -> new ArmorCapacityUpgradeItem(1, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<ArmorCapacityUpgradeItem> ARMOR_UPGRADE_CAPACITY_MK2 =
            ITEMS.register("armor_upgrade_capacity_mk2", () -> new ArmorCapacityUpgradeItem(2, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<ArmorCapacityUpgradeItem> ARMOR_UPGRADE_CAPACITY_MK3 =
            ITEMS.register("armor_upgrade_capacity_mk3", () -> new ArmorCapacityUpgradeItem(3, new Item.Properties().stacksTo(4)));

    // 通用防禦升級（適用於所有魔力盔甲）
    public static final DeferredItem<ArmorDefenseUpgradeItem> ARMOR_UPGRADE_DEFENSE_MK0 =
            ITEMS.register("armor_upgrade_defense_mk0", () -> new ArmorDefenseUpgradeItem(0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<ArmorDefenseUpgradeItem> ARMOR_UPGRADE_DEFENSE_MK1 =
            ITEMS.register("armor_upgrade_defense_mk1", () -> new ArmorDefenseUpgradeItem(1, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<ArmorDefenseUpgradeItem> ARMOR_UPGRADE_DEFENSE_MK2 =
            ITEMS.register("armor_upgrade_defense_mk2", () -> new ArmorDefenseUpgradeItem(2, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<ArmorDefenseUpgradeItem> ARMOR_UPGRADE_DEFENSE_MK3 =
            ITEMS.register("armor_upgrade_defense_mk3", () -> new ArmorDefenseUpgradeItem(3, new Item.Properties().stacksTo(4)));

    // 胸甲護盾升級（兩種互斥）
    public static final DeferredItem<ChestplateUpgradeItem> CHESTPLATE_UPGRADE_SHIELD_REDUCTION_MK0 =
            ITEMS.register("chestplate_upgrade_shield_reduction_mk0", () -> new ChestplateUpgradeItem(ChestplateUpgradeBehavior.SHIELD_REDUCTION, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<ChestplateUpgradeItem> CHESTPLATE_UPGRADE_SHIELD_REDUCTION_MK1 =
            ITEMS.register("chestplate_upgrade_shield_reduction_mk1", () -> new ChestplateUpgradeItem(ChestplateUpgradeBehavior.SHIELD_REDUCTION, 1, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<ChestplateUpgradeItem> CHESTPLATE_UPGRADE_SHIELD_REDUCTION_MK2 =
            ITEMS.register("chestplate_upgrade_shield_reduction_mk2", () -> new ChestplateUpgradeItem(ChestplateUpgradeBehavior.SHIELD_REDUCTION, 2, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<ChestplateUpgradeItem> CHESTPLATE_UPGRADE_SHIELD_REDUCTION_MK3 =
            ITEMS.register("chestplate_upgrade_shield_reduction_mk3", () -> new ChestplateUpgradeItem(ChestplateUpgradeBehavior.SHIELD_REDUCTION, 3, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<ChestplateUpgradeItem> CHESTPLATE_UPGRADE_SHIELD_ABSORB_MK0 =
            ITEMS.register("chestplate_upgrade_shield_absorb_mk0", () -> new ChestplateUpgradeItem(ChestplateUpgradeBehavior.SHIELD_ABSORB, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<ChestplateUpgradeItem> CHESTPLATE_UPGRADE_SHIELD_ABSORB_MK1 =
            ITEMS.register("chestplate_upgrade_shield_absorb_mk1", () -> new ChestplateUpgradeItem(ChestplateUpgradeBehavior.SHIELD_ABSORB, 1, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<ChestplateUpgradeItem> CHESTPLATE_UPGRADE_SHIELD_ABSORB_MK2 =
            ITEMS.register("chestplate_upgrade_shield_absorb_mk2", () -> new ChestplateUpgradeItem(ChestplateUpgradeBehavior.SHIELD_ABSORB, 2, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<ChestplateUpgradeItem> CHESTPLATE_UPGRADE_SHIELD_ABSORB_MK3 =
            ITEMS.register("chestplate_upgrade_shield_absorb_mk3", () -> new ChestplateUpgradeItem(ChestplateUpgradeBehavior.SHIELD_ABSORB, 3, new Item.Properties().stacksTo(4)));

    // 頭盔升級
    public static final DeferredItem<HelmetUpgradeItem> HELMET_UPGRADE_NIGHT_VISION =
            ITEMS.register("helmet_upgrade_night_vision", () -> new HelmetUpgradeItem(HelmetUpgradeBehavior.NIGHT_VISION, 0, new Item.Properties().stacksTo(4)));

    // 護腿升級
    public static final DeferredItem<LeggingsUpgradeItem> LEGGINGS_UPGRADE_MULTI_JUMP_MK0 =
            ITEMS.register("leggings_upgrade_multi_jump_mk0", () -> new LeggingsUpgradeItem(LeggingsUpgradeBehavior.MULTI_JUMP, 0, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<LeggingsUpgradeItem> LEGGINGS_UPGRADE_MULTI_JUMP_MK1 =
            ITEMS.register("leggings_upgrade_multi_jump_mk1", () -> new LeggingsUpgradeItem(LeggingsUpgradeBehavior.MULTI_JUMP, 1, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<LeggingsUpgradeItem> LEGGINGS_UPGRADE_MULTI_JUMP_MK2 =
            ITEMS.register("leggings_upgrade_multi_jump_mk2", () -> new LeggingsUpgradeItem(LeggingsUpgradeBehavior.MULTI_JUMP, 2, new Item.Properties().stacksTo(4)));
    public static final DeferredItem<LeggingsUpgradeItem> LEGGINGS_UPGRADE_MULTI_JUMP_MK3 =
            ITEMS.register("leggings_upgrade_multi_jump_mk3", () -> new LeggingsUpgradeItem(LeggingsUpgradeBehavior.MULTI_JUMP, 3, new Item.Properties().stacksTo(4)));

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

    // ── 音樂唱片 ────────────────────────────────────────────────────────────────
    public static final DeferredItem<Item> DISC_QUANTIFIED_MANA_A =
            ITEMS.register("disc_quantified_mana_a", () -> new Item(new Item.Properties()
                    .stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModJukeboxSongs.QUANTIFIED_MANA_A)));
    public static final DeferredItem<Item> DISC_QUANTIFIED_MANA_B =
            ITEMS.register("disc_quantified_mana_b", () -> new Item(new Item.Properties()
                    .stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModJukeboxSongs.QUANTIFIED_MANA_B)));
    public static final DeferredItem<Item> DISC_MEMORY_TWO_SIDES_A =
            ITEMS.register("disc_memory_two_sides_a", () -> new Item(new Item.Properties()
                    .stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModJukeboxSongs.MEMORY_TWO_SIDES_A)));
    public static final DeferredItem<Item> DISC_MEMORY_TWO_SIDES_B =
            ITEMS.register("disc_memory_two_sides_b", () -> new Item(new Item.Properties()
                    .stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModJukeboxSongs.MEMORY_TWO_SIDES_B)));
    public static final DeferredItem<Item> DISC_KNOWLEDGE_SHORTCUT_A =
            ITEMS.register("disc_knowledge_shortcut_a", () -> new Item(new Item.Properties()
                    .stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModJukeboxSongs.KNOWLEDGE_SHORTCUT_A)));
    public static final DeferredItem<Item> DISC_KNOWLEDGE_SHORTCUT_B =
            ITEMS.register("disc_knowledge_shortcut_b", () -> new Item(new Item.Properties()
                    .stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModJukeboxSongs.KNOWLEDGE_SHORTCUT_B)));
    public static final DeferredItem<Item> DISC_U_KEY_CORE_A =
            ITEMS.register("disc_u_key_core_a", () -> new Item(new Item.Properties()
                    .stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModJukeboxSongs.U_KEY_CORE_A)));
    public static final DeferredItem<Item> DISC_U_KEY_CORE_B =
            ITEMS.register("disc_u_key_core_b", () -> new Item(new Item.Properties()
                    .stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModJukeboxSongs.U_KEY_CORE_B)));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
