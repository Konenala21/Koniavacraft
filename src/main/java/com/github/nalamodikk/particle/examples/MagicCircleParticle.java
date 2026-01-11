package com.github.nalamodikk.particle.examples;

import com.github.nalamodikk.particle.ControlableParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/**
 * 魔法陣粒子
 *
 * 演示：自由旋轉、持續旋轉動畫
 */
public class MagicCircleParticle extends ControlableParticle {

    private final SpriteSet sprites;
    private float rotationSpeed = 2.0f; // 度/tick

    public MagicCircleParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.setSpriteFromAge(sprites);
        
        // 初始設定
        this.lifetime = 200;
        this.quadSize = 1.0f;
        this.setFaceToCamera(false); // 自由旋轉模式
        
        // 初始水平放置 (繞 X 軸旋轉 90 度)
        this.setRotation(new Quaternionf().rotateX((float) Math.toRadians(90)));
    }

    @Override
    public void tick() {
        super.tick();
        
        // 自轉動畫
        Quaternionf currentRot = new Quaternionf(); // 這裡需要獲取當前旋轉，但 ControlableParticle 沒有 getter
        // 為了簡單起見，我們在這裡維護一個旋轉狀態，或者修改 ControlableParticle 暴露 getter
        // 暫時使用一個累積變量
        float angle = (this.age * rotationSpeed) % 360;
        
        // 基礎旋轉（水平） * 自轉（繞 Y 軸）
        Quaternionf rot = new Quaternionf()
            .rotateX((float) Math.toRadians(90))
            .rotateY((float) Math.toRadians(angle));
            
        this.setRotation(rot);
        
        // 保持貼圖
        this.setSpriteFromAge(sprites);
    }

    public static class Provider implements ParticleProvider<com.github.nalamodikk.particle.CooParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public net.minecraft.client.particle.Particle createParticle(com.github.nalamodikk.particle.CooParticleOptions type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new MagicCircleParticle(level, x, y, z, sprites);
        }
    }
}
