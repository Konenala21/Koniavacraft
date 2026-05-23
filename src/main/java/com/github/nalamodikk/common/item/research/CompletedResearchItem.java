package com.github.nalamodikk.common.item.research;

import com.github.nalamodikk.narasystem.nara.event.NaraServerEvents;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.research.network.KnowledgeSyncPacket;
import com.github.nalamodikk.research.template.ResearchRegistry;
import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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

import java.util.List;

/**
 * Physical record of a completed research puzzle — returned to the player
 * after the puzzle grid is solved.
 *
 * Right-clicking in air APPLIES the research results to the player
 * (saves to PlayerKnowledge) and triggers the firework ceremony.
 * This is intentional: the puzzle solving is separate from "committing" the result.
 *
 * Texture: placeholder (missing) until the artist creates the asset.
 */
public class CompletedResearchItem extends Item {

    public CompletedResearchItem(Properties properties) {
        super(properties);
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static ItemStack forResearch(ResourceLocation researchId) {
        ItemStack stack = new ItemStack(ModItems.COMPLETED_RESEARCH.get());
        stack.set(ModDataComponents.RESEARCH_ID, researchId);
        return stack;
    }

    // ── Use — this is where the research is officially committed ──────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceLocation researchId = getResearchId(stack);
        if (researchId == null) return InteractionResultHolder.fail(stack);

        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            ServerLevel serverLevel = sp.serverLevel();
            var data      = ResearchSavedData.get(serverLevel);
            boolean isNew = data.completeResearch(sp.getUUID(), researchId);

            if (isNew) {
                // First time committing — trigger ceremony
                boolean isVeryFirst = data.getOrCreate(sp.getUUID())
                        .getCompletedResearch().size() == 1;
                if (isVeryFirst) {
                    spawnFirstResearchCelebration(sp, serverLevel);
                    NaraServerEvents.scheduleFirstResearchTutorial(sp);
                }

                ResearchRegistry.get(researchId).ifPresent(t -> {
                    sp.sendSystemMessage(Component.translatable(
                            "research.koniava.complete_notify",
                            Component.translatable(t.getTitleKey()))
                            .withStyle(ChatFormatting.GREEN));
                    List<RecipeHolder<?>> toAward = serverLevel.getRecipeManager().getRecipes()
                            .stream().filter(h -> t.getUnlockedRecipes().contains(h.id())).toList();
                    if (!toAward.isEmpty()) sp.awardRecipes(toAward);
                });

                // Consume the scroll
                stack.shrink(1);

                // 同步至客戶端
                KnowledgeSyncPacket.sendTo(sp);
            } else {
                // Already committed before — still consume but just say so
                stack.shrink(1);
                sp.sendSystemMessage(Component.translatable(
                        "research.koniava.already_completed",
                        Component.translatable(ResearchRegistry.get(researchId)
                                .map(t -> t.getTitleKey()).orElse(researchId.toString())))
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ── Tooltip / Name ────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> lines, TooltipFlag flag) {
        ResourceLocation id = getResearchId(stack);
        if (id != null) {
            ResearchRegistry.get(id).ifPresent(t ->
                    lines.add(Component.translatable(t.getTitleKey())
                            .withStyle(ChatFormatting.AQUA)));
        }
        lines.add(Component.translatable("item.koniava.completed_research.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation id = getResearchId(stack);
        if (id != null) {
            return ResearchRegistry.get(id)
                    .map(t -> (Component) Component.translatable(
                            "item.koniava.completed_research.named",
                            Component.translatable(t.getTitleKey())))
                    .orElse(super.getName(stack));
        }
        return super.getName(stack);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static ResourceLocation getResearchId(ItemStack stack) {
        return stack.get(ModDataComponents.RESEARCH_ID);
    }

    private static void spawnFirstResearchCelebration(ServerPlayer player, ServerLevel level) {
        double x = player.getX(), y = player.getY() + 1.5, z = player.getZ();
        level.sendParticles(ParticleTypes.FIREWORK, x, y,     z, 60, 0.6, 0.8, 0.6, 0.12);
        level.sendParticles(ParticleTypes.FIREWORK, x, y + 1, z, 40, 0.4, 0.4, 0.4, 0.18);
        level.sendParticles(ParticleTypes.FLASH,    x, y,     z,  1, 0,   0,   0,   0);
        level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.PLAYERS, 2f, 1f);
        level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST,
                SoundSource.PLAYERS, 1.5f, 0.85f);
        level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_TWINKLE,
                SoundSource.PLAYERS, 1f, 1.1f);
    }
}
