package com.github.nalamodikk.particle.commands;

import com.github.nalamodikk.particle.ICooParticle;

/**
 * 顏色變更指令
 */
public record ColorTransitionCommand(float r, float g, float b) implements IParticleCommand {
    @Override
    public void execute(ICooParticle particle) {
        particle.setColor(r, g, b);
    }
}
