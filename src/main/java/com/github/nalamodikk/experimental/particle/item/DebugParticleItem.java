package com.github.nalamodikk.experimental.particle.item;

import com.github.nalamodikk.particle.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 粒子效果測試工具
 * 右鍵使用生成旋轉魔法陣粒子效果
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

        if (!level.isClientSide) {
            // 在伺服器端生成粒子效果
            if (player != null && player.isCrouching()) {
                // 蹲下右鍵：使用自定義粒子魔法陣
                ParticleHelper.createManaGeneratorEffect(level, pos);
                player.displayClientMessage(Component.literal("§d自定義粒子魔法陣"), true);
            } else {
                // 普通右鍵：使用原版粒子圓環（對比測試）
                ParticleHelper.createSimpleCircle(level, pos);
                player.displayClientMessage(Component.literal("§e原版粒子圓環（測試）"), true);
            }
        }

        return InteractionResult.SUCCESS;
    }
}