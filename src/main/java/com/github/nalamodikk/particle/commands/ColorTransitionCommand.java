package com.github.nalamodikk.particle.commands;

import com.github.nalamodikk.particle.ICooParticle;
import org.joml.Vector3f;

/**
 * ?遴???撞??刻?
 */
public class ColorTransitionCommand implements IParticleCommand {
    private final float r, g, b;

    public ColorTransitionCommand(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public void execute(ICooParticle particle) {
        particle.setColor(r, g, b);
    }
}