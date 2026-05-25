package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyLeggingsItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class LeggingsDoubleJumpHandler {

    public static final int DOUBLE_JUMP_MANA_COST = 300;
    private static final double DOUBLE_JUMP_VELOCITY = 0.40;

    private static final Set<UUID> hasDoubleJumped = new HashSet<>();

    public static void handleDoubleJump(ServerPlayer player) {
        if (player.getAbilities().flying || player.isSpectator()) return;

        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!(leggings.getItem() instanceof ManaAlloyLeggingsItem)) return;
        if (hasDoubleJumped.contains(player.getUUID())) return;

        int mana = ManaArmorItem.getMana(leggings);
        if (mana < DOUBLE_JUMP_MANA_COST) {
            player.displayClientMessage(
                    Component.translatable("message.koniava.leggings.not_enough_mana"), true);
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, DOUBLE_JUMP_VELOCITY, motion.z);
        player.hurtMarked = true;

        ManaArmorItem.setMana(leggings, mana - DOUBLE_JUMP_MANA_COST);
        hasDoubleJumped.add(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.onGround() || player.isInWater() || player.isInLava()) {
            hasDoubleJumped.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        hasDoubleJumped.remove(event.getEntity().getUUID());
    }
}
