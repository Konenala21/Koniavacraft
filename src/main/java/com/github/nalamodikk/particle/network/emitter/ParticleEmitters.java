package com.github.nalamodikk.particle.network.emitter;

import com.github.nalamodikk.particle.network.ServerControler;
import com.github.nalamodikk.particle.utils.RelativeLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * ????瞉?????? */
public interface ParticleEmitters extends ServerControler<ParticleEmitters> {
    Vec3 getPos();
    void setPos(Vec3 pos);

    Level getWorld();
    void setWorld(Level world);

    int getTick();
    void setTick(int tick);

    int getMaxTick();
    void setMaxTick(int maxTick);

    int getDelay();
    void setDelay(int delay);

    UUID getUuid();
    void setUuid(UUID uuid);

    boolean isCancelled();
    void setCancelled(boolean cancelled);

    boolean isPlaying();
    void setPlaying(boolean playing);

    String getEmittersID();

    void start();
    void stop();
    void tick();

    void spawnParticle(Vec3 pos, float lerpProgress);

    void update(ParticleEmitters emitters);

    StreamCodec<FriendlyByteBuf, ParticleEmitters> getCodec();

    @Override
    default ParticleEmitters getValue() {
        return this;
    }

    @Override
    default void remove() {
        setCancelled(true);
    }

    @Override
    default void rotateAsAxis(double angle) {}

    @Override
    default void rotateToPoint(RelativeLocation to) {}

    @Override
    default void rotateToWithAngle(RelativeLocation to, double angle) {}

    @Override
    default void teleportTo(Vec3 to) {
        setPos(to);
    }

    @Override
    default void teleportTo(double x, double y, double z) {
        teleportTo(new Vec3(x, y, z));
    }
}
