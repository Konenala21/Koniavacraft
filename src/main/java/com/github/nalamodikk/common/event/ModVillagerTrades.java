package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 村民交易:圖書管理員(LIBRARIAN,等級 3)收鏡核碎片換基礎本源精華。
 *
 * 本源的 renewable 來源:掃描是一次性,但組裝技能會消耗本源,所以用鏡中 boss 掉的
 * 鏡核碎片(可重複 farm)跟村民換精華,再用精華補基礎本源。
 *
 * 註:原本想做自訂「本源研究員」職業 + 研究台當職業方塊,但研究台是有 BlockEntity 的
 * 方塊,當 POI 會卡頓且村民不易任職,先改掛 vanilla 圖書管理員(知識主題相近)。要正式
 * 的本源研究員職業之後用專屬簡單方塊另做。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ModVillagerTrades {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.LIBRARIAN) return;
        List<VillagerTrades.ItemListing> tier3 = event.getTrades().get(3);
        if (tier3 != null) {
            tier3.add(new ShardForEssence());
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
