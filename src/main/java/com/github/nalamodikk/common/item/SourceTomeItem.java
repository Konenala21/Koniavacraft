package com.github.nalamodikk.common.item;

import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.research.network.KnowledgeSyncPacket;
import com.github.nalamodikk.research.template.ResearchRegistry;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 源典 (Source Tome) — unlocks all research and sets tier to max.
 * Late-game item with no crafting recipe.
 */
public class SourceTomeItem extends Item {

    private static final int MAX_TIER = 12;

    public SourceCodexItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            var savedData = ResearchSavedData.get(sp.serverLevel());
            var knowledge = savedData.getOrCreate(sp.getUUID());

            for (var research : ResearchRegistry.all()) {
                knowledge.completeResearch(research.getId());
            }
            knowledge.setTier(MAX_TIER);
            savedData.setDirty();
            KnowledgeSyncPacket.sendTo(sp);

            // Award all unlocked recipes
            Set<net.minecraft.resources.ResourceLocation> recipeIds = new HashSet<>();
            for (var r : ResearchRegistry.all()) recipeIds.addAll(r.getUnlockedRecipes());
            if (!recipeIds.isEmpty()) {
                List<RecipeHolder<?>> holders = sp.serverLevel().getRecipeManager().getRecipes()
                        .stream().filter(h -> recipeIds.contains(h.id())).toList();
                if (!holders.isEmpty()) sp.awardRecipes(holders);
            }

            level.playSound(null, sp.blockPosition(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 0.8f);
            sp.sendSystemMessage(
                    Component.translatable("item.koniava.source_codex.activated")
                            .withStyle(ChatFormatting.GOLD));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable("item.koniava.source_codex.tooltip.1")
                .withStyle(ChatFormatting.GOLD));
        tips.add(Component.translatable("item.koniava.source_codex.tooltip.2")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
