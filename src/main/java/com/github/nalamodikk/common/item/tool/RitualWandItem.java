package com.github.nalamodikk.common.item.tool;

import com.github.nalamodikk.common.block.blockentity.altar.AspectAltarBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RitualWandItem extends Item {

    public RitualWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
        if (!(be instanceof AspectAltarBlockEntity altar)) return InteractionResult.PASS;

        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Component result = altar.tryActivate(player.getUUID());
        player.displayClientMessage(result, true);
        return InteractionResult.SUCCESS;
    }
}
