package com.github.nalamodikk.research.dynamic;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

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

        // --- Tags (Broad categories) ---
        tag(ItemTags.LOGS, ModAspects.WOOD);
        tag(ItemTags.PLANKS, ModAspects.WOOD);
        tag(ItemTags.LEAVES, ModAspects.WOOD, ModAspects.WATER);
        tag(ItemTags.SAPLINGS, ModAspects.GROWTH, ModAspects.WOOD);
        tag(ItemTags.WOOL, ModAspects.VITALITY, ModAspects.WOOD);
        
        // NeoForge Common Tags
        tag(c("ores"), ModAspects.EARTH, ModAspects.METAL);
        tag(c("ingots"), ModAspects.METAL);
        tag(c("dusts"), ModAspects.EARTH);
        tag(c("gems"), ModAspects.CRYSTAL);
        tag(c("raw_materials"), ModAspects.EARTH, ModAspects.METAL);
        tag(c("crops"), ModAspects.GROWTH, ModAspects.VITALITY);
    }

    private static TagKey<Item> c(String path) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM, 
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", path));
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
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        List<Aspect> aspects = new ArrayList<>(FIXED_ATOMS.getOrDefault(id, List.of()));

        // 1. Tag Check
        if (aspects.isEmpty()) {
            for (var entry : TAG_ATOMS.entrySet()) {
                if (item.builtInRegistryHolder().is(entry.getKey())) {
                    aspects.addAll(entry.getValue());
                    break;
                }
            }
        }

        // 2. Heuristic Check (Class-based detection for "All Items")
        if (aspects.isEmpty()) {
            aspects.addAll(getHeuristicAspects(item));
        }

        if (aspects.isEmpty()) return List.of();

        // Apply Seed-Based Perturbation
        return perturb(aspects, item, seed);
    }

    private static List<Aspect> getHeuristicAspects(Item item) {
        List<Aspect> h = new ArrayList<>();
        // Food items always have Vitality
        if (item.getComponents().has(net.minecraft.core.component.DataComponents.FOOD)) {
            h.add(ModAspects.VITALITY);
        }
        // Tools/Armor heuristics
        if (item instanceof net.minecraft.world.item.TieredItem tiered) {
            h.add(ModAspects.METAL);
            if (tiered.getTier() == net.minecraft.world.item.Tiers.DIAMOND) h.add(ModAspects.CRYSTAL);
        }
        if (item instanceof net.minecraft.world.item.ArmorItem armor) {
            h.add(ModAspects.EARTH);
            if (armor.getMaterial().value().commonArmorTagName().isPresent()) h.add(ModAspects.METAL);
        }
        // Enchanted books
        if (item == Items.ENCHANTED_BOOK) {
            h.add(ModAspects.MANA);
            h.add(ModAspects.ANIMA);
        }
        return h;
    }

    private static List<Aspect> perturb(List<Aspect> base, Item item, long seed) {
        Random random = new Random(seed ^ BuiltInRegistries.ITEM.getKey(item).hashCode());
        // 20% chance to add a minor secondary aspect from a "logical" pool
        if (random.nextFloat() < 0.2f) {
            List<Aspect> pool = getLogicalPool(base);
            if (!pool.isEmpty()) {
                Aspect extra = pool.get(random.nextInt(pool.size()));
                if (!base.contains(extra)) {
                    List<Aspect> perturbed = new ArrayList<>(base);
                    perturbed.add(extra);
                    return perturbed;
                }
            }
        }
        return base;
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
        }
        return new ArrayList<>(pool);
    }
}
