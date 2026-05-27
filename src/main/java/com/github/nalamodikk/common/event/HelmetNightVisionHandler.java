package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.common.item.equipment.armor.HelmetUpgradeBehavior;
import com.github.nalamodikk.common.item.equipment.armor.HelmetUpgradeItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyHelmetItem;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class HelmetNightVisionHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof ManaAlloyHelmetItem)) return;
        if (!Boolean.TRUE.equals(helmet.get(ModDataComponents.NIGHT_VISION_ACTIVE))) return;
        if (!hasNightVisionUpgrade(helmet)) return;

        var existing = player.getEffect(MobEffects.NIGHT_VISION);
        if (existing == null || existing.getDuration() < 60) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
        }
    }

    public static boolean hasNightVisionUpgrade(ItemStack helmet) {
        for (ItemStack upg : ManaArmorItem.getData(helmet).upgrades().values()) {
            if (upg.getItem() instanceof HelmetUpgradeItem hu
                    && hu.getBehavior() == HelmetUpgradeBehavior.NIGHT_VISION)
                return true;
        }
        return false;
    }
}
