package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.wand.WandCoreData;
import com.github.nalamodikk.common.item.wand.WandRodItem;
import com.github.nalamodikk.common.item.wand.upgrade.WandUpgradeBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class WandRangeEventHandler {

    private static final ResourceLocation WAND_RANGE_ID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "wand_range_bonus");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        AttributeInstance rangeAttr = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (rangeAttr == null) return;

        rangeAttr.removeModifier(WAND_RANGE_ID);

        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof WandRodItem)) return;

        WandCoreData data = WandRodItem.getData(mainHand);
        int rangeBonus = data.sumUpgradeBonus(WandUpgradeBehavior.RANGE);
        if (rangeBonus <= 0) return;

        rangeAttr.addTransientModifier(new AttributeModifier(
                WAND_RANGE_ID,
                (double) rangeBonus,
                AttributeModifier.Operation.ADD_VALUE
        ));
    }
}
