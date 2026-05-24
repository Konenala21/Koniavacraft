package com.github.nalamodikk.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FormationParticle extends TextureSheetParticle {

    private final FormationParticleOptions opts;
    private double orbitAngle;

    // TRACK_PLAYER only
    private UUID trackUuid;
    private int spawnGeneration;

    protected FormationParticle(ClientLevel level, double x, double y, double z,
                                 double vx, double vy, double vz,
                                 SpriteSet sprites, FormationParticleOptions opts) {
        super(level, x, y, z, vx, vy, vz);
        this.opts = opts;
        this.orbitAngle = Math.atan2(z - opts.cz(), x - opts.cx());

        pickSprite(sprites);

        switch (opts.behavior()) {
            case FormationParticleOptions.ORBIT -> {
                this.lifetime = 200;
                this.quadSize = 0.15f;
                this.rCol = 1.0f; this.gCol = 0.82f; this.bCol = 0.28f;
                this.hasPhysics = false;
            }
            case FormationParticleOptions.RISE -> {
                this.lifetime = 80;
                this.quadSize = 0.10f;
                this.rCol = 1.0f; this.gCol = 0.95f; this.bCol = 0.60f;
                this.hasPhysics = false;
                this.yd = 0.018;
            }
            case FormationParticleOptions.BURST -> {
                this.lifetime = 35;
                this.quadSize = 0.22f;
                this.rCol = 1.0f; this.gCol = 1.0f; this.bCol = 0.90f;
                this.hasPhysics = false;
            }
            case FormationParticleOptions.POINT -> {
                this.lifetime = 6;
                this.quadSize = 0.18f;
                this.rCol = 1.0f; this.gCol = 0.85f; this.bCol = 0.30f;
                this.hasPhysics = false;
            }
            case FormationParticleOptions.STATIC -> {
                this.lifetime = 100;
                this.quadSize = 0.18f;
                this.rCol = 1.0f; this.gCol = 0.88f; this.bCol = 0.32f;
                this.hasPhysics = false;
            }
            case FormationParticleOptions.TRACK_PLAYER -> {
                long msb = Double.doubleToRawLongBits(opts.cx());
                long lsb = Double.doubleToRawLongBits(opts.cy());
                this.trackUuid = new UUID(msb, lsb);
                this.orbitAngle = opts.cz(); // cz stores initial angle for TRACK_PLAYER
                this.spawnGeneration = ClientInteractiveFormationManager.getGeneration(this.trackUuid);
                this.lifetime = 400;
                this.quadSize = 0.07f;
                this.rCol = 0.60f; this.gCol = 0.90f; this.bCol = 1.0f;
                this.hasPhysics = false;
            }
        }
        this.alpha = 1.0f;
    }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;

        switch (opts.behavior()) {
            case FormationParticleOptions.ORBIT -> {
                orbitAngle += opts.speed();
                x = opts.cx() + opts.radius() * Math.cos(orbitAngle);
                z = opts.cz() + opts.radius() * Math.sin(orbitAngle);
                y = opts.cy() + 0.08 + Math.sin(age * 0.09) * 0.06;
                alpha = 0.55f + 0.45f * (float) Math.abs(Math.sin(age * 0.13));
            }
            case FormationParticleOptions.RISE -> {
                y += yd;
                yd *= 0.99;
                alpha = Math.max(0f, 1.0f - (float) age / lifetime);
                quadSize *= 0.985f;
            }
            case FormationParticleOptions.BURST -> {
                x += xd; y += yd; z += zd;
                xd *= 0.87; yd *= 0.87; zd *= 0.87;
                alpha = Math.max(0f, 1.0f - (float) age / lifetime);
            }
            case FormationParticleOptions.POINT -> {
                alpha = Math.max(0f, 1.0f - (float) age / lifetime);
            }
            case FormationParticleOptions.STATIC -> {
                alpha = age < 80 ? 1.0f : Math.max(0f, 1.0f - (float)(age - 80) / 20f);
            }
            case FormationParticleOptions.TRACK_PLAYER -> {
                if (spawnGeneration != ClientInteractiveFormationManager.getGeneration(trackUuid)) {
                    remove(); return;
                }
                Player target = level.getPlayerByUUID(trackUuid);
                if (target == null) { remove(); return; }

                double yaw   = Math.toRadians(target.getYRot());
                double pitch = Math.toRadians(target.getXRot());
                double lx = -Math.sin(yaw) * Math.cos(pitch);
                double ly = -Math.sin(pitch);
                double lz =  Math.cos(yaw) * Math.cos(pitch);

                double ax = target.getX()    + lx * 2.5;
                double ay = target.getEyeY() + ly * 2.5;
                double az = target.getZ()    + lz * 2.5;

                double rx, ry, rz;
                if (Math.abs(ly) > 0.99) {
                    rx = 1; ry = 0; rz = 0;
                } else {
                    rx = lz; ry = 0; rz = -lx;
                    double len = Math.sqrt(rx * rx + rz * rz);
                    rx /= len; rz /= len;
                }
                // look × right = correct "up" in the look-plane (world-up when horizontal)
                double ux = ly * rz - lz * ry;
                double uy = lz * rx - lx * rz;
                double uz = lx * ry - ly * rx;

                orbitAngle += opts.speed();
                double c = Math.cos(orbitAngle), s = Math.sin(orbitAngle);
                float r = opts.radius();
                x = ax + r * (c * rx + s * ux);
                y = ay + r * (c * ry + s * uy);
                z = az + r * (c * rz + s * uz);
                alpha = 0.6f + 0.4f * (float) Math.abs(Math.sin(age * 0.08));
            }
        }

        age++;
        if (age >= lifetime || alpha <= 0f) remove();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return FormationParticleRenderType.INSTANCE;
    }

    public static class Factory implements ParticleProvider<FormationParticleOptions> {
        private final SpriteSet sprites;

        public Factory(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(FormationParticleOptions opts, ClientLevel level,
                                                  double x, double y, double z,
                                                  double vx, double vy, double vz) {
            return new FormationParticle(level, x, y, z, vx, vy, vz, sprites, opts);
        }
    }
}
