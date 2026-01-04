package com.github.nalamodikk.experimental.effects;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 魔法效果註冊表 - 管理所有活躍的魔法效果
 * 
 * 這個類負責：
 * - 註冊新的效果實例
 * - 每 tick 更新所有效果
 * - 移除已完成的效果
 * - 提供效果查詢功能
 */
@OnlyIn(Dist.CLIENT)
public class MagicEffectRegistry {
    
    private static final MagicEffectRegistry INSTANCE = new MagicEffectRegistry();
    
    private final List<MagicEffect> activeEffects = new CopyOnWriteArrayList<>();
    private final List<MagicEffect> effectsToAdd = new ArrayList<>();
    private final List<MagicEffect> effectsToRemove = new ArrayList<>();
    
    private MagicEffectRegistry() {
        // 單例模式
    }
    
    /**
     * 獲取註冊表實例
     * @return 註冊表實例
     */
    public static MagicEffectRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * 註冊新的效果實例
     * @param effect 要註冊的效果
     */
    public void registerEffect(MagicEffect effect) {
        if (effect != null && !activeEffects.contains(effect)) {
            synchronized (effectsToAdd) {
                effectsToAdd.add(effect);
            }
        }
    }
    
    /**
     * 移除效果實例
     * @param effect 要移除的效果
     */
    public void removeEffect(MagicEffect effect) {
        if (effect != null) {
            synchronized (effectsToRemove) {
                effectsToRemove.add(effect);
            }
        }
    }
    
    /**
     * 每 tick 更新所有效果 - 應該在客戶端 tick 事件中調用
     */
    public void tick() {
        // 添加新效果
        synchronized (effectsToAdd) {
            if (!effectsToAdd.isEmpty()) {
                activeEffects.addAll(effectsToAdd);
                effectsToAdd.clear();
            }
        }
        
        // 移除標記的效果
        synchronized (effectsToRemove) {
            if (!effectsToRemove.isEmpty()) {
                activeEffects.removeAll(effectsToRemove);
                effectsToRemove.clear();
            }
        }
        
        // 更新所有活躍效果
        List<MagicEffect> effectsToRemoveThisTick = new ArrayList<>();
        
        for (MagicEffect effect : activeEffects) {
            try {
                effect.tick();
                
                // 檢查效果是否已完成
                if (effect.isFinished()) {
                    effect.onComplete();
                    effectsToRemoveThisTick.add(effect);
                }
            } catch (Exception e) {
                // 記錄錯誤並移除有問題的效果
                System.err.println("Error updating magic effect: " + e.getMessage());
                effectsToRemoveThisTick.add(effect);
            }
        }
        
        // 移除完成或有錯誤的效果
        if (!effectsToRemoveThisTick.isEmpty()) {
            activeEffects.removeAll(effectsToRemoveThisTick);
        }
    }
    
    /**
     * 渲染所有效果 - 應該在渲染事件中調用
     * @param partialTicks 部分 tick 時間
     */
    public void render(float partialTicks) {
        for (MagicEffect effect : activeEffects) {
            try {
                if (effect.shouldRender()) {
                    effect.render(partialTicks);
                }
            } catch (Exception e) {
                // 記錄渲染錯誤但不移除效果
                System.err.println("Error rendering magic effect: " + e.getMessage());
            }
        }
    }
    
    /**
     * 獲取所有活躍效果的副本
     * @return 活躍效果列表
     */
    public List<MagicEffect> getActiveEffects() {
        return new ArrayList<>(activeEffects);
    }
    
    /**
     * 獲取特定類型的活躍效果
     * @param effectClass 效果類型
     * @param <T> 效果類型泛型
     * @return 該類型的效果列表
     */
    @SuppressWarnings("unchecked")
    public <T extends MagicEffect> List<T> getActiveEffects(Class<T> effectClass) {
        List<T> results = new ArrayList<>();
        for (MagicEffect effect : activeEffects) {
            if (effectClass.isInstance(effect)) {
                results.add((T) effect);
            }
        }
        return results;
    }
    
    /**
     * 獲取活躍效果數量
     * @return 效果數量
     */
    public int getActiveEffectCount() {
        return activeEffects.size();
    }
    
    /**
     * 清除所有活躍效果
     */
    public void clearAllEffects() {
        for (MagicEffect effect : activeEffects) {
            try {
                effect.onComplete();
            } catch (Exception e) {
                // 忽略清理時的錯誤
            }
        }
        
        activeEffects.clear();
        
        synchronized (effectsToAdd) {
            effectsToAdd.clear();
        }
        
        synchronized (effectsToRemove) {
            effectsToRemove.clear();
        }
    }
    
    /**
     * 獲取調試信息
     * @return 調試信息字串
     */
    public String getDebugInfo() {
        return String.format("Active Effects: %d, Pending Add: %d, Pending Remove: %d", 
            activeEffects.size(), effectsToAdd.size(), effectsToRemove.size());
    }
}