package com.github.nalamodikk.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * 迷盲：每 tick 清掉怪物的攻擊目標，讓它在持續時間內無法鎖定你 = 真的「看不到」。
 *
 * vanilla 失明（BLINDNESS）只影響玩家視野，對怪物 AI 完全沒用，怪照樣攻擊。
 * 這個效果直接讓 Mob 丟失目標，致盲才真的有控場意義。
 */
public class DazeEffect extends MobEffect {

    public DazeEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // 每 tick 清目標
    }
}
