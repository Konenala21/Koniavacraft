package com.github.nalamodikk.particle.effects;

import com.github.nalamodikk.particle.CooParticleOptions;
import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.ModParticles;
import com.github.nalamodikk.particle.ParticleController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * ?堆撓????謒???
 *
 * ?皝???豢??亙?賹????????殉???﹝??????∵
 */
@OnlyIn(Dist.CLIENT)
public class ClientMagicCircleEffect {

    private final ClientLevel level;
    private final Vec3 center;
    private final List<CircleRing> rings = new ArrayList<>();

    private int tickCounter = 0;
    private final int duration;
    private boolean isActive = true;

    public ClientMagicCircleEffect(BlockPos pos, Config config) {
        this.level = (ClientLevel) Minecraft.getInstance().level;
        if (this.level == null) {
            this.isActive = false;
            this.duration = 0;
            this.center = Vec3.ZERO; // ?豲???center ?頦??箏???芰?
            return;
        }

        this.center = Vec3.atCenterOf(pos).add(0, config.yOffset, 0);
        this.duration = config.duration;

        // ?賹??叟城???抆?
        for (int i = 0; i < config.ringCount; i++) {
            float radius = config.baseRadius + i * config.radiusStep;
            int particleCount = (int)(config.particlesPerRing * (1 + i * 0.3f));
            float rotationSpeed = config.baseRotationSpeed * (1 + i * 0.15f);

            int color = config.ringColors.length > i ?
                config.ringColors[i] : config.ringColors[0];

            CircleRing ring = new CircleRing(
                radius,
                particleCount,
                rotationSpeed,
                config.particleSize,
                color,
                config.particleAlpha
            );

            rings.add(ring);
            ring.spawnParticles(level, center);
        }
    }

    /**
     * ??tick ?皝?
     */
    public void tick() {
        if (!isActive) return;

        tickCounter++;

        // ?皝????????
        for (CircleRing ring : rings) {
            ring.update(center);
        }

        // ?潘撓貔??秋??荒??
        if (duration > 0 && tickCounter >= duration) {
            stop();
        }
    }

    /**
     * ?謚怨翰???
     */
    public void stop() {
        isActive = false;
        for (CircleRing ring : rings) {
            ring.removeParticles();
        }
    }

    public boolean isActive() {
        return isActive;
    }

    // ========== ??豢豯???抆? ==========

    private static class CircleRing {
        private final float radius;
        private final int particleCount;
        private final float rotationSpeed; // ?瞍?tick
        private final float particleSize;
        private final int color;
        private final float alpha;

        private final List<ParticleController> particleControllers = new ArrayList<>();
        private float currentRotation = 0;

        public CircleRing(float radius, int particleCount, float rotationSpeed,
                         float particleSize, int color, float alpha) {
            this.radius = radius;
            this.particleCount = particleCount;
            this.rotationSpeed = (float)Math.toRadians(rotationSpeed); // ?改??蝞??
            this.particleSize = particleSize;
            this.color = color;
            this.alpha = alpha;
        }

        /**
         * ?賹???抆???????
         */
        public void spawnParticles(ClientLevel level, Vec3 center) {
            for (int i = 0; i < particleCount; i++) {
                float angle = (float)(2 * Math.PI * i / particleCount);
                Vec3 pos = calculatePosition(center, angle);

                // ??????鞈?
                CooParticleOptions options = new CooParticleOptions(
                    particleSize,
                    color,
                    alpha
                );

                // ?輯撒???????????????????桀???sprite??
                ControlableParticle particle = (ControlableParticle) Minecraft.getInstance().particleEngine
                    .createParticle(options, pos.x, pos.y, pos.z, 0, 0, 0);

                if (particle != null) {
                    // ?桀???賹????
                    particle.setLifetime(200); // 10 ??

                    // ????對???
                    ParticleController controller = new ParticleController(particle.getParticleId());
                    particleControllers.add(controller);
                }
            }
        }

        /**
         * ?皝?????選??剖???????改?
         */
        public void update(Vec3 center) {
            // ?皝?????恃?瞍?
            currentRotation += rotationSpeed;
            if (currentRotation > Math.PI * 2) {
                currentRotation -= Math.PI * 2;
            }

            // ?皝??伍????殉???選???
            for (int i = 0; i < particleControllers.size(); i++) {
                float baseAngle = (float)(2 * Math.PI * i / particleCount);
                float angle = baseAngle + currentRotation;

                Vec3 newPos = calculatePosition(center, angle);
                particleControllers.get(i).setPosition(newPos);
            }
        }

        /**
         * ?殷??????????
         */
        private Vec3 calculatePosition(Vec3 center, float angle) {
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y;
            double z = center.z + Math.sin(angle) * radius;
            return new Vec3(x, y, z);
        }

        /**
         * ??謒???????
         */
        public void removeParticles() {
            for (ParticleController controller : particleControllers) {
                controller.remove();
            }
            particleControllers.clear();
        }
    }

    // ========== ????==========

    public static class Config {
        public int ringCount = 3;
        public float baseRadius = 0.6f;
        public float radiusStep = 0.25f;
        public int particlesPerRing = 24;
        public float baseRotationSpeed = 3f; // ??tick
        public float particleSize = 0.3f; // ?竣銋?????蝡??伐??皜豢??????
        public float particleAlpha = 0.8f;
        public float yOffset = 0.05f;
        public int duration = 60; // ticks

        public int[] ringColors = {
            0x9D46DF, // ?剁
            0x6B7FF7, // ?????
            0x3BBAF7  // ??
        };

        public static Config manaGenerator() {
            Config config = new Config();
            config.duration = 40; // 2 ??
            return config;
        }

        public static Config persistent() {
            Config config = new Config();
            config.duration = -1; // ?防?
            return config;
        }
    }
}
