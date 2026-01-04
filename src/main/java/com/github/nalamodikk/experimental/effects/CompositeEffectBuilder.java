package com.github.nalamodikk.experimental.effects;

import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 複合效果建構器 - 組合多種魔法效果
 * 
 * 示例用法：
 * <pre>
 * MagicEffectAPI.createCompositeEffect()
 *     .addMagicCircle(circle -> circle
 *         .at(pos)
 *         .withColor(0xFFFF0000)
 *         .withDuration(100))
 *     .addBeam(beam -> beam
 *         .from(startPos)
 *         .to(endPos)
 *         .withColor(0xFF00FF00))
 *     .addParticleTrail(trail -> trail
 *         .between(startPos, endPos)
 *         .withDensity(10))
 *     .withDelay(20) // 20 ticks 後開始
 *     .spawn(level);
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public class CompositeEffectBuilder {
    
    private final List<EffectComponent> components = new ArrayList<>();
    private int delay = 0;
    private Consumer<CompositeEffect> onComplete;
    private Consumer<CompositeEffect> onTick;
    
    CompositeEffectBuilder() {
        // 套件私有構造函數
    }
    
    /**
     * 添加魔法陣效果
     * @param configurator 魔法陣配置函數
     * @return 建構器實例
     */
    public CompositeEffectBuilder addMagicCircle(Function<MagicCircleBuilder, MagicCircleBuilder> configurator) {
        MagicCircleBuilder builder = new MagicCircleBuilder();
        MagicCircleBuilder configured = configurator.apply(builder);
        components.add(new EffectComponent(EffectType.MAGIC_CIRCLE, configured, 0));
        return this;
    }
    
    /**
     * 添加延遲的魔法陣效果
     * @param configurator 魔法陣配置函數
     * @param delayTicks 延遲時間 (ticks)
     * @return 建構器實例
     */
    public CompositeEffectBuilder addMagicCircle(Function<MagicCircleBuilder, MagicCircleBuilder> configurator, int delayTicks) {
        MagicCircleBuilder builder = new MagicCircleBuilder();
        MagicCircleBuilder configured = configurator.apply(builder);
        components.add(new EffectComponent(EffectType.MAGIC_CIRCLE, configured, delayTicks));
        return this;
    }
    
    /**
     * 添加射線效果
     * @param configurator 射線配置函數
     * @return 建構器實例
     */
    public CompositeEffectBuilder addBeam(Function<BeamBuilder, BeamBuilder> configurator) {
        BeamBuilder builder = new BeamBuilder();
        BeamBuilder configured = configurator.apply(builder);
        components.add(new EffectComponent(EffectType.BEAM, configured, 0));
        return this;
    }
    
    /**
     * 添加延遲的射線效果
     * @param configurator 射線配置函數
     * @param delayTicks 延遲時間 (ticks)
     * @return 建構器實例
     */
    public CompositeEffectBuilder addBeam(Function<BeamBuilder, BeamBuilder> configurator, int delayTicks) {
        BeamBuilder builder = new BeamBuilder();
        BeamBuilder configured = configurator.apply(builder);
        components.add(new EffectComponent(EffectType.BEAM, configured, delayTicks));
        return this;
    }
    
    /**
     * 添加粒子軌跡效果
     * @param configurator 粒子軌跡配置函數
     * @return 建構器實例
     */
    public CompositeEffectBuilder addParticleTrail(Function<ParticleTrailBuilder, ParticleTrailBuilder> configurator) {
        ParticleTrailBuilder builder = new ParticleTrailBuilder();
        ParticleTrailBuilder configured = configurator.apply(builder);
        components.add(new EffectComponent(EffectType.PARTICLE_TRAIL, configured, 0));
        return this;
    }
    
    /**
     * 添加延遲的粒子軌跡效果
     * @param configurator 粒子軌跡配置函數
     * @param delayTicks 延遲時間 (ticks)
     * @return 建構器實例
     */
    public CompositeEffectBuilder addParticleTrail(Function<ParticleTrailBuilder, ParticleTrailBuilder> configurator, int delayTicks) {
        ParticleTrailBuilder builder = new ParticleTrailBuilder();
        ParticleTrailBuilder configured = configurator.apply(builder);
        components.add(new EffectComponent(EffectType.PARTICLE_TRAIL, configured, delayTicks));
        return this;
    }
    
    /**
     * 設置整體延遲時間
     * @param ticks 延遲時間 (ticks)
     * @return 建構器實例
     */
    public CompositeEffectBuilder withDelay(int ticks) {
        this.delay = Math.max(0, ticks);
        return this;
    }
    
    /**
     * 設置完成回調
     * @param callback 當所有效果結束時調用的回調
     * @return 建構器實例
     */
    public CompositeEffectBuilder onComplete(Consumer<CompositeEffect> callback) {
        this.onComplete = callback;
        return this;
    }
    
    /**
     * 設置每 tick 回調
     * @param callback 每 tick 調用的回調
     * @return 建構器實例
     */
    public CompositeEffectBuilder onTick(Consumer<CompositeEffect> callback) {
        this.onTick = callback;
        return this;
    }
    
    /**
     * 創建並在世界中生成複合效果
     * @param level 世界實例
     * @return 創建的效果實例，如果創建失敗則返回 null
     */
    public CompositeEffect spawn(Level level) {
        if (components.isEmpty()) {
            throw new IllegalStateException("At least one effect component must be added before spawning composite effect");
        }
        
        if (!level.isClientSide) {
            return null; // 只在客戶端渲染
        }
        
        // TODO: 實現實際的複合效果創建邏輯
        CompositeEffect effect = new CompositeEffect(components, delay, onComplete, onTick);
        
        // 註冊到效果管理器
        MagicEffectRegistry.getInstance().registerEffect(effect);
        
        return effect;
    }
    
    /**
     * 創建效果實例但不立即生成
     * @return 創建的效果實例
     */
    public CompositeEffect build() {
        if (components.isEmpty()) {
            throw new IllegalStateException("At least one effect component must be added before building composite effect");
        }
        
        return new CompositeEffect(components, delay, onComplete, onTick);
    }
    
    /**
     * 效果類型枚舉
     */
    public enum EffectType {
        MAGIC_CIRCLE,
        BEAM,
        PARTICLE_TRAIL
    }
    
    /**
     * 效果組件 - 包含效果類型、建構器和延遲時間
     */
    public static class EffectComponent {
        private final EffectType type;
        private final Object builder;
        private final int delay;
        
        public EffectComponent(EffectType type, Object builder, int delay) {
            this.type = type;
            this.builder = builder;
            this.delay = delay;
        }
        
        public EffectType getType() {
            return type;
        }
        
        public Object getBuilder() {
            return builder;
        }
        
        public int getDelay() {
            return delay;
        }
        
        @SuppressWarnings("unchecked")
        public <T> T getBuilder(Class<T> builderClass) {
            if (builderClass.isInstance(builder)) {
                return (T) builder;
            }
            throw new IllegalArgumentException("Builder is not of type " + builderClass.getSimpleName());
        }
    }
}