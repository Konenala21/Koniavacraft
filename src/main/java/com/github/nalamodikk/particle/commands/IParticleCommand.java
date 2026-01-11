package com.github.nalamodikk.particle.commands;

import com.github.nalamodikk.particle.ICooParticle;

/**
 * 粒子指令介面
 */
@FunctionalInterface
public interface IParticleCommand {
    /**
     * 執行指令
     * @param particle 目標粒子實體
     */
    void execute(ICooParticle particle);
}
