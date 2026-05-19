package com.github.nalamodikk.common.item;

import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.research.network.KnowledgeSyncPacket;
import com.github.nalamodikk.research.template.ResearchRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
 * 共識眼鏡 — right-click another player to merge both players' completed research.
 * Stores the last synced player name in NBT for tooltip display.
 */
public class ConsensusGlassesItem extends Item {

    private static final String TAG_SYNCED_NAME = "synced_player_name";

    public ConsensusGlassesItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, net.minecraft.world.InteractionHand hand) {
        if (!(target instanceof ServerPlayer targetPlayer)) return InteractionResult.PASS;
        if (!(user instanceof ServerPlayer sourcePlayer)) return InteractionResult.PASS;
        if (sourcePlayer.getUUID().equals(targetPlayer.getUUID())) return InteractionResult.PASS;

        mergeResearch(sourcePlayer, targetPlayer);

        // Record synced player name in NBT
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                stack.getHoverName()); // keep existing display name
        var tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putString(TAG_SYNCED_NAME, targetPlayer.getName().getString());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));

        Level level = sourcePlayer.level();
        level.playSound(null, sourcePlayer.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);

        sourcePlayer.sendSystemMessage(
                Component.translatable("item.koniava.consensus_glasses.sync_done",
                        targetPlayer.getName()).withStyle(ChatFormatting.GREEN));
        targetPlayer.sendSystemMessage(
                Component.translatable("item.koniava.consensus_glasses.sync_received",
                        sourcePlayer.getName()).withStyle(ChatFormatting.GREEN));

        return InteractionResult.SUCCESS;
    }

    private static void mergeResearch(ServerPlayer a, ServerPlayer b) {
        var savedData = ResearchSavedData.get(a.serverLevel());
        var ka = savedData.getOrCreate(a.getUUID());
        var kb = savedData.getOrCreate(b.getUUID());

        Set<net.minecraft.resources.ResourceLocation> union = new HashSet<>();
        for (var r : ResearchRegistry.all()) {
            var id = r.getId();
            if (ka.hasCompleted(id) || kb.hasCompleted(id)) union.add(id);
        }

        for (var id : union) {
            ka.completeResearch(id);
            kb.completeResearch(id);
        }

        // Award recipes for newly unlocked research
        Set<net.minecraft.resources.ResourceLocation> recipeIds = new HashSet<>();
        for (var id : union) {
            ResearchRegistry.get(id).ifPresent(t -> recipeIds.addAll(t.getUnlockedRecipes()));
        }
        if (!recipeIds.isEmpty()) {
            List<RecipeHolder<?>> holders = a.serverLevel().getRecipeManager().getRecipes()
                    .stream().filter(h -> recipeIds.contains(h.id())).toList();
            if (!holders.isEmpty()) { a.awardRecipes(holders); b.awardRecipes(holders); }
        }

        savedData.setDirty();
        KnowledgeSyncPacket.sendTo(a);
        KnowledgeSyncPacket.sendTo(b);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable("item.koniava.consensus_glasses.tooltip")
                .withStyle(ChatFormatting.GRAY));

        var tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (tag.contains(TAG_SYNCED_NAME)) {
            tips.add(Component.translatable("item.koniava.consensus_glasses.synced_with",
                    tag.getString(TAG_SYNCED_NAME)).withStyle(ChatFormatting.AQUA));
        }
    }
}
