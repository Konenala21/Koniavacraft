package com.github.nalamodikk.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 靈焰：靈魂燃燒。比流血更快、更重的魔法 DoT(無視護甲),用於暗+火的「煉獄火」反應。
 */
public class SoulburnEffect extends MobEffect {

    public SoulburnEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.invulnerableTime = 0;
        entity.hurt(entity.damageSources().magic(), 2.0F + amplifier);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 15 == 0; // 比流血快
    }
}
