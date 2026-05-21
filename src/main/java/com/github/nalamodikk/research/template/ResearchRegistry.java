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

    /** Innate research granted automatically when the player first binds Nara. Unlocks the Research Table. */
    public static final ResearchTemplate RESEARCH_BASICS = register(
            ResearchTemplate.builder(id("research_basics"))
                    .tier(1)
                    .innate()
                    .unlocks(id("research_table"))
                    .build()
    );

    /** First research: player scans mana grass and water, discovers mana flows. */
    public static final ResearchTemplate MANA_BASICS = register(
            ResearchTemplate.builder(id("mana_basics"))
                    .tier(1)
                    .aspects(ModAspects.WATER, ModAspects.MANA)
                    .holeRatio(0.20)
                    .prerequisites(id("research_basics"))
                    .unlocks(id("nara_watch"))
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
                    .unlocks(id("basic_arcane_conduit"))
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

    /**
     * Tools: player learns to forge mana-infused tools and the basic tech wand.
     * Blade↔Mechanism (both involve shaping metal), Crystal provides the mana conduit.
     */
    public static final ResearchTemplate MANA_TOOLS = register(
            ResearchTemplate.builder(id("mana_tools"))
                    .tier(1)
                    .aspects(ModAspects.BLADE, ModAspects.MECHANISM, ModAspects.CRYSTAL)
                    .holeRatio(0.30)
                    .prerequisites(id("mana_crystallisation"))
                    .unlocks(id("basic_tech_wand"), id("mana_pickaxe"))
                    .build()
    );

    // ── Tier 1 continued — 魔法期完整 ────────────────────────────────────────

    /**
     * Mana infusion: player learns to infuse items with mana.
     * Vitality↔Lifeflow (Vitality∈Lifeflow), Lifeflow↔Mana (Mana∈Lifeflow).
     */
    public static final ResearchTemplate MANA_INFUSION = register(
            ResearchTemplate.builder(id("mana_infusion"))
                    .tier(1)
                    .aspects(ModAspects.VITALITY, ModAspects.LIFEFLOW, ModAspects.MANA)
                    .holeRatio(0.32)
                    .prerequisites(id("mana_basics"), id("mana_crystallisation"))
                    .unlocks(id("mana_infuser"))
                    .build()
    );

    /**
     * Advanced conduits: higher bandwidth mana transport.
     * Mechanism↔Momentum (shared Metal component), Momentum↔Energy (shared WU component).
     */
    public static final ResearchTemplate ADVANCED_CONDUITS = register(
            ResearchTemplate.builder(id("advanced_conduits"))
                    .tier(1)
                    .aspects(ModAspects.MECHANISM, ModAspects.MOMENTUM, ModAspects.ENERGY)
                    .holeRatio(0.33)
                    .prerequisites(id("mana_flow"), id("mana_crystallisation"))
                    .unlocks(id("advanced_arcane_conduit"), id("elite_arcane_conduit"))
                    .build()
    );

    /**
     * Mana Deployer: mana-powered dispenser for automation.
     * Mechanism↔Momentum (both involve directed force), Momentum↔Mana (shared WU component).
     */
    public static final ResearchTemplate MANA_DEPLOYER = register(
            ResearchTemplate.builder(id("mana_deployer"))
                    .tier(1)
                    .aspects(ModAspects.MECHANISM, ModAspects.MOMENTUM, ModAspects.MANA)
                    .holeRatio(0.30)
                    .prerequisites(id("mana_flow"))
                    .unlocks(id("mana_deployer"))
                    .build()
    );

    /**
     * Altar construction: player learns to build the Aspect Altar multiblock.
     * Resonance↔Crystal (both involve structured mana), Crystal↔Mana (shared component).
     */
    public static final ResearchTemplate ASPECT_ALTAR = register(
            ResearchTemplate.builder(id("aspect_altar"))
                    .tier(1)
                    .aspects(ModAspects.RESONANCE, ModAspects.CRYSTAL, ModAspects.MANA)
                    .holeRatio(0.35)
                    .prerequisites(id("mana_crystallisation"))
                    .unlocks(id("aspect_altar"), id("aspect_pedestal"))
                    .build()
    );

    /**
     * Altar ring upgrade: player studies the resonance ring upgrade structure.
     * Resonance↔Mechanism (rings are structured resonance amplifiers), Mechanism↔Crystal (mechanical crystal mounts).
     */
    public static final ResearchTemplate ALTAR_RING_UPGRADE = register(
            ResearchTemplate.builder(id("altar_ring_upgrade"))
                    .tier(1)
                    .aspects(ModAspects.RESONANCE, ModAspects.MECHANISM, ModAspects.CRYSTAL)
                    .holeRatio(0.33)
                    .prerequisites(id("aspect_altar"))
                    .build()
    );

    // ── Tier 2 — 魔法期深化 ──────────────────────────────────────────────────

    /**
     * Mana weapons: player learns to forge mana-powered weapons.
     * Blade↔Mechanism (weapon construction), Mechanism↔Momentum (directed force).
     * Requires both altar knowledge and understanding of ring upgrade structures.
     */
    public static final ResearchTemplate MANA_WEAPONS = register(
            ResearchTemplate.builder(id("mana_weapons"))
                    .tier(2)
                    .aspects(ModAspects.BLADE, ModAspects.MECHANISM, ModAspects.MOMENTUM)
                    .holeRatio(0.38)
                    .prerequisites(id("aspect_altar"), id("altar_ring_upgrade"))
                    .unlocks(id("floating_turret"))
                    .build()
    );

    // Tier 3–4 entries to be added as content expands.
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
