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
        AspectExpression.addAllUnique(candidates, getLogicalPool(aspects));
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

    private static List<Aspect> getLogicalPool(List<Aspect> base) {
        // Simple logic: if it has Water, it might have Life; if it has Metal, it might have Corrosion
        Set<Aspect> pool = new HashSet<>();
        for (Aspect a : base) {
            if (a == ModAspects.WATER) pool.add(ModAspects.VITALITY);
            if (a == ModAspects.METAL) { pool.add(ModAspects.CORROSION); pool.add(ModAspects.RESONANCE); }
            if (a == ModAspects.FIRE) { pool.add(ModAspects.ENERGY); pool.add(ModAspects.VAPOR); }
            if (a == ModAspects.EARTH) { pool.add(ModAspects.GRAVITY); pool.add(ModAspects.GROWTH); }
            if (a == ModAspects.WOOD) { pool.add(ModAspects.GROWTH); pool.add(ModAspects.ANIMA); }
            if (a == ModAspects.MANA) { pool.add(ModAspects.RESONANCE); pool.add(ModAspects.RADIANCE); pool.add(ModAspects.LIFEFLOW); }
            if (a == ModAspects.CRYSTAL) { pool.add(ModAspects.RADIANCE); pool.add(ModAspects.GRAVITY); }
            if (a == ModAspects.VITALITY) { pool.add(ModAspects.NOURISH); pool.add(ModAspects.GROWTH); pool.add(ModAspects.LIFEFLOW); }
        }
        return new ArrayList<>(pool);
    }
}
