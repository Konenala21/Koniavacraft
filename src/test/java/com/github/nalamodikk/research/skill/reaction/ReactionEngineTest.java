package com.github.nalamodikk.research.skill.reaction;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.skill.SkillEffectOp;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic tests for the chemistry reaction engine. No Minecraft context:
 * ModAspects are plain objects and ReactionEngine.run never executes an op's
 * apply() (it only collects the ops + chain count). These lock in the trait-rule
 * behavior and the SDR self-trigger guard, and would have caught the
 * Energy+Arcana -> Overload regression that code-review found.
 */
class ReactionEngineTest {

    private static List<SkillEffectOp> ops(Aspect... effects) {
        return ReactionEngine.run(List.of(effects)).ops();
    }

    private static int chain(Aspect... effects) {
        return ReactionEngine.run(List.of(effects)).chain();
    }

    @Test
    void basicTwoTraitReactionFires() {
        // fire + cold = thermal shock
        assertTrue(ops(ModAspects.PHLOGISTON, ModAspects.FROST).contains(SkillEffectOp.THERMAL_SHOCK));
    }

    @Test
    void overloadFiresForTwoArcaneSources() {
        // regression guard: Energy + Arcana are both pure ARCANE; the engine must
        // still overload them (the old ARCANE+METAL-only rule silently dropped this).
        assertTrue(ops(ModAspects.ENERGY, ModAspects.ARCANA).contains(SkillEffectOp.OVERLOAD),
                "Energy + Arcana should overload");
        // Energy + Crystal (crystal carries METAL) also overloads.
        assertTrue(ops(ModAspects.ENERGY, ModAspects.CRYSTAL).contains(SkillEffectOp.OVERLOAD),
                "Energy + Crystal should overload");
    }

    @Test
    void singleAspectProducesNoReaction() {
        // run() needs >= 2 distinct sources; one aspect alone reacts with nothing.
        assertTrue(ops(ModAspects.PHLOGISTON).isEmpty());
        assertTrue(ops(ModAspects.MAGMA).isEmpty());
    }

    @Test
    void sdrGuardPreventsSingleMultiTraitSelfTrigger() {
        // Magma carries BOTH heat and force, but Eruption (heat+force) must NOT fire
        // from magma alone — it needs two distinct sources. Frost provides neither
        // heat nor force, so Eruption stays off while Thermal Shock (heat+cold) fires.
        List<SkillEffectOp> result = ops(ModAspects.MAGMA, ModAspects.FROST);
        assertTrue(result.contains(SkillEffectOp.THERMAL_SHOCK), "magma(heat) + frost(cold) -> thermal shock");
        assertFalse(result.contains(SkillEffectOp.ERUPTION), "magma's own heat+force must not self-trigger eruption");
    }

    @Test
    void productCascadeReachesSuperheatedPlasma() {
        // fire+cold -> thermal shock (+steam); steam+electric -> superheated plasma.
        assertTrue(ops(ModAspects.PHLOGISTON, ModAspects.FROST, ModAspects.ZHEN)
                .contains(SkillEffectOp.SUPERHEATED_PLASMA), "steam product should cascade into plasma");
    }

    @Test
    void corrodeCascadeReachesHydrogenBlast() {
        // acid+metal -> corrode (+volatile); volatile+heat -> hydrogen blast.
        assertTrue(ops(ModAspects.CORROSION, ModAspects.CRYSTAL, ModAspects.PHLOGISTON)
                .contains(SkillEffectOp.HYDROGEN_BLAST), "volatile product should cascade into hydrogen blast");
    }

    @Test
    void conductionAddsChainJumps() {
        // electric + water = conduction (+2 chain), no op.
        assertTrue(chain(ModAspects.ZHEN, ModAspects.KAN) >= 2, "conduction should add chain jumps");
    }

    @Test
    void maelstromFiresFromForceWaterWater() {
        // storm(force) + kan(water+force) + dui(water+force) satisfies FORCE+WATER+WATER.
        assertTrue(ops(ModAspects.STORM, ModAspects.KAN, ModAspects.DUI).contains(SkillEffectOp.MAELSTROM));
    }
}
