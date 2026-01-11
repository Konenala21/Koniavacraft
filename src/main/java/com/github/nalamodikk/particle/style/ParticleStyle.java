package com.github.nalamodikk.particle.style;

import com.github.nalamodikk.particle.utils.math.RelativeLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

/**
 * 粒子樣式基礎介面
 */
public interface ParticleStyle {
    UUID getUuid();
    
    /**
     * 獲取當前幀的所有粒子數據與相對位置
     */
    Map<StyleData, RelativeLocation> getCurrentFrames();

    /**
     * 在指定位置顯示特效
     */
    void display(Level world, Vec3 pos);

    /**
     * 特效邏輯更新
     */
    void tick();

    /**
     * 移除特效
     */
    void remove();

    /**
     * 樣式數據類別
     * 定義單一粒子的渲染屬性
     */
    class StyleData {
        public final UUID uuid = UUID.randomUUID();
        // 這裡未來會連結到渲染器與控制器回呼
        public final String particleType; // 暫時用 String 或 ResourceLocation 代替

        public StyleData(String particleType) {
            this.particleType = particleType;
        }
    }
}
