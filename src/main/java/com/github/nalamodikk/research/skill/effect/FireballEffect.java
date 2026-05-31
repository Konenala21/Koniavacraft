package com.github.nalamodikk.research.skill.effect;

import com.github.nalamodikk.common.entity.SpellProjectileEntity;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.skill.SkillContext;
import com.github.nalamodikk.research.skill.SkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.Map;

/**
 * Demo skill: launch a fireball.
 *
 * Cost is hardcoded as fire 1 + water 3 to show per-aspect amounts working.
 * Both are primary aspects (players start with 5 of each), so this is castable
 * without scanning anything first: good for testing the pipeline.
 */
public class FireballEffect implements SkillEffect {

    @Override
    public Map<ResourceLocation, Integer> cost() {
        return Map.of(
                ModAspects.FIRE.getId(), 1,
                ModAspects.WATER.getId(), 3
        );
    }

    @Override
    public void execute(SkillContext ctx) {
        ServerPlayer caster = ctx.caster();
        SpellProjectileEntity fireball = SpellProjectileEntity.shoot(ctx.level(), caster, 6.0F, 4);
        ctx.level().addFreshEntity(fireball);

        ctx.level().playSound(null, caster.blockPosition(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.4F);
    }
}
