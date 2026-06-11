package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 追蹤「哪個玩家正開著哪格船箱子」，用 container close 事件把關蓋訊號送回 ShipEntity。
 * 船箱子的 GUI 是影子箱子的選單，vanilla 沒有「這個選單屬於船 X 的 local L」的資訊，所以開箱時
 * (ShipEntity.interactWithPick)登記，玩家關容器時(PlayerContainerEvent.Close)查表 → onChestClosed。
 * 換開另一個容器時 vanilla 會先 Close 舊的再 Open 新的，所以計數對得上；track 裡也保險收掉殘留。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public final class ShipChestTracker {
    private ShipChestTracker() {}

    private record Ref(ShipEntity ship, BlockPos local) {}
    private static final Map<UUID, Ref> OPEN = new HashMap<>();

    public static void track(ServerPlayer player, ShipEntity ship, BlockPos local) {
        Ref prev = OPEN.put(player.getUUID(), new Ref(ship, local.immutable()));
        if (prev != null && !prev.ship().isRemoved()) prev.ship().onChestClosed(prev.local());
    }

    @SubscribeEvent
    public static void onClose(PlayerContainerEvent.Close event) {
        Ref ref = OPEN.remove(event.getEntity().getUUID());
        if (ref != null && !ref.ship().isRemoved()) ref.ship().onChestClosed(ref.local());
    }
}
