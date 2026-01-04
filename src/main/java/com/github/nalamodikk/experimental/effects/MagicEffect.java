package com.github.nalamodikk.experimental.effects;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 魔法效果基類 - 所有魔法效果的抽象基類
 * 
 * 所有具體的效果類型都應該繼承這個類並實現必要的方法
 */
@OnlyIn(Dist.CLIENT)
public abstract class MagicEffect {
    
    protected int age = 0;
    protected int maxAge;
    protected boolean finished = false;
    
    /**
     * 構造函數
     * @param duration 效果持續時間 (ticks)
     */
    public MagicEffect(int duration) {
        this.maxAge = Math.max(1, duration);
    }
    
    /**
     * 每 tick 更新效果
     */
    public void tick() {
        age++;
        if (age >= maxAge) {
            finished = true;
        }
        onTick();
    }
    
    /**
     * 渲染效果
     * @param partialTicks 部分 tick 時間
     */
    public abstract void render(float partialTicks);
    
    /**
     * 檢查效果是否已完成
     * @return 如果效果已完成則返回 true
     */
    public boolean isFinished() {
        return finished;
    }
    
    /**
     * 檢查效果是否應該渲染
     * @return 如果應該渲染則返回 true
     */
    public boolean shouldRender() {
        return !finished;
    }
    
    /**
     * 獲取效果的年齡
     * @return 效果存在的 tick 數量
     */
    public int getAge() {
        return age;
    }
    
    /**
     * 獲取效果的最大年齡
     * @return 效果的最大持續時間
     */
    public int getMaxAge() {
        return maxAge;
    }
    
    /**
     * 獲取效果的進度 (0.0 - 1.0)
     * @return 效果進度
     */
    public float getProgress() {
        return Math.min(1.0f, (float) age / (float) maxAge);
    }
    
    /**
     * 獲取效果的剩餘進度 (1.0 - 0.0)
     * @return 剩餘進度
     */
    public float getRemainingProgress() {
        return 1.0f - getProgress();
    }
    
    /**
     * 手動標記效果為完成
     */
    public void finish() {
        this.finished = true;
    }
    
    /**
     * 延長效果持續時間
     * @param additionalTicks 額外的 tick 數量
     */
    public void extend(int additionalTicks) {
        this.maxAge += Math.max(0, additionalTicks);
    }
    
    /**
     * 設置效果持續時間
     * @param duration 新的持續時間
     */
    public void setDuration(int duration) {
        this.maxAge = Math.max(1, duration);
        if (this.age >= this.maxAge) {
            this.finished = true;
        }
    }
    
    /**
     * 每 tick 回調 - 子類可以重寫此方法來實現自定義邏輯
     */
    protected void onTick() {
        // 預設實現為空
    }
    
    /**
     * 完成回調 - 當效果結束時調用
     */
    protected void onComplete() {
        // 預設實現為空
    }
    
    /**
     * 獲取效果類型名稱 (用於調試)
     * @return 效果類型名稱
     */
    public String getEffectType() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 獲取調試信息
     * @return 調試信息字串
     */
    public String getDebugInfo() {
        return String.format("%s [Age: %d/%d, Progress: %.2f%%]", 
            getEffectType(), age, maxAge, getProgress() * 100);
    }
}