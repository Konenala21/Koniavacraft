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
 */
public class DebugParticleItem extends Item {

    public DebugParticleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().above();
        Player player = context.getPlayer();

        if (!level.isClientSide && player != null) {
            if (player.isCrouching()) {
                // 蹲下右鍵：單一 3D 旋轉魔法陣
                ParticleHelper.createSingleRotatingCircle(level, pos);
                player.displayClientMessage(Component.literal("§d生成單一 3D 旋轉魔法陣 (MagicCircleParticle)"), true);
            } else {
                // 普通右鍵：導向魔力流
                BlockPos target = pos.offset(5, 5, 5); // 假設一個目標
                ParticleHelper.createGuidedFlow(level, pos, target);
                player.displayClientMessage(Component.literal("§b生成導向魔力流 (GuidedFlowParticle)"), true);
            }
        }

        return InteractionResult.SUCCESS;
    }
}
