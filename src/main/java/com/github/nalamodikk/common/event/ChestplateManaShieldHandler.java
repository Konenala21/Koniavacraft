package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.common.item.equipment.armor.ManaAlloyChestplateItem;
import com.github.nalamodikk.common.network.packet.client.armor.ManaShieldHitPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ChestplateManaShieldHandler {

    public static final float SHIELD_RATE = 0.4f;
    public static final int MANA_PER_DAMAGE = 10;

    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof ManaAlloyChestplateItem)) return;

        int mana = ManaArmorItem.getMana(chest);
        if (mana <= 0) return;

        float incoming = event.getNewDamage();
        if (incoming <= 0) return;

        float wantAbsorb = incoming * SHIELD_RATE;
        int manaNeeded = (int) Math.ceil(wantAbsorb * MANA_PER_DAMAGE);

        float actualAbsorb;
        if (manaNeeded <= mana) {
            actualAbsorb = wantAbsorb;
            ManaArmorItem.setMana(chest, mana - manaNeeded);
        } else {
            actualAbsorb = mana / (float) MANA_PER_DAMAGE;
            ManaArmorItem.setMana(chest, 0);
        }

        event.setNewDamage(Math.max(0f, incoming - actualAbsorb));

        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS,
                0.9f, 0.85f + player.level().getRandom().nextFloat() * 0.3f);
        PacketDistributor.sendToPlayer(player, new ManaShieldHitPacket(player.getId()));
    }
}
