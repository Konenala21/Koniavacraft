package com.github.nalamodikk.particle.helper;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.scheduler.CooScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * 碰撞檢測助手
 * 提供粒子與環境的碰撞檢測和處理
 */
public class CollisionHelper {

    /**
     * 檢測方塊碰撞
     * @param particle 粒子
     * @param level 世界
     * @param onCollision 碰撞回調
     * @return 任務 UUID
     */
    public static UUID detectBlockCollision(ControlableParticle particle, Level level, Consumer<BlockPos> onCollision) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            BlockPos blockPos = new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));

            if (!level.isEmptyBlock(blockPos)) {
                onCollision.accept(blockPos);
            }
        }, 0, 1);
    }

    /**
     * 方塊碰撞時移除粒子
     * @param particle 粒子
     * @param level 世界
     * @return 任務 UUID
     */
    public static UUID removeOnBlockCollision(ControlableParticle particle, Level level) {
        return detectBlockCollision(particle, level, blockPos -> particle.remove());
    }

    /**
     * 方塊碰撞時反彈
     * @param particle 粒子
     * @param level 世界
     * @param restitution 恢復係數
     * @return 任務 UUID
     */
    public static UUID bounceOnBlockCollision(ControlableParticle particle, Level level, double restitution) {
        return detectBlockCollision(particle, level, blockPos -> {
            Vec3 pos = particle.getPosition();
            Vec3 velocity = particle.getVelocity();

            // 簡單的反彈邏輯
            Vec3 newVelocity = new Vec3(
                velocity.x,
                -velocity.y * restitution,
                velocity.z
            );
            particle.setVelocity(newVelocity);

            // 將粒子移出方塊
            particle.teleportTo(pos.x, blockPos.getY() + 1.1, pos.z);
        });
    }

    /**
     * 邊界碰撞檢測（AABB 包圍盒）
     * @param particle 粒子
     * @param bounds 邊界框
     * @param onCollision 碰撞回調
     * @return 任務 UUID
     */
    public static UUID detectBoundsCollision(ControlableParticle particle, AABB bounds, Runnable onCollision) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();

            if (!bounds.contains(pos)) {
                onCollision.run();
            }
        }, 0, 1);
    }

    /**
     * 超出邊界時移除粒子
     * @param particle 粒子
     * @param bounds 邊界框
     * @return 任務 UUID
     */
    public static UUID removeOnBoundsExit(ControlableParticle particle, AABB bounds) {
        return detectBoundsCollision(particle, bounds, particle::remove);
    }

    /**
     * 邊界反彈（保持在邊界內）
     * @param particle 粒子
     * @param bounds 邊界框
     * @param restitution 恢復係數
     * @return 任務 UUID
     */
    public static UUID bounceInBounds(ControlableParticle particle, AABB bounds, double restitution) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 velocity = particle.getVelocity();

            double newVx = velocity.x;
            double newVy = velocity.y;
            double newVz = velocity.z;

            // X 軸邊界檢查
            if (pos.x < bounds.minX || pos.x > bounds.maxX) {
                newVx = -velocity.x * restitution;
            }

            // Y 軸邊界檢查
            if (pos.y < bounds.minY || pos.y > bounds.maxY) {
                newVy = -velocity.y * restitution;
            }

            // Z 軸邊界檢查
            if (pos.z < bounds.minZ || pos.z > bounds.maxZ) {
                newVz = -velocity.z * restitution;
            }

            if (newVx != velocity.x || newVy != velocity.y || newVz != velocity.z) {
                particle.setVelocity(new Vec3(newVx, newVy, newVz));

                // 將粒子推回邊界內
                double clampedX = Math.max(bounds.minX, Math.min(bounds.maxX, pos.x));
                double clampedY = Math.max(bounds.minY, Math.min(bounds.maxY, pos.y));
                double clampedZ = Math.max(bounds.minZ, Math.min(bounds.maxZ, pos.z));
                particle.teleportTo(clampedX, clampedY, clampedZ);
            }
        }, 0, 1);
    }

    /**
     * 球形邊界碰撞
     * @param particle 粒子
     * @param center 球心
     * @param radius 半徑
     * @param onCollision 碰撞回調
     * @return 任務 UUID
     */
    public static UUID detectSphereCollision(ControlableParticle particle, Vec3 center, double radius, Runnable onCollision) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            double distance = pos.distanceTo(center);

            if (distance > radius) {
                onCollision.run();
            }
        }, 0, 1);
    }

    /**
     * 球形邊界反彈
     * @param particle 粒子
     * @param center 球心
     * @param radius 半徑
     * @param restitution 恢復係數
     * @return 任務 UUID
     */
    public static UUID bounceInSphere(ControlableParticle particle, Vec3 center, double radius, double restitution) {
        return CooScheduler.getInstance().runTaskTimer(() -> {
            if (particle.isRemoved()) {
                return;
            }

            Vec3 pos = particle.getPosition();
            Vec3 toCenter = pos.subtract(center);
            double distance = toCenter.length();

            if (distance > radius) {
                // 計算反射方向（沿半徑方向）
                Vec3 normal = toCenter.normalize();
                Vec3 velocity = particle.getVelocity();

                // 反射速度
                double dotProduct = velocity.dot(normal);
                Vec3 reflection = velocity.subtract(normal.scale(2 * dotProduct));
                particle.setVelocity(reflection.scale(restitution));

                // 將粒子推回球內
                Vec3 correctedPos = center.add(normal.scale(radius * 0.99));
                particle.teleportTo(correctedPos);
            }
        }, 0, 1);
    }
}
