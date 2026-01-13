package com.github.nalamodikk.particle;

import com.github.nalamodikk.particle.commands.IParticleCommand;
import com.github.nalamodikk.particle.commands.SetVelocityCommand;
import com.github.nalamodikk.particle.commands.RotateToCommand;
import com.github.nalamodikk.particle.commands.ColorTransitionCommand;
import com.github.nalamodikk.particle.animation.PathMotion;
import com.github.nalamodikk.particle.utils.RelativeLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * 蝎??批?? * ?冽?其撩??恥?嗥垢?摩銝剔甇交?嗥?摮? */
public class ParticleController {

    private final UUID particleId;

    public ParticleController(UUID particleId) {
        this.particleId = particleId;
    }

    public ParticleController setPosition(double x, double y, double z) {
        queueCommand(particle -> particle.setPosition(x, y, z));
        return this;
    }

    public ParticleController setPosition(Vec3 pos) {
        return setPosition(pos.x, pos.y, pos.z);
    }

    public ParticleController setVelocity(double vx, double vy, double vz) {
        queueCommand(new SetVelocityCommand(vx, vy, vz));
        return this;
    }

    public ParticleController setVelocity(Vec3 velocity) {
        return setVelocity(velocity.x, velocity.y, velocity.z);
    }

    public ParticleController setColor(float r, float g, float b) {
        queueCommand(new ColorTransitionCommand(r, g, b));
        return this;
    }

    public ParticleController setAlpha(float alpha) {
        queueCommand(particle -> particle.setAlpha(alpha));
        return this;
    }

    public ParticleController setScale(float scale) {
        queueCommand(particle -> particle.setScale(scale));
        return this;
    }

    public ParticleController setRotation(Quaternionf rotation) {
        queueCommand(new RotateToCommand(rotation));
        return this;
    }

    public ParticleController setFaceToCamera(boolean faceToCamera) {
        queueCommand(particle -> particle.setFaceToCamera(faceToCamera));
        return this;
    }

    public void remove() {
        queueCommand(ICooParticle::remove);
    }

    public boolean isAlive() {
        return ParticleManager.getInstance().getParticle(particleId).isPresent();
    }

    public ParticleController execute(IParticleCommand action) {
        queueCommand(action);
        return this;
    }

    public ParticleController setPathMotion(PathMotion motion, int duration) {
        final int[] age = {0};
        addPreTickAction(p -> {
            if (age[0] < duration) {
                double progress = (double) age[0] / duration;
                Vec3 position = motion.getPositionAt((float) progress);
                p.teleportTo(position);
            }
            age[0]++;
        });
        return this;
    }

    public ParticleController addPreTickAction(Consumer<ControlableParticle> action) {
        queueCommand(particle -> {
            if (particle instanceof ControlableParticle cp) {
                cp.addPreTickAction(action);
            }
        });
        return this;
    }

    public ParticleController addPostTickAction(Consumer<ControlableParticle> action) {
        queueCommand(particle -> {
            if (particle instanceof ControlableParticle cp) {
                cp.addPostTickAction(action);
            }
        });
        return this;
    }

    private void queueCommand(IParticleCommand command) {
        ParticleManager.getInstance().queueCommand(particleId, command);
    }

    public UUID getParticleId() {
        return particleId;
    }
}