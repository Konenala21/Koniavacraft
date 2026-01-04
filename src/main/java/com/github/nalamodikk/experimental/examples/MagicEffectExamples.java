package com.github.nalamodikk.experimental.examples;

import com.github.nalamodikk.experimental.effects.MagicEffectAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * MagicEffect API 使用範例
 * 
 * 展示如何在 Koniavacraft 中使用魔法效果 API 創建各種視覺效果
 */
@OnlyIn(Dist.CLIENT)
public class MagicEffectExamples {
    
    /**
     * 範例 1: 簡單的魔法陣效果
     * 適用於：方塊放置、機器啟動等場景
     */
    public static void createSimpleMagicCircle(Level level, BlockPos pos) {
        MagicEffectAPI.spawnMagicCircle(level, pos, 0xFFFFFF00, 100); // 黃色，持續 5 秒
    }
    
    /**
     * 範例 2: 高級魔法陣效果
     * 適用於：儀式法陣、傳送門等重要場景
     */
    public static void createAdvancedMagicCircle(Level level, BlockPos pos) {
        MagicEffectAPI.createMagicCircle()
            .at(pos)
            .withOffset(0, 0.1, 0) // 稍微抬高
            .withColor(0xFF9966FF) // 紫色
            .withSize(2.5f) // 較大的魔法陣
            .withRotationSpeed(0.8f) // 較快的旋轉
            .withDuration(200) // 10 秒
            .withGlowEffect(true)
            .onComplete(effect -> {
                // 魔法陣完成時的回調
                createCompletionParticles(level, pos);
            })
            .spawn(level);
    }
    
    /**
     * 範例 3: 簡單的能量射線
     * 適用於：魔力傳輸、攻擊效果等
     */
    public static void createSimpleBeam(Level level, Vec3 from, Vec3 to) {
        MagicEffectAPI.spawnBeam(level, from, to, 0xFF00FFFF, 60); // 青色，持續 3 秒
    }
    
    /**
     * 範例 4: 脈衝能量射線
     * 適用於：充能過程、能量傳輸等
     */
    public static void createPulsingBeam(Level level, Vec3 from, Vec3 to) {
        MagicEffectAPI.createBeam()
            .from(from)
            .to(to)
            .withColor(0xFF00FF00) // 綠色
            .withThickness(0.4f) // 較粗的射線
            .withPulseEffect(true)
            .withPulseSpeed(2.0f) // 快速脈衝
            .withDuration(120) // 6 秒
            .withParticleTrail(true) // 帶粒子軌跡
            .spawn(level);
    }
    
    /**
     * 範例 5: 實體間的動態射線
     * 適用於：實體連接、追蹤效果等
     */
    public static void createEntityBeam(Level level, Entity from, Entity to) {
        MagicEffectAPI.createBeam()
            .fromEntity(from)
            .toEntity(to)
            .withColor(0xFFFF6600) // 橙色
            .withThickness(0.2f)
            .withFadeInOut(true)
            .withDuration(80) // 4 秒
            .onTick(beam -> {
                // 每 tick 檢查實體是否仍然有效
                if (from.isRemoved() || to.isRemoved()) {
                    beam.finish();
                }
            })
            .spawn(level);
    }
    
    /**
     * 範例 6: 粒子軌跡效果
     * 適用於：路徑指示、魔法流動等
     */
    public static void createParticleTrail(Level level, Vec3 start, Vec3 end) {
        MagicEffectAPI.createParticleTrail()
            .between(start, end)
            .withParticle(ParticleTypes.ENCHANT)
            .withDensity(8) // 較高的密度
            .withSpeed(0.1f)
            .withLifetime(60) // 粒子存活 3 秒
            .withColorGradient(0xFFFFFF00, 0xFFFF0000) // 黃色到紅色的漸變
            .withSpiral(0.3f, 1.5f) // 螺旋效果
            .withDuration(100) // 總持續 5 秒
            .spawn(level);
    }
    
    /**
     * 範例 7: 跟隨實體的粒子軌跡
     * 適用於：實體光環、移動效果等
     */
    public static void createEntityTrail(Level level, Entity entity) {
        MagicEffectAPI.createParticleTrail()
            .followEntity(entity)
            .withParticle(ParticleTypes.SOUL)
            .withDensity(3)
            .withSpread(0.2f) // 輕微擴散
            .withDuration(300) // 15 秒
            .onComplete(trail -> {
                // 軌跡結束時的清理工作
            })
            .spawn(level);
    }
    
    /**
     * 範例 8: 複合施法效果
     * 組合魔法陣、射線和粒子軌跡創建完整的施法場面
     */
    public static void createSpellCastingEffect(Level level, BlockPos casterPos, Vec3 targetPos) {
        Vec3 casterCenter = Vec3.atCenterOf(casterPos);
        
        MagicEffectAPI.createCompositeEffect()
            // 首先顯示魔法陣
            .addMagicCircle(circle -> circle
                .at(casterPos)
                .withColor(0xFFAA00FF) // 紫色魔法陣
                .withSize(1.8f)
                .withDuration(120)
                .withGlowEffect(true))
            
            // 延遲 20 ticks 後發射射線
            .addBeam(beam -> beam
                .from(casterCenter.add(0, 0.5, 0))
                .to(targetPos)
                .withColor(0xFFAA00FF) // 與魔法陣相同顏色
                .withThickness(0.25f)
                .withPulseEffect(true)
                .withDuration(60), 20) // 延遲 20 ticks
            
            // 同時添加粒子軌跡
            .addParticleTrail(trail -> trail
                .between(casterCenter, targetPos)
                .withParticle(ParticleTypes.WITCH)
                .withDensity(5)
                .withColorGradient(0xFFAA00FF, 0xFFFFFFFF)
                .withDuration(80), 20) // 延遲 20 ticks
            
            .onComplete(composite -> {
                // 整個法術完成時創建爆炸粒子效果
                createSpellImpactEffect(level, targetPos);
            })
            .spawn(level);
    }
    
    /**
     * 範例 9: 機器工作效果
     * 適用於：Koniavacraft 的機器運作視覺反饋
     */
    public static void createMachineWorkingEffect(Level level, BlockPos machinePos, BlockPos targetPos) {
        Vec3 machineCenter = Vec3.atCenterOf(machinePos).add(0, 1.0, 0);
        Vec3 targetCenter = Vec3.atCenterOf(targetPos);
        
        // 機器頂部的小型魔法陣
        MagicEffectAPI.createMagicCircle()
            .at(machinePos)
            .withOffset(0, 1.2, 0)
            .withColor(0xFF00AAFF) // 藍色，表示科技感
            .withSize(0.8f) // 小型魔法陣
            .withRotationSpeed(1.2f) // 快速旋轉
            .withDuration(100)
            .spawn(level);
        
        // 向目標發射能量射線
        MagicEffectAPI.createBeam()
            .from(machineCenter)
            .to(targetCenter)
            .withColor(0xFF00AAFF)
            .withThickness(0.15f)
            .withPulseEffect(true)
            .withPulseSpeed(3.0f) // 快速脈衝表示高效率
            .withDuration(80)
            .spawn(level);
    }
    
    /**
     * 範例 10: 傳送效果
     * 完整的傳送視覺效果序列
     */
    public static void createTeleportEffect(Level level, BlockPos fromPos, BlockPos toPos) {
        Vec3 fromCenter = Vec3.atCenterOf(fromPos);
        Vec3 toCenter = Vec3.atCenterOf(toPos);
        
        MagicEffectAPI.createCompositeEffect()
            // 起始位置的傳送門
            .addMagicCircle(circle -> circle
                .at(fromPos)
                .withColor(0xFFFF00FF) // 洋紅色
                .withSize(2.0f)
                .withDuration(100)
                .withGlowEffect(true))
            
            // 目標位置的傳送門（稍有延遲）
            .addMagicCircle(circle -> circle
                .at(toPos)
                .withColor(0xFFFF00FF)
                .withSize(2.0f)
                .withDuration(100)
                .withGlowEffect(true), 10)
            
            // 連接兩個傳送門的能量流
            .addBeam(beam -> beam
                .from(fromCenter)
                .to(toCenter)
                .withColor(0xFFFF00FF)
                .withThickness(0.3f)
                .withAnimation(true)
                .withAnimationSpeed(2.0f)
                .withDuration(60), 20)
            
            // 傳送粒子效果
            .addParticleTrail(trail -> trail
                .between(fromCenter, toCenter)
                .withParticle(ParticleTypes.PORTAL)
                .withDensity(10)
                .withSpeed(0.2f)
                .withDuration(80), 15)
            
            .spawn(level);
    }
    
    // ========== 輔助方法 ==========
    
    private static void createCompletionParticles(Level level, BlockPos pos) {
        // 創建魔法陣完成時的粒子爆發效果
        Vec3 center = Vec3.atCenterOf(pos).add(0, 0.1, 0);
        
        MagicEffectAPI.createParticleTrail()
            .along(center) // 單點爆發
            .withParticle(ParticleTypes.FIREWORK)
            .withDensity(15)
            .withSpeed(0.3f)
            .withSpread(1.0f) // 大範圍擴散
            .withDuration(20) // 短暫爆發
            .spawn(level);
    }
    
    private static void createSpellImpactEffect(Level level, Vec3 impactPos) {
        // 創建法術命中時的衝擊效果
        MagicEffectAPI.createCompositeEffect()
            .addMagicCircle(circle -> circle
                .at(new BlockPos((int)impactPos.x, (int)impactPos.y, (int)impactPos.z))
                .withColor(0xFFFFFFFF) // 白色爆炸
                .withSize(1.5f)
                .withDuration(30)) // 短暫閃光
            
            .addParticleTrail(trail -> trail
                .along(impactPos)
                .withParticle(ParticleTypes.EXPLOSION)
                .withDensity(20)
                .withSpeed(0.4f)
                .withSpread(1.5f)
                .withDuration(15))
            
            .spawn(level);
    }
}