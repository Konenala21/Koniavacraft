package com.github.nalamodikk.barrages;

import com.github.nalamodikk.display.DisplayEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 重構後的彈幕基類
 * 繼承自 DisplayEntity 以獲取插值渲染與自動同步能力
 */
public abstract class Barrage extends DisplayEntity {
    protected Vec3 direction = Vec3.ZERO;
    protected HitBox hitBox = HitBox.of(0.5, 0.5, 0.5);
    protected boolean active = false;

    public Barrage(UUID uuid) {
        super(uuid);
    }

    @Override
    public void tick() {
        super.tick();
        if (active) {
            onUpdate();
        }
    }

    /**
     * 彈幕具體的物理與邏輯更新
     */
    protected abstract void onUpdate();

    public void launch(Vec3 pos, Vec3 direction) {
        this.pos = pos;
        this.prevPos = pos;
        this.direction = direction;
        this.active = true;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        buf.writeDouble(direction.x);
        buf.writeDouble(direction.y);
        buf.writeDouble(direction.z);
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        this.pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.direction = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
