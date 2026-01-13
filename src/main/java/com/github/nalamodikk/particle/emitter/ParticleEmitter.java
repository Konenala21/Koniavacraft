package com.github.nalamodikk.particle.emitter;

import com.github.nalamodikk.particle.network.ServerControler;
import com.github.nalamodikk.particle.utils.RelativeLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 粒子發射器接口
 * 負責持續發射粒子並管理其生命週期
 */
public interface ParticleEmitter extends ServerControler<ParticleEmitter> {

    // ========== 基礎屬性 ==========

    Vec3 getPos();
    void setPos(Vec3 pos);

    Level getWorld();
    void setWorld(Level world);

    int getTick();
    void setTick(int tick);

    /**
     * 最大生命週期（-1 表示無限）
     */
    int getMaxTick();
    void setMaxTick(int maxTick);

    /**
     * 發射延遲（每 N tick 發射一次）
     */
    int getDelay();
    void setDelay(int delay);

    UUID getUuid();

    boolean isCancelled();
    void setCancelled(boolean cancelled);

    boolean isPlaying();
    void setPlaying(boolean playing);

    /**
     * 可見範圍（用於網絡同步優化）
     */
    double getVisibleRange();
    void setVisibleRange(double range);

    // ========== 生命週期方法 ==========

    /**
     * 獲取發射器 ID（用於註冊和網絡同步）
     */
    String getEmitterId();

    /**
     * 開始發射粒子
     */
    void start();

    /**
     * 停止發射粒子
     */
    void stop();

    /**
     * 每 tick 更新
     */
    void tick();

    /**
     * 在指定位置發射粒子
     * @param pos 發射位置
     * @param lerpProgress 插值進度（0.0-1.0）
     */
    void spawnParticle(Vec3 pos, float lerpProgress);

    /**
     * 更新發射器狀態
     * @param emitter 新的發射器數據
     */
    void update(ParticleEmitter emitter);

    /**
     * 獲取編解碼器
     */
    StreamCodec<FriendlyByteBuf, ? extends ParticleEmitter> getCodec();

    // ========== ServerControler 實現 ==========

    /**
     * 獲取控制 UUID（與 getUuid() 相同）
     */
    default UUID controlUUID() {
        return getUuid();
    }

    /**
     * 獲取控制對象（返回自己）
     */
    default ParticleEmitter getControlObject() {
        return this;
    }

    @Override
    default ParticleEmitter getValue() {
        return this;
    }

    @Override
    default void remove() {
        setCancelled(true);
    }

    @Override
    default void spawn(Level world, Vec3 pos) {
        if (!(world instanceof ServerLevel)) {
            return;
        }
        setWorld(world);
        setPos(pos);
        // ParticleEmittersManager 會處理實際的生成
    }

    @Override
    default void rotateAsAxis(double angle) {
        // 發射器通常不需要旋轉
    }

    @Override
    default void rotateToPoint(RelativeLocation to) {
        // 發射器通常不需要旋轉
    }

    @Override
    default void rotateToWithAngle(RelativeLocation to, double angle) {
        // 發射器通常不需要旋轉
    }

    @Override
    default void teleportTo(Vec3 to) {
        setPos(to);
    }

    @Override
    default void teleportTo(double x, double y, double z) {
        teleportTo(new Vec3(x, y, z));
    }
}
