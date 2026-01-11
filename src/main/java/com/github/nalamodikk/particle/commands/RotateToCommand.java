package com.github.nalamodikk.particle.commands;

import com.github.nalamodikk.particle.ICooParticle;
import org.joml.Quaternionf;

/**
 * 設定粒子旋轉指令
 */
public record RotateToCommand(Quaternionf rotation) implements IParticleCommand {
    @Override
    public void execute(ICooParticle particle) {
        particle.setRotation(rotation);
    }
}
