package com.github.nalamodikk.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 冰緩：每 tick 把水平速度乘上一個係數,而不是只降「移動速度屬性」。
 *
 * vanilla 緩速只改 MOVEMENT_SPEED 屬性,對用飛行/自訂移動的怪(終界龍、惡魂、
 * 夜魅、凋零等)沒效果。冰緩直接縮放實際速度,所以連飛行怪都會被拖慢,但不像
 * 定身完全歸零(保留垂直速度,讓它還能浮動/掉落)。
 *
 * 強度依 amplifier:amp0 ×0.6、amp1 ×0.45、amp2 ×0.3、amp3 ×0.15(最低 ×0.15)。
 */
public class ChillEffect extends MobEffect {

    public ChillEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        double factor = Math.max(0.15, 0.6 - amplifier * 0.15);
        Vec3 dm = entity.getDeltaMovement();
        entity.setDeltaMovement(dm.x * factor, dm.y, dm.z * factor);
        entity.hurtMarked = true; // 同步給 client,避免橡皮筋
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // 每 tick 縮放
    }
}
