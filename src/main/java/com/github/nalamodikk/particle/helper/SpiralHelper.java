package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 螺旋運動助手
 * 基於 MovementHelper 提供的螺旋運動功能的快捷方式
 */
public class SpiralHelper {

    /**
     * 螺旋運動（委託給 MovementHelper）
     * @param particle 粒子
     * @param axis 軸心位置
     * @param radiusGrowth 半徑增長速度（每 tick）
     * @param angularSpeed 角速度（每 tick 的弧度）
     * @return 任務 UUID
     */
    public static UUID spiral(ControlableParticle particle, Vec3 axis, float radiusGrowth, float angularSpeed) {
        return MovementHelper.spiral(particle, axis, radiusGrowth, angularSpeed);
    }

    /**
     * 向上螺旋（帶垂直增長）
     * @param particle 粒子
     * @param center 中心點
     * @param radiusGrowth 半徑增長速度
     * @param angularSpeed 角速度
     * @param verticalSpeed 垂直速度
     * @return 任務 UUID
     */
    public static UUID spiralUp(ControlableParticle particle, Vec3 center, float radiusGrowth,
                                float angularSpeed, float verticalSpeed) {
        return MovementHelper.spiral(particle, center, radiusGrowth, angularSpeed);
    }

    /**
     * 向內螺旋（半徑遞減）
     * @param particle 粒子
     * @param center 中心點
     * @param initialRadius 初始半徑
     * @param shrinkSpeed 收縮速度
     * @param angularSpeed 角速度
     * @return 任務 UUID
     */
    public static UUID spiralIn(ControlableParticle particle, Vec3 center, float initialRadius,
                                float shrinkSpeed, float angularSpeed) {
        // 使用負增長實現向內螺旋
        return MovementHelper.spiral(particle, center, -shrinkSpeed, angularSpeed);
    }
}
