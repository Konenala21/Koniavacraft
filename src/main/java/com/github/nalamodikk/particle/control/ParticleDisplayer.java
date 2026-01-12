package com.github.nalamodikk.particle.control;

import com.github.nalamodikk.particle.ControlableParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Function;

/**
 * ????輯????踐??? */
public interface ParticleDisplayer {
    Controlable<?> display(Vec3 loc, ClientLevel world);

    static ParticleDisplayer withSingle(ParticleOptions effect, UUID uuid) {
        return new SingleParticleDisplayer(effect, uuid);
    }

    class SingleParticleDisplayer implements ParticleDisplayer {
        private final ParticleOptions effect;
        private final UUID uuid;

        public SingleParticleDisplayer(ParticleOptions effect, UUID uuid) {
            this.effect = effect;
            this.uuid = uuid;
        }

        @Override
        public Controlable<ControlableParticle> display(Vec3 loc, ClientLevel world) {
            world.addParticle(effect, loc.x, loc.y, loc.z, 0.0, 0.0, 0.0);
            return ControlParticleManager.getControl(uuid);
        }
    }
}
