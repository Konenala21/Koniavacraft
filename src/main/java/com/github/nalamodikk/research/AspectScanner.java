package com.github.nalamodikk.research;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps block IDs to the aspects they contain, used by the Nara Watch scan mechanic.
 *
 * Primary aspects (Water/Fire/Wood/Metal/Earth/Wu) are always known; compound
 * aspects (Mana/Energy/Crystal/Resonance) must be discovered by scanning.
 */
public final class AspectScanner {

    private static final Map<ResourceLocation, List<Aspect>> BLOCK_MAP = new HashMap<>();

    static {
        // ── Vanilla — mostly primary aspects ──────────────────────────────────
        b("minecraft:water",                  ModAspects.WATER);
        b("minecraft:flowing_water",           ModAspects.WATER);
        b("minecraft:ice",                     ModAspects.WATER, ModAspects.EARTH);
        b("minecraft:packed_ice",              ModAspects.WATER, ModAspects.EARTH);
        b("minecraft:blue_ice",               ModAspects.WATER, ModAspects.EARTH);
        b("minecraft:snow_block",              ModAspects.WATER, ModAspects.EARTH);
        b("minecraft:lava",                    ModAspects.FIRE,  ModAspects.EARTH);
        b("minecraft:fire",                    ModAspects.FIRE);
        b("minecraft:magma_block",             ModAspects.FIRE,  ModAspects.EARTH);
        b("minecraft:grass_block",             ModAspects.WOOD,  ModAspects.EARTH);
        b("minecraft:dirt",                    ModAspects.EARTH);
        b("minecraft:gravel",                  ModAspects.EARTH);
        b("minecraft:sand",                    ModAspects.EARTH);
        b("minecraft:red_sand",                ModAspects.EARTH);
        b("minecraft:stone",                   ModAspects.EARTH, ModAspects.METAL);
        b("minecraft:deepslate",               ModAspects.EARTH, ModAspects.METAL);
        b("minecraft:cobblestone",             ModAspects.EARTH);
        b("minecraft:oak_log",                 ModAspects.WOOD);
        b("minecraft:birch_log",               ModAspects.WOOD);
        b("minecraft:spruce_log",              ModAspects.WOOD);
        b("minecraft:jungle_log",              ModAspects.WOOD);
        b("minecraft:acacia_log",              ModAspects.WOOD);
        b("minecraft:dark_oak_log",            ModAspects.WOOD);
        b("minecraft:mangrove_log",            ModAspects.WOOD);
        b("minecraft:cherry_log",              ModAspects.WOOD);
        b("minecraft:iron_ore",                ModAspects.EARTH, ModAspects.METAL);
        b("minecraft:deepslate_iron_ore",      ModAspects.EARTH, ModAspects.METAL);
        b("minecraft:gold_ore",                ModAspects.EARTH, ModAspects.METAL);
        b("minecraft:deepslate_gold_ore",      ModAspects.EARTH, ModAspects.METAL);
        b("minecraft:diamond_ore",             ModAspects.EARTH, ModAspects.METAL);
        b("minecraft:deepslate_diamond_ore",   ModAspects.EARTH, ModAspects.METAL);
        b("minecraft:coal_ore",                ModAspects.EARTH, ModAspects.FIRE);
        b("minecraft:deepslate_coal_ore",      ModAspects.EARTH, ModAspects.FIRE);
        b("minecraft:iron_block",              ModAspects.METAL);
        b("minecraft:gold_block",              ModAspects.METAL);
        b("minecraft:diamond_block",           ModAspects.METAL);
        b("minecraft:glowstone",               ModAspects.FIRE,  ModAspects.WU);
        b("minecraft:obsidian",                ModAspects.EARTH, ModAspects.FIRE);
        b("minecraft:amethyst_block",          ModAspects.METAL, ModAspects.WU);
        b("minecraft:amethyst_cluster",        ModAspects.METAL, ModAspects.WU);
        b("minecraft:budding_amethyst",        ModAspects.METAL, ModAspects.WU);
        b("minecraft:sculk",                   ModAspects.EARTH, ModAspects.WU);
        b("minecraft:sculk_sensor",            ModAspects.EARTH, ModAspects.WU);
        b("minecraft:end_stone",               ModAspects.EARTH, ModAspects.WU);
        b("minecraft:netherrack",              ModAspects.FIRE,  ModAspects.EARTH);

        // ── Koniava — reveals compound aspects ────────────────────────────────
        // Scanning these blocks is HOW players discover compound aspects.
        b("koniava:magic_ore",                 ModAspects.MANA,  ModAspects.EARTH);
        b("koniava:deepslate_magic_ore",       ModAspects.MANA,  ModAspects.EARTH);
        b("koniava:mana_block",                ModAspects.MANA,  ModAspects.METAL);
        b("koniava:mana_generator",            ModAspects.ENERGY, ModAspects.FIRE);
        b("koniava:solar_mana_collector",      ModAspects.ENERGY, ModAspects.FIRE);
        b("koniava:mana_grinder",              ModAspects.CRYSTAL, ModAspects.METAL);
        b("koniava:mana_crafting_table",       ModAspects.CRYSTAL, ModAspects.RESONANCE);
        b("koniava:basic_arcane_conduit",      ModAspects.MANA,  ModAspects.WU);
        b("koniava:advanced_arcane_conduit",   ModAspects.MANA,  ModAspects.RESONANCE);
        b("koniava:elite_arcane_conduit",      ModAspects.MANA,  ModAspects.CRYSTAL);
        b("koniava:research_table",            ModAspects.WU,    ModAspects.MANA);
    }

    private static void b(String id, Aspect... aspects) {
        BLOCK_MAP.put(ResourceLocation.parse(id), List.of(aspects));
    }

    /** Returns the aspects associated with the given block state, or an empty list if unknown. */
    public static List<Aspect> getAspectsFor(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return BLOCK_MAP.getOrDefault(id, List.of());
    }

    private AspectScanner() {}
}
