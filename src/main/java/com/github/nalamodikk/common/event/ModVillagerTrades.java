package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModItems;
import com.github.nalamodikk.register.ModVillagers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 村民交易:本源研究員({@link ModVillagers#ASPECT_RESEARCHER},職業方塊=本源研究桌)
 * 收鏡核碎片換基礎本源精華。
 *
 * 本源的 renewable 來源:掃描是一次性,但組裝技能會消耗本源,所以用鏡中 boss 掉的
 * 鏡核碎片(可重複 farm)跟村民換精華,再用精華補基礎本源。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ModVillagerTrades {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != ModVillagers.ASPECT_RESEARCHER.get()) return;
        // 自訂職業沒有 vanilla 預設交易,各等級的 list 可能不存在,要自己建。
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
        for (int level = 1; level <= 2; level++) {
            List<VillagerTrades.ItemListing> tier = trades.get(level);
            if (tier == null) {
                tier = new ArrayList<>();
                trades.put(level, tier);
            }
            tier.add(new ShardForEssence());
        }
    }

    /** 鏡核碎片 ×1 → 基礎本源精華 ×1。 */
    private static class ShardForEssence implements VillagerTrades.ItemListing {
        @Override
        public @Nullable MerchantOffer getOffer(Entity trader, RandomSource random) {
            return new MerchantOffer(
                    new ItemCost(ModItems.MIRROR_CORE_SHARD.get(), 1),
                    new ItemStack(ModItems.BASIC_ASPECT_ESSENCE.get(), 1),
                    12, 5, 0.05F);
        }
    }
}
