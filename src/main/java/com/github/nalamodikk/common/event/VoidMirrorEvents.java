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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class VoidMirrorEvents {

    // 戰鬥中在鏡中世界放置的方塊（分身的牆 + 玩家蓋的 + 寶箱），重製時設回空氣
    private static final Set<Long> MODIFIED_BLOCKS = new HashSet<>();
    // 機甲組裝時挖走的環境方塊（{pos: 原始狀態}），重製時還原原狀（避免地形殘留空洞）
    private static final Map<Long, BlockState> MINED_TERRAIN = new HashMap<>();

    public static void addModifiedBlock(long packedPos) {
        MODIFIED_BLOCKS.add(packedPos);
    }

    public static void addMinedTerrain(long packedPos, BlockState original) {
        MINED_TERRAIN.putIfAbsent(packedPos, original); // 第一次挖才記，避免後續同位置被覆蓋遮蔽原狀
    }

    // 鏡中世界禁止：界伏盒 + 終界箱（玩家可能從場外鏡像範圍藏裝備、進場內取出規避鏡像）
    private static boolean isBannedContainerItem(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem bi)) return false;
        return bi.getBlock() instanceof ShulkerBoxBlock || bi.getBlock() instanceof EnderChestBlock;
    }

    private static boolean isBannedContainerBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof ShulkerBoxBlock || state.getBlock() instanceof EnderChestBlock;
    }

    // 鏡中世界禁止放置/開啟禁用容器（界伏盒、終界箱）：杜絕逃課帶裝備進場
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().dimension().equals(ModDimensions.VOID_MIRROR)) return;
        if (isBannedContainerItem(event.getItemStack())
                || isBannedContainerBlock(event.getLevel().getBlockState(event.getPos()))) {
            event.setCanceled(true);
            notifyContainerBlocked(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().dimension().equals(ModDimensions.VOID_MIRROR)) return;
        if (isBannedContainerItem(event.getItemStack())) {
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

    // 玩家在鏡中世界破壞方塊 → 記原始狀態，重製時還原（避免戰鬥後 arena 一堆洞）
    // 玩家自己之前放的（MODIFIED_BLOCKS 已記）不還原 → 反正 reset 會把它變成 AIR
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (!sl.dimension().equals(ModDimensions.VOID_MIRROR)) return;
        long key = event.getPos().asLong();
        if (MODIFIED_BLOCKS.contains(key)) return;
        BlockState state = event.getState();
        if (state.isAir()) return;
        MINED_TERRAIN.putIfAbsent(key, state);
    }

    // 鏡中世界內的爆炸（TNT / 苦力怕 / 砲彈等）也會炸出洞 → 對每個受影響方塊記原狀
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (!sl.dimension().equals(ModDimensions.VOID_MIRROR)) return;
        for (BlockPos pos : event.getAffectedBlocks()) {
            long key = pos.asLong();
            if (MODIFIED_BLOCKS.contains(key)) continue;
            BlockState state = sl.getBlockState(pos);
            if (state.isAir()) continue;
            MINED_TERRAIN.putIfAbsent(key, state);
        }
    }

    // 重製場地：清掉戰鬥放置的方塊（→AIR）、還原機甲挖走的環境方塊（→原狀）、移除所有 boss 相關實體
    private static void resetArena(ServerLevel mirror) {
        for (long l : MODIFIED_BLOCKS) {
            BlockPos p = BlockPos.of(l);
            if (mirror.isLoaded(p)) {
                // SUPPRESS_DROPS：MODIFIED_BLOCKS 含上一場的獎勵箱，setBlockAndUpdate 還原成 air 會觸發
                // Containers.dropContents 把上一場沒拿的物品噴一地。flag 32 才不掉落（arena 清場本就不該掉東西）。
                mirror.setBlock(p, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        MODIFIED_BLOCKS.clear();
        for (Map.Entry<Long, BlockState> e : MINED_TERRAIN.entrySet()) {
            BlockPos p = BlockPos.of(e.getKey());
            if (mirror.isLoaded(p)) {
                mirror.setBlockAndUpdate(p, e.getValue());
            }
        }
        MINED_TERRAIN.clear();

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
