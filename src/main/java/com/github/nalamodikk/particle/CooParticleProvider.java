package com.github.nalamodikk.particle;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;

/**
 * ??????????? */
public class CooParticleProvider implements ParticleProvider<CooParticleOptions> {

    private final SpriteSet sprites;

    public CooParticleProvider(SpriteSet sprites) {
        this.sprites = sprites;
    }

    @Override
    public Particle createParticle(CooParticleOptions options, ClientLevel level,
                                   double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        ControlableParticle particle = new ControlableParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options.getUuid());

        // ??伍??鞈?
        particle.pickSprite(sprites);
        particle.setScale(options.getSize());

        // ?桀??剜?湛
        int color = options.getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        particle.setColor(r, g, b);
        particle.setAlpha(options.getAlpha());

        // ?桀???豲??賹撞
        particle.setVelocity(xSpeed, ySpeed, zSpeed);

        KoniavacraftMod.LOGGER.debug("???CooParticleProvider: ?剜?={}, ?遴??0x{}, ?????{}",
            options.getSize(), Integer.toHexString(color), options.getAlpha());

        return particle;
    }
}
