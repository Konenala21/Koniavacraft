package com.github.nalamodikk.common.item;

import com.github.nalamodikk.common.entity.TrainingDummyEntity;
import com.github.nalamodikk.register.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 訓練假人放置物：右鍵地面在點到的方塊上方生成一隻訓練假人。
 */
public class TrainingDummyItem extends Item {

    public TrainingDummyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        Direction face = context.getClickedFace();
        BlockPos spawnPos = context.getClickedPos().relative(face);

        TrainingDummyEntity dummy = ModEntities.TRAINING_DUMMY.get().create(serverLevel);
        if (dummy == null) return InteractionResult.FAIL;

        Player player = context.getPlayer();
        float yaw = player != null ? player.getYRot() + 180.0F : 0.0F; // 面向放置者
        dummy.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, yaw, 0.0F);
        // moveTo 只設 yRot；Mob 渲染看的是身體/頭旋轉，要一起設(含前一幀值)才真的面向玩家，像盔甲架。
        dummy.yBodyRot = yaw;
        dummy.yBodyRotO = yaw;
        dummy.setYHeadRot(yaw);
        dummy.yHeadRotO = yaw;
        serverLevel.addFreshEntity(dummy);

        context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
