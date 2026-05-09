package com.github.nalamodikk.research.template;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Central registry for all {@link ResearchTemplate} entries.
 *
 * Entries are registered in code here. Future versions can supplement
 * or override these with JSON datapacks via a reload listener.
 *
 * Tier 1–4  : 魔法期 — player thinks they are learning magic
 * Tier 5–8  : 過渡期 — cracks appear in the magical worldview
 * Tier 9–12 : 科技期 — mana is revealed as primitive energy
 */
public final class ResearchRegistry {

    private static final Map<ResourceLocation, ResearchTemplate> REGISTRY = new LinkedHashMap<>();

    // ── Tier 1 — 魔法期入門 ──────────────────────────────────────────────────

    /** First research: player scans mana grass and water, discovers mana flows. */
    public static final ResearchTemplate MANA_BASICS = register(
            ResearchTemplate.builder(id("mana_basics"))
                    .tier(1)
                    .aspects(ModAspects.WATER, ModAspects.MANA)
                    .holeRatio(0.20)
                    .unlocks(id("mana_scanner"))
                    .build()
    );

    /**
     * Second: player studies how mana moves.
     * WU + Water → both are components of Mana, so WU↔Mana and Water↔Mana all connect.
     */
    public static final ResearchTemplate MANA_FLOW = register(
            ResearchTemplate.builder(id("mana_flow"))
                    .tier(1)
                    .aspects(ModAspects.WU, ModAspects.WATER, ModAspects.MANA)
                    .holeRatio(0.25)
                    .prerequisites(id("mana_basics"))
                    .unlocks(id("arcane_conduit"))
                    .build()
    );

    /**
     * Third: player learns to generate mana from fuel.
     * Fire↔Energy (Fire∈Energy), Energy↔Mana (shared WU component).
     */
    public static final ResearchTemplate MANA_GENERATION = register(
            ResearchTemplate.builder(id("mana_generation"))
                    .tier(1)
                    .aspects(ModAspects.FIRE, ModAspects.ENERGY, ModAspects.MANA)
                    .holeRatio(0.28)
                    .prerequisites(id("mana_flow"))
                    .unlocks(id("mana_generator"), id("solar_mana_collector"))
                    .build()
    );

    /**
     * Fourth: player studies mana crystals — tier 1 capstone.
     * Crystal↔Resonance (shared Metal component), Resonance↔Mana (shared WU component).
     */
    public static final ResearchTemplate MANA_CRYSTALLISATION = register(
            ResearchTemplate.builder(id("mana_crystallisation"))
                    .tier(1)
                    .aspects(ModAspects.CRYSTAL, ModAspects.RESONANCE, ModAspects.MANA)
                    .holeRatio(0.30)
                    .prerequisites(id("mana_generation"))
                    .unlocks(id("mana_grinder"), id("mana_crafting_table"))
                    .build()
    );

    // ── Tier 2 — 魔法期深化（placeholder，後續補充）────────────────────────────

    // Tier 2–4 entries to be added as content expands.
    // They follow the same pattern: aspects get more varied, holeRatio increases (0.30–0.45).

    // ── Registry utilities ───────────────────────────────────────────────────

    private static ResearchTemplate register(ResearchTemplate template) {
        REGISTRY.put(template.getId(), template);
        return template;
    }

    public static Optional<ResearchTemplate> get(ResourceLocation id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static Collection<ResearchTemplate> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static List<ResearchTemplate> allForTier(int tier) {
        return REGISTRY.values().stream()
                .filter(t -> t.getTier() == tier)
                .toList();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, path);
    }

    private ResearchRegistry() {}
}
