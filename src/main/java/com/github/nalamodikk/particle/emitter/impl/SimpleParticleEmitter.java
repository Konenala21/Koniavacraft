package com.github.nalamodikk.particle.emitter.impl;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.emitter.AbstractParticleEmitter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 簡單粒子發射器示例
 * 在固定位置發射原版粒子
 */
public class SimpleParticleEmitter extends AbstractParticleEmitter {
    public static final String ID = KoniavacraftMod.MOD_ID + ":simple_emitter";

    /** 編解碼器 */
    public static final StreamCodec<FriendlyByteBuf, SimpleParticleEmitter> CODEC = new StreamCodec<>() {
        @Override
        public SimpleParticleEmitter decode(FriendlyByteBuf buf) {
            Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            int tick = buf.readInt();
            int maxTick = buf.readInt();
            int delay = buf.readInt();
            UUID uuid = buf.readUUID();
            boolean cancelled = buf.readBoolean();
            boolean playing = buf.readBoolean();
            double visibleRange = buf.readDouble();
            int particleCount = buf.readInt();
            double spread = buf.readDouble();

            SimpleParticleEmitter emitter = new SimpleParticleEmitter(pos, null);
            emitter.setTick(tick);
            emitter.setMaxTick(maxTick);
            emitter.setDelay(delay);
            emitter.setCancelled(cancelled);
            emitter.setPlaying(playing);
            emitter.setVisibleRange(visibleRange);
            emitter.particleCount = particleCount;
            emitter.spread = spread;

            return emitter;
        }

        @Override
        public void encode(FriendlyByteBuf buf, SimpleParticleEmitter emitter) {
            buf.writeDouble(emitter.pos.x);
            buf.writeDouble(emitter.pos.y);
            buf.writeDouble(emitter.pos.z);
            buf.writeInt(emitter.tick);
            buf.writeInt(emitter.maxTick);
            buf.writeInt(emitter.delay);
            buf.writeUUID(emitter.uuid);
            buf.writeBoolean(emitter.cancelled);
            buf.writeBoolean(emitter.playing);
            buf.writeDouble(emitter.visibleRange);
            buf.writeInt(emitter.particleCount);
            buf.writeDouble(emitter.spread);
        }
    };

    // ========== 發射器參數 ==========

    /** 每次發射的粒子數量 */
    private int particleCount = 5;

    /** 粒子擴散範圍 */
    private double spread = 0.5;

    /** 要發射的粒子類型 */
    private ParticleOptions particleType = ParticleTypes.FLAME;

    public SimpleParticleEmitter(Vec3 pos, Level world) {
        super(pos, world);
        this.maxTick = 100; // 5 秒
        this.delay = 2; // 每 2 tick 發射一次
    }

    public SimpleParticleEmitter setParticleCount(int count) {
        this.particleCount = count;
        return this;
    }

    public SimpleParticleEmitter setSpread(double spread) {
        this.spread = spread;
        return this;
    }

    public SimpleParticleEmitter setParticleType(ParticleOptions type) {
        this.particleType = type;
        return this;
    }

    @Override
    public String getEmitterId() {
        return ID;
    }

    @Override
    public StreamCodec<FriendlyByteBuf, SimpleParticleEmitter> getCodec() {
        return CODEC;
    }

    @Override
    protected void doTick() {
        // 可以在這裡添加服務器端邏輯
        // 例如：檢測周圍實體、更新位置等
    }

    @Override
    public void spawnParticle(Vec3 pos, float lerpProgress) {
        if (world == null || !world.isClientSide()) {
            return;
        }

        ClientLevel clientLevel = (ClientLevel) world;

        // 在指定位置周圍隨機發射粒子
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (Math.random() - 0.5) * spread;
            double offsetY = (Math.random() - 0.5) * spread;
            double offsetZ = (Math.random() - 0.5) * spread;

            double velocityX = (Math.random() - 0.5) * 0.1;
            double velocityY = (Math.random() - 0.5) * 0.1;
            double velocityZ = (Math.random() - 0.5) * 0.1;

            clientLevel.addParticle(
                particleType,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                velocityX,
                velocityY,
                velocityZ
            );
        }
    }

    @Override
    public void update(com.github.nalamodikk.particle.emitter.ParticleEmitter emitter) {
        super.update(emitter);
        if (emitter instanceof SimpleParticleEmitter simple) {
            this.particleCount = simple.particleCount;
            this.spread = simple.spread;
            // 注意：particleType 不能通過網絡同步，需要客戶端自己設置
        }
    }
}
