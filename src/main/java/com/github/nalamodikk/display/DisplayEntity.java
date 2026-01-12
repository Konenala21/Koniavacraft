package com.github.nalamodikk.display;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * 虛擬顯示實體基類
 * 用於處理非 Minecraft Entity 的渲染物體
 */
public abstract class DisplayEntity {
    protected final UUID uuid;
    protected Vec3 pos = Vec3.ZERO;
    protected Vec3 prevPos = Vec3.ZERO;
    protected float yaw = 0;
    protected float pitch = 0;
    protected float roll = 0;
    protected Vector3f rotation = new Vector3f(0, 0, 0);
    protected Vector3f prevRotation = new Vector3f(0, 0, 0);
    protected Vector3f scale = new Vector3f(1, 1, 1);
    protected Vector3f prevScale = new Vector3f(1, 1, 1);

    protected boolean removed = false;
    protected int age = 0;

    public DisplayEntity(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    /**
     * 每 Tick 更新邏輯 (伺服器與客戶端皆會執行)
     */
    public void tick() {
        this.prevPos = this.pos;
        this.prevRotation.set(this.rotation);
        this.prevScale.set(this.scale);
        this.age++;
    }

    /**
     * 獲取插值後的位置 (用於客戶端流暢渲染)
     */
    public Vec3 getLerpPos(float partialTick) {
        return new Vec3(
                prevPos.x + (pos.x - prevPos.x) * partialTick,
                prevPos.y + (pos.y - prevPos.y) * partialTick,
                prevPos.z + (pos.z - prevPos.z) * partialTick);
    }

    public void remove() {
        this.removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }

    /**
     * 序列化數據至緩衝區 (用於同步)
     */
    public abstract void write(FriendlyByteBuf buf);

    /**
     * 從緩衝區讀取數據
     */
    public abstract void read(FriendlyByteBuf buf);

    // Getter / Setter 省略...
    public void setPos(Vec3 pos) {
        this.pos = pos;
    }

    public Vec3 getPos() {
        return pos;
    }

    public int getAge() {
        return age;
    }
}
