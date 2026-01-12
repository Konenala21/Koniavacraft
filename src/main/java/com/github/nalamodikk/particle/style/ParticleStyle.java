package com.github.nalamodikk.particle.style;

import com.github.nalamodikk.particle.utils.RelativeLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

/**
 * 粒子樣式接口
 */
public interface ParticleStyle {
    UUID getUuid();

    /**
     * 獲取當前幀的粒子數據
     */
    Map<StyleData, RelativeLocation> getCurrentFrames();

    /**
     * 顯示粒子樣式
     */
    void display(Level world, Vec3 pos);

    /**
     * 每 tick 更新
     */
    void tick();

    /**
     * 移除樣式
     */
    void remove();

    /**
     * 粒子樣式數據
     */
    class StyleData {
        public final UUID uuid = UUID.randomUUID();
        public final String particleType;

        public StyleData(String particleType) {
            this.particleType = particleType;
        }

        public UUID getUuid() {
            return uuid;
        }
    }
}
