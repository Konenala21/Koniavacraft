package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.dimension.VoidMirrorTeleport;
import com.github.nalamodikk.common.entity.PlayerCloneEntity;
import com.github.nalamodikk.common.entity.NaraPhantomEntity;
import com.github.nalamodikk.common.entity.FloatingTurretEntity;
import com.github.nalamodikk.common.entity.SpaceCrackEntity;
import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.narasystem.nara.network.client.NaraTauntPacket;
import com.github.nalamodikk.register.ModDataAttachments;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class VoidMirrorEvents {

    // 戰鬥中在鏡中世界放置的方塊（分身的牆 + 玩家蓋的 + 寶箱），重製時清掉
    private static final Set<Long> MODIFIED_BLOCKS = new HashSet<>();

    public static void addModifiedBlock(long packedPos) {
        MODIFIED_BLOCKS.add(packedPos);
    }

    private static boolean isShulkerItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
    }

    // 鏡中世界禁止放置/開啟界伏盒子：杜絕把裝備藏盒子規避鏡像、再於場內取出的逃課
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().dimension().equals(ModDimensions.VOID_MIRROR)) return;
        if (isShulkerItem(event.getItemStack())
                || event.getLevel().getBlockState(event.getPos()).getBlock() instanceof ShulkerBoxBlock) {
            event.setCanceled(true);
            notifyContainerBlocked(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().dimension().equals(ModDimensions.VOID_MIRROR)) return;
        if (isShulkerItem(event.getItemStack())) {
            event.setCanceled(true);
            notifyContainerBlocked(event.getEntity());
        }
    }

    private static void notifyContainerBlocked(Player player) {
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(
                    Component.translatable("message.koniava.void_mirror.container_disabled"), true);
        }
    }

    // 玩家在鏡中世界放置方塊 → 記錄，重製時清除
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() == null) return;
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        if (!sl.dimension().equals(ModDimensions.VOID_MIRROR)) return;
        MODIFIED_BLOCKS.add(event.getPos().asLong());
    }

    // 重製場地：清掉戰鬥放置的方塊 + 移除所有 boss 相關實體
    private static void resetArena(ServerLevel mirror) {
        for (long l : MODIFIED_BLOCKS) {
            BlockPos p = BlockPos.of(l);
            if (mirror.isLoaded(p)) {
                mirror.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            }
        }
        MODIFIED_BLOCKS.clear();

        AABB box = new AABB(BlockPos.ZERO).inflate(300);
        mirror.getEntitiesOfClass(PlayerCloneEntity.class, box).forEach(Entity::discard);
        mirror.getEntitiesOfClass(NaraPhantomEntity.class, box).forEach(Entity::discard);
        mirror.getEntitiesOfClass(FloatingTurretEntity.class, box).forEach(Entity::discard);
        mirror.getEntitiesOfClass(SpaceCrackEntity.class, box).forEach(Entity::discard);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel sl)) return;
        if (!sl.dimension().equals(ModDimensions.VOID_MIRROR)) return;

        // 玩家在鏡中世界死亡：移除其分身，讓重新進入時是一場全新戰鬥
        UUID id = player.getUUID();
        for (PlayerCloneEntity clone : sl.getEntitiesOfClass(PlayerCloneEntity.class,
                new AABB(BlockPos.ZERO).inflate(260),
                c -> c.getSourceUUID().map(id::equals).orElse(false))) {
            clone.discard();
        }
    }

    @SubscribeEvent
    public static void onLeaveDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getFrom().equals(ModDimensions.VOID_MIRROR)) return;
        ServerLevel mirror = player.server.getLevel(ModDimensions.VOID_MIRROR);
        if (mirror == null) return;

        // 離開鏡中世界：移除該玩家的分身與娜拉幻影，避免殘留導致重進時重複生成
        UUID id = player.getUUID();
        AABB box = new AABB(BlockPos.ZERO).inflate(260);
        for (PlayerCloneEntity clone : mirror.getEntitiesOfClass(PlayerCloneEntity.class, box,
                c -> c.getSourceUUID().map(id::equals).orElse(false))) {
            clone.discard();
        }
        for (NaraPhantomEntity nara : mirror.getEntitiesOfClass(NaraPhantomEntity.class, box,
                n -> n.getSourceUUID().map(id::equals).orElse(false))) {
            nara.discard();
        }

        // 沒有玩家留在鏡中世界 → 重製場地（清方塊 + 清剩餘 boss 實體）
        if (mirror.players().isEmpty()) {
            resetArena(mirror);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // 仍持有返回點 = 死在鏡中世界尚未離開，重生後送回裂縫外 + 娜拉嘲諷
        if (player.getData(ModDataAttachments.RETURN_POINT.get()).isPresent()) {
            VoidMirrorTeleport.exit(player);
            PacketDistributor.sendToPlayer(player, NaraTauntPacket.INSTANCE);
        }
    }
}
