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

    // 類型一：每 1 點「減傷後傷害」消耗的魔力 + Mk3 回血
    public static final float REDUCTION_MANA_PER_DAMAGE = 7.5f;
    public static final int   HEAL_MK = 3;
    public static final float HEAL_FRACTION = 0.5f;
    public static final int   HEAL_DURATION_TICKS = 60; // 3 秒

    // 類型二：Mk3 每擊固定免傷
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

        int mana = ManaArmorItem.getMana(chest);
        if (mana <= 0) return; // 魔力耗盡 → 護盾失效

        float incoming = event.getNewDamage();
        if (incoming <= 0) return;

        ChestplateUpgradeBehavior behavior = shield.getBehavior();
        int mk = shield.getMk();

        if (behavior.isShieldReduction()) {
            float pct = behavior.getBonusForMk(mk) / 100f;
            float taken = incoming * (1f - pct);
            int cost = Math.round(taken * REDUCTION_MANA_PER_DAMAGE);
            ManaArmorItem.setMana(chest, Math.max(0, mana - cost));
            event.setNewDamage(Math.max(0f, taken));
            playBlock(player);
            if (mk >= HEAL_MK && taken > 0) {
                scheduleHeal(player, taken * HEAL_FRACTION);
            }
        } else if (behavior.isShieldAbsorb()) {
            int ratio = behavior.getBonusForMk(mk); // 魔力 / 1 點傷害
            float remaining = incoming;

            // Mk3：先免傷 20（免費，不耗魔力）
            if (mk >= ABSORB_FLAT_IMMUNE_MK) {
                remaining = Math.max(0f, remaining - ABSORB_FLAT_IMMUNE);
                if (remaining <= 0f) {
                    event.setNewDamage(0f);
                    playBlock(player);
                    return;
                }
            }

            // 用魔力擋剩餘傷害：每 1 點傷害扣 ratio 魔力
            int blockable = mana / ratio;                 // 目前魔力最多能擋幾點
            int toBlock = (int) Math.min(blockable, Math.ceil(remaining));
            if (toBlock > 0) {
                ManaArmorItem.setMana(chest, mana - toBlock * ratio);
                remaining -= toBlock;
                playBlock(player);
            } else if (mk >= ABSORB_FLAT_IMMUNE_MK) {
                playBlock(player); // 免傷 20 已生效
            }
            event.setNewDamage(Math.max(0f, remaining));
        }
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
