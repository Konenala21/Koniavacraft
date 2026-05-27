package com.github.nalamodikk.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * 定身：每 tick 歸零水平移動（允許下墜），並停止生物導航。
 * 對生物為硬定身；對玩家因客戶端預測僅能軟性限制（會有橡皮筋）。
 * 衝刺/多段跳等裝備技能另外在各自封包入口檢查此效果擋下。
 */
public class RootMobEffect extends MobEffect {

    public RootMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Vec3 dm = entity.getDeltaMovement();
        entity.setDeltaMovement(0.0, Math.min(dm.y, 0.0), 0.0);
        entity.hurtMarked = true;
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
