package com.github.nalamodikk.common.item.tool;

import com.github.nalamodikk.common.multiblock.api.IWandActivatable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class AdvancedTechWandItem extends BasicTechWandItem {

    public AdvancedTechWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        // 任何實作 IWandActivatable 的多方塊結構都能用法杖觸發
        if (!level.isClientSide() && player != null
                && level.getBlockEntity(pos) instanceof IWandActivatable activatable) {
            player.displayClientMessage(activatable.onWandActivate(player), true);
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }
}
