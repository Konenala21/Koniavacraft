package com.github.nalamodikk.research.dynamic;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines the "Atoms" - the base materials that the inference engine uses as a root.
 * These have semi-fixed core aspects but can be perturbed by world seeds.
 */
public class BaseMaterialRegistry {

    private static final Map<ResourceLocation, List<Aspect>> FIXED_ATOMS = new HashMap<>();
    private static final Map<TagKey<Item>, List<Aspect>> TAG_ATOMS = new HashMap<>();

    static {
        // --- Core Elements (Atoms) ---
        atom(Items.WATER_BUCKET, ModAspects.WATER);
        atom(Items.LAVA_BUCKET, ModAspects.FIRE, ModAspects.EARTH);
        atom(Items.IRON_INGOT, ModAspects.METAL);
        atom(Items.GOLD_INGOT, ModAspects.METAL, ModAspects.RADIANCE);
        atom(Items.DIAMOND, ModAspects.CRYSTAL, ModAspects.RADIANCE);
        atom(Items.COAL, ModAspects.FIRE, ModAspects.EARTH);
        atom(Items.REDSTONE, ModAspects.RESONANCE, ModAspects.WU);
        atom(Items.GLOWSTONE_DUST, ModAspects.RADIANCE, ModAspects.WU);
        atom(Items.AMETHYST_SHARD, ModAspects.CRYSTAL, ModAspects.WU);
        atom(Items.EMERALD, ModAspects.VITALITY, ModAspects.CRYSTAL);
        atom(Items.QUARTZ, ModAspects.CRYSTAL, ModAspects.EARTH);

        // --- High-Level Life Items (Life Flow Sources) ---
        atom(Items.GOLDEN_APPLE, ModAspects.VITALITY, ModAspects.LIFEFLOW);
        atom(Items.ENCHANTED_GOLDEN_APPLE, ModAspects.LIFEFLOW, ModAspects.MANA, ModAspects.RADIANCE);
        atom(Items.TOTEM_OF_UNDYING, ModAspects.LIFEFLOW, ModAspects.ANIMA, ModAspects.RESONANCE);
        atom(Items.DRAGON_EGG, ModAspects.LIFEFLOW, ModAspects.WU, ModAspects.GRAVITY);
        atom(Items.NETHER_STAR, ModAspects.LIFEFLOW, ModAspects.RADIANCE, ModAspects.WU);
        atom(Items.BEACON, ModAspects.QIAN, ModAspects.RADIANCE, ModAspects.CRYSTAL);
        atom(Items.CONDUIT, ModAspects.KAN, ModAspects.WATER, ModAspects.RESONANCE);

        // --- Elemental Seeds (Ensure all aspects have at least one scan source) ---
        atom(Items.SUGAR, ModAspects.VAPOR, ModAspects.GROWTH);
        atom(Items.GLASS_BOTTLE, ModAspects.VAPOR, ModAspects.CRYSTAL);
        atom(Items.SLIME_BALL, ModAspects.CORROSION, ModAspects.VITALITY);
        atom(Items.FERMENTED_SPIDER_EYE, ModAspects.CORROSION, ModAspects.ANIMA);
        atom(Items.SPYGLASS, ModAspects.REFRACTION, ModAspects.METAL);
        atom(Items.PRISMARINE_SHARD, ModAspects.REFRACTION, ModAspects.WATER);
        atom(Items.OBSIDIAN, ModAspects.GRAVITY, ModAspects.EARTH, ModAspects.FIRE);
        atom(Items.ANVIL, ModAspects.GRAVITY, ModAspects.METAL);
        atom(Items.MAGMA_CREAM, ModAspects.MAGMA, ModAspects.FIRE);
        atom(Items.BLAZE_POWDER, ModAspects.ENERGY, ModAspects.FIRE);
        atom(Items.GUNPOWDER, ModAspects.ENERGY, ModAspects.EARTH);

        // --- Missing Seeds (Completing the 24 Aspects) ---
        atom(Items.CHARCOAL, ModAspects.PHLOGISTON, ModAspects.FIRE);
        atom(Items.BONE_MEAL, ModAspects.KUN, ModAspects.GROWTH);
        atom(Items.LIGHTNING_ROD, ModAspects.ZHEN, ModAspects.METAL, ModAspects.ENERGY);
        atom(Items.FEATHER, ModAspects.XUN, ModAspects.VAPOR);
        atom(Items.GLOW_BERRIES, ModAspects.LI, ModAspects.RADIANCE, ModAspects.VITALITY);
        atom(Items.DEEPSLATE, ModAspects.GEN, ModAspects.EARTH, ModAspects.GRAVITY);
        atom(Items.WET_SPONGE, ModAspects.DUI, ModAspects.WATER);
        atom(Items.FIREWORK_ROCKET, ModAspects.MOMENTUM, ModAspects.ENERGY);

        // --- Common Blocks ---
        atom(Items.STONE, ModAspects.EARTH, ModAspects.METAL);
        atom(Items.COBBLESTONE, ModAspects.EARTH);
        atom(Items.DIRT, ModAspects.EARTH);
        atom(Items.GRASS_BLOCK, ModAspects.WOOD, ModAspects.EARTH);
        atom(Items.SAND, ModAspects.EARTH);
        atom(Items.GRAVEL, ModAspects.EARTH);
        atom(Items.OBSIDIAN, ModAspects.EARTH, ModAspects.FIRE);
        atom(Items.NETHERRACK, ModAspects.FIRE, ModAspects.EARTH);
        atom(Items.SOUL_SAND, ModAspects.EARTH, ModAspects.WU);
        atom(Items.SCULK, ModAspects.EARTH, ModAspects.WU);
        atom(Items.END_STONE, ModAspects.EARTH, ModAspects.WU);
        atom(Items.ICE, ModAspects.WATER, ModAspects.EARTH);

        // --- Koniavacraft Items ---
        atom(ModItems.MANA_DUST.get(), ModAspects.MANA);
        atom(ModItems.RAW_MANA_DUST.get(), ModAspects.MANA, ModAspects.EARTH);
        atom(ModItems.REFINED_MANA_DUST.get(), ModAspects.MANA, ModAspects.RADIANCE);
        atom(ModItems.MANA_INGOT.get(), ModAspects.MANA, ModAspects.METAL);
        atom(ModItems.MANA_CRYSTAL_FRAGMENT.get(), ModAspects.MANA, ModAspects.CRYSTAL);

        // --- Koniavacraft Blocks ---
        atom(ModBlocks.MANA_BLOCK.get().asItem(), ModAspects.MANA, ModAspects.METAL);
        atom(ModBlocks.MAGIC_ORE.get().asItem(), ModAspects.MANA, ModAspects.EARTH);
        atom(ModBlocks.DEEPSLATE_MAGIC_ORE.get().asItem(), ModAspects.MANA, ModAspects.EARTH);
        atom(ModBlocks.MANA_SOIL.get().asItem(), ModAspects.MANA, ModAspects.EARTH);
        atom(ModBlocks.DEEP_MANA_SOIL.get().asItem(), ModAspects.MANA, ModAspects.EARTH);
        atom(ModBlocks.MANA_GRASS_BLOCK.get().asItem(), ModAspects.MANA, ModAspects.EARTH, ModAspects.WOOD);
        atom(ModBlocks.MANA_BLOOM.get().asItem(), ModAspects.MANA, ModAspects.WOOD, ModAspects.VITALITY);

        // --- Extended Aspect Seeds (48 new aspects) ---

        // 暗影 Shadow
        atom(Items.INK_SAC,                 ModAspects.SHADOW,      ModAspects.WU);
        // 冰寒 Frost
        atom(Items.PACKED_ICE,              ModAspects.FROST,       ModAspects.WATER);
        atom(Items.BLUE_ICE,                ModAspects.FROST,       ModAspects.WATER);
        // 知識 Cognition
        atom(Items.BOOK,                    ModAspects.COGNITION,   ModAspects.ANIMA);
        atom(Items.EXPERIENCE_BOTTLE,       ModAspects.COGNITION,   ModAspects.MANA);
        // 毒素 Venom
        atom(Items.SPIDER_EYE,              ModAspects.VENOM,       ModAspects.VITALITY);
        atom(Items.POISONOUS_POTATO,        ModAspects.VENOM,       ModAspects.GROWTH);
        // 亡靈 Undead
        atom(Items.BONE,                    ModAspects.UNDEAD,      ModAspects.EARTH);
        atom(Items.ROTTEN_FLESH,            ModAspects.UNDEAD,      ModAspects.VITALITY);
        // 療癒 Mending
        atom(Items.HONEY_BOTTLE,            ModAspects.MENDING,     ModAspects.VITALITY);
        atom(Items.GLISTERING_MELON_SLICE,  ModAspects.MENDING,     ModAspects.VITALITY);
        // 污染 Taint
        atom(Items.WITHER_ROSE,             ModAspects.TAINT,       ModAspects.WU);
        atom(Items.SUSPICIOUS_STEW,         ModAspects.TAINT,       ModAspects.VITALITY);
        // 束縛 Binding
        atom(Items.LEAD,                    ModAspects.BINDING,     ModAspects.METAL);
        atom(Items.STRING,                  ModAspects.BINDING,     ModAspects.WOOD);
        // 飛翔 Flight
        atom(Items.PHANTOM_MEMBRANE,        ModAspects.FLIGHT,      ModAspects.VITALITY);
        atom(Items.ELYTRA,                  ModAspects.FLIGHT,      ModAspects.ENERGY);
        // 豐收 Harvest
        atom(Items.WHEAT,                   ModAspects.HARVEST,     ModAspects.GROWTH);
        atom(Items.BREAD,                   ModAspects.HARVEST,     ModAspects.NOURISH);
        // 器具 Instrument
        atom(Items.STICK,                   ModAspects.INSTRUMENT,  ModAspects.WOOD);
        atom(Items.FLINT,                   ModAspects.INSTRUMENT,  ModAspects.EARTH);
        // 異界 Eldritch
        atom(Items.ENDER_PEARL,             ModAspects.ELDRITCH,    ModAspects.WU);
        atom(Items.ENDER_EYE,               ModAspects.ELDRITCH,    ModAspects.WU);
        // 財富 Wealth
        atom(Items.GOLD_NUGGET,             ModAspects.WEALTH,      ModAspects.METAL);
        // 奧術 Arcana
        atom(Items.BLAZE_ROD,               ModAspects.ARCANA,      ModAspects.FIRE);
        // 飢渴 Famine
        atom(Items.DRIED_KELP,              ModAspects.FAMINE,      ModAspects.WATER);
        atom(Items.BOWL,                    ModAspects.FAMINE,      ModAspects.WOOD);
        // 虛空 Void
        atom(Items.CHORUS_FRUIT,            ModAspects.VOID_ASPECT, ModAspects.WU);
        atom(Items.CHORUS_FLOWER,           ModAspects.VOID_ASPECT, ModAspects.WU);
        // 死滅 Death
        atom(Items.DEAD_BUSH,               ModAspects.DEATH,       ModAspects.EARTH);
        atom(Items.SKELETON_SKULL,          ModAspects.DEATH,       ModAspects.WU);
        // 肉身 Corpus
        atom(Items.LEATHER,                 ModAspects.CORPUS,      ModAspects.VITALITY);
        atom(Items.PORKCHOP,                ModAspects.CORPUS,      ModAspects.VITALITY);
        // 原質 Primordial
        atom(Items.MUD,                     ModAspects.PRIMORDIAL,  ModAspects.WATER);
        atom(Items.CLAY_BALL,               ModAspects.PRIMORDIAL,  ModAspects.WATER);
        // 繁衍 Propagation
        atom(Items.EGG,                     ModAspects.PROPAGATION, ModAspects.VITALITY);
        atom(Items.TURTLE_EGG,              ModAspects.PROPAGATION, ModAspects.VITALITY);
        // 血氣 Vitae
        atom(Items.DRAGON_BREATH,           ModAspects.VITAE,       ModAspects.WU);
        atom(Items.NETHER_WART,             ModAspects.VITAE,       ModAspects.FIRE);
        // 蒸汽 Steam
        atom(Items.CAULDRON,                ModAspects.STEAM,       ModAspects.VAPOR);
        atom(Items.BREWING_STAND,           ModAspects.STEAM,       ModAspects.FIRE);
        // 感知 Sensus
        atom(Items.CLOCK,                   ModAspects.SENSUS,      ModAspects.METAL);
        atom(Items.COMPASS,                 ModAspects.SENSUS,      ModAspects.METAL);
        // 鋒刃 Blade
        atom(Items.IRON_SWORD,              ModAspects.BLADE,       ModAspects.METAL);
        atom(Items.FLINT_AND_STEEL,         ModAspects.BLADE,       ModAspects.FIRE);
        // 防護 Warding
        atom(Items.SHIELD,                  ModAspects.WARDING,     ModAspects.METAL);
        atom(Items.TURTLE_SCUTE,            ModAspects.WARDING,     ModAspects.VITALITY);
        // 掘進 Excavation
        atom(Items.IRON_PICKAXE,            ModAspects.EXCAVATION,  ModAspects.METAL);
        atom(Items.DIAMOND_PICKAXE,         ModAspects.EXCAVATION,  ModAspects.CRYSTAL);
        // 機械 Machine
        atom(Items.PISTON,                  ModAspects.MACHINE,     ModAspects.MECHANISM);
        atom(Items.DISPENSER,               ModAspects.MACHINE,     ModAspects.MECHANISM);
        // 爐火 Furnace
        atom(Items.FURNACE,                 ModAspects.FURNACE,     ModAspects.FIRE);
        atom(Items.BLAST_FURNACE,           ModAspects.FURNACE,     ModAspects.FIRE);
        // 煉化 Alchemy
        atom(Items.POTION,                  ModAspects.ALCHEMY,     ModAspects.ARCANA);
        atom(Items.SPLASH_POTION,           ModAspects.ALCHEMY,     ModAspects.FIRE);
        // 光暈 Aura
        atom(Items.SHROOMLIGHT,             ModAspects.AURA,        ModAspects.RADIANCE);
        atom(Items.SEA_LANTERN,             ModAspects.AURA,        ModAspects.WATER);
        // 靈魂 Spiritus
        atom(Items.SOUL_TORCH,              ModAspects.SPIRITUS,    ModAspects.WU);
        atom(Items.SOUL_CAMPFIRE,           ModAspects.SPIRITUS,    ModAspects.WU);
        // 獸性 Bestia
        atom(Items.BEEF,                    ModAspects.BESTIA,      ModAspects.VITALITY);
        atom(Items.MUTTON,                  ModAspects.BESTIA,      ModAspects.VITALITY);
        // 慾望 Desire
        atom(Items.GOLDEN_CARROT,           ModAspects.DESIRE,      ModAspects.VITALITY);
        atom(Items.COOKIE,                  ModAspects.DESIRE,      ModAspects.GROWTH);
        // 風暴 Storm
        atom(Items.TRIDENT,                 ModAspects.STORM,       ModAspects.WATER);
        // 人性 Humanity
        atom(Items.PAPER,                   ModAspects.HUMANITY,    ModAspects.WOOD);
        atom(Items.WRITTEN_BOOK,            ModAspects.HUMANITY,    ModAspects.ANIMA);
        // 本能 Instinct
        atom(Items.RABBIT_FOOT,             ModAspects.INSTINCT,    ModAspects.VITALITY);
        atom(Items.RABBIT_HIDE,             ModAspects.INSTINCT,    ModAspects.VITALITY);
        // 強化 Fortify
        atom(Items.IRON_CHESTPLATE,         ModAspects.FORTIFY,     ModAspects.METAL);
        // 齒輪 Gear
        atom(Items.HOPPER,                  ModAspects.GEAR,        ModAspects.MECHANISM);
        atom(Items.COMPARATOR,              ModAspects.GEAR,        ModAspects.RESONANCE);
        // 管道 Pipeline
        atom(Items.IRON_BARS,               ModAspects.PIPELINE,    ModAspects.METAL);
        // 電弧 Arc
        atom(Items.COPPER_INGOT,            ModAspects.ARC,         ModAspects.ENERGY);
        // 信仰 Faith
        atom(Items.CANDLE,                  ModAspects.FAITH,       ModAspects.RADIANCE);
        // 知慧 Wisdom
        atom(Items.ENCHANTED_BOOK,          ModAspects.WISDOM,      ModAspects.MANA);
        // 語言 Language
        atom(Items.MAP,                     ModAspects.LANGUAGE,    ModAspects.WOOD);
        atom(Items.NAME_TAG,                ModAspects.LANGUAGE,    ModAspects.ANIMA);
        // 秩序 Order
        atom(Items.BRICK,                   ModAspects.ORDER,       ModAspects.EARTH);
        atom(Items.NETHER_BRICK,            ModAspects.ORDER,       ModAspects.FIRE);
        // 交易 Commerce
        atom(Items.BUNDLE,                  ModAspects.COMMERCE,    ModAspects.WEALTH);
        atom(Items.GOLD_BLOCK,              ModAspects.COMMERCE,    ModAspects.WEALTH);
        // 律法 Law
        atom(Items.CHISELED_STONE_BRICKS,   ModAspects.LAW,         ModAspects.EARTH);
        atom(Items.TRIPWIRE_HOOK,           ModAspects.LAW,         ModAspects.METAL);
        // 自動 Automation
        atom(Items.OBSERVER,                ModAspects.AUTOMATION,  ModAspects.RESONANCE);
        atom(Items.REPEATER,                ModAspects.AUTOMATION,  ModAspects.MECHANISM);
        // 文明 Civilization
        atom(Items.BELL,                    ModAspects.CIVILIZATION, ModAspects.METAL);

        // --- Tags (Broad categories) ---
        tag(ItemTags.LOGS, ModAspects.WOOD);
        tag(ItemTags.PLANKS, ModAspects.WOOD);
        tag(ItemTags.LEAVES, ModAspects.WOOD, ModAspects.WATER);
        tag(ItemTags.SAPLINGS, ModAspects.GROWTH, ModAspects.WOOD);
        tag(ItemTags.WOOL, ModAspects.VITALITY, ModAspects.WOOD);
        tag(ItemTags.FLOWERS, ModAspects.WOOD, ModAspects.GROWTH, ModAspects.VITALITY);
        tag(ItemTags.SMALL_FLOWERS, ModAspects.WOOD, ModAspects.GROWTH);
        tag(ItemTags.MEAT, ModAspects.VITALITY);
        tag(ItemTags.FISHES, ModAspects.WATER, ModAspects.VITALITY);
        tag(ItemTags.SWORDS, ModAspects.METAL, ModAspects.ENERGY);
        tag(ItemTags.PICKAXES, ModAspects.METAL, ModAspects.MECHANISM);
        tag(ItemTags.AXES, ModAspects.METAL, ModAspects.MECHANISM);
        tag(ItemTags.SHOVELS, ModAspects.METAL, ModAspects.EARTH);
        tag(ItemTags.HOES, ModAspects.METAL, ModAspects.GROWTH);
        tag(ItemTags.RAILS, ModAspects.METAL, ModAspects.MECHANISM);
        tag(ItemTags.BOATS, ModAspects.WOOD, ModAspects.MECHANISM);
        tag(ItemTags.BOOKSHELF_BOOKS, ModAspects.WOOD, ModAspects.ANIMA);
        tag(ItemTags.COALS, ModAspects.FIRE, ModAspects.EARTH);
        tag(ItemTags.ARROWS, ModAspects.WOOD, ModAspects.ENERGY);
        tag(ItemTags.STONE_TOOL_MATERIALS, ModAspects.EARTH);
        tag(ItemTags.BEACON_PAYMENT_ITEMS, ModAspects.METAL, ModAspects.RADIANCE);

        // NeoForge Common Tags
        tag(c("ores"), ModAspects.EARTH, ModAspects.METAL);
        tag(c("ingots"), ModAspects.METAL);
        tag(c("dusts"), ModAspects.EARTH);
        tag(c("gems"), ModAspects.CRYSTAL);
        tag(c("raw_materials"), ModAspects.EARTH, ModAspects.METAL);
        tag(c("crops"), ModAspects.GROWTH, ModAspects.VITALITY);
    }

    private static TagKey<Item> c(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static void atom(Item item, Aspect... aspects) {
        FIXED_ATOMS.put(BuiltInRegistries.ITEM.getKey(item), List.of(aspects));
    }

    private static void tag(TagKey<Item> tag, Aspect... aspects) {
        TAG_ATOMS.put(tag, List.of(aspects));
    }

    /**
     * Get base aspects for an item, perturbed by the world seed.
     */
    public static List<Aspect> getBaseAspects(Item item, long seed) {
        return getSemanticAspects(item, seed);
    }

    public static List<Aspect> getSemanticAspects(Item item, long genomeSeed) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        List<Aspect> aspects = new ArrayList<>(FIXED_ATOMS.getOrDefault(id, List.of()));
        List<Aspect> candidates = new ArrayList<>();

        // 1. Tag Check
        if (aspects.isEmpty()) {
            for (Map.Entry<TagKey<Item>, List<Aspect>> entry : TAG_ATOMS.entrySet()) {
                if (item.builtInRegistryHolder().is(entry.getKey())) {
                    AspectExpression.addAllUnique(aspects, entry.getValue());
                    break;
                }
            }
        } else {
            AspectExpression.addAllUnique(candidates, getLogicalPool(aspects));
        }

        // 2. Heuristic Check (Class-based detection for "All Items")
        if (aspects.isEmpty()) {
            aspects.addAll(getHeuristicAspects(item));
        } else {
            AspectExpression.addAllUnique(candidates, getHeuristicAspects(item));
        }

        // 3. Registry path keywords for modded content.
        List<Aspect> keywordAspects = getKeywordAspects(id);
        if (aspects.isEmpty()) {
            AspectExpression.addAllUnique(aspects, keywordAspects);
        } else {
            AspectExpression.addAllUnique(candidates, keywordAspects);
        }

        if (aspects.isEmpty()) {
            return List.of();
        }
        expandCandidatePool(aspects, candidates);
        return AspectExpression.express(id, aspects, candidates, genomeSeed, capacityForItem(item, aspects));
    }

    public static List<Aspect> getFallbackAspects(Item item, long genomeSeed) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        List<Aspect> semantic = getSemanticAspects(item, genomeSeed);
        if (!semantic.isEmpty()) {
            return semantic;
        }
        return AspectExpression.fallback(id, genomeSeed, 2);
    }

    private static List<Aspect> getHeuristicAspects(Item item) {
        List<Aspect> h = new ArrayList<>();
        // Food items always have Vitality
        if (item.components().has(DataComponents.FOOD)) {
            h.add(ModAspects.VITALITY);
            h.add(ModAspects.NOURISH);
        }
        // Tools/Armor heuristics
        if (item instanceof TieredItem tiered) {
            h.add(ModAspects.METAL);
            h.add(ModAspects.MECHANISM);
            if (tiered.getTier() == Tiers.DIAMOND) h.add(ModAspects.CRYSTAL);
        }
        if (item instanceof ArmorItem armor) {
            h.add(ModAspects.EARTH);
            if (isMetalArmor(armor)) h.add(ModAspects.METAL);
            if (armor.getMaterial().is(ArmorMaterials.DIAMOND)) h.add(ModAspects.CRYSTAL);
            if (armor.getMaterial().is(ArmorMaterials.GOLD)) h.add(ModAspects.RADIANCE);
        }
        // Enchanted books
        if (item == Items.ENCHANTED_BOOK) {
            h.add(ModAspects.MANA);
            h.add(ModAspects.ANIMA);
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id.getPath().endsWith("_spawn_egg")) {
            h.add(ModAspects.VITALITY);
            h.add(ModAspects.ANIMA);
        }
        return h;
    }

    private static boolean isMetalArmor(ArmorItem armor) {
        return armor.getMaterial().is(ArmorMaterials.CHAIN)
                || armor.getMaterial().is(ArmorMaterials.IRON)
                || armor.getMaterial().is(ArmorMaterials.GOLD)
                || armor.getMaterial().is(ArmorMaterials.DIAMOND)
                || armor.getMaterial().is(ArmorMaterials.NETHERITE);
    }

    private static int capacityForItem(Item item, List<Aspect> aspects) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();
        if (path.contains("boss") || path.contains("dragon") || path.contains("wither") || item == Items.NETHER_STAR) {
            return 5;
        }
        if (aspects.contains(ModAspects.MANA) || path.contains("magic") || path.contains("mana") || path.contains("golden_apple") || item == Items.TOTEM_OF_UNDYING) {
            return 4;
        }
        if (item instanceof TieredItem || item instanceof ArmorItem || path.contains("redstone")) {
            return 3;
        }
        return 2;
    }

    private static List<Aspect> getKeywordAspects(ResourceLocation id) {
        String path = id.getPath();
        List<Aspect> aspects = new ArrayList<>();
        if (path.contains("mana") || path.contains("magic") || path.contains("arcane")) {
            aspects.add(ModAspects.MANA);
        }
        if (path.contains("life") || path.contains("heart") || path.contains("totem")) {
            aspects.add(ModAspects.LIFEFLOW);
        }
        if (path.contains("ore") || path.contains("stone") || path.contains("deepslate") || path.contains("dirt") || path.contains("sand")) {
            aspects.add(ModAspects.EARTH);
        }
        if (path.contains("ingot") || path.contains("metal") || path.contains("iron") || path.contains("gold") || path.contains("copper")) {
            aspects.add(ModAspects.METAL);
        }
        if (path.contains("crystal") || path.contains("gem") || path.contains("diamond") || path.contains("amethyst") || path.contains("quartz")) {
            aspects.add(ModAspects.CRYSTAL);
        }
        if (path.contains("log") || path.contains("wood") || path.contains("plank") || path.contains("stem")) {
            aspects.add(ModAspects.WOOD);
        }
        if (path.contains("leaf") || path.contains("flower") || path.contains("crop") || path.contains("seed") || path.contains("sapling")) {
            aspects.add(ModAspects.GROWTH);
            aspects.add(ModAspects.VITALITY);
        }
        if (path.contains("water") || path.contains("ice") || path.contains("snow")) {
            aspects.add(ModAspects.WATER);
        }
        if (path.contains("lava") || path.contains("magma") || path.contains("blaze") || path.contains("fire")) {
            aspects.add(ModAspects.FIRE);
        }
        if (path.contains("redstone") || path.contains("echo") || path.contains("sculk")) {
            aspects.add(ModAspects.RESONANCE);
        }
        if (path.contains("soul") || path.contains("ender") || path.contains("end_") || path.contains("void")) {
            aspects.add(ModAspects.WU);
            aspects.add(ModAspects.ANIMA);
        }
        if (path.contains("glow") || path.contains("light") || path.contains("torch") || path.contains("lantern")) {
            aspects.add(ModAspects.RADIANCE);
        }
        if (path.contains("gear") || path.contains("machine") || path.contains("piston") || path.contains("rail")) {
            aspects.add(ModAspects.MECHANISM);
        }
        return aspects;
    }

    /**
     * Expands the candidate pool three levels deep so Tier-C/D compound aspects
     * (SENSUS, WISDOM, COMMERCE, LANGUAGE, etc.) can surface for items that earn
     * them through indirect thematic chains.
     */
    public static void expandCandidatePool(List<Aspect> base, List<Aspect> candidates) {
        List<Aspect> pass1 = new ArrayList<>(getLogicalPool(base));
        AspectExpression.addAllUnique(candidates, pass1);
        List<Aspect> pass2 = new ArrayList<>(getLogicalPool(pass1));
        AspectExpression.addAllUnique(candidates, pass2);
        // Third pass: reaches SENSUS, WISDOM, LANGUAGE, COMMERCE from e.g. COGNITION/WEALTH
        AspectExpression.addAllUnique(candidates, getLogicalPool(pass2));
    }

    static List<Aspect> getLogicalPool(List<Aspect> base) {
        Set<Aspect> pool = new HashSet<>();
        for (Aspect a : base) {
            // Primary chains
            if (a == ModAspects.WATER)     { pool.add(ModAspects.VITALITY); pool.add(ModAspects.FROST); }
            if (a == ModAspects.METAL)     { pool.add(ModAspects.CORROSION); pool.add(ModAspects.RESONANCE); pool.add(ModAspects.INSTRUMENT); }
            if (a == ModAspects.FIRE)      { pool.add(ModAspects.ENERGY); pool.add(ModAspects.VAPOR); pool.add(ModAspects.FURNACE); }
            if (a == ModAspects.EARTH)     { pool.add(ModAspects.GRAVITY); pool.add(ModAspects.GROWTH); pool.add(ModAspects.CORPUS); }
            if (a == ModAspects.WOOD)      { pool.add(ModAspects.GROWTH); pool.add(ModAspects.ANIMA); pool.add(ModAspects.HARVEST); }
            if (a == ModAspects.WU)        { pool.add(ModAspects.SHADOW); pool.add(ModAspects.ELDRITCH); pool.add(ModAspects.VOID_ASPECT); }
            // First-order chains
            if (a == ModAspects.MANA)      { pool.add(ModAspects.RESONANCE); pool.add(ModAspects.RADIANCE); pool.add(ModAspects.LIFEFLOW); pool.add(ModAspects.ARCANA); pool.add(ModAspects.TAINT); }
            if (a == ModAspects.CRYSTAL)   { pool.add(ModAspects.RADIANCE); pool.add(ModAspects.GRAVITY); pool.add(ModAspects.FROST); }
            if (a == ModAspects.VITALITY)  { pool.add(ModAspects.NOURISH); pool.add(ModAspects.GROWTH); pool.add(ModAspects.LIFEFLOW); pool.add(ModAspects.VITAE); pool.add(ModAspects.CORPUS); pool.add(ModAspects.BESTIA); }
            if (a == ModAspects.ANIMA)     { pool.add(ModAspects.COGNITION); pool.add(ModAspects.SPIRITUS); pool.add(ModAspects.UNDEAD); pool.add(ModAspects.DEATH); }
            if (a == ModAspects.ENERGY)    { pool.add(ModAspects.FLIGHT); pool.add(ModAspects.ARC); pool.add(ModAspects.STEAM); }
            if (a == ModAspects.CORROSION) { pool.add(ModAspects.VENOM); pool.add(ModAspects.TAINT); pool.add(ModAspects.FAMINE); }
            if (a == ModAspects.GRAVITY)   { pool.add(ModAspects.VOID_ASPECT); pool.add(ModAspects.DEATH); }
            if (a == ModAspects.RESONANCE) { pool.add(ModAspects.ARCANA); pool.add(ModAspects.ELDRITCH); }
            if (a == ModAspects.MECHANISM) { pool.add(ModAspects.MACHINE); pool.add(ModAspects.INSTRUMENT); pool.add(ModAspects.GEAR); }
            if (a == ModAspects.RADIANCE)  { pool.add(ModAspects.SHADOW); pool.add(ModAspects.COGNITION); pool.add(ModAspects.WEALTH); pool.add(ModAspects.AURA); }
            // Extended chains
            if (a == ModAspects.ARCANA)    { pool.add(ModAspects.ALCHEMY); pool.add(ModAspects.SPIRITUS); pool.add(ModAspects.AURA); pool.add(ModAspects.FAITH); }
            if (a == ModAspects.COGNITION) { pool.add(ModAspects.SENSUS); pool.add(ModAspects.WISDOM); pool.add(ModAspects.LANGUAGE); pool.add(ModAspects.DESIRE); }
            if (a == ModAspects.INSTRUMENT){ pool.add(ModAspects.BLADE); pool.add(ModAspects.WARDING); pool.add(ModAspects.MACHINE); pool.add(ModAspects.EXCAVATION); }
            if (a == ModAspects.MACHINE)   { pool.add(ModAspects.GEAR); pool.add(ModAspects.PIPELINE); pool.add(ModAspects.AUTOMATION); }
            if (a == ModAspects.SENSUS)    { pool.add(ModAspects.HUMANITY); pool.add(ModAspects.INSTINCT); pool.add(ModAspects.LANGUAGE); }
            if (a == ModAspects.HUMANITY)  { pool.add(ModAspects.ORDER); pool.add(ModAspects.COMMERCE); }
            if (a == ModAspects.WEALTH)    { pool.add(ModAspects.COMMERCE); }
            if (a == ModAspects.ORDER)     { pool.add(ModAspects.LAW); pool.add(ModAspects.AUTOMATION); pool.add(ModAspects.CIVILIZATION); }
        }
        return new ArrayList<>(pool);
    }
}
