package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.equipment.ManaArmorItem;
import com.github.nalamodikk.common.item.equipment.armor.ChestplateUpgradeBehavior;
import com.github.nalamodikk.common.item.equipment.armor.ChestplateUpgradeItem;
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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ChestplateManaShieldHandler {

    // 類型一回血
    public static final int   HEAL_MK = 3;
    public static final float HEAL_FRACTION = 0.5f;
    public static final int   HEAL_DURATION_TICKS = 60; // 3 秒

    // 類型二吸收盾
    public static final int ABSORB_MANA_PER_POINT = 75;
    public static final int ABSORB_FLAT_IMMUNE_MK = 3;
    public static final int ABSORB_FLAT_IMMUNE    = 20;

    // 待回補的血量：UUID → {剩餘總量, 每 tick 量}
    private static final Map<UUID, float[]> PENDING_HEAL = new HashMap<>();

    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof ManaAlloyChestplateItem)) return;

        ChestplateUpgradeItem shield = ManaAlloyChestplateItem.getShieldUpgrade(chest);
        if (shield == null) return;

        float incoming = event.getNewDamage();
        if (incoming <= 0) return;

        ChestplateUpgradeBehavior behavior = shield.getBehavior();
        int mk = shield.getMk();

        if (behavior.isShieldReduction()) {
            float pct = behavior.getBonusForMk(mk) / 100f;
            float taken = incoming * (1f - pct);
            event.setNewDamage(Math.max(0f, taken));
            playBlock(player);
            if (mk >= HEAL_MK && taken > 0) {
                scheduleHeal(player, taken * HEAL_FRACTION);
            }
        } else if (behavior.isShieldAbsorb()) {
            float taken = absorb(player, chest, behavior, mk, incoming);
            event.setNewDamage(Math.max(0f, taken));
        }
    }

    private static float absorb(ServerPlayer player, ItemStack chest,
                                ChestplateUpgradeBehavior behavior, int mk, float incoming) {
        int cap = behavior.getBonusForMk(mk);
        int energy = Math.min(ManaAlloyChestplateItem.getShieldEnergy(chest), cap);

        float remaining = incoming;

        // Mk3：盾有值時，每次受擊先免傷 20（不扣盾）
        if (mk >= ABSORB_FLAT_IMMUNE_MK && energy > 0) {
            remaining = Math.max(0f, remaining - ABSORB_FLAT_IMMUNE);
            if (remaining <= 0f) {
                ManaAlloyChestplateItem.setShieldEnergy(chest, energy);
                playBlock(player);
                return 0f;
            }
        }

        boolean blocked = false;

        // 先用現有盾值吸收
        if (energy > 0) {
            int used = (int) Math.min(energy, Math.ceil(remaining));
            energy -= used;
            remaining -= used;
            blocked = true;
        }

        // 盾不足 → 燒魔力補盾（補到上限，受魔力限制）後繼續吸收
        if (remaining > 0 && energy == 0) {
            int mana = ManaArmorItem.getMana(chest);
            int refill = Math.min(cap, mana / ABSORB_MANA_PER_POINT);
            if (refill > 0) {
                ManaArmorItem.setMana(chest, mana - refill * ABSORB_MANA_PER_POINT);
                energy = refill;
                int used = (int) Math.min(energy, Math.ceil(remaining));
                energy -= used;
                remaining -= used;
                blocked = true;
            }
        }

        ManaAlloyChestplateItem.setShieldEnergy(chest, energy);
        if (blocked) playBlock(player);
        return remaining;
    }

    private static void scheduleHeal(ServerPlayer player, float total) {
        float[] existing = PENDING_HEAL.get(player.getUUID());
        float carry = existing != null ? existing[0] : 0f;
        float newTotal = carry + total;
        PENDING_HEAL.put(player.getUUID(), new float[]{ newTotal, newTotal / HEAL_DURATION_TICKS });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        float[] heal = PENDING_HEAL.get(player.getUUID());
        if (heal == null) return;
        if (heal[0] <= 0f || !player.isAlive()) {
            PENDING_HEAL.remove(player.getUUID());
            return;
        }
        float amount = Math.min(heal[0], heal[1]);
        player.heal(amount);
        heal[0] -= amount;
        if (heal[0] <= 0.001f) PENDING_HEAL.remove(player.getUUID());
    }

    private static void playBlock(ServerPlayer player) {
        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS,
                0.9f, 0.85f + player.level().getRandom().nextFloat() * 0.3f);
        PacketDistributor.sendToPlayer(player, new ManaShieldHitPacket(player.getId()));
    }
}
