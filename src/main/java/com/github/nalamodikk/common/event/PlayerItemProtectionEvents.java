package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles special item protection logic, such as the Nara Watch being soulbound
 * (kept on death) and never despawning.
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class PlayerItemProtectionEvents {

    private static final Map<UUID, List<ItemStack>> SOULBOUND_STORAGE = new HashMap<>();

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
            return;
        }

        List<ItemEntity> toRemove = new ArrayList<>();
        List<ItemStack> toKeep = new ArrayList<>();

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.is(ModItems.NARA_WATCH.get())) {
                toKeep.add(stack.copy());
                toRemove.add(drop);
            }
        }

        if (!toKeep.isEmpty()) {
            event.getDrops().removeAll(toRemove);
            SOULBOUND_STORAGE.put(player.getUUID(), toKeep);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        
        // If keepInventory is true, vanilla handles it
        if (newPlayer.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) {
            return;
        }

        List<ItemStack> keptItems = SOULBOUND_STORAGE.remove(oldPlayer.getUUID());
        if (keptItems != null) {
            for (ItemStack stack : keptItems) {
                if (!newPlayer.getInventory().add(stack)) {
                    newPlayer.drop(stack, false);
                }
            }
        }
    }
}
