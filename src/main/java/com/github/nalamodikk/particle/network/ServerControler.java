package com.github.nalamodikk.particle.network;

import com.github.nalamodikk.particle.utils.RelativeLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * ?∠捂???踐?????豰?
 * ??踐???嗆????∟??????殉???撖?????? */
public interface ServerControler<T> {
    void teleportTo(Vec3 to);

    void teleportTo(double x, double y, double z);

    void rotateToPoint(RelativeLocation to);

    void rotateToWithAngle(RelativeLocation to, double angle);

    void rotateAsAxis(double angle);

    void remove();

    /**
     * ???????賹?
     */
    void spawn(Level world, Vec3 pos);

    T getValue();
}
