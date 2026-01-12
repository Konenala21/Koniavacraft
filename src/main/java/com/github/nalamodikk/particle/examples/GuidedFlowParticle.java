package com.github.nalamodikk.particle.examples;

import com.github.nalamodikk.particle.ControlableParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 導引流動粒子 */
public class GuidedFlowParticle extends ControlableParticle {

    private final SpriteSet sprites;

    public GuidedFlowParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites, UUID uuid) {
        super(level, x, y, z, vx, vy, vz, uuid);  // 傳遞速度給父類
        this.sprites = sprites;

        // 設置 sprite
        this.pickSprite(sprites);

        this.lifetime = 60;
        this.quadSize = 0.5f;
        this.alpha = 1.0f;

        this.setFaceToCamera(true);

        // ✅ 參考框架的做法：使用 addPreTickAction
        // 注意：在構造函數中 'this' 指向當前粒子對象，可以訪問 protected 字段
        this.addPreTickAction(particle -> {
            // 手動應用速度來移動粒子
            this.x += this.xd;
            this.y += this.yd;
            this.z += this.zd;

            this.setSpriteFromAge(sprites);
            float lifeRatio = (float) this.age / this.lifetime;
            this.setAlpha(1.0f - lifeRatio);
        });
    }

    public static class Provider implements ParticleProvider<GuidedFlowOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public net.minecraft.client.particle.Particle createParticle(GuidedFlowOptions type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new GuidedFlowParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, type.getUuid());
        }
    }
}
