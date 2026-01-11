package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.commands.IParticleCommand;
import com.github.nalamodikk.particle.commands.SetVelocityCommand;
import com.github.nalamodikk.particle.commands.RotateToCommand;
import com.github.nalamodikk.particle.commands.ColorTransitionCommand;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.UUID;

/**
 * 粒子控制器
 *
 * 開發者通過此類控制粒子的行為
 */
public class ParticleController {

    private final UUID particleId;

    public ParticleController(UUID particleId) {
        this.particleId = particleId;
    }

    /**
     * 設置粒子位置
     */
    public ParticleController setPosition(double x, double y, double z) {
        queueCommand(particle -> particle.setPosition(x, y, z));
        return this;
    }

    /**
     * 設置粒子位置（Vec3）
     */
    public ParticleController setPosition(Vec3 pos) {
        return setPosition(pos.x, pos.y, pos.z);
    }

    /**
     * 設置粒子速度
     */
    public ParticleController setVelocity(double vx, double vy, double vz) {
        queueCommand(new SetVelocityCommand(vx, vy, vz));
        return this;
    }

    /**
     * 設置粒子速度（Vec3）
     */
    public ParticleController setVelocity(Vec3 velocity) {
        return setVelocity(velocity.x, velocity.y, velocity.z);
    }

    /**
     * 設置粒子顏色（RGB，0-1）
     */
    public ParticleController setColor(float r, float g, float b) {
        queueCommand(new ColorTransitionCommand(r, g, b));
        return this;
    }

    /**
     * 設置粒子顏色（整數 RGB）
     */
    public ParticleController setColor(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        return setColor(r, g, b);
    }

    /**
     * 設置粒子透明度（0-1）
     */
    public ParticleController setAlpha(float alpha) {
        queueCommand(particle -> particle.setAlpha(alpha));
        return this;
    }

    /**
     * 設置粒子大小
     */
    public ParticleController setScale(float scale) {
        queueCommand(particle -> particle.setScale(scale));
        return this;
    }

    /**
     * 設置粒子旋轉
     */
    public ParticleController setRotation(Quaternionf rotation) {
        queueCommand(new RotateToCommand(rotation));
        return this;
    }

    /**
     * 設置粒子旋轉（歐拉角，度數）
     */
    public ParticleController setRotation(float pitch, float yaw, float roll) {
        Quaternionf rotation = new Quaternionf()
            .rotateY((float)Math.toRadians(yaw))
            .rotateX((float)Math.toRadians(pitch))
            .rotateZ((float)Math.toRadians(roll));
        return setRotation(rotation);
    }

    /**
     * 設置是否面向相機
     */
    public ParticleController setFaceToCamera(boolean faceToCamera) {
        queueCommand(particle -> particle.setFaceToCamera(faceToCamera));
        return this;
    }

    /**
     * 移除粒子
     */
    public void remove() {
        queueCommand(ICooParticle::remove);
    }

    /**
     * 檢查粒子是否仍然存在
     */
    public boolean isAlive() {
        return ParticleManager.getInstance().getParticle(particleId).isPresent();
    }

    /**
     * 執行自定義邏輯
     */
    public ParticleController execute(IParticleCommand action) {
        queueCommand(action);
        return this;
    }

    // ========== 內部方法 ==========

    private void queueCommand(IParticleCommand command) {
        ParticleManager.getInstance().queueCommand(particleId, command);
    }

    public UUID getParticleId() {
        return particleId;
    }
}