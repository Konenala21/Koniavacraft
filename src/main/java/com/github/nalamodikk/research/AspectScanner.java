package com.github.nalamodikk.research;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps block IDs and item IDs to their aspects for the Nara Watch scan mechanic.
 *
 * Primary aspects are always known; compound aspects must be discovered by scanning.
 * Scanning Koniava blocks/items is the intended way to discover compound aspects.
 */
public final class AspectScanner {

    private static final Map<ResourceLocation, List<Aspect>> BLOCK_MAP = new HashMap<>();
    private static final Map<ResourceLocation, List<Aspect>> ITEM_MAP  = new HashMap<>();

    static {
        // ── Blocks: vanilla — primary aspects ─────────────────────────────────
        b("minecraft:water",                  ModAspects.WATER);
        b("minecraft:flowing_water",           ModAspects.WATER);
        b("minecraft:ice",                     ModAspects.WATER,  ModAspects.EARTH);
        b("minecraft:packed_ice",              ModAspects.WATER,  ModAspects.EARTH);
        b("minecraft:blue_ice",               ModAspects.WATER,  ModAspects.EARTH);
        b("minecraft:snow_block",              ModAspects.WATER,  ModAspects.EARTH);
        b("minecraft:lava",                    ModAspects.FIRE,   ModAspects.EARTH);
        b("minecraft:fire",                    ModAspects.FIRE);
        b("minecraft:magma_block",             ModAspects.FIRE,   ModAspects.EARTH);
        b("minecraft:grass_block",             ModAspects.WOOD,   ModAspects.EARTH);
        b("minecraft:dirt",                    ModAspects.EARTH);
        b("minecraft:gravel",                  ModAspects.EARTH);
        b("minecraft:sand",                    ModAspects.EARTH);
        b("minecraft:stone",                   ModAspects.EARTH,  ModAspects.METAL);
        b("minecraft:deepslate",               ModAspects.EARTH,  ModAspects.METAL);
        b("minecraft:cobblestone",             ModAspects.EARTH);
        b("minecraft:oak_log",                 ModAspects.WOOD);
        b("minecraft:birch_log",               ModAspects.WOOD);
        b("minecraft:spruce_log",              ModAspects.WOOD);
        b("minecraft:jungle_log",              ModAspects.WOOD);
        b("minecraft:acacia_log",              ModAspects.WOOD);
        b("minecraft:dark_oak_log",            ModAspects.WOOD);
        b("minecraft:mangrove_log",            ModAspects.WOOD);
        b("minecraft:cherry_log",              ModAspects.WOOD);
        b("minecraft:iron_ore",                ModAspects.EARTH,  ModAspects.METAL);
        b("minecraft:deepslate_iron_ore",      ModAspects.EARTH,  ModAspects.METAL);
        b("minecraft:gold_ore",                ModAspects.EARTH,  ModAspects.METAL);
        b("minecraft:deepslate_gold_ore",      ModAspects.EARTH,  ModAspects.METAL);
        b("minecraft:diamond_ore",             ModAspects.EARTH,  ModAspects.METAL);
        b("minecraft:deepslate_diamond_ore",   ModAspects.EARTH,  ModAspects.METAL);
        b("minecraft:coal_ore",                ModAspects.EARTH,  ModAspects.FIRE);
        b("minecraft:deepslate_coal_ore",      ModAspects.EARTH,  ModAspects.FIRE);
        b("minecraft:copper_ore",              ModAspects.EARTH,  ModAspects.METAL);
        b("minecraft:deepslate_copper_ore",    ModAspects.EARTH,  ModAspects.METAL);
        b("minecraft:iron_block",              ModAspects.METAL);
        b("minecraft:gold_block",              ModAspects.METAL);
        b("minecraft:diamond_block",           ModAspects.METAL);
        b("minecraft:glowstone",               ModAspects.FIRE,   ModAspects.WU);
        b("minecraft:obsidian",                ModAspects.EARTH,  ModAspects.FIRE);
        b("minecraft:netherrack",              ModAspects.FIRE,   ModAspects.EARTH);
        b("minecraft:soul_sand",               ModAspects.EARTH,  ModAspects.WU);
        b("minecraft:amethyst_block",          ModAspects.METAL,  ModAspects.WU);
        b("minecraft:amethyst_cluster",        ModAspects.METAL,  ModAspects.WU);
        b("minecraft:sculk",                   ModAspects.EARTH,  ModAspects.WU);
        b("minecraft:sculk_sensor",            ModAspects.EARTH,  ModAspects.WU);
        b("minecraft:end_stone",               ModAspects.EARTH,  ModAspects.WU);
        b("minecraft:vine",                    ModAspects.WOOD,   ModAspects.WATER);

        // ── Blocks: Koniava — reveals compound aspects ─────────────────────────
        b("koniava:magic_ore",                 ModAspects.MANA,      ModAspects.EARTH);
        b("koniava:deepslate_magic_ore",       ModAspects.MANA,      ModAspects.EARTH);
        b("koniava:mana_block",                ModAspects.MANA,      ModAspects.METAL);
        b("koniava:mana_generator",            ModAspects.ENERGY,    ModAspects.FIRE);
        b("koniava:solar_mana_collector",      ModAspects.ENERGY,    ModAspects.FIRE);
        b("koniava:mana_grinder",              ModAspects.CRYSTAL,   ModAspects.METAL);
        b("koniava:mana_crafting_table",       ModAspects.CRYSTAL,   ModAspects.RESONANCE);
        b("koniava:basic_arcane_conduit",      ModAspects.MANA,      ModAspects.WU);
        b("koniava:advanced_arcane_conduit",   ModAspects.MANA,      ModAspects.RESONANCE);
        b("koniava:elite_arcane_conduit",      ModAspects.MANA,      ModAspects.CRYSTAL);
        b("koniava:research_table",            ModAspects.WU,        ModAspects.MANA);

        // ── Items: 原料（TC4 參考）────────────────────────────────────────────────
        i("minecraft:coal",                    ModAspects.FIRE,      ModAspects.EARTH);
        i("minecraft:charcoal",                ModAspects.FIRE,      ModAspects.WOOD);
        i("minecraft:flint",                   ModAspects.EARTH,     ModAspects.METAL);
        i("minecraft:iron_ingot",              ModAspects.METAL);
        i("minecraft:raw_iron",                ModAspects.EARTH,     ModAspects.METAL);
        i("minecraft:gold_ingot",              ModAspects.METAL,     ModAspects.RADIANCE);
        i("minecraft:raw_gold",                ModAspects.EARTH,     ModAspects.METAL);
        i("minecraft:copper_ingot",            ModAspects.METAL,     ModAspects.CORROSION);
        i("minecraft:raw_copper",              ModAspects.EARTH,     ModAspects.METAL);
        i("minecraft:diamond",                 ModAspects.CRYSTAL,   ModAspects.RADIANCE);
        i("minecraft:emerald",                 ModAspects.VITALITY,  ModAspects.CRYSTAL);
        i("minecraft:quartz",                  ModAspects.CRYSTAL,   ModAspects.EARTH);
        i("minecraft:amethyst_shard",          ModAspects.CRYSTAL,   ModAspects.WU);
        i("minecraft:redstone",                ModAspects.RESONANCE, ModAspects.WU);
        i("minecraft:lapis_lazuli",            ModAspects.WATER,     ModAspects.WU);
        i("minecraft:glowstone_dust",          ModAspects.FIRE,      ModAspects.WU);
        i("minecraft:netherite_ingot",         ModAspects.METAL,     ModAspects.FIRE);
        i("minecraft:netherite_scrap",         ModAspects.METAL,     ModAspects.FIRE);

        // ── Items: 魔法/稀有材料 ─────────────────────────────────────────────────
        i("minecraft:blaze_rod",               ModAspects.FIRE,      ModAspects.ENERGY);
        i("minecraft:blaze_powder",            ModAspects.FIRE,      ModAspects.ENERGY);
        i("minecraft:ender_pearl",             ModAspects.WU,        ModAspects.ENERGY);
        i("minecraft:ender_eye",               ModAspects.WU,        ModAspects.RESONANCE);
        i("minecraft:nether_star",             ModAspects.RESONANCE, ModAspects.ENERGY);
        i("minecraft:dragon_breath",           ModAspects.WU,        ModAspects.ENERGY);
        i("minecraft:echo_shard",              ModAspects.WU,        ModAspects.RESONANCE);
        i("minecraft:enchanted_book",          ModAspects.WU,        ModAspects.MANA);
        i("minecraft:experience_bottle",       ModAspects.WU,        ModAspects.MANA);
        i("minecraft:totem_of_undying",        ModAspects.VITALITY,  ModAspects.WU);
        i("minecraft:heart_of_the_sea",        ModAspects.WATER,     ModAspects.WU);
        i("minecraft:nautilus_shell",          ModAspects.WATER,     ModAspects.EARTH);
        i("minecraft:disc_fragment_5",         ModAspects.WU,        ModAspects.RESONANCE);

        // ── Items: 生物掉落 ──────────────────────────────────────────────────────
        i("minecraft:slime_ball",              ModAspects.VITALITY,  ModAspects.WATER);
        i("minecraft:string",                  ModAspects.VITALITY,  ModAspects.WOOD);
        i("minecraft:feather",                 ModAspects.VITALITY,  ModAspects.WOOD);
        i("minecraft:leather",                 ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:bone",                    ModAspects.EARTH,     ModAspects.VITALITY);
        i("minecraft:bone_meal",               ModAspects.EARTH,     ModAspects.GROWTH);
        i("minecraft:rotten_flesh",            ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:spider_eye",              ModAspects.VITALITY,  ModAspects.WU);
        i("minecraft:gunpowder",               ModAspects.PHLOGISTON, ModAspects.EARTH);
        i("minecraft:ghast_tear",              ModAspects.WATER,     ModAspects.ANIMA);
        i("minecraft:magma_cream",             ModAspects.MAGMA,     ModAspects.VITALITY);
        i("minecraft:phantom_membrane",        ModAspects.ANIMA,     ModAspects.EARTH);
        i("minecraft:shulker_shell",           ModAspects.EARTH,     ModAspects.GRAVITY);
        i("minecraft:prismarine_shard",        ModAspects.WATER,     ModAspects.CRYSTAL);
        i("minecraft:prismarine_crystals",     ModAspects.WATER,     ModAspects.CRYSTAL);
        i("minecraft:rabbit_hide",             ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:rabbit_foot",             ModAspects.VITALITY,  ModAspects.ANIMA);
        i("minecraft:turtle_scute",            ModAspects.EARTH,     ModAspects.WATER);
        i("minecraft:armadillo_scute",         ModAspects.EARTH,     ModAspects.VITALITY);
        i("minecraft:breeze_rod",              ModAspects.VAPOR,     ModAspects.ENERGY);

        // ── Items: 食物 ──────────────────────────────────────────────────────────
        i("minecraft:apple",                   ModAspects.VITALITY,  ModAspects.WOOD);
        i("minecraft:bread",                   ModAspects.VITALITY,  ModAspects.GROWTH);
        i("minecraft:cooked_beef",             ModAspects.VITALITY,  ModAspects.FIRE);
        i("minecraft:cooked_porkchop",         ModAspects.VITALITY,  ModAspects.FIRE);
        i("minecraft:cooked_chicken",          ModAspects.VITALITY,  ModAspects.FIRE);
        i("minecraft:cooked_mutton",           ModAspects.VITALITY,  ModAspects.FIRE);
        i("minecraft:cooked_rabbit",           ModAspects.VITALITY,  ModAspects.FIRE);
        i("minecraft:cooked_cod",              ModAspects.VITALITY,  ModAspects.FIRE);
        i("minecraft:cooked_salmon",           ModAspects.VITALITY,  ModAspects.FIRE);
        i("minecraft:beef",                    ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:porkchop",                ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:chicken",                 ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:cod",                     ModAspects.VITALITY,  ModAspects.WATER);
        i("minecraft:salmon",                  ModAspects.VITALITY,  ModAspects.WATER);
        i("minecraft:carrot",                  ModAspects.VITALITY,  ModAspects.GROWTH);
        i("minecraft:potato",                  ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:baked_potato",            ModAspects.VITALITY,  ModAspects.FIRE);
        i("minecraft:golden_apple",            ModAspects.VITALITY,  ModAspects.RADIANCE);
        i("minecraft:enchanted_golden_apple",  ModAspects.VITALITY,  ModAspects.MANA);
        i("minecraft:cake",                    ModAspects.VITALITY,  ModAspects.MECHANISM);
        i("minecraft:cookie",                  ModAspects.VITALITY,  ModAspects.WOOD);
        i("minecraft:pumpkin_pie",             ModAspects.VITALITY,  ModAspects.GROWTH);
        i("minecraft:mushroom_stew",           ModAspects.VITALITY,  ModAspects.WOOD);
        i("minecraft:rabbit_stew",             ModAspects.VITALITY,  ModAspects.NOURISH);
        i("minecraft:egg",                     ModAspects.VITALITY);
        i("minecraft:milk_bucket",             ModAspects.VITALITY,  ModAspects.WATER);
        i("minecraft:honey_bottle",            ModAspects.VITALITY,  ModAspects.VITALITY);
        i("minecraft:honeycomb",               ModAspects.VITALITY,  ModAspects.WOOD);
        i("minecraft:sugar",                   ModAspects.VITALITY,  ModAspects.WATER);
        i("minecraft:nether_wart",             ModAspects.VITALITY,  ModAspects.FIRE);
        i("minecraft:sweet_berries",           ModAspects.VITALITY,  ModAspects.WOOD);
        i("minecraft:glow_berries",            ModAspects.VITALITY,  ModAspects.RADIANCE);

        // ── Items: 農業/植物 ─────────────────────────────────────────────────────
        i("minecraft:wheat",                   ModAspects.GROWTH,    ModAspects.WOOD);
        i("minecraft:wheat_seeds",             ModAspects.GROWTH,    ModAspects.WOOD);
        i("minecraft:pumpkin_seeds",           ModAspects.GROWTH,    ModAspects.WOOD);
        i("minecraft:melon_seeds",             ModAspects.GROWTH,    ModAspects.WATER);
        i("minecraft:melon_slice",             ModAspects.VITALITY,  ModAspects.WATER);
        i("minecraft:sugar_cane",              ModAspects.VITALITY,  ModAspects.WATER);
        i("minecraft:bamboo",                  ModAspects.WOOD,      ModAspects.GROWTH);
        i("minecraft:cactus",                  ModAspects.WOOD,      ModAspects.EARTH);
        i("minecraft:lily_pad",                ModAspects.WOOD,      ModAspects.WATER);
        i("minecraft:kelp",                    ModAspects.WOOD,      ModAspects.WATER);
        i("minecraft:dried_kelp",              ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:chorus_fruit",            ModAspects.VITALITY,  ModAspects.WU);
        i("minecraft:beetroot",                ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:beetroot_seeds",          ModAspects.GROWTH,    ModAspects.EARTH);
        i("minecraft:cocoa_beans",             ModAspects.WOOD,      ModAspects.VITALITY);
        i("minecraft:torchflower_seeds",       ModAspects.GROWTH,    ModAspects.FIRE);
        i("minecraft:pitcher_pod",             ModAspects.GROWTH,    ModAspects.WATER);

        // ── Items: 工具 ──────────────────────────────────────────────────────────
        i("minecraft:stick",                   ModAspects.WOOD);
        i("minecraft:wooden_sword",            ModAspects.WOOD,      ModAspects.FIRE);
        i("minecraft:wooden_pickaxe",          ModAspects.WOOD,      ModAspects.METAL);
        i("minecraft:wooden_axe",              ModAspects.WOOD,      ModAspects.MECHANISM);
        i("minecraft:wooden_shovel",           ModAspects.WOOD,      ModAspects.EARTH);
        i("minecraft:wooden_hoe",              ModAspects.WOOD,      ModAspects.GROWTH);
        i("minecraft:stone_sword",             ModAspects.EARTH,     ModAspects.FIRE);
        i("minecraft:stone_pickaxe",           ModAspects.EARTH,     ModAspects.METAL);
        i("minecraft:stone_axe",               ModAspects.EARTH,     ModAspects.WOOD);
        i("minecraft:stone_shovel",            ModAspects.EARTH,     ModAspects.EARTH);
        i("minecraft:stone_hoe",               ModAspects.EARTH,     ModAspects.GROWTH);
        i("minecraft:iron_sword",              ModAspects.METAL,     ModAspects.FIRE);
        i("minecraft:iron_pickaxe",            ModAspects.METAL,     ModAspects.EARTH);
        i("minecraft:iron_axe",                ModAspects.METAL,     ModAspects.WOOD);
        i("minecraft:iron_shovel",             ModAspects.METAL,     ModAspects.EARTH);
        i("minecraft:iron_hoe",                ModAspects.METAL,     ModAspects.GROWTH);
        i("minecraft:golden_sword",            ModAspects.METAL,     ModAspects.RADIANCE);
        i("minecraft:golden_pickaxe",          ModAspects.METAL,     ModAspects.RADIANCE);
        i("minecraft:diamond_sword",           ModAspects.CRYSTAL,   ModAspects.FIRE);
        i("minecraft:diamond_pickaxe",         ModAspects.CRYSTAL,   ModAspects.EARTH);
        i("minecraft:diamond_axe",             ModAspects.CRYSTAL,   ModAspects.WOOD);
        i("minecraft:netherite_sword",         ModAspects.METAL,     ModAspects.FIRE);
        i("minecraft:netherite_pickaxe",       ModAspects.METAL,     ModAspects.FIRE);
        i("minecraft:bow",                     ModAspects.WOOD,      ModAspects.VITALITY);
        i("minecraft:crossbow",                ModAspects.WOOD,      ModAspects.METAL);
        i("minecraft:arrow",                   ModAspects.WOOD,      ModAspects.METAL);
        i("minecraft:trident",                 ModAspects.WATER,     ModAspects.METAL);
        i("minecraft:shield",                  ModAspects.WOOD,      ModAspects.METAL);
        i("minecraft:fishing_rod",             ModAspects.WOOD,      ModAspects.WATER);
        i("minecraft:flint_and_steel",         ModAspects.FIRE,      ModAspects.METAL);
        i("minecraft:shears",                  ModAspects.METAL,     ModAspects.VITALITY);
        i("minecraft:brush",                   ModAspects.WOOD,      ModAspects.EARTH);
        i("minecraft:spyglass",                ModAspects.METAL,     ModAspects.CRYSTAL);

        // ── Items: 防具 ──────────────────────────────────────────────────────────
        i("minecraft:leather_helmet",          ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:leather_chestplate",      ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:leather_leggings",        ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:leather_boots",           ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:iron_helmet",             ModAspects.METAL);
        i("minecraft:iron_chestplate",         ModAspects.METAL);
        i("minecraft:iron_leggings",           ModAspects.METAL);
        i("minecraft:iron_boots",              ModAspects.METAL);
        i("minecraft:diamond_helmet",          ModAspects.CRYSTAL,   ModAspects.METAL);
        i("minecraft:diamond_chestplate",      ModAspects.CRYSTAL,   ModAspects.METAL);
        i("minecraft:diamond_leggings",        ModAspects.CRYSTAL,   ModAspects.METAL);
        i("minecraft:diamond_boots",           ModAspects.CRYSTAL,   ModAspects.METAL);
        i("minecraft:netherite_helmet",        ModAspects.METAL,     ModAspects.FIRE);
        i("minecraft:netherite_chestplate",    ModAspects.METAL,     ModAspects.FIRE);
        i("minecraft:netherite_leggings",      ModAspects.METAL,     ModAspects.FIRE);
        i("minecraft:netherite_boots",         ModAspects.METAL,     ModAspects.FIRE);
        i("minecraft:elytra",                  ModAspects.ANIMA,     ModAspects.WU);
        i("minecraft:turtle_helmet",           ModAspects.WATER,     ModAspects.EARTH);

        // ── Items: 書/知識 ────────────────────────────────────────────────────────
        i("minecraft:paper",                   ModAspects.WU,        ModAspects.WOOD);
        i("minecraft:book",                    ModAspects.WU,        ModAspects.WOOD);
        i("minecraft:written_book",            ModAspects.WU,        ModAspects.MANA);
        i("minecraft:writable_book",           ModAspects.WU,        ModAspects.WOOD);
        i("minecraft:knowledge_book",          ModAspects.WU,        ModAspects.MANA);
        i("minecraft:map",                     ModAspects.WATER,     ModAspects.WU);
        i("minecraft:compass",                 ModAspects.METAL,     ModAspects.WU);
        i("minecraft:clock",                   ModAspects.METAL,     ModAspects.WU);
        i("minecraft:name_tag",                ModAspects.WU,        ModAspects.VITALITY);

        // ── Items: 容器/雜項 ─────────────────────────────────────────────────────
        i("minecraft:bucket",                  ModAspects.METAL);
        i("minecraft:water_bucket",            ModAspects.METAL,     ModAspects.WATER);
        i("minecraft:lava_bucket",             ModAspects.METAL,     ModAspects.FIRE);
        i("minecraft:powder_snow_bucket",      ModAspects.METAL,     ModAspects.WATER);
        i("minecraft:glass_bottle",            ModAspects.CRYSTAL,   ModAspects.EARTH);
        i("minecraft:saddle",                  ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:lead",                    ModAspects.VITALITY,  ModAspects.EARTH);
        i("minecraft:torch",                   ModAspects.FIRE,      ModAspects.WOOD);
        i("minecraft:soul_torch",              ModAspects.FIRE,      ModAspects.ANIMA);
        i("minecraft:lantern",                 ModAspects.FIRE,      ModAspects.METAL);
        i("minecraft:soul_lantern",            ModAspects.FIRE,      ModAspects.ANIMA);
        i("minecraft:snowball",                ModAspects.WATER,     ModAspects.EARTH);
        i("minecraft:fire_charge",             ModAspects.FIRE,      ModAspects.PHLOGISTON);
        i("minecraft:wool",                    ModAspects.VITALITY,  ModAspects.WOOD);
        i("minecraft:clay_ball",               ModAspects.EARTH,     ModAspects.WATER);
        i("minecraft:brick",                   ModAspects.EARTH,     ModAspects.FIRE);
        i("minecraft:nether_brick",            ModAspects.FIRE,      ModAspects.EARTH);
        i("minecraft:obsidian",                ModAspects.EARTH,     ModAspects.FIRE);

        // ── Items: 藥水 ──────────────────────────────────────────────────────────
        i("minecraft:potion",                  ModAspects.WATER,     ModAspects.MANA);
        i("minecraft:splash_potion",           ModAspects.WATER,     ModAspects.MANA);
        i("minecraft:lingering_potion",        ModAspects.WATER,     ModAspects.MANA);
        i("minecraft:fermented_spider_eye",    ModAspects.WU,        ModAspects.VITALITY);

        // ── Items: Koniava ─────────────────────────────────────────────────────
        i("koniava:mana_dust",                 ModAspects.MANA);
        i("koniava:raw_mana_dust",             ModAspects.MANA,      ModAspects.EARTH);
        i("koniava:condensed_mana_dust",       ModAspects.MANA);
        i("koniava:refined_mana_dust",         ModAspects.MANA,      ModAspects.RESONANCE);
        i("koniava:mana_ingot",                ModAspects.MANA,      ModAspects.METAL);
        i("koniava:mana_crystal_fragment",     ModAspects.CRYSTAL,   ModAspects.MANA);
    }

    private static void b(String id, Aspect... aspects) {
        BLOCK_MAP.put(ResourceLocation.parse(id), List.of(aspects));
    }

    private static void i(String id, Aspect... aspects) {
        ITEM_MAP.put(ResourceLocation.parse(id), List.of(aspects));
    }

    public static List<Aspect> getAspectsFor(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return BLOCK_MAP.getOrDefault(id, List.of());
    }

    public static List<Aspect> getAspectsForItem(Item item) {
        // 1. Direct ID match
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        List<Aspect> direct = ITEM_MAP.get(id);
        if (direct != null) return direct;

        // 2. Tag-based fallback
        return getTagAspects(item);
    }

    private static List<Aspect> getTagAspects(Item item) {
        var h = item.builtInRegistryHolder();

        // ── Vanilla tags ──────────────────────────────────────────────────────
        if (h.is(ItemTags.COALS))   return l(ModAspects.FIRE,     ModAspects.EARTH);
        if (h.is(ItemTags.LOGS))    return l(ModAspects.WOOD);
        if (h.is(ItemTags.PLANKS))  return l(ModAspects.WOOD);
        if (h.is(ItemTags.LEAVES))  return l(ModAspects.WOOD,     ModAspects.WATER);
        if (h.is(ItemTags.WOOL))    return l(ModAspects.VITALITY, ModAspects.WOOD);
        if (h.is(ItemTags.SWORDS))  return l(ModAspects.METAL);
        if (h.is(ItemTags.PICKAXES))return l(ModAspects.METAL,    ModAspects.EARTH);
        if (h.is(ItemTags.AXES))    return l(ModAspects.METAL,    ModAspects.WOOD);
        if (h.is(ItemTags.SHOVELS)) return l(ModAspects.METAL,    ModAspects.EARTH);
        if (h.is(ItemTags.HOES))    return l(ModAspects.METAL,    ModAspects.GROWTH);
        if (h.is(ItemTags.ARROWS))  return l(ModAspects.WOOD,     ModAspects.METAL);

        // ── Common (c:) tags — NeoForge 1.21 convention ───────────────────────
        if (h.is(c("ores")))          return l(ModAspects.EARTH,    ModAspects.METAL);
        if (h.is(c("ingots")))        return l(ModAspects.METAL);
        if (h.is(c("gems")))          return l(ModAspects.CRYSTAL);
        if (h.is(c("dusts")))         return l(ModAspects.EARTH);
        if (h.is(c("nuggets")))       return l(ModAspects.METAL);
        if (h.is(c("raw_materials"))) return l(ModAspects.EARTH,    ModAspects.METAL);
        if (h.is(c("crops")))         return l(ModAspects.GROWTH,   ModAspects.WOOD);
        if (h.is(c("seeds")))         return l(ModAspects.GROWTH,   ModAspects.WOOD);
        if (h.is(c("foods/meat")))    return l(ModAspects.VITALITY, ModAspects.EARTH);
        if (h.is(c("foods/fruit")))   return l(ModAspects.VITALITY, ModAspects.WOOD);
        if (h.is(c("foods")))         return l(ModAspects.VITALITY);
        if (h.is(c("dyes")))          return l(ModAspects.WOOD,     ModAspects.WATER);
        if (h.is(c("glass")))         return l(ModAspects.EARTH,    ModAspects.FIRE);
        if (h.is(c("stone_crafting_materials"))) return l(ModAspects.EARTH);

        return List.of();
    }

    private static TagKey<Item> c(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static List<Aspect> l(Aspect... aspects) {
        return List.of(aspects);
    }

    private AspectScanner() {}
}
