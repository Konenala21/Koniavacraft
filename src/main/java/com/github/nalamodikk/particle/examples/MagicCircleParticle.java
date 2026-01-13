package com.github.nalamodikk.particle.examples;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.helper.AlphaHelper;
import com.github.nalamodikk.particle.helper.ColorHelper;
import com.github.nalamodikk.particle.helper.LifetimeHelper;
import com.github.nalamodikk.particle.helper.ScaleHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.UUID;

/**
 * 魔法陣粒子
 * 展示 Helper 功能：淡入淡出、脈衝縮放、顏色過渡、旋轉
 */
public class MagicCircleParticle extends ControlableParticle {

    private final SpriteSet sprites;
    private final float rotationSpeed = 2.0f;

    public MagicCircleParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, UUID uuid) {
        super(level, x, y, z, 0, 0, 0, uuid);  // 速度為 0（靜止不動）
        this.sprites = sprites;

        // 設置 sprite
        this.pickSprite(sprites);

        this.lifetime = 200;
        this.quadSize = 0.5f;  // 從小開始
        this.alpha = 0.0f;     // 從透明開始

        this.setFaceToCamera(false);
        this.setRotation(new Quaternionf().rotateX((float) Math.toRadians(90)));

        // === 使用 Helper 系統展示功能 ===

        // 1. 淡入效果（前 20 tick）
        AlphaHelper.fadeIn(this, 20, 1.0f);

        // 2. 設置生命週期並在最後 20% 時間淡出
        LifetimeHelper.setMaxAgeWithFade(this, 200, 0.8f);

        // 3. 脈衝縮放（在 0.5 和 2.0 之間脈衝，週期 60 tick）
        ScaleHelper.pulse(this, 0.5f, 2.0f, 60);

        // 4. 顏色從藍色過渡到紫色
        this.setColor(0.3f, 0.6f, 1.0f); // 開始為淺藍色
        java.awt.Color fromColor = new java.awt.Color(77, 153, 255); // 淺藍色
        java.awt.Color toColor = new java.awt.Color(204, 51, 255); // 紫色
        ColorHelper.colorTransition(this, fromColor, toColor, 100);

        // ✅ 參考框架的做法：使用 addPreTickAction 而不是 override tick()
        // 注意：在構造函數中 'this' 指向當前粒子對象，可以訪問 protected 字段
        this.addPreTickAction(particle -> {
            float angle = (this.age * rotationSpeed) % 360;
            Quaternionf rot = new Quaternionf()
                .rotateX((float) Math.toRadians(90))
                .rotateY((float) Math.toRadians(angle));
            this.setRotation(rot);
            this.setSpriteFromAge(sprites);
        });
    }

    public static class Provider implements ParticleProvider<MagicCircleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public net.minecraft.client.particle.Particle createParticle(MagicCircleOptions type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new MagicCircleParticle(level, x, y, z, sprites, type.getUuid());
        }
    }
}
