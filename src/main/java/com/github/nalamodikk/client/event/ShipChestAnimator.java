package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.space.ship.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 船上箱子開蓋動畫(治本版)。船的箱子真身在影子維度、視覺船的 render BE 不被 tick，所以開箱預設沒有開蓋動畫。
 * 改由 server 在玩家真的開/關了某格船箱子時送 {@link com.github.nalamodikk.common.network.packet.client.ship.ShipChestLidPacket}
 * (權威訊號)，這裡只跟著訊號開合蓋，不再靠「畫面上有沒有 GUI」去猜 → 不會抖、不會拍。
 * 每格箱子每 tick 推 lidAnimateTick；關了且蓋子收完才清掉。可同時追多格(多人/多箱)。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public final class ShipChestAnimator {
    private ShipChestAnimator() {}

    private record Key(int shipId, BlockPos local) {}

    private static final Map<Key, Boolean> wantOpen = new HashMap<>();

    /** 收到 server 的權威開/關蓋訊號(ShipChestLidPacket.handle 呼叫)。 */
    public static void setLid(int shipId, BlockPos local, boolean open) {
        wantOpen.put(new Key(shipId, local.immutable()), open);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (wantOpen.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { wantOpen.clear(); return; }

        Iterator<Map.Entry<Key, Boolean>> it = wantOpen.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Key, Boolean> e = it.next();
            Key key = e.getKey();
            boolean open = e.getValue();

            Entity ent = mc.level.getEntity(key.shipId());
            if (!(ent instanceof ShipEntity ship) || ship.isRemoved()) { it.remove(); continue; }
            BlockEntity be = ship.getRenderBlockEntities().get(key.local());
            if (!(be instanceof ChestBlockEntity chest)) { it.remove(); continue; }

            chest.triggerEvent(1, open ? 1 : 0);                                            // 事件 1 = 開蓋；count>0 開、0 關
            ChestBlockEntity.lidAnimateTick(ship.level(), key.local(), chest.getBlockState(), chest); // 推進蓋子插值
            if (!open && chest.getOpenNess(1.0f) <= 0.0f) it.remove();                      // 關完了 → 清掉
        }
    }
}
