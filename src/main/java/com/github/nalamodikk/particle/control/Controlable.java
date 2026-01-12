package com.github.nalamodikk.particle.control;

import com.github.nalamodikk.particle.utils.RelativeLocation;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/**
 * ?????伐????? */
public interface Controlable<T> {
    UUID controlUUID();
    
    void rotateToPoint(RelativeLocation to);
    
    void rotateToWithAngle(RelativeLocation to, double angle);

    /**
     * ?筆????
     * @param radian ?瞍?
     */
    void rotateAsAxis(double radian);
    
    void teleportTo(Vec3 pos);
    
    void teleportTo(double x, double y, double z);
    
    void remove();
    
    T getControlObject();

    @SuppressWarnings("unchecked")
    default <S> S getControlCasted() {
        return (S) getControlObject();
    }
}
