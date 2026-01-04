package com.github.nalamodikk.experimental.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 魔法效果 API - Koniavacraft 魔法視覺效果庫的主要入口
 * 
 * 這個 API 允許其他模組創建和管理高級魔法視覺效果，包括：
 * - 魔法陣 (Magic Circles)
 * - 能量射線 (Energy Beams) 
 * - 粒子軌跡 (Particle Trails)
 * 
 * @author Koniavacraft Team
 * @version 1.0.0
 * @since 1.21.1
 */
@OnlyIn(Dist.CLIENT)
public final class MagicEffectAPI {
    
    private static final String MOD_ID = "koniava";
    private static final String VERSION = "1.0.0";
    
    private MagicEffectAPI() {
        // 防止實例化
    }
    
    /**
     * 獲取 API 版本號
     * @return API 版本字串
     */
    public static String getVersion() {
        return VERSION;
    }
    
    /**
     * 檢查 API 是否可用
     * @return 如果 Koniavacraft 模組已加載且 API 可用則返回 true
     */
    public static boolean isAvailable() {
        // TODO: 實現模組檢查邏輯
        return true;
    }
    
    // ========== 魔法陣 API ==========
    
    /**
     * 創建一個新的魔法陣效果建構器
     * @return 魔法陣建構器實例
     */
    public static MagicCircleBuilder createMagicCircle() {
        return new MagicCircleBuilder();
    }
    
    /**
     * 在指定位置創建簡單的魔法陣
     * @param level 世界實例
     * @param pos 方塊位置
     * @param color 顏色 (ARGB 格式)
     * @param duration 持續時間 (ticks)
     */
    public static void spawnMagicCircle(Level level, BlockPos pos, int color, int duration) {
        createMagicCircle()
            .at(pos)
            .withColor(color)
            .withDuration(duration)
            .spawn(level);
    }
    
    // ========== 能量射線 API ==========
    
    /**
     * 創建一個新的能量射線效果建構器
     * @return 射線建構器實例
     */
    public static BeamBuilder createBeam() {
        return new BeamBuilder();
    }
    
    /**
     * 在兩點間創建簡單的能量射線
     * @param level 世界實例
     * @param from 起始位置
     * @param to 目標位置
     * @param color 顏色 (ARGB 格式)
     * @param duration 持續時間 (ticks)
     */
    public static void spawnBeam(Level level, Vec3 from, Vec3 to, int color, int duration) {
        createBeam()
            .from(from)
            .to(to)
            .withColor(color)
            .withDuration(duration)
            .spawn(level);
    }
    
    // ========== 粒子軌跡 API ==========
    
    /**
     * 創建一個新的粒子軌跡效果建構器
     * @return 粒子軌跡建構器實例
     */
    public static ParticleTrailBuilder createParticleTrail() {
        return new ParticleTrailBuilder();
    }
    
    // ========== 複合效果 API ==========
    
    /**
     * 創建一個複合效果建構器，可以組合多種效果
     * @return 複合效果建構器實例
     */
    public static CompositeEffectBuilder createCompositeEffect() {
        return new CompositeEffectBuilder();
    }
    
    /**
     * 創建施法效果 (魔法陣 + 射線的組合)
     * @param level 世界實例
     * @param casterPos 施法者位置
     * @param targetPos 目標位置
     * @param spellColor 法術顏色
     */
    public static void createSpellCastEffect(Level level, Vec3 casterPos, Vec3 targetPos, int spellColor) {
        createCompositeEffect()
            .addMagicCircle(circle -> circle
                .at(new BlockPos((int)casterPos.x, (int)casterPos.y, (int)casterPos.z))
                .withColor(spellColor)
                .withDuration(60))
            .addBeam(beam -> beam
                .from(casterPos.add(0, 0.1, 0))
                .to(targetPos)
                .withColor(spellColor)
                .withDuration(40))
            .spawn(level);
    }
}