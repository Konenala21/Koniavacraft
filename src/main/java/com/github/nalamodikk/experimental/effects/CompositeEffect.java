package com.github.nalamodikk.experimental.effects;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 複合效果實現類
 * 
 * 管理多個效果的組合，支援延遲執行和協調多個效果的生命周期
 */
@OnlyIn(Dist.CLIENT)
public class CompositeEffect extends MagicEffect {
    
    private final List<CompositeEffectBuilder.EffectComponent> components;
    private final int initialDelay;
    private final Consumer<CompositeEffect> onCompleteCallback;
    private final Consumer<CompositeEffect> onTickCallback;
    
    // 執行狀態
    private final List<MagicEffect> activeSubEffects = new ArrayList<>();
    private final List<Boolean> componentSpawned;
    private boolean allComponentsSpawned = false;
    
    public CompositeEffect(List<CompositeEffectBuilder.EffectComponent> components, int delay,
                           Consumer<CompositeEffect> onComplete, Consumer<CompositeEffect> onTick) {
        // 計算最大持續時間：初始延遲 + 最大組件延遲 + 組件持續時間
        super(calculateMaxDuration(components, delay));
        
        this.components = new ArrayList<>(components);
        this.initialDelay = delay;
        this.onCompleteCallback = onComplete;
        this.onTickCallback = onTick;
        
        // 初始化組件狀態追蹤
        this.componentSpawned = new ArrayList<>();
        for (int i = 0; i < components.size(); i++) {
            componentSpawned.add(false);
        }
    }
    
    private static int calculateMaxDuration(List<CompositeEffectBuilder.EffectComponent> components, int delay) {
        int maxDuration = delay;
        
        for (CompositeEffectBuilder.EffectComponent component : components) {
            int componentStart = delay + component.getDelay();
            int componentDuration = estimateComponentDuration(component);
            maxDuration = Math.max(maxDuration, componentStart + componentDuration);
        }
        
        return Math.max(maxDuration, 60); // 至少 60 ticks
    }
    
    private static int estimateComponentDuration(CompositeEffectBuilder.EffectComponent component) {
        // 嘗試從建構器獲取持續時間，預設為 60 ticks
        switch (component.getType()) {
            case MAGIC_CIRCLE:
                // TODO: 從 MagicCircleBuilder 獲取持續時間
                return 60;
            case BEAM:
                // TODO: 從 BeamBuilder 獲取持續時間
                return 60;
            case PARTICLE_TRAIL:
                // TODO: 從 ParticleTrailBuilder 獲取持續時間
                return 60;
            default:
                return 60;
        }
    }
    
    @Override
    protected void onTick() {
        int currentAge = getAge();
        
        // 檢查是否過了初始延遲
        if (currentAge < initialDelay) {
            return;
        }
        
        // 生成需要在當前時間點啟動的組件
        for (int i = 0; i < components.size(); i++) {
            if (!componentSpawned.get(i)) {
                CompositeEffectBuilder.EffectComponent component = components.get(i);
                int spawnTime = initialDelay + component.getDelay();
                
                if (currentAge >= spawnTime) {
                    MagicEffect subEffect = spawnComponent(component);
                    if (subEffect != null) {
                        activeSubEffects.add(subEffect);
                        // 不需要註冊到全域註冊表，因為複合效果會管理子效果
                    }
                    componentSpawned.set(i, true);
                }
            }
        }
        
        // 檢查是否所有組件都已生成
        if (!allComponentsSpawned) {
            allComponentsSpawned = componentSpawned.stream().allMatch(spawned -> spawned);
        }
        
        // 更新所有活躍的子效果
        activeSubEffects.removeIf(effect -> {
            effect.tick();
            if (effect.isFinished()) {
                effect.onComplete();
                return true;
            }
            return false;
        });
        
        // 檢查是否應該結束複合效果
        if (allComponentsSpawned && activeSubEffects.isEmpty()) {
            finish();
        }
        
        // 調用自定義 tick 回調
        if (onTickCallback != null) {
            onTickCallback.accept(this);
        }
    }
    
    private MagicEffect spawnComponent(CompositeEffectBuilder.EffectComponent component) {
        try {
            switch (component.getType()) {
                case MAGIC_CIRCLE:
                    MagicCircleBuilder circleBuilder = component.getBuilder(MagicCircleBuilder.class);
                    return circleBuilder.build();
                    
                case BEAM:
                    BeamBuilder beamBuilder = component.getBuilder(BeamBuilder.class);
                    return beamBuilder.build();
                    
                case PARTICLE_TRAIL:
                    ParticleTrailBuilder trailBuilder = component.getBuilder(ParticleTrailBuilder.class);
                    return trailBuilder.build();
                    
                default:
                    System.err.println("Unknown effect component type: " + component.getType());
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Error spawning composite effect component: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    protected void onComplete() {
        // 確保所有子效果都被清理
        for (MagicEffect subEffect : activeSubEffects) {
            try {
                subEffect.finish();
                subEffect.onComplete();
            } catch (Exception e) {
                // 忽略清理時的錯誤
            }
        }
        activeSubEffects.clear();
        
        if (onCompleteCallback != null) {
            onCompleteCallback.accept(this);
        }
    }
    
    @Override
    public void render(float partialTicks) {
        // 渲染所有活躍的子效果
        for (MagicEffect subEffect : activeSubEffects) {
            if (subEffect.shouldRender()) {
                subEffect.render(partialTicks);
            }
        }
    }
    
    @Override
    public boolean shouldRender() {
        // 如果有任何子效果需要渲染，則渲染複合效果
        return activeSubEffects.stream().anyMatch(MagicEffect::shouldRender) || !allComponentsSpawned;
    }
    
    // ========== Getter 方法 ==========
    
    public List<CompositeEffectBuilder.EffectComponent> getComponents() {
        return new ArrayList<>(components);
    }
    
    public int getInitialDelay() {
        return initialDelay;
    }
    
    public List<MagicEffect> getActiveSubEffects() {
        return new ArrayList<>(activeSubEffects);
    }
    
    public int getActiveSubEffectCount() {
        return activeSubEffects.size();
    }
    
    public boolean areAllComponentsSpawned() {
        return allComponentsSpawned;
    }
    
    /**
     * 獲取特定類型的活躍子效果
     * @param effectClass 效果類型
     * @param <T> 效果類型泛型
     * @return 該類型的子效果列表
     */
    @SuppressWarnings("unchecked")
    public <T extends MagicEffect> List<T> getActiveSubEffects(Class<T> effectClass) {
        List<T> results = new ArrayList<>();
        for (MagicEffect effect : activeSubEffects) {
            if (effectClass.isInstance(effect)) {
                results.add((T) effect);
            }
        }
        return results;
    }
    
    /**
     * 獲取已生成的組件數量
     * @return 已生成的組件數量
     */
    public int getSpawnedComponentCount() {
        return (int) componentSpawned.stream().mapToInt(spawned -> spawned ? 1 : 0).sum();
    }
    
    /**
     * 獲取組件生成進度 (0.0 - 1.0)
     * @return 生成進度
     */
    public float getSpawnProgress() {
        if (components.isEmpty()) {
            return 1.0f;
        }
        return (float) getSpawnedComponentCount() / (float) components.size();
    }
    
    @Override
    public String getEffectType() {
        return "Composite";
    }
    
    @Override
    public String getDebugInfo() {
        return String.format("%s [Components: %d/%d spawned, Sub-effects: %d active, Delay: %d]", 
            super.getDebugInfo(), getSpawnedComponentCount(), components.size(), 
            activeSubEffects.size(), initialDelay);
    }
}