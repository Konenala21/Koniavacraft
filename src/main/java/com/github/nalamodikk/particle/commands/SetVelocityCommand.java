package com.github.nalamodikk.particle.commands;

import com.github.nalamodikk.particle.ICooParticle;

/**
 * 設定粒子速度指令
 */
public record SetVelocityCommand(double vx, double vy, double vz) implements IParticleCommand {
    @Override
    public void execute(ICooParticle particle) {
        particle.setVelocity(vx, vy, vz);
    }
}
