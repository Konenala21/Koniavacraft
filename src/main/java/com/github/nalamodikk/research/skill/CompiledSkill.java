package com.github.nalamodikk.research.skill;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Consumer;

/**
 * A {@link SkillEffect} produced by {@link SkillCompiler} from an aspect combination.
 *
 * The cost is the aspects the combination consumes; the action runs the compiled
 * delivery (carrier) + payloads (effects) + tweaks (modifiers).
 */
public record CompiledSkill(Map<ResourceLocation, Integer> cost,
                            Consumer<SkillContext> action) implements SkillEffect {

    @Override
    public void execute(SkillContext ctx) {
        action.accept(ctx);
    }
}
