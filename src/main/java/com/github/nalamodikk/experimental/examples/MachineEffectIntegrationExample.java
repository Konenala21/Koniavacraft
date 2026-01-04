package com.github.nalamodikk.experimental.examples;

import com.github.nalamodikk.experimental.effects.MagicEffectAPI;
import com.github.nalamodikk.common.utils.effects.MagicEffectHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 機器效果整合示例
 * 
 * 展示如何在現有的 Koniavacraft 機器中整合魔法效果
 * 這些程式碼片段可以直接複製到對應的 BlockEntity 類別中
 */
public class MachineEffectIntegrationExample {
    
    /**
     * ========================================
     * ManaGeneratorBlockEntity 整合示例
     * ========================================
     * 
     * 在 ManaGeneratorBlockEntity.java 的 tick() 方法中添加：
     */
    @OnlyIn(Dist.CLIENT)
    public static void addManaGeneratorEffects(Level level, BlockPos pos, boolean isWorking, int workProgress) {
        if (!level.isClientSide) return;
        
        // 基礎工作效果 - 每 60 ticks 顯示一次
        if (isWorking && level.getGameTime() % 60 == 0) {
            MagicEffectHelper.createManaGeneratorEffect(level, pos);
        }
        
        // 高效率時的額外效果
        if (isWorking && workProgress > 80) {
            // 每 40 ticks 顯示強化效果
            if (level.getGameTime() % 40 == 0) {
                MagicEffectAPI.createMagicCircle()
                    .at(pos)
                    .withOffset(0, 1.5, 0)
                    .withColor(MagicEffectHelper.KONIAVA_GREEN)
                    .withSize(0.8f)
                    .withRotationSpeed(1.0f)
                    .withDuration(50)
                    .withGlowEffect(true)
                    .spawn(level);
            }
        }
    }
    
    /**
     * ========================================
     * ArcaneConduitBlockEntity 整合示例
     * ========================================
     * 
     * 在 ArcaneConduitBlockEntity.java 中添加網路視覺化：
     */
    @OnlyIn(Dist.CLIENT)
    public static void addConduitNetworkEffects(Level level, BlockPos conduitPos, BlockPos[] connectedPositions, 
                                               boolean isTransferring, int transferAmount) {
        if (!level.isClientSide) return;
        
        // 顯示網路連接 - 只在配置模式或除錯時顯示
        if (shouldShowNetworkConnections()) {
            MagicEffectHelper.createConduitNetworkEffect(level, conduitPos, connectedPositions);
        }
        
        // 顯示實際傳輸效果
        if (isTransferring && connectedPositions.length > 0) {
            Vec3 conduitCenter = Vec3.atCenterOf(conduitPos);
            
            for (BlockPos targetPos : connectedPositions) {
                if (level.getGameTime() % 20 == 0) { // 每秒一次
                    MagicEffectHelper.createManaTransferEffect(level, conduitCenter, 
                        Vec3.atCenterOf(targetPos), transferAmount);
                }
            }
        }
    }
    
    private static boolean shouldShowNetworkConnections() {
        // 實現邏輯：檢查玩家是否手持配置工具等
        return false;
    }
    
    /**
     * ========================================
     * ManaCraftingTableBlockEntity 整合示例
     * ========================================
     * 
     * 在合成過程中添加視覺效果：
     */
    @OnlyIn(Dist.CLIENT)
    public static void addCraftingEffects(Level level, BlockPos pos, int craftingProgress, int maxProgress, boolean isComplete) {
        if (!level.isClientSide) return;
        
        // 合成進行中的效果
        if (craftingProgress > 0 && craftingProgress < maxProgress) {
            if (level.getGameTime() % 30 == 0) { // 每 1.5 秒
                float progress = (float) craftingProgress / maxProgress;
                
                MagicEffectAPI.createMagicCircle()
                    .at(pos)
                    .withOffset(0, 0.8, 0)
                    .withColor(interpolateColor(MagicEffectHelper.KONIAVA_YELLOW, MagicEffectHelper.KONIAVA_PURPLE, progress))
                    .withSize(1.2f + progress * 0.5f) // 隨進度增大
                    .withRotationSpeed(0.5f + progress)
                    .withDuration(40)
                    .withGlowEffect(true)
                    .spawn(level);
            }
        }
        
        // 合成完成的爆發效果
        if (isComplete) {
            MagicEffectHelper.createManaCraftingEffect(level, pos);
            
            // 額外的完成慶祝效果
            MagicEffectAPI.createCompositeEffect()
                .addMagicCircle(circle -> circle
                    .at(pos)
                    .withColor(MagicEffectHelper.KONIAVA_WHITE)
                    .withSize(2.0f)
                    .withDuration(60))
                
                .addParticleTrail(trail -> trail
                    .along(Vec3.atCenterOf(pos).add(0, 1, 0))
                    .withDensity(15)
                    .withSpeed(0.3f)
                    .withSpread(1.0f)
                    .withDuration(40), 10)
                
                .spawn(level);
        }
    }
    
    /**
     * ========================================
     * ManaInfuserBlockEntity 整合示例
     * ========================================
     * 
     * 在灌注過程中添加視覺效果：
     */
    @OnlyIn(Dist.CLIENT)
    public static void addInfusionEffects(Level level, BlockPos infuserPos, Vec3 itemPos, 
                                         int infusionProgress, int maxProgress, boolean isActive) {
        if (!level.isClientSide) return;
        
        if (isActive && level.getGameTime() % 25 == 0) { // 每 1.25 秒
            MagicEffectHelper.createManaInfusionEffect(level, infuserPos, itemPos);
            
            // 根據進度調整效果強度
            float progress = (float) infusionProgress / maxProgress;
            
            if (progress > 0.5f) {
                // 進度過半時添加額外的能量流
                MagicEffectAPI.createBeam()
                    .from(Vec3.atCenterOf(infuserPos).add(0, 0.5, 0))
                    .to(itemPos)
                    .withColor(MagicEffectHelper.KONIAVA_PURPLE)
                    .withThickness(0.1f + progress * 0.2f)
                    .withPulseEffect(true)
                    .withPulseSpeed(2.0f + progress * 2.0f)
                    .withDuration(30)
                    .spawn(level);
            }
        }
    }
    
    /**
     * ========================================
     * SolarManaCollectorBlockEntity 整合示例
     * ========================================
     * 
     * 在太陽能收集時添加視覺效果：
     */
    @OnlyIn(Dist.CLIENT)
    public static void addSolarCollectionEffects(Level level, BlockPos collectorPos, boolean isCollecting, 
                                                 int collectionRate, boolean isDaytime) {
        if (!level.isClientSide) return;
        
        if (isCollecting && isDaytime && level.getGameTime() % 80 == 0) { // 每 4 秒
            MagicEffectHelper.createSolarCollectionEffect(level, collectorPos);
            
            // 根據收集效率調整效果
            if (collectionRate > 50) { // 高效率時
                MagicEffectAPI.createParticleTrail()
                    .along(Vec3.atCenterOf(collectorPos).add(0, 2, 0),
                           Vec3.atCenterOf(collectorPos).add(0, 1.2, 0))
                    .withDensity(8)
                    .withSpeed(0.05f)
                    .withLifetime(60)
                    .withColorGradient(MagicEffectHelper.KONIAVA_YELLOW, MagicEffectHelper.KONIAVA_WHITE)
                    .withDuration(60)
                    .spawn(level);
            }
        }
    }
    
    /**
     * ========================================
     * 通用錯誤效果
     * ========================================
     * 
     * 在任何機器發生錯誤時顯示：
     */
    @OnlyIn(Dist.CLIENT)
    public static void addErrorEffect(Level level, BlockPos machinePos, String errorType) {
        if (!level.isClientSide) return;
        
        MagicEffectHelper.createMachineErrorEffect(level, machinePos);
        
        // 根據錯誤類型添加不同的效果
        switch (errorType) {
            case "NO_POWER":
                // 能量不足 - 閃爍的紅色圓環
                MagicEffectAPI.createMagicCircle()
                    .at(machinePos)
                    .withOffset(0, 1.0, 0)
                    .withColor(MagicEffectHelper.KONIAVA_RED)
                    .withSize(1.0f)
                    .withRotationSpeed(0)
                    .withDuration(40)
                    .withAlpha(0.6f)
                    .spawn(level);
                break;
                
            case "BLOCKED_OUTPUT":
                // 輸出阻塞 - 橙色警告效果
                MagicEffectAPI.createMagicCircle()
                    .at(machinePos)
                    .withOffset(0, 1.0, 0)
                    .withColor(0xFFFF8800)
                    .withSize(1.0f)
                    .withRotationSpeed(-0.5f)
                    .withDuration(60)
                    .spawn(level);
                break;
                
            case "INVALID_RECIPE":
                // 無效配方 - 紫色錯誤效果
                MagicEffectAPI.createMagicCircle()
                    .at(machinePos)
                    .withOffset(0, 1.0, 0)
                    .withColor(0xFFAA00AA)
                    .withSize(1.0f)
                    .withRotationSpeed(0.3f)
                    .withDuration(80)
                    .spawn(level);
                break;
        }
    }
    
    /**
     * 顏色插值輔助方法
     */
    private static int interpolateColor(int color1, int color2, float progress) {
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int) (a1 + (a2 - a1) * progress);
        int r = (int) (r1 + (r2 - r1) * progress);
        int g = (int) (g1 + (g2 - g1) * progress);
        int b = (int) (b1 + (b2 - b1) * progress);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}