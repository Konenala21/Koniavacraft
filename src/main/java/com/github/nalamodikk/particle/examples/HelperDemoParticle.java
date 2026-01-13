package com.github.nalamodikk.particle.examples;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.helper.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Helper 演示粒子
 * 根據不同的模式展示不同的 Helper 功能組合
 */
public class HelperDemoParticle extends ControlableParticle {

    /**
     * 演示模式枚舉
     */
    public enum DemoMode {
        /** 基礎效果：淡入淡出、縮放、顏色 */
        BASIC_EFFECTS,
        /** 螺旋運動：螺旋上升、噪聲擾動 */
        SPIRAL_MOTION,
        /** 物理效果：重力、彈跳、速度限制 */
        PHYSICS,
        /** 吸引力：漩渦吸引、軌跡 */
        ATTRACTOR,
        /** 完整展示：組合多個效果 */
        FULL_DEMO
    }

    private final SpriteSet sprites;

    public HelperDemoParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites,
                             DemoMode mode, UUID uuid) {
        super(level, x, y, z, 0, 0, 0, uuid);
        this.sprites = sprites;
        this.pickSprite(sprites);
        this.setFaceToCamera(true);

        // 根據模式應用不同的 Helper 組合
        switch (mode) {
            case BASIC_EFFECTS -> applyBasicEffects();
            case SPIRAL_MOTION -> applySpiralMotion(x, y, z);
            case PHYSICS -> applyPhysicsEffects(level);
            case ATTRACTOR -> applyAttractorEffects(x, y, z, level);
            case FULL_DEMO -> applyFullDemo(x, y, z, level);
        }

        // 更新 sprite 動畫
        this.addPreTickAction(particle -> this.setSpriteFromAge(sprites));
    }

    /**
     * 基礎效果演示
     */
    private void applyBasicEffects() {
        this.setLifetime(120);
        this.setScale(0.3f);
        this.setAlpha(0.0f);
        this.setColor(0.3f, 0.6f, 1.0f); // 藍色

        // 淡入
        AlphaHelper.fadeIn(this, 20, 1.0f);

        // 生命週期結束時淡出
        LifetimeHelper.setMaxAgeWithFade(this, 120, 0.8f);

        // 脈衝縮放
        ScaleHelper.pulse(this, 0.3f, 0.8f, 40);

        // 顏色過渡到紫色
        java.awt.Color fromColor = new java.awt.Color(77, 153, 255); // 藍色
        java.awt.Color toColor = new java.awt.Color(204, 51, 255); // 紫色
        ColorHelper.colorTransition(this, fromColor, toColor, 60);
    }

    /**
     * 螺旋運動演示
     */
    private void applySpiralMotion(double x, double y, double z) {
        this.setLifetime(100);
        this.setScale(0.4f);
        this.setAlpha(0.8f);
        this.setColor(0.2f, 1.0f, 0.5f); // 綠色

        Vec3 center = new Vec3(x, y, z);

        // 螺旋上升運動
        SpiralHelper.spiral(this, center, 0.02f, (float) (Math.PI / 15));

        // 縮放動畫
        ScaleHelper.scaleOverTime(this, 0.2f, 0.6f, 50);
    }

    /**
     * 物理效果演示
     */
    private void applyPhysicsEffects(ClientLevel level) {
        this.setLifetime(80);
        this.setScale(0.5f);
        this.setAlpha(1.0f);
        this.setColor(1.0f, 0.5f, 0.0f); // 橙色

        // 應用重力
        GravityHelper.applyGravity(this);

        // 速度限制
        VelocityHelper.limitSpeed(this, 2.0);

        // 創建軌跡
        TrailHelper.createTrail(this, level, ParticleTypes.FLAME, 3);

        // 淡出並移除
        LifetimeHelper.fadeOutAndRemove(this, 20);
    }

    /**
     * 吸引力效果演示
     */
    private void applyAttractorEffects(double x, double y, double z, ClientLevel level) {
        this.setLifetime(100);
        this.setScale(0.4f);
        this.setAlpha(1.0f);
        this.setColor(1.0f, 0.3f, 0.3f); // 紅色

        Vec3 attractorCenter = new Vec3(x, y + 2, z);

        // 漩渦吸引
        AttractorHelper.vortexAttractor(this, attractorCenter, 0.05, 0.3);

        // 輕微重力
        GravityHelper.applyGravity(this, 0.015);

        // 速度限制
        VelocityHelper.limitSpeed(this, 3.0);

        // 透明度脈衝
        AlphaHelper.pulse(this, 0.5f, 1.0f, 25);

        // 縮小並移除
        LifetimeHelper.shrinkAndRemove(this, 25);
    }

    /**
     * 完整演示：組合多種效果
     */
    private void applyFullDemo(double x, double y, double z, ClientLevel level) {
        this.setLifetime(150);
        this.setScale(0.2f);
        this.setAlpha(0.0f);
        this.setColor(0.5f, 0.5f, 1.0f); // 淡紫色

        Vec3 center = new Vec3(x, y, z);

        // 1. 淡入
        AlphaHelper.fadeIn(this, 15, 1.0f);

        // 2. 環繞運動
        OrbitHelper.orbit(this, center, 2.0f, (float) (Math.PI / 30));

        // 3. 脈衝縮放
        ScaleHelper.pulse(this, 0.2f, 0.7f, 50);

        // 4. 顏色脈衝
        java.awt.Color color1 = new java.awt.Color(128, 128, 255); // 淡紫
        java.awt.Color color2 = new java.awt.Color(255, 128, 128); // 淡紅
        ColorHelper.pulse(this, color1, color2, 40);

        // 5. 添加軌跡
        TrailHelper.createFadingTrail(this, level, ParticleTypes.END_ROD, 5, 3);

        // 6. 生命週期結束時淡出
        LifetimeHelper.setMaxAgeWithFade(this, 150, 0.85f);
    }

    /**
     * Provider 用於粒子註冊
     */
    public static class Provider implements ParticleProvider<HelperDemoOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public net.minecraft.client.particle.Particle createParticle(HelperDemoOptions options, ClientLevel level,
                                                                     double x, double y, double z,
                                                                     double xSpeed, double ySpeed, double zSpeed) {
            return new HelperDemoParticle(level, x, y, z, sprites, options.getMode(), options.getUuid());
        }
    }
}
