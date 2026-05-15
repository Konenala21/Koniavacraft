package com.github.nalamodikk.common.item.tool;

import com.github.nalamodikk.common.multiblock.api.IWandActivatable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class AdvancedTechWandItem extends BasicTechWandItem {

    public AdvancedTechWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public TechWandMode[] getSupportedModes() {
        return new TechWandMode[]{
                TechWandMode.CONFIGURE_IO,
                TechWandMode.DIRECTION_CONFIG,
                TechWandMode.ROTATE,
                TechWandMode.MULTIBLOCK
        };
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        // MULTIBLOCK 模式 + 蹲下：觸發結構驗證
        if (level.getBlockEntity(pos) instanceof IWandActivatable activatable
                && player != null && player.isCrouching()
                && getMode(stack) == TechWandMode.MULTIBLOCK) {
            if (!level.isClientSide()) {
                player.displayClientMessage(activatable.onWandActivate(player), true);
            }
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }
}
