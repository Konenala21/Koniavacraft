package com.github.nalamodikk.particle.commands;

import com.github.nalamodikk.particle.ICooParticle;

/**
 * ?????刻??鈭
 */
@FunctionalInterface
public interface IParticleCommand {
    /**
     * ?????刻?
     * @param particle ????????
     */
    void execute(ICooParticle particle);
}