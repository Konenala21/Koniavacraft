package com.github.nalamodikk.particle.examples;

import com.github.nalamodikk.particle.ControlableParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/**
 * 導向魔力流粒子
 *
 * 演示：面向相機（Billboard）、外部速度控制
 */
public class GuidedFlowParticle extends ControlableParticle {

    private final SpriteSet sprites;

    public GuidedFlowParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.setSpriteFromAge(sprites);
        
        this.lifetime = 60;
        this.quadSize = 0.2f;
        this.setFaceToCamera(true); // 面向相機
        
        this.setVelocity(vx, vy, vz);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(sprites);
        
        // 淡出效果
        float lifeRatio = (float) this.age / this.lifetime;
        this.setAlpha(1.0f - lifeRatio);
    }

    public static class Provider implements ParticleProvider<GuidedFlowOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public net.minecraft.client.particle.Particle createParticle(GuidedFlowOptions type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new GuidedFlowParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        }
    }
}
