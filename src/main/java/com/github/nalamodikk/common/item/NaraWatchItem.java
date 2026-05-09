package com.github.nalamodikk.common.item;

import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.research.network.WatchSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NaraWatchItem extends Item {

    public NaraWatchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            var knowledge = ResearchSavedData.get(sp.serverLevel()).getOrCreate(sp.getUUID());
            WatchSyncPacket.sendTo(sp, knowledge.getCompletedResearch(), knowledge.getCurrentTier());
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
