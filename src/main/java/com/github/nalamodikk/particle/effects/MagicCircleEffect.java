package com.github.nalamodikk.particle.effects;

import com.github.nalamodikk.particle.CooParticleOptions;
import com.github.nalamodikk.particle.ModParticles;
import com.github.nalamodikk.particle.ParticleController;
import com.github.nalamodikk.particle.ParticleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ??亥行??????殉????
 *
 * ?賹??叟城????????伍????璇??殉????????
 */
public class MagicCircleEffect {

    private final Level level;
    private final Vec3 center;
    private final List<CircleRing> rings = new ArrayList<>();

    private int tickCounter = 0;
    private final int duration;
    private boolean isActive = true;

    public MagicCircleEffect(Level level, BlockPos pos, MagicCircleConfig config) {
        this.level = level;
        this.center = Vec3.atCenterOf(pos).add(0, config.yOffset, 0);
        this.duration = config.duration;

        // ?賹??叟城???抆?
        for (int i = 0; i < config.ringCount; i++) {
            float radius = config.baseRadius + i * config.radiusStep;
            int particleCount = (int)(config.particlesPerRing * (1 + i * 0.2f)); // ?叟□?????皜???
            float rotationSpeed = config.baseRotationSpeed * (1 + i * 0.1f); // ?伍??賹撞??????

            CircleRing ring = new CircleRing(
                radius,
                particleCount,
                rotationSpeed,
                config.particleSize,
                config.ringColors.length > i ? config.ringColors[i] : config.ringColors[0]
            );

            rings.add(ring);
        }

        // ?∴???賹????????
        spawnAllParticles();
    }

    /**
     * ?賹????????????
     */
    private void spawnAllParticles() {
        for (CircleRing ring : rings) {
            ring.spawnParticles(level, center);
        }
    }

    /**
     * ??tick ?皝?????選???
     */
    public void tick() {
        if (!isActive) return;

        tickCounter++;

        // ?皝????????
        for (CircleRing ring : rings) {
            ring.update(center, tickCounter);
        }

        // ?潘撓貔??秋??荒??
        if (duration > 0 && tickCounter >= duration) {
            stop();
        }
    }

    /**
     * ?謚怨翰????﹝摰????????
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

    // ========== ??豢豯??獢?????==========

    private static class CircleRing {
        private final float radius;
        private final int particleCount;
        private final float rotationSpeed;
        private final float particleSize;
        private final int color;

        private final List<ParticleController> particles = new ArrayList<>();
        private float currentRotation = 0;

        public CircleRing(float radius, int particleCount, float rotationSpeed, float particleSize, int color) {
            this.radius = radius;
            this.particleCount = particleCount;
            this.rotationSpeed = rotationSpeed; // ??tick
            this.particleSize = particleSize;
            this.color = color;
        }

        /**
         * ?賹???抆???????????
         */
        public void spawnParticles(Level level, Vec3 center) {
            for (int i = 0; i < particleCount; i++) {
                float angle = (float)(2 * Math.PI * i / particleCount);
                Vec3 pos = calculatePosition(center, angle, 0);

                CooParticleOptions options = new CooParticleOptions(particleSize, color, 0.8f);

                // ?賹????
                level.addParticle(options, pos.x, pos.y, pos.z, 0, 0, 0);

                // ????????對????????賹??綽?????桅?????????
                // ??威???秋撩??謘???????謜?????????ID
                // ?芬??撖??垢?????豯刈瞏??哨?颲?
            }
        }

        /**
         * ?皝?????選??剖????改?
         */
        public void update(Vec3 center, int ticks) {
            // ?殷?????????恃?瞍?
            currentRotation += Math.toRadians(rotationSpeed);
            if (currentRotation > Math.PI * 2) {
                currentRotation -= Math.PI * 2;
            }

            // ?皝??伍????殉???選???
            for (int i = 0; i < particles.size(); i++) {
                float baseAngle = (float)(2 * Math.PI * i / particleCount);
                float angle = baseAngle + currentRotation;

                Vec3 newPos = calculatePosition(center, angle, 0);
                particles.get(i).setPosition(newPos);
            }
        }

        /**
         * ?殷??????????刻﹝??選???
         */
        private Vec3 calculatePosition(Vec3 center, float angle, float yOffset) {
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + yOffset;
            double z = center.z + Math.sin(angle) * radius;
            return new Vec3(x, y, z);
        }

        /**
         * ??謒???????
         */
        public void removeParticles() {
            for (ParticleController particle : particles) {
                particle.remove();
            }
            particles.clear();
        }
    }

    // ========== ????==========

    public static class MagicCircleConfig {
        public int ringCount = 3; // ??抆??鞈?
        public float baseRadius = 0.5f; // ????????
        public float radiusStep = 0.3f; // ?伍?????筐?
        public int particlesPerRing = 20; // ?伍??????
        public float baseRotationSpeed = 2f; // ?蝞?????賹撞??瞍?tick??
        public float particleSize = 0.1f; // ????剜?
        public float yOffset = 0.1f; // Y ?岳???
        public int duration = -1; // ?蹓??蹇???1 = ?????

        // ?伍?????????剛??????
        public int[] ringColors = {
            0x9D46DF, // ?剁??????
            0x3B82F7, // ????????
            0x00FFC8  // ????????
        };

        public static MagicCircleConfig defaultConfig() {
            return new MagicCircleConfig();
        }

        public static MagicCircleConfig largeConfig() {
            MagicCircleConfig config = new MagicCircleConfig();
            config.ringCount = 5;
            config.baseRadius = 0.8f;
            config.radiusStep = 0.4f;
            config.particlesPerRing = 30;
            return config;
        }

        public static MagicCircleConfig fastConfig() {
            MagicCircleConfig config = new MagicCircleConfig();
            config.baseRotationSpeed = 5f;
            return config;
        }
    }
}
