package com.github.nalamodikk.experimental.particle.item;

import com.github.nalamodikk.experimental.effects.MagicEffectAPI;
import com.github.nalamodikk.common.utils.effects.MagicEffectHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 能量爆發測試工具
 * 右鍵使用生成能量爆發粒子效果
 */
public class DebugParticleItem extends Item {

    public DebugParticleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().above(); // 在點擊方塊上方生成
        Player player = context.getPlayer();

        if (level.isClientSide && player != null) {
            testMagicCircleEffects(level, pos, player.isCrouching());
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 測試各種魔法陣效果
     */
    @OnlyIn(Dist.CLIENT)
    private void testMagicCircleEffects(Level level, BlockPos pos, boolean advanced) {
        if (!advanced) {
            // 基礎測試：簡單的黃色魔法陣
            testBasicMagicCircle(level, pos);
        } else {
            // 進階測試：多種效果組合
            testAdvancedMagicCircles(level, pos);
        }
    }

    /**
     * 測試基礎魔法陣
     */
    @OnlyIn(Dist.CLIENT)
    private void testBasicMagicCircle(Level level, BlockPos pos) {
        // 創建一個簡單的黃色魔法陣
        MagicEffectAPI.createMagicCircle()
                .at(pos)
                .withColor(MagicEffectHelper.KONIAVA_YELLOW)
                .withSize(1.5f)
                .withRotationSpeed(0.5f)
                .withDuration(200) // 10 秒
                .withGlowEffect(true)
                .spawn(level);
    }

    /**
     * 測試進階魔法陣效果
     */
    @OnlyIn(Dist.CLIENT)
    private void testAdvancedMagicCircles(Level level, BlockPos pos) {
        // 測試 1: 多層同心圓魔法陣
        testConcentricCircles(level, pos);

        // 測試 2: 顏色變化魔法陣
        testColorChangingCircle(level, pos.offset(3, 0, 0));

        // 測試 3: 大小變化魔法陣
        testSizeChangingCircle(level, pos.offset(-3, 0, 0));

        // 測試 4: 持久魔法陣
        testPersistentCircle(level, pos.offset(0, 0, 3));

        // 測試 5: 發光效果比較
        testGlowComparison(level, pos.offset(0, 0, -3));
    }

    /**
     * 測試同心圓效果
     */
    @OnlyIn(Dist.CLIENT)
    private void testConcentricCircles(Level level, BlockPos pos) {
        // 外圈 - 藍色，慢速旋轉
        MagicEffectAPI.createMagicCircle()
                .at(pos)
                .withColor(MagicEffectHelper.KONIAVA_BLUE)
                .withSize(2.5f)
                .withRotationSpeed(0.3f)
                .withDuration(300)
                .withGlowEffect(true)
                .spawn(level);

        // 中圈 - 紫色，中速旋轉
        MagicEffectAPI.createMagicCircle()
                .at(pos)
                .withOffset(0, 0.05, 0)
                .withColor(MagicEffectHelper.KONIAVA_PURPLE)
                .withSize(1.8f)
                .withRotationSpeed(-0.6f) // 反向旋轉
                .withDuration(300)
                .withGlowEffect(true)
                .spawn(level);

        // 內圈 - 綠色，快速旋轉
        MagicEffectAPI.createMagicCircle()
                .at(pos)
                .withOffset(0, 0.1, 0)
                .withColor(MagicEffectHelper.KONIAVA_GREEN)
                .withSize(1.2f)
                .withRotationSpeed(1.0f)
                .withDuration(300)
                .withGlowEffect(true)
                .spawn(level);
    }

    /**
     * 測試顏色變化魔法陣
     */
    @OnlyIn(Dist.CLIENT)
    private void testColorChangingCircle(Level level, BlockPos pos) {
        // 創建一個會改變顏色的魔法陣
        MagicEffectAPI.createMagicCircle()
                .at(pos)
                .withColor(MagicEffectHelper.KONIAVA_RED)
                .withSize(1.8f)
                .withRotationSpeed(0.8f)
                .withDuration(400)
                .onTick(circle -> {
                    // 根據生命周期改變顏色 (模擬效果，實際需要修改渲染器支援)
                    float progress = circle.getProgress();
                    if (progress > 0.7f) {
                        // 最後 30% 時間變為白色 (需要動態顏色支援)
                    }
                })
                .spawn(level);
    }

    /**
     * 測試大小變化魔法陣
     */
    @OnlyIn(Dist.CLIENT)
    private void testSizeChangingCircle(Level level, BlockPos pos) {
        MagicEffectAPI.createMagicCircle()
                .at(pos)
                .withColor(MagicEffectHelper.KONIAVA_PURPLE)
                .withSize(0.5f) // 從小開始
                .withRotationSpeed(1.2f)
                .withDuration(250)
                .withGlowEffect(true)
                .onTick(circle -> {
                    // 逐漸增大效果 (淡入效果已內建，這裡展示概念)
                })
                .spawn(level);
    }

    /**
     * 測試持久魔法陣
     */
    @OnlyIn(Dist.CLIENT)
    private void testPersistentCircle(Level level, BlockPos pos) {
        MagicEffectAPI.createMagicCircle()
                .at(pos)
                .withColor(MagicEffectHelper.KONIAVA_WHITE)
                .withSize(1.0f)
                .withRotationSpeed(0.2f)
                .persistent(true) // 持久存在
                .withGlowEffect(false) // 不發光，更穩定的視覺
                .onComplete(circle -> {
                    // 10 秒後自動結束 (模擬自動清理)
                    new Thread(() -> {
                        try {
                            Thread.sleep(10000);
                            circle.finish();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                })
                .spawn(level);
    }

    /**
     * 測試發光效果比較
     */
    @OnlyIn(Dist.CLIENT)
    private void testGlowComparison(Level level, BlockPos pos) {
        // 無發光效果
        MagicEffectAPI.createMagicCircle()
                .at(pos.offset(-1, 0, 0))
                .withColor(MagicEffectHelper.KONIAVA_GREEN)
                .withSize(1.2f)
                .withGlowEffect(false)
                .withDuration(200)
                .spawn(level);

        // 有發光效果
        MagicEffectAPI.createMagicCircle()
                .at(pos.offset(1, 0, 0))
                .withColor(MagicEffectHelper.KONIAVA_GREEN)
                .withSize(1.2f)
                .withGlowEffect(true)
                .withDuration(200)
                .spawn(level);
    }
}