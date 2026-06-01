package com.github.nalamodikk.common.item;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.knowledge.PlayerKnowledge;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.research.network.AspectSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 本源精華:右鍵消耗 1 個,給玩家固定一組本源各 {@code amount} 點。
 *
 * 掃描是一次性的(掃過就不再給),但本源在編碼台組裝技能時會被消耗,所以需要一個
 * renewable 的本源來源。這就是水龍頭:基礎精華給六大基礎本源,再用本源合成台兩兩
 * 合成出高階本源。做成帶任意本源清單,之後可加別組精華。
 */
public class AspectEssenceItem extends Item {

    private final List<Aspect> aspects;
    private final int amount;

    public AspectEssenceItem(Properties props, int amount, Aspect... aspects) {
        super(props);
        this.amount = amount;
        this.aspects = List.of(aspects);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            ResearchSavedData data = ResearchSavedData.get(sp.serverLevel());
            PlayerKnowledge knowledge = data.getOrCreate(sp.getUUID());
            for (Aspect aspect : aspects) {
                knowledge.discoverAspect(aspect, amount);
            }
            data.setDirty();
            AspectSyncPacket.sendTo(sp);
            stack.shrink(1);

            level.playSound(null, sp.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.2f);
            sp.sendSystemMessage(Component.translatable(
                    getDescriptionId() + ".used", amount).withStyle(ChatFormatting.AQUA));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable(getDescriptionId() + ".tooltip", amount)
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
