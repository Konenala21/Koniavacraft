package com.github.nalamodikk.research.skill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side data handed to a {@link SkillEffect} when it executes.
 *
 * Keep this minimal: an effect can derive aim direction, eye position, etc.
 * from the caster. Add fields here only when a real effect needs them.
 */
public record SkillContext(ServerLevel level, ServerPlayer caster) {
}
