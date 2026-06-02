package com.github.nalamodikk.research.skill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * 技能命中/視覺共用的小工具,避免每個效果 op 與載體 cast 方法重抄同一段 boilerplate。
 */
public final class SkillTargeting {

    private SkillTargeting() {}

    private static final Vec3 DEFAULT_DIR = new Vec3(0, 0, 1);

    /** 方向正規化;近乎零向量時退回固定方向,避免 NaN。 */
    public static Vec3 safeDir(Vec3 dir) {
        return dir.lengthSqr() < 1.0E-4 ? DEFAULT_DIR : dir.normalize();
    }

    /** 對 box 內所有「不是施放者、還活著」的生物執行 action(範圍效果共用)。 */
    public static void forEachEnemy(ServerLevel level, AABB box, @Nullable LivingEntity caster,
                                    Consumer<LivingEntity> action) {
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive())) {
            action.accept(le);
        }
    }
}
