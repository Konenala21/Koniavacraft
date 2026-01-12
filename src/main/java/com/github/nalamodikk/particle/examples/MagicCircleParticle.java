package com.github.nalamodikk.particle.examples;

import com.github.nalamodikk.particle.ControlableParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.UUID;

/**
 * 魔法陣粒子 */
public class MagicCircleParticle extends ControlableParticle {

    private final SpriteSet sprites;
    private final float rotationSpeed = 2.0f;

    public MagicCircleParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, UUID uuid) {
        super(level, x, y, z, 0, 0, 0, uuid);  // 速度為 0（靜止不動）
        this.sprites = sprites;

        // 設置 sprite
        this.pickSprite(sprites);

        this.lifetime = 200;
        this.quadSize = 2.0f;
        this.alpha = 1.0f;

        this.setFaceToCamera(false);
        this.setRotation(new Quaternionf().rotateX((float) Math.toRadians(90)));

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
