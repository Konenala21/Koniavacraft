package com.github.nalamodikk.display.impl;

import com.github.nalamodikk.display.DisplayEntity;
import com.github.nalamodikk.display.DisplayEntityRegister;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 魔法陣顯示實體
 * 用於展示旋轉的魔法陣效果
 */
@DisplayEntityRegister(type = "magic_circle")
public class MagicCircleDisplayEntity extends DisplayEntity {
    private float radius = 2.0f;
    private float rotationSpeed = 2.0f;
    private int color = 0x00FFFF; // 青色
    private float alpha = 0.8f;

    public MagicCircleDisplayEntity(UUID uuid) {
        super(uuid);
    }

    @Override
    public void tick() {
        super.tick();
        // 自動旋轉
        this.yaw += rotationSpeed;
        if (this.yaw > 360)
            this.yaw -= 360;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        buf.writeFloat(yaw);
        buf.writeFloat(radius);
        buf.writeInt(color);
        buf.writeFloat(alpha);
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.yaw = buf.readFloat();
        this.radius = buf.readFloat();
        this.color = buf.readInt();
        this.alpha = buf.readFloat();
    }

    // Getters and Setters
    public float getYaw() {
        return yaw;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public float getRotationSpeed() {
        return rotationSpeed;
    }

    public void setRotationSpeed(float speed) {
        this.rotationSpeed = speed;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }
}
