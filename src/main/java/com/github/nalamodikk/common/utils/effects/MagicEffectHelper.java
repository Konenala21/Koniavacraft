package com.github.nalamodikk.common.utils.effects;

import com.github.nalamodikk.experimental.effects.MagicEffectAPI;
import com.github.nalamodikk.experimental.effects.MagicEffectRegistry;
import com.github.nalamodikk.experimental.examples.MagicEffectExamples;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 魔法效果輔助工具類
 * 
 * 為 Koniavacraft 的各個組件提供便利的魔法效果創建方法
 */
public class MagicEffectHelper {
    
    // Koniavacraft 主題色彩
    public static final int KONIAVA_BLUE = 0xFF00AAFF;      // 科技藍
    public static final int KONIAVA_PURPLE = 0xFF9966FF;    // 魔法紫
    public static final int KONIAVA_GREEN = 0xFF00FF88;     // 能量綠
    public static final int KONIAVA_YELLOW = 0xFFFFDD00;    // 警告黃
    public static final int KONIAVA_RED = 0xFFFF3366;       // 錯誤紅
    public static final int KONIAVA_WHITE = 0xFFFFFFFF;     // 純白
    
    /**
     * 為魔力生成器創建工作效果
     * @param level 世界
     * @param generatorPos 生成器位置
     */
    @OnlyIn(Dist.CLIENT)
    public static void createManaGeneratorEffect(Level level, BlockPos generatorPos) {
        if (!level.isClientSide) return;
        
        MagicEffectAPI.createMagicCircle()
            .at(generatorPos)
            .withOffset(0, 1.1, 0) // 在生成器頂部
            .withColor(KONIAVA_GREEN)
            .withSize(0.8f)
            .withRotationSpeed(0.6f)
            .withDuration(100) // 5 秒
            .withGlowEffect(true)
            .spawn(level);
    }
    
    /**
     * 為魔力傳輸創建射線效果
     * @param level 世界
     * @param from 起始位置
     * @param to 目標位置
     * @param manaAmount 魔力數量 (影響射線粗細和亮度)
     */
    @OnlyIn(Dist.CLIENT)
    public static void createManaTransferEffect(Level level, Vec3 from, Vec3 to, int manaAmount) {
        if (!level.isClientSide) return;
        
        float thickness = Math.min(0.4f, 0.1f + (manaAmount / 10000.0f) * 0.3f);
        int duration = Math.max(40, Math.min(120, manaAmount / 100));
        
        MagicEffectAPI.createBeam()
            .from(from)
            .to(to)
            .withColor(KONIAVA_BLUE)
            .withThickness(thickness)
            .withPulseEffect(true)
            .withPulseSpeed(1.5f)
            .withDuration(duration)
            .withFadeInOut(true)
            .spawn(level);
    }
    
    /**
     * 為魔力合成創建儀式效果
     * @param level 世界
     * @param craftingPos 合成台位置
     */
    @OnlyIn(Dist.CLIENT)
    public static void createManaCraftingEffect(Level level, BlockPos craftingPos) {
        if (!level.isClientSide) return;
        
        MagicEffectExamples.createAdvancedMagicCircle(level, craftingPos);
        
        // 額外的粒子效果
        Vec3 center = Vec3.atCenterOf(craftingPos).add(0, 0.5, 0);
        MagicEffectAPI.createParticleTrail()
            .along(center)
            .withParticle(ParticleTypes.ENCHANT)
            .withDensity(8)
            .withSpeed(0.15f)
            .withSpread(0.8f)
            .withColorGradient(KONIAVA_PURPLE, KONIAVA_WHITE)
            .withSpiral(0.4f, 1.0f)
            .withDuration(120)
            .spawn(level);
    }
    
    /**
     * 為魔力灌注創建充能效果
     * @param level 世界
     * @param infuserPos 灌注器位置
     * @param itemPos 物品位置
     */
    @OnlyIn(Dist.CLIENT)
    public static void createManaInfusionEffect(Level level, BlockPos infuserPos, Vec3 itemPos) {
        if (!level.isClientSide) return;
        
        Vec3 infuserCenter = Vec3.atCenterOf(infuserPos).add(0, 0.8, 0);
        
        MagicEffectAPI.createCompositeEffect()
            // 灌注器的魔法陣
            .addMagicCircle(circle -> circle
                .at(infuserPos)
                .withOffset(0, 0.1, 0)
                .withColor(KONIAVA_YELLOW)
                .withSize(1.2f)
                .withDuration(150))
            
            // 向物品發射能量
            .addBeam(beam -> beam
                .from(infuserCenter)
                .to(itemPos)
                .withColor(KONIAVA_YELLOW)
                .withThickness(0.2f)
                .withPulseEffect(true)
                .withDuration(120), 20)
            
            // 物品周圍的粒子效果
            .addParticleTrail(trail -> trail
                .along(itemPos)
                .withParticle(ParticleTypes.GLOW)
                .withDensity(6)
                .withSpread(0.3f)
                .withDuration(100), 30)
            
            .spawn(level);
    }
    
    /**
     * 為奧術導管創建網路連接效果
     * @param level 世界
     * @param conduitPos 導管位置
     * @param connectedPositions 連接的位置列表
     */
    @OnlyIn(Dist.CLIENT)
    public static void createConduitNetworkEffect(Level level, BlockPos conduitPos, BlockPos... connectedPositions) {
        if (!level.isClientSide) return;
        
        Vec3 conduitCenter = Vec3.atCenterOf(conduitPos);
        
        // 為每個連接創建射線
        for (BlockPos targetPos : connectedPositions) {
            Vec3 targetCenter = Vec3.atCenterOf(targetPos);
            
            MagicEffectAPI.createBeam()
                .from(conduitCenter)
                .to(targetCenter)
                .withColor(KONIAVA_BLUE)
                .withThickness(0.1f)
                .withPulseEffect(true)
                .withPulseSpeed(0.8f)
                .withDuration(60)
                .withAlpha(0.6f) // 半透明，不太顯眼
                .spawn(level);
        }
    }
    
    /**
     * 為太陽能收集器創建收集效果
     * @param level 世界
     * @param collectorPos 收集器位置
     */
    @OnlyIn(Dist.CLIENT)
    public static void createSolarCollectionEffect(Level level, BlockPos collectorPos) {
        if (!level.isClientSide) return;
        
        // 從天空向下的能量束
        Vec3 skyPos = Vec3.atCenterOf(collectorPos).add(0, 10, 0);
        Vec3 collectorCenter = Vec3.atCenterOf(collectorPos).add(0, 1.0, 0);
        
        MagicEffectAPI.createBeam()
            .from(skyPos)
            .to(collectorCenter)
            .withColor(KONIAVA_YELLOW)
            .withThickness(0.3f)
            .withAnimation(true)
            .withAnimationSpeed(1.2f)
            .withDuration(80)
            .withFadeInOut(true)
            .spawn(level);
        
        // 收集器頂部的吸收效果
        MagicEffectAPI.createParticleTrail()
            .along(collectorCenter)
            .withParticle(ParticleTypes.END_ROD)
            .withDensity(5)
            .withSpeed(0.1f)
            .withSpread(0.5f)
            .withDuration(60)
            .spawn(level);
    }
    
    /**
     * 為機器錯誤創建警告效果
     * @param level 世界
     * @param machinePos 機器位置
     */
    @OnlyIn(Dist.CLIENT)
    public static void createMachineErrorEffect(Level level, BlockPos machinePos) {
        if (!level.isClientSide) return;
        
        MagicEffectAPI.createMagicCircle()
            .at(machinePos)
            .withOffset(0, 1.2, 0)
            .withColor(KONIAVA_RED)
            .withSize(1.0f)
            .withRotationSpeed(-1.0f) // 反向旋轉表示錯誤
            .withDuration(60)
            .withGlowEffect(true)
            .spawn(level);
    }
    
    /**
     * 為機器升級創建升級效果
     * @param level 世界
     * @param machinePos 機器位置
     */
    @OnlyIn(Dist.CLIENT)
    public static void createMachineUpgradeEffect(Level level, BlockPos machinePos) {
        if (!level.isClientSide) return;
        
        MagicEffectExamples.createSpellCastingEffect(level, machinePos, Vec3.atCenterOf(machinePos).add(0, 2, 0));
    }
    
    /**
     * 為傳送/瞬移創建效果
     * @param level 世界
     * @param fromPos 起始位置
     * @param toPos 目標位置
     */
    @OnlyIn(Dist.CLIENT)
    public static void createTeleportationEffect(Level level, BlockPos fromPos, BlockPos toPos) {
        if (!level.isClientSide) return;
        
        MagicEffectExamples.createTeleportEffect(level, fromPos, toPos);
    }
    
    /**
     * 檢查魔法效果 API 是否可用
     * @return 如果可用則返回 true
     */
    public static boolean isApiAvailable() {
        return MagicEffectAPI.isAvailable();
    }
    
    /**
     * 獲取當前活躍效果數量 (用於調試)
     * @return 活躍效果數量
     */
    @OnlyIn(Dist.CLIENT)
    public static int getActiveEffectCount() {
        return MagicEffectRegistry.getInstance().getActiveEffectCount();
    }
    
    /**
     * 清除所有效果 (用於調試或重置)
     */
    @OnlyIn(Dist.CLIENT)
    public static void clearAllEffects() {
        MagicEffectRegistry.getInstance().clearAllEffects();
    }
    
    /**
     * 獲取調試信息
     * @return 調試信息字串
     */
    @OnlyIn(Dist.CLIENT)
    public static String getDebugInfo() {
        return MagicEffectRegistry.getInstance().getDebugInfo();
    }
}