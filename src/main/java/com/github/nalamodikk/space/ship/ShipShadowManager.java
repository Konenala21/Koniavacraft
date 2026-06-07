package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 飛船影子維度的區域分配器（SavedData，存 overworld）。每台船分到 ship_shadow 維度裡一個固定區域，
 * 它的 contraption 方塊真正放在那裡 tick（機器運轉/作物生長）。slot 平鋪間隔夠大，船之間不重疊。
 * v1：slot 不重用（free 只移除對應，nextSlot 一直增），夠用；之後再做緊湊回收。
 */
public class ShipShadowManager extends SavedData {

    private static final String DATA_NAME = KoniavacraftMod.MOD_ID + "_ship_shadow";
    private static final int SLOT_SPACING = 256; // 各船區域間隔（遠大於最大船 32），不重疊

    private int nextSlot = 0;
    private final Map<UUID, Integer> slots = new HashMap<>();

    public static ShipShadowManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ShipShadowManager::new, ShipShadowManager::load, null),
                DATA_NAME
        );
    }

    /** 取得影子維度的 ServerLevel（datapack 維度，永遠載入）。 */
    public static ServerLevel shadowLevel(MinecraftServer server) {
        return server.getLevel(ModDimensions.SHIP_SHADOW);
    }

    /**
     * BE 是否活在飛船影子維度。機器選單的 stillValid 用：影子機器離玩家很遠，距離檢查必失敗 → GUI 秒關；
     * server 端選單綁的是影子 BE，這裡回 true 直接放行(玩家其實站在視覺船上互動，跨維度是實作細節)。
     */
    public static boolean isShadowBE(net.minecraft.world.level.block.entity.BlockEntity be) {
        return be != null && be.getLevel() != null && be.getLevel().dimension().equals(ModDimensions.SHIP_SHADOW);
    }

    /** 分配（或取回既有）某船的影子錨點。 */
    public BlockPos allocate(UUID shipId) {
        int slot = slots.computeIfAbsent(shipId, k -> nextSlot++);
        setDirty();
        return anchorOf(slot);
    }

    /** 釋放某船的 slot（拆解/移除時）。 */
    public void free(UUID shipId) {
        if (slots.remove(shipId) != null) setDirty();
    }

    private static BlockPos anchorOf(int slot) {
        return new BlockPos(slot * SLOT_SPACING, 64, 0);
    }

    // ── 影子區域的放置/清除/載入 ──────────────────────────────────────────────

    /** 把 contraption 方塊放進影子（不旋轉，影子是 canonical 框）。機器/作物在這裡 tick。需先 force-load。 */
    public static void placeInShadow(ServerLevel shadow, BlockPos anchor, ShipContraption c) {
        c.addToWorld(shadow, anchor, Rotation.NONE);
    }

    /** 清掉影子裡這台船的方塊（拆解/移除時）。 */
    public static void clearShadow(ServerLevel shadow, BlockPos anchor, ShipContraption c) {
        for (BlockPos local : c.getBlocks().keySet()) {
            shadow.setBlock(anchor.offset(local), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** force-load（或解除）影子區域的區塊，讓機器持續 tick。 */
    public static void setForceLoad(ServerLevel shadow, BlockPos anchor, AABB bounds, boolean load) {
        int minCx = SectionPos.blockToSectionCoord(anchor.getX() + (int) Math.floor(bounds.minX));
        int maxCx = SectionPos.blockToSectionCoord(anchor.getX() + (int) Math.ceil(bounds.maxX));
        int minCz = SectionPos.blockToSectionCoord(anchor.getZ() + (int) Math.floor(bounds.minZ));
        int maxCz = SectionPos.blockToSectionCoord(anchor.getZ() + (int) Math.ceil(bounds.maxZ));
        for (int cx = minCx; cx <= maxCx; cx++)
            for (int cz = minCz; cz <= maxCz; cz++)
                shadow.setChunkForced(cx, cz, load);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Next", nextSlot);
        ListTag list = new ListTag();
        slots.forEach((id, slot) -> {
            CompoundTag e = new CompoundTag();
            e.putUUID("Id", id);
            e.putInt("Slot", slot);
            list.add(e);
        });
        tag.put("Slots", list);
        return tag;
    }

    public static ShipShadowManager load(CompoundTag tag, HolderLookup.Provider registries) {
        ShipShadowManager data = new ShipShadowManager();
        data.nextSlot = tag.getInt("Next");
        ListTag list = tag.getList("Slots", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            data.slots.put(e.getUUID("Id"), e.getInt("Slot"));
        }
        return data;
    }
}
