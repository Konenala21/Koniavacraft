package com.github.nalamodikk.common.entity.control;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * 浮游砲控制彈規則中心：boss 控制抗性 + 同種控制不疊加。
 */
public final class TurretControlHelper {

    private TurretControlHelper() {}

    /** 標記此 tag 的實體（所有 boss，含原版與模組）擁有控制抗性。 */
    public static final TagKey<EntityType<?>> CONTROL_RESISTANT =
            TagKey.create(Registries.ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "control_resistant"));

    /** boss 控制抗性：控制效果時間打折的比例。 */
    public static final float BOSS_RESISTANCE = 0.5f;

    public static boolean isResistant(LivingEntity entity) {
        return entity.getType().is(CONTROL_RESISTANT);
    }

    /**
     * 套用控制效果。同種效果已存在時不套用；boss 的控制時間依抗性打折（不免疫）。
     * @return 是否成功套用
     */
    public static boolean applyControl(LivingEntity target, Holder<MobEffect> effect, int durationTicks) {
        if (target.hasEffect(effect)) return false; // 同類不疊加 / 不刷新延長
        int dur = durationTicks;
        if (isResistant(target)) {
            dur = Math.max(1, Math.round(durationTicks * (1f - BOSS_RESISTANCE)));
        }
        target.addEffect(new MobEffectInstance(effect, dur, 0, false, true, true));
        return true;
    }

    /**
     * 技能控制效果:跟控制彈共用 boss 抗性(時間打折,不免疫),但允許刷新/帶等級
     * (技能可連續施放,不做「同種不疊加」)。給技能的定身/迷盲/冰緩用。
     */
    public static void applySkillControl(LivingEntity target, Holder<MobEffect> effect, int durationTicks, int amplifier) {
        int dur = durationTicks;
        if (isResistant(target)) {
            dur = Math.max(1, Math.round(durationTicks * (1f - BOSS_RESISTANCE)));
        }
        target.addEffect(new MobEffectInstance(effect, dur, amplifier, false, true, true));
    }
}
