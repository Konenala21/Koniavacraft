package com.github.nalamodikk.particle.commands;

import com.github.nalamodikk.particle.ICooParticle;
import org.joml.Quaternionf;

/**
 * ?桀??剛????????刻?
 */
public class RotateToCommand implements IParticleCommand {
    private final Quaternionf rotation;

    public RotateToCommand(Quaternionf rotation) {
        this.rotation = rotation;
    }

    @Override
    public void execute(ICooParticle particle) {
        particle.setRotation(rotation);
    }
}