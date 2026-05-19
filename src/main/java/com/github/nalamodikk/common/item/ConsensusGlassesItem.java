package com.github.nalamodikk.common.item;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.research.network.KnowledgeSyncPacket;
import com.github.nalamodikk.research.template.ResearchRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 共識眼鏡 — knowledge snapshot item.
 *
 * First use: binds to the player, snapshots their research and aspects into item NBT.
 * Subsequent use by same player: shows "already bound" message.
 * Subsequent use by different player: copies the snapshot into that player's knowledge.
 */
public class ConsensusGlassesItem extends Item {

    private static final String TAG_BOUND_UUID        = "bound_uuid";
    private static final String TAG_BOUND_NAME        = "bound_name";
    private static final String TAG_SNAPSHOT_RESEARCH = "snapshot_research";
    private static final String TAG_SNAPSHOT_ASPECTS  = "snapshot_aspects";

    public ConsensusGlassesItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        CompoundTag tag = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        if (!tag.contains(TAG_BOUND_UUID)) {
            bindToPlayer(sp, stack, tag, hand);
        } else {
            UUID boundId;
            try {
                boundId = UUID.fromString(tag.getString(TAG_BOUND_UUID));
            } catch (IllegalArgumentException e) {
                return InteractionResultHolder.fail(stack);
            }

            if (sp.getUUID().equals(boundId)) {
                sp.sendSystemMessage(
                        Component.translatable("item.koniava.consensus_glasses.already_bound")
                                .withStyle(ChatFormatting.YELLOW));
            } else {
                copyToPlayer(sp, stack, tag);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private static void bindToPlayer(ServerPlayer sp, ItemStack stack, CompoundTag tag, InteractionHand hand) {
        var savedData = ResearchSavedData.get(sp.serverLevel());
        var knowledge = savedData.getOrCreate(sp.getUUID());

        tag.putString(TAG_BOUND_UUID, sp.getUUID().toString());
        tag.putString(TAG_BOUND_NAME, sp.getName().getString());

        ListTag researchSnap = new ListTag();
        for (ResourceLocation id : knowledge.getCompletedResearch()) {
            researchSnap.add(StringTag.valueOf(id.toString()));
        }
        tag.put(TAG_SNAPSHOT_RESEARCH, researchSnap);

        CompoundTag aspectSnap = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : knowledge.getDiscoveredAspectsMap().entrySet()) {
            aspectSnap.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put(TAG_SNAPSHOT_ASPECTS, aspectSnap);

        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(tag));
        sp.setItemInHand(hand, stack);
        sp.getInventory().setChanged();

        sp.level().playSound(null, sp.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
        sp.sendSystemMessage(
                Component.translatable("item.koniava.consensus_glasses.bind_success")
                        .withStyle(ChatFormatting.GREEN));
    }

    private static void copyToPlayer(ServerPlayer sp, ItemStack stack, CompoundTag tag) {
        String boundName = tag.getString(TAG_BOUND_NAME);
        var savedData = ResearchSavedData.get(sp.serverLevel());
        var knowledge = savedData.getOrCreate(sp.getUUID());

        ListTag researchSnap = tag.getList(TAG_SNAPSHOT_RESEARCH, Tag.TAG_STRING);
        for (int i = 0; i < researchSnap.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(researchSnap.getString(i));
            if (id != null) knowledge.completeResearch(id);
        }

        CompoundTag aspectSnap = tag.getCompound(TAG_SNAPSHOT_ASPECTS);
        for (String key : aspectSnap.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            Aspect aspect = id != null ? ModAspects.get(id) : null;
            if (aspect != null) {
                int stored = aspectSnap.getInt(key);
                int current = knowledge.getAspectCount(id);
                if (stored > current) knowledge.setAspectAmount(id, stored);
            }
        }

        Set<ResourceLocation> recipeIds = new HashSet<>();
        for (int i = 0; i < researchSnap.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(researchSnap.getString(i));
            if (id != null) ResearchRegistry.get(id).ifPresent(t -> recipeIds.addAll(t.getUnlockedRecipes()));
        }
        if (!recipeIds.isEmpty()) {
            List<RecipeHolder<?>> holders = sp.serverLevel().getRecipeManager().getRecipes()
                    .stream().filter(h -> recipeIds.contains(h.id())).toList();
            if (!holders.isEmpty()) sp.awardRecipes(holders);
        }

        savedData.setDirty();
        KnowledgeSyncPacket.sendTo(sp);

        sp.level().playSound(null, sp.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);
        sp.sendSystemMessage(
                Component.translatable("item.koniava.consensus_glasses.copy_success", boundName)
                        .withStyle(ChatFormatting.GREEN));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tips, TooltipFlag flag) {
        CompoundTag tag = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();

        if (!tag.contains(TAG_BOUND_UUID)) {
            tips.add(Component.translatable("item.koniava.consensus_glasses.tooltip.unbound")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            String name = tag.getString(TAG_BOUND_NAME);
            tips.add(Component.translatable("item.koniava.consensus_glasses.bound_to", name)
                    .withStyle(ChatFormatting.AQUA));
            tips.add(Component.translatable("item.koniava.consensus_glasses.tooltip.copy")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
