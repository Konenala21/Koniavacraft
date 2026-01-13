package com.github.nalamodikk.particle.utils.math;

import net.minecraft.world.phys.Vec3;

/**
 * 物理工具類
 * 提供碰撞、彈跳等物理計算
 */
public class PhysicsUtil {

    /** 標準重力常數 */
    public static final double STANDARD_GRAVITY = 0.04;

    /** 空氣阻力係數 */
    public static final double AIR_RESISTANCE = 0.98;

    /**
     * 計算彈跳後的速度
     * @param velocity 入射速度
     * @param normal 碰撞面法線（已歸一化）
     * @param restitution 恢復係數（0-1，1表示完全彈性）
     * @return 彈跳後的速度
     */
    public static Vec3 bounce(Vec3 velocity, Vec3 normal, double restitution) {
        double dotProduct = velocity.dot(normal);
        Vec3 reflection = velocity.subtract(normal.scale(2 * dotProduct));
        return reflection.scale(restitution);
    }

    /**
     * 計算摩擦力影響後的速度
     * @param velocity 當前速度
     * @param friction 摩擦係數（0-1）
     * @return 摩擦後的速度
     */
    public static Vec3 applyFriction(Vec3 velocity, double friction) {
        return velocity.scale(1.0 - friction);
    }

    /**
     * 應用重力
     * @param velocity 當前速度
     * @param gravity 重力加速度
     * @return 應用重力後的速度
     */
    public static Vec3 applyGravity(Vec3 velocity, double gravity) {
        return velocity.add(0, -gravity, 0);
    }

    /**
     * 應用空氣阻力
     * @param velocity 當前速度
     * @param resistance 阻力係數（0-1）
     * @return 應用阻力後的速度
     */
    public static Vec3 applyAirResistance(Vec3 velocity, double resistance) {
        return velocity.scale(resistance);
    }

    /**
     * 檢查球體碰撞
     * @param pos1 球體1位置
     * @param radius1 球體1半徑
     * @param pos2 球體2位置
     * @param radius2 球體2半徑
     * @return 是否碰撞
     */
    public static boolean checkSphereCollision(Vec3 pos1, double radius1, Vec3 pos2, double radius2) {
        double minDist = radius1 + radius2;
        return pos1.distanceToSqr(pos2) <= minDist * minDist;
    }

    /**
     * 計算球體碰撞後的速度（彈性碰撞）
     * @param vel1 球體1速度
     * @param mass1 球體1質量
     * @param vel2 球體2速度
     * @param mass2 球體2質量
     * @param normal 碰撞法線
     * @return 球體1碰撞後的速度
     */
    public static Vec3 elasticCollision(Vec3 vel1, double mass1, Vec3 vel2, double mass2, Vec3 normal) {
        double v1n = vel1.dot(normal);
        double v2n = vel2.dot(normal);
        
        double v1nAfter = ((mass1 - mass2) * v1n + 2 * mass2 * v2n) / (mass1 + mass2);
        
        Vec3 v1nVec = normal.scale(v1n);
        Vec3 v1t = vel1.subtract(v1nVec);
        
        return v1t.add(normal.scale(v1nAfter));
    }

    /**
     * 計算向心力
     * @param velocity 速度
     * @param radius 半徑
     * @param mass 質量
     * @return 向心力
     */
    public static double centripetalForce(double velocity, double radius, double mass) {
        return (mass * velocity * velocity) / radius;
    }

    /**
     * 計算終端速度（考慮空氣阻力）
     * @param gravity 重力
     * @param dragCoefficient 阻力係數
     * @return 終端速度
     */
    public static double terminalVelocity(double gravity, double dragCoefficient) {
        return Math.sqrt(gravity / dragCoefficient);
    }

    /**
     * 拋物線運動位置計算
     * @param initialPos 初始位置
     * @param initialVel 初始速度
     * @param gravity 重力
     * @param time 時間
     * @return 當前位置
     */
    public static Vec3 projectileMotion(Vec3 initialPos, Vec3 initialVel, double gravity, double time) {
        return new Vec3(
            initialPos.x + initialVel.x * time,
            initialPos.y + initialVel.y * time - 0.5 * gravity * time * time,
            initialPos.z + initialVel.z * time
        );
    }

    /**
     * 計算達到目標需要的初速度（拋物線運動）
     * @param start 起點
     * @param target 目標點
     * @param gravity 重力
     * @param angle 發射角度（弧度）
     * @return 初速度向量
     */
    public static Vec3 calculateLaunchVelocity(Vec3 start, Vec3 target, double gravity, double angle) {
        Vec3 displacement = target.subtract(start);
        double horizontalDist = Math.sqrt(displacement.x * displacement.x + displacement.z * displacement.z);
        double verticalDist = displacement.y;
        
        double tanAngle = Math.tan(angle);
        double cosAngle = Math.cos(angle);
        
        double speed = Math.sqrt(
            (gravity * horizontalDist * horizontalDist) / 
            (2 * cosAngle * cosAngle * (horizontalDist * tanAngle - verticalDist))
        );
        
        Vec3 direction = new Vec3(displacement.x, 0, displacement.z).normalize();
        Vec3 horizontalVel = direction.scale(speed * Math.cos(angle));
        
        return new Vec3(horizontalVel.x, speed * Math.sin(angle), horizontalVel.z);
    }
}
