package com.github.nalamodikk.particle.control;

import com.github.nalamodikk.particle.ControlableParticle;
import com.github.nalamodikk.particle.utils.RelativeLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ?????對??? */
public class ParticleControler implements Controlable<ControlableParticle> {
    private final UUID uuid;
    private ControlableParticle particle;
    private boolean initialized = false;
    private final List<Consumer<ControlableParticle>> invokeQueue = new ArrayList<>();
    
    /**
     * tick ????鞊????
     */
    public final Map<String, Object> bufferedData = new ConcurrentHashMap<>();
    
    public Consumer<ControlableParticle> initInvoker = (p) -> {};

    public ParticleControler(UUID uuid) {
        this.uuid = uuid;
    }

    public ParticleControler addPreTickAction(Consumer<ControlableParticle> action) {
        invokeQueue.add(action);
        return this;
    }

    public void loadParticle(ControlableParticle particle) {
        if (this.particle != null) return;
        if (!particle.getParticleId().equals(uuid)) {
            throw new IllegalArgumentException("Particle UUID mismatch");
        }
        this.particle = particle;
    }

    public void particleInit() {
        if (initialized || particle == null) return;
        initInvoker.accept(particle);
        initialized = true;
    }

    public void doTick() {
        if (particle == null) return;
        for (Consumer<ControlableParticle> action : invokeQueue) {
            action.accept(particle);
        }
        if (particle.isRemoved()) {
            ControlParticleManager.removeControl(uuid);
        }
    }

    @Override
    public UUID controlUUID() {
        return uuid;
    }

    @Override
    public void rotateToPoint(RelativeLocation to) {
        // Particle implementation
    }

    @Override
    public void rotateToWithAngle(RelativeLocation to, double angle) {
        // Particle implementation
    }

    @Override
    public void rotateAsAxis(double radian) {
        // Particle implementation
    }

    @Override
    public void teleportTo(Vec3 pos) {
        if (particle != null) particle.teleportTo(pos);
    }

    @Override
    public void teleportTo(double x, double y, double z) {
        if (particle != null) particle.teleportTo(x, y, z);
    }

    @Override
    public void remove() {
        if (particle != null) particle.remove();
    }

    @Override
    public ControlableParticle getControlObject() {
        return particle;
    }
}
