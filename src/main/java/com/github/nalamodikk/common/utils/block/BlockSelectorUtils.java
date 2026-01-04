package com.github.nalamodikk.common.utils.block;



import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BlockSelectorUtils {

    public static BlockPos getTargetBlock(Player player, double maxDistance) {
        // 獲取玩家當前世界
        Level level = player.level();

        // 玩家當前的視角位置和方向
        Vec3 eyePosition = player.getEyePosition(1.0F); // 玩家眼睛的位置
        Vec3 lookVector = player.getLookAngle(); // 玩家視角的方向向量

        // 計算視線的終點
        Vec3 endPosition = eyePosition.add(lookVector.scale(maxDistance));

        // 執行射線追蹤
        BlockHitResult hitResult = level.clip(new ClipContext(
                eyePosition, endPosition, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player
        ));

        // 如果擊中了方塊，返回該方塊的位置
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult.getBlockPos();
        }

        // 如果未擊中方塊，返回 null
        return null;
    }
}
