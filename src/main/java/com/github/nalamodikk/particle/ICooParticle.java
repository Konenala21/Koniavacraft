package com.github.nalamodikk.particle;

import org.joml.Quaternionf;

import java.util.UUID;

/**
 * ?????對??鈭
 * ?堊垓?????踐??????剛????????????? */
public interface ICooParticle {
    void setPosition(double x, double y, double z);
    void setVelocity(double vx, double vy, double vz);
    void setColor(float r, float g, float b);
    void setAlpha(float alpha);
    void setScale(float scale);
    void setRotation(Quaternionf rotation);
    void setFaceToCamera(boolean faceToCamera);
    void remove();
    UUID getParticleId();
}
