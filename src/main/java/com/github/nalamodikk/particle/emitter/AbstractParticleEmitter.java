package com.github.nalamodikk.particle.emitter;

import com.github.nalamodikk.particle.manager.ParticleEmittersManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 粒子發射器抽象基類
 * 提供通用的發射器功能實現
 */
public abstract class AbstractParticleEmitter implements ParticleEmitter {
    protected Vec3 pos;
    protected Level world;
    protected int tick = 0;
    protected int maxTick = 120; // 預設 6 秒
    protected int delay = 1; // 每 tick 發射
    protected final UUID uuid;
    protected boolean cancelled = false;
    protected boolean playing = false;
    protected double visibleRange = 32.0;

    protected AbstractParticleEmitter(Vec3 pos, Level world) {
        this.pos = pos;
        this.world = world;
        this.uuid = UUID.randomUUID();
    }

    protected AbstractParticleEmitter(Vec3 pos, Level world, UUID uuid) {
        this.pos = pos;
        this.world = world;
        this.uuid = uuid;
    }

    // ========== Getter/Setter ==========

    @Override
    public Vec3 getPos() {
        return pos;
    }

    @Override
    public void setPos(Vec3 pos) {
        this.pos = pos;
    }

    @Override
    public Level getWorld() {
        return world;
    }

    @Override
    public void setWorld(Level world) {
        this.world = world;
    }

    @Override
    public int getTick() {
        return tick;
    }

    @Override
    public void setTick(int tick) {
        this.tick = tick;
    }

    @Override
    public int getMaxTick() {
        return maxTick;
    }

    @Override
    public void setMaxTick(int maxTick) {
        this.maxTick = maxTick;
    }

    @Override
    public int getDelay() {
        return delay;
    }

    @Override
    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isPlaying() {
        return playing;
    }

    @Override
    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    @Override
    public double getVisibleRange() {
        return visibleRange;
    }

    @Override
    public void setVisibleRange(double range) {
        this.visibleRange = range;
    }

    // ========== 生命週期方法 ==========

    @Override
    public void start() {
        if (playing) {
            return;
        }
        playing = true;

        // 服務器端通知更新
        if (world != null && !world.isClientSide()) {
            ParticleEmittersManager.getInstance().updateEmitter(this);
        }
    }

    @Override
    public void stop() {
        cancelled = true;

        // 服務器端通知更新
        if (world != null && !world.isClientSide()) {
            ParticleEmittersManager.getInstance().updateEmitter(this);
        }
    }

    @Override
    public void tick() {
        if (cancelled || !playing) {
            return;
        }

        if (world == null) {
            return;
        }

        // 子類實現的 tick 邏輯
        doTick();

        // 服務器端只負責計數，不生成粒子
        if (!world.isClientSide()) {
            increaseTick();
            return;
        }

        // 客戶端：按延遲發射粒子
        if (tick % Math.max(1, delay) == 0) {
            spawnParticle(pos, 1.0f);
        }

        increaseTick();
    }

    /**
     * 子類實現的 tick 邏輯（服務器和客戶端都會執行）
     */
    protected abstract void doTick();

    /**
     * 增加 tick 計數，檢查是否達到最大生命週期
     */
    private void increaseTick() {
        tick++;
        if (maxTick != -1 && tick >= maxTick) {
            stop();
        }
    }

    /**
     * 更新發射器狀態（從網絡包接收數據時調用）
     * @param emitter 新的發射器數據
     */
    @Override
    public void update(ParticleEmitter emitter) {
        this.pos = emitter.getPos();
        this.tick = emitter.getTick();
        this.maxTick = emitter.getMaxTick();
        this.delay = emitter.getDelay();
        this.cancelled = emitter.isCancelled();
        this.playing = emitter.isPlaying();
        this.visibleRange = emitter.getVisibleRange();
    }
}
