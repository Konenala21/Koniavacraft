package com.github.nalamodikk.research.skill;

import java.util.function.Consumer;

/**
 * A {@link SkillEffect} produced by {@link SkillCompiler} from an aspect combination.
 *
 * The cost is the aspect gate the combination requires plus its mana price; the
 * action runs the compiled delivery (carrier) + payloads (effects) + tweaks
 * (modifiers).
 */
public record CompiledSkill(SkillCost cost,
                            Consumer<SkillContext> action) implements SkillEffect {

    @Override
    public void execute(SkillContext ctx) {
        action.accept(ctx);
    }
}
