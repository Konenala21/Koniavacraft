package com.github.nalamodikk.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 流血：每秒造成一次無視護甲的真實傷害(用魔法傷害繞過護甲),持續到效果結束。
 * 比中毒更狠的是它能把目標打死(中毒不會),且不被護甲擋。獸性效果與部分反應用它。
 */
public class BleedEffect extends MobEffect {

    public BleedEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.invulnerableTime = 0;
        entity.hurt(entity.damageSources().magic(), 1.0F + amplifier);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0; // 每秒一次
    }
}
