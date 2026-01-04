package com.github.nalamodikk.experimental.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Consumer;

/**
 * 魔法陣效果建構器 - 使用流暢 API 模式構建魔法陣
 * 
 * 示例用法：
 * <pre>
 * MagicEffectAPI.createMagicCircle()
 *     .at(blockPos)
 *     .withColor(0xFFFFFF00) // 黃色
 *     .withSize(2.0f)
 *     .withRotationSpeed(1.0f)
 *     .withDuration(100)
 *     .withGlowEffect(true)
 *     .spawn(level);
 * </pre>
 */
@OnlyIn(Dist.CLIENT)
public class MagicCircleBuilder {
    
    private BlockPos position;
    private Vec3 offset = Vec3.ZERO;
    private int color = 0xFFFFFF00; // 預設黃色
    private float size = 1.5f;
    private float rotationSpeed = 0.5f;
    private int duration = 60;
    private boolean glowEffect = true;
    private float alpha = 1.0f;
    private ResourceLocation runeTexture;
    private boolean persistant = false;
    private Consumer<MagicCircleEffect> onComplete;
    private Consumer<MagicCircleEffect> onTick;
    
    MagicCircleBuilder() {
        // 套件私有構造函數
    }
    
    /**
     * 設置魔法陣的位置
     * @param pos 方塊位置
     * @return 建構器實例
     */
    public MagicCircleBuilder at(BlockPos pos) {
        this.position = pos;
        return this;
    }
    
    /**
     * 設置魔法陣的位置
     * @param x X 座標
     * @param y Y 座標
     * @param z Z 座標
     * @return 建構器實例
     */
    public MagicCircleBuilder at(double x, double y, double z) {
        this.position = new BlockPos((int)x, (int)y, (int)z);
        return this;
    }
    
    /**
     * 設置相對於位置的偏移
     * @param offset 偏移向量
     * @return 建構器實例
     */
    public MagicCircleBuilder withOffset(Vec3 offset) {
        this.offset = offset;
        return this;
    }
    
    /**
     * 設置相對於位置的偏移
     * @param x X 偏移
     * @param y Y 偏移
     * @param z Z 偏移
     * @return 建構器實例
     */
    public MagicCircleBuilder withOffset(double x, double y, double z) {
        this.offset = new Vec3(x, y, z);
        return this;
    }
    
    /**
     * 設置魔法陣顏色
     * @param color ARGB 顏色值
     * @return 建構器實例
     */
    public MagicCircleBuilder withColor(int color) {
        this.color = color;
        return this;
    }
    
    /**
     * 設置魔法陣顏色 (RGB + Alpha)
     * @param r 紅色 (0-255)
     * @param g 綠色 (0-255)
     * @param b 藍色 (0-255)
     * @param a 透明度 (0-255)
     * @return 建構器實例
     */
    public MagicCircleBuilder withColor(int r, int g, int b, int a) {
        this.color = (a << 24) | (r << 16) | (g << 8) | b;
        return this;
    }
    
    /**
     * 設置魔法陣大小
     * @param size 大小倍數 (1.0 = 正常大小)
     * @return 建構器實例
     */
    public MagicCircleBuilder withSize(float size) {
        this.size = Math.max(0.1f, size);
        return this;
    }
    
    /**
     * 設置旋轉速度
     * @param speed 每 tick 的旋轉角度 (弧度)
     * @return 建構器實例
     */
    public MagicCircleBuilder withRotationSpeed(float speed) {
        this.rotationSpeed = speed;
        return this;
    }
    
    /**
     * 設置持續時間
     * @param ticks 持續的 tick 數量
     * @return 建構器實例
     */
    public MagicCircleBuilder withDuration(int ticks) {
        this.duration = Math.max(1, ticks);
        return this;
    }
    
    /**
     * 設置是否啟用發光效果
     * @param glow 是否發光
     * @return 建構器實例
     */
    public MagicCircleBuilder withGlowEffect(boolean glow) {
        this.glowEffect = glow;
        return this;
    }
    
    /**
     * 設置透明度
     * @param alpha 透明度 (0.0 - 1.0)
     * @return 建構器實例
     */
    public MagicCircleBuilder withAlpha(float alpha) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        return this;
    }
    
    /**
     * 設置自定義符文紋理
     * @param texture 紋理資源位置
     * @return 建構器實例
     */
    public MagicCircleBuilder withRuneTexture(ResourceLocation texture) {
        this.runeTexture = texture;
        return this;
    }
    
    /**
     * 設置魔法陣是否持續存在 (不會自動消失)
     * @param persistant 是否持續
     * @return 建構器實例
     */
    public MagicCircleBuilder persistent(boolean persistant) {
        this.persistant = persistant;
        return this;
    }
    
    /**
     * 設置完成回調
     * @param callback 當效果結束時調用的回調
     * @return 建構器實例
     */
    public MagicCircleBuilder onComplete(Consumer<MagicCircleEffect> callback) {
        this.onComplete = callback;
        return this;
    }
    
    /**
     * 設置每 tick 回調
     * @param callback 每 tick 調用的回調
     * @return 建構器實例
     */
    public MagicCircleBuilder onTick(Consumer<MagicCircleEffect> callback) {
        this.onTick = callback;
        return this;
    }
    
    /**
     * 創建並在世界中生成魔法陣效果
     * @param level 世界實例
     * @return 創建的效果實例，如果創建失敗則返回 null
     */
    public MagicCircleEffect spawn(Level level) {
        if (position == null) {
            throw new IllegalStateException("Position must be set before spawning magic circle");
        }
        
        if (!level.isClientSide) {
            return null; // 只在客戶端渲染
        }
        
        // TODO: 實現實際的魔法陣創建邏輯
        MagicCircleEffect effect = new MagicCircleEffect(
            position, offset, color, size, rotationSpeed, 
            duration, glowEffect, alpha, runeTexture, 
            persistant, onComplete, onTick
        );
        
        // 註冊到效果管理器
        MagicEffectRegistry.getInstance().registerEffect(effect);
        
        return effect;
    }
    
    /**
     * 創建效果實例但不立即生成
     * @return 創建的效果實例
     */
    public MagicCircleEffect build() {
        if (position == null) {
            throw new IllegalStateException("Position must be set before building magic circle");
        }
        
        return new MagicCircleEffect(
            position, offset, color, size, rotationSpeed, 
            duration, glowEffect, alpha, runeTexture, 
            persistant, onComplete, onTick
        );
    }
}