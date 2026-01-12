package com.github.nalamodikk.particle.commands;

import com.github.nalamodikk.particle.ICooParticle;

/**
 * ?桀??剛????賹撞??刻?
 */
public class SetVelocityCommand implements IParticleCommand {
    private final double vx, vy, vz;

    public SetVelocityCommand(double vx, double vy, double vz) {
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
    }

    @Override
    public void execute(ICooParticle particle) {
        particle.setVelocity(vx, vy, vz);
    }
}