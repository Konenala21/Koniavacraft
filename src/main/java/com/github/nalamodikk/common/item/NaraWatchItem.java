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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NaraWatchItem extends Item {

    private static final int    SCAN_TICKS = 10;  // 0.5 seconds
    private static final double SCAN_RANGE = 4.5;

    public NaraWatchItem(Properties properties) {
        super(properties);
    }

    // ── Right-click ───────────────────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Aiming at a block or item entity → start 0.5s hold
        var blockHit = player.pick(SCAN_RANGE, 0f, false);
        if (blockHit.getType() == HitResult.Type.BLOCK
                || getItemEntityTarget(player) != null) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        // Not aiming at anything scannable → open research tree
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            var knowledge = ResearchSavedData.get(sp.serverLevel()).getOrCreate(sp.getUUID());
            WatchSyncPacket.sendTo(sp, knowledge.getCompletedResearch(), knowledge.getCurrentTier(),
                    knowledge.getDiscoveredAspects());
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
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        int elapsed = SCAN_TICKS - remainingUseDuration;

        // Rising tick sound
        if (elapsed % 2 == 0) {
            float pitch = 0.7f + (elapsed / (float) SCAN_TICKS) * 0.8f;
            level.playSound(null, entity.blockPosition(),
                    SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.35f, pitch);
        }

        // Particles at scan target
        if (!(entity instanceof Player player)) return;
        Vec3 targetPos = null;

        ItemEntity ie = getItemEntityTarget(player);
        if (ie != null) {
            targetPos = ie.position().add(0, 0.5, 0);
        } else {
            var hit = player.pick(SCAN_RANGE, 0f, false);
            if (hit instanceof BlockHitResult bhr) {
                targetPos = Vec3.atCenterOf(bhr.getBlockPos());
            }
        }

        if (targetPos != null) {
            float progress = elapsed / (float) SCAN_TICKS;
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    targetPos.x, targetPos.y, targetPos.z,
                    1 + (int)(progress * 3), 0.2, 0.2, 0.2, 0.02);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;

        // Try item entity first, then block
        ItemEntity itemEntity = getItemEntityTarget(player);
        List<Aspect> aspects;

        if (itemEntity != null) {
            aspects = AspectScanner.getAspectsForItem(itemEntity.getItem().getItem());
        } else {
            var hit = player.pick(SCAN_RANGE, 0f, false);
            if (!(hit instanceof BlockHitResult blockHit)) return stack;
            aspects = AspectScanner.getAspectsFor(level.getBlockState(blockHit.getBlockPos()));
        }

        if (aspects.isEmpty()) {
            player.sendSystemMessage(Component.translatable("item.koniava.nara_watch.scan.unknown"));
            return stack;
        }

        var knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
        List<Aspect> newOnes = new ArrayList<>();
        for (Aspect a : aspects) {
            if (knowledge.discoverAspect(a)) newOnes.add(a);
        }
        ResearchSavedData.get(player.serverLevel()).setDirty();
        AspectSyncPacket.sendTo(player);

        if (!newOnes.isEmpty()) {
            player.sendSystemMessage(Component.translatable("item.koniava.nara_watch.scan.discovered"));
            for (Aspect a : newOnes)
                player.sendSystemMessage(Component.literal("  ▸ ").append(
                        a.getName().copy().withStyle(ChatFormatting.AQUA)));
        } else {
            String names = aspects.stream().map(a -> a.getName().getString())
                    .reduce((a, b) -> a + ", " + b).orElse("");
            player.sendSystemMessage(Component.translatable("item.koniava.nara_watch.scan.known",
                    Component.literal(names).withStyle(ChatFormatting.GRAY)));
        }

        Vec3 pos = itemEntity != null ? itemEntity.position() : player.position();
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.3f);

        return stack;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Nullable
    private static ItemEntity getItemEntityTarget(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(SCAN_RANGE));
        AABB searchBox = new AABB(eye, end).inflate(1.0);

        for (Entity e : player.level().getEntities(player, searchBox)) {
            if (!(e instanceof ItemEntity ie)) continue;
            Optional<Vec3> hit = ie.getBoundingBox().inflate(0.2).clip(eye, end);
            if (hit.isPresent()) return ie;
        }
        return null;
    }
}
