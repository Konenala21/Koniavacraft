package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 環繞運動助手
 * 基於 MovementHelper 提供的環繞運動功能的快捷方式
 */
public class OrbitHelper {

    /**
     * 環繞運動（委託給 MovementHelper）
     * @param particle 粒子
     * @param center 中心點
     * @param radius 半徑
     * @param speed 角速度（每 tick 的弧度）
     * @return 任務 UUID
     */
    public static UUID orbit(ControlableParticle particle, Vec3 center, float radius, float speed) {
        return MovementHelper.orbit(particle, center, radius, speed);
    }

    /**
     * 快速環繞
     * @param particle 粒子
     * @param center 中心點
     * @param radius 半徑
     * @return 任務 UUID
     */
    public static UUID fastOrbit(ControlableParticle particle, Vec3 center, float radius) {
        return MovementHelper.orbit(particle, center, radius, (float) (Math.PI / 10)); // 18 度/tick
    }

    /**
     * 慢速環繞
     * @param particle 粒子
     * @param center 中心點
     * @param radius 半徑
     * @return 任務 UUID
     */
    public static UUID slowOrbit(ControlableParticle particle, Vec3 center, float radius) {
        return MovementHelper.orbit(particle, center, radius, (float) (Math.PI / 90)); // 2 度/tick
    }

    /**
     * 逆時針環繞
     * @param particle 粒子
     * @param center 中心點
     * @param radius 半徑
     * @param speed 角速度（正值）
     * @return 任務 UUID
     */
    public static UUID counterClockwiseOrbit(ControlableParticle particle, Vec3 center, float radius, float speed) {
        return MovementHelper.orbit(particle, center, radius, -speed); // 負速度 = 逆時針
    }

    /**
     * 橢圓環繞
     * @param particle 粒子
     * @param center 中心點
     * @param radiusX X 軸半徑
     * @param radiusZ Z 軸半徑
     * @param speed 角速度
     * @return 任務 UUID
     */
    public static UUID ellipticalOrbit(ControlableParticle particle, Vec3 center, float radiusX, float radiusZ, float speed) {
        // 這個需要自定義實現，暫時使用圓形軌道
        return MovementHelper.orbit(particle, center, (radiusX + radiusZ) / 2, speed);
    }
}
