package com.github.nalamodikk.common.item;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.AspectScanner;
import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.knowledge.PlayerKnowledge;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.research.network.AspectSyncPacket;
import com.github.nalamodikk.research.network.WatchSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class NaraWatchItem extends Item {

    private static final int SCAN_TICKS = 10;
    private static final double SCAN_RANGE = 4.5;

    public NaraWatchItem(Properties properties) {
        super(properties);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isHoldingWatch(event.getEntity(), event.getHand())) {
            event.setUseBlock(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isHoldingWatch(event.getEntity(), event.getHand())) {
            event.setCanceled(true);
        }
    }

    private static boolean isHoldingWatch(Player player, InteractionHand hand) {
        return player.getItemInHand(hand).getItem() instanceof NaraWatchItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ScanTarget target = findScanTarget(player);

        if (target != null) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        if (!player.isShiftKeyDown()) {
            openResearchTree(player, level);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }

        int elapsed = SCAN_TICKS - remaining;
        playScanningEffects(level, player, elapsed);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return stack;
        }

        ScanTarget target = findScanTarget(player);
        if (target == null) {
            return stack;
        }

        processScanResult(player, target);
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return SCAN_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    private void openResearchTree(Player player, Level level) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerKnowledge knowledge = ResearchSavedData.get(serverPlayer.serverLevel())
                    .getOrCreate(serverPlayer.getUUID());
            WatchSyncPacket.sendTo(serverPlayer, knowledge.getCompletedResearch(), knowledge.getAvailableResearchOverrides(),
                    knowledge.getLockedResearch(), knowledge.getCurrentTier(),
                    knowledge.getDiscoveredAspectsMap());
        }
    }

    private void playScanningEffects(Level level, Player player, int elapsed) {
        if (elapsed % 2 == 0) {
            float pitch = 0.7f + (elapsed / (float) SCAN_TICKS) * 0.8f;
            level.playSound(null, player.blockPosition(),
                    SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.35f, pitch);
        }

        ScanTarget target = findScanTarget(player);
        if (target != null && level instanceof ServerLevel serverLevel) {
            Vec3 pos = target.pos();
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.x, pos.y, pos.z, 2, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void processScanResult(ServerPlayer player, ScanTarget target) {
        List<Aspect> aspects = target.aspects(player.serverLevel());
        if (aspects.isEmpty()) {
            player.sendSystemMessage(Component.translatable("item.koniava.nara_watch.scan.unknown"));
            return;
        }

        ResearchSavedData savedData = ResearchSavedData.get(player.serverLevel());
        PlayerKnowledge knowledge = savedData.getOrCreate(player.getUUID());
        ResourceLocation id = target.id();

        if (knowledge.recordItemScan(id)) {
            int reward = ThreadLocalRandom.current().nextInt(2, 6);
            List<AspectReward> rewards = new ArrayList<>();
            for (Aspect aspect : aspects) {
                boolean newlyDiscovered = knowledge.discoverAspect(aspect, reward);
                rewards.add(new AspectReward(aspect, reward, newlyDiscovered));
            }

            notifyDiscovery(player, rewards);
            playSuccessEffects(player.level(), target.pos());
            savedData.setDirty();
            AspectSyncPacket.sendTo(player);
        } else {
            player.sendSystemMessage(Component.translatable("item.koniava.nara_watch.scan.duplicate")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    private void notifyDiscovery(ServerPlayer player, List<AspectReward> rewards) {
        player.sendSystemMessage(Component.translatable("item.koniava.nara_watch.scan.first_time")
                .withStyle(ChatFormatting.GOLD));

        for (AspectReward reward : rewards) {
            Component aspectName = reward.aspect().getName().copy()
                    .withStyle(reward.newlyDiscovered() ? ChatFormatting.AQUA : ChatFormatting.GRAY);
            player.sendSystemMessage(Component.translatable(
                    "item.koniava.nara_watch.scan.aspect_entry",
                    aspectName,
                    reward.amount()).withStyle(ChatFormatting.YELLOW));
        }
    }

    private void playSuccessEffects(Level level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.3f);
    }

    @Nullable
    private ScanTarget findScanTarget(Player player) {
        ItemEntity itemEntity = getItemEntityTarget(player);
        if (itemEntity != null) {
            return new ScanTarget.ItemTarget(itemEntity);
        }

        Entity entity = getEntityTarget(player);
        if (entity != null) {
            return new ScanTarget.EntityTarget(entity);
        }

        HitResult hit = pickBlockOrFluid(player);
        if (hit instanceof BlockHitResult blockHit
                && blockHit.getType() != HitResult.Type.MISS) {
            if (!player.level().getBlockState(blockHit.getBlockPos()).isAir()) {
                return new ScanTarget.BlockTarget(blockHit, player.level());
            }
            if (!player.level().getFluidState(blockHit.getBlockPos()).isEmpty()) {
                return new ScanTarget.FluidTarget(blockHit, player.level());
            }
        }

        return null;
    }

    private HitResult pickBlockOrFluid(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(SCAN_RANGE));
        ClipContext context = new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player);
        return player.level().clip(context);
    }

    @Nullable
    private ItemEntity getItemEntityTarget(Player player) {
        return getEntity(player, ItemEntity.class, 1.0, 0.2);
    }

    @Nullable
    private Entity getEntityTarget(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(SCAN_RANGE));
        AABB box = new AABB(eye, end).inflate(1.5);

        for (Entity entity : player.level().getEntities(player, box)) {
            Entity target = entity instanceof PartEntity<?> part ? part.getParent() : entity;
            if (target instanceof Player || target instanceof ItemEntity) {
                continue;
            }

            if (entity.getBoundingBox().inflate(0.1).clip(eye, end).isPresent()) {
                return target;
            }
        }
        return null;
    }

    @Nullable
    private <T extends Entity> T getEntity(Player player, Class<T> clazz, double inflateBox, double inflateHitbox) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(SCAN_RANGE));
        AABB box = new AABB(eye, end).inflate(inflateBox);

        for (T entity : player.level().getEntitiesOfClass(clazz, box, candidate -> candidate != player)) {
            if (entity.getBoundingBox().inflate(inflateHitbox).clip(eye, end).isPresent()) {
                return entity;
            }
        }
        return null;
    }

    private interface ScanTarget {
        ResourceLocation id();

        Vec3 pos();

        List<Aspect> aspects(ServerLevel level);

        record ItemTarget(ItemEntity entity) implements ScanTarget {
            @Override
            public ResourceLocation id() {
                return BuiltInRegistries.ITEM.getKey(entity.getItem().getItem());
            }

            @Override
            public Vec3 pos() {
                return entity.position().add(0.0, 0.5, 0.0);
            }

            @Override
            public List<Aspect> aspects(ServerLevel level) {
                return AspectScanner.getAspectsForItem(entity.getItem().getItem(), level);
            }
        }

        record EntityTarget(Entity entity) implements ScanTarget {
            @Override
            public ResourceLocation id() {
                return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            }

            @Override
            public Vec3 pos() {
                return entity.position().add(0.0, entity.getBbHeight() / 2.0, 0.0);
            }

            @Override
            public List<Aspect> aspects(ServerLevel level) {
                return AspectScanner.getAspectsForEntity(entity, level);
            }
        }

        record BlockTarget(BlockHitResult hit, Level targetLevel) implements ScanTarget {
            @Override
            public ResourceLocation id() {
                return BuiltInRegistries.BLOCK.getKey(targetLevel.getBlockState(hit.getBlockPos()).getBlock());
            }

            @Override
            public Vec3 pos() {
                return Vec3.atCenterOf(hit.getBlockPos());
            }

            @Override
            public List<Aspect> aspects(ServerLevel level) {
                return AspectScanner.getAspectsFor(level.getBlockState(hit.getBlockPos()), level);
            }
        }

        record FluidTarget(BlockHitResult hit, Level targetLevel) implements ScanTarget {
            @Override
            public ResourceLocation id() {
                return BuiltInRegistries.FLUID.getKey(targetLevel.getFluidState(hit.getBlockPos()).getType());
            }

            @Override
            public Vec3 pos() {
                return Vec3.atCenterOf(hit.getBlockPos());
            }

            @Override
            public List<Aspect> aspects(ServerLevel level) {
                return AspectScanner.getAspectsForFluid(level.getFluidState(hit.getBlockPos()), level);
            }
        }
    }

    private record AspectReward(Aspect aspect, int amount, boolean newlyDiscovered) {
    }
}
