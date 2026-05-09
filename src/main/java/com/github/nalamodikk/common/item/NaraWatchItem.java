package com.github.nalamodikk.common.item;

import com.github.nalamodikk.research.AspectScanner;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.research.network.AspectSyncPacket;
import com.github.nalamodikk.research.network.WatchSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

public class NaraWatchItem extends Item {

    private static final int  SCAN_TICKS = 10;   // 0.5 seconds
    private static final double SCAN_RANGE = 4.5;

    public NaraWatchItem(Properties properties) {
        super(properties);
    }

    // ── Right-click ───────────────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Aiming at a block → start 0.5 s hold; finishUsingItem will scan
        var hit = player.pick(SCAN_RANGE, 0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        // Not aiming at anything → open research tree immediately
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            var knowledge = ResearchSavedData.get(sp.serverLevel()).getOrCreate(sp.getUUID());
            WatchSyncPacket.sendTo(sp, knowledge.getCompletedResearch(), knowledge.getCurrentTier());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ── Hold behaviour ────────────────────────────────────────────────────────

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return SCAN_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW; // subtle arm-raise while holding
    }

    /** Plays a rising tick sound every 2 ticks while holding to scan. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide()) return;
        int elapsed = SCAN_TICKS - remainingUseDuration;
        if (elapsed % 2 == 0) {
            float pitch = 0.7f + (elapsed / (float) SCAN_TICKS) * 0.8f;
            level.playSound(null, entity.blockPosition(),
                    SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.35f, pitch);
        }
    }

    /** Called after the full hold duration — do the actual scan here. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;

        var hit = player.pick(SCAN_RANGE, 0f, false);
        if (!(hit instanceof BlockHitResult blockHit)) return stack;

        var blockState = level.getBlockState(blockHit.getBlockPos());
        List<Aspect> aspects = AspectScanner.getAspectsFor(blockState);

        if (aspects.isEmpty()) {
            player.sendSystemMessage(
                    Component.translatable("item.koniava.nara_watch.scan.unknown"));
            return stack;
        }

        // Discover new aspects
        var knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
        List<Aspect> newOnes = new ArrayList<>();
        for (Aspect a : aspects) {
            if (knowledge.discoverAspect(a)) newOnes.add(a);
        }
        ResearchSavedData.get(player.serverLevel()).setDirty();

        // Sync updated discoveries to client cache
        AspectSyncPacket.sendTo(player);

        // Feedback message
        if (!newOnes.isEmpty()) {
            player.sendSystemMessage(
                    Component.translatable("item.koniava.nara_watch.scan.discovered"));
            for (Aspect a : newOnes) {
                player.sendSystemMessage(
                        Component.literal("  ▸ ").append(
                                a.getName().copy().withStyle(ChatFormatting.AQUA)));
            }
        } else {
            String names = aspects.stream()
                    .map(a -> a.getName().getString())
                    .reduce((a, b) -> a + ", " + b).orElse("");
            player.sendSystemMessage(Component.translatable(
                    "item.koniava.nara_watch.scan.known",
                    Component.literal(names).withStyle(ChatFormatting.GRAY)));
        }

        // Completion sound
        level.playSound(null, blockHit.getBlockPos(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.3f);

        return stack;
    }
}
