package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.client.turret.DamageNumberPacket;
import com.github.nalamodikk.register.ModDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class DamageNumberEventHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        // Only care about player-caused damage
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;

        float dmg = event.getNewDamage();
        if (dmg <= 0) return;

        byte dmgType;
        if (event.getSource().is(ModDamageTypes.FLOATING_TURRET)) {
            dmgType = DamageNumberPacket.MAGIC;
        } else {
            dmgType = isCriticalHit(attacker) ? DamageNumberPacket.CRIT : DamageNumberPacket.NORMAL;
        }

        double x = event.getEntity().getX();
        double y = event.getEntity().getY() + event.getEntity().getBbHeight() + 0.2;
        double z = event.getEntity().getZ();

        PacketDistributor.sendToPlayer(attacker,
                new DamageNumberPacket(x, y, z, dmg, dmgType, event.getEntity().getId()));
    }

    private static boolean isCriticalHit(Player player) {
        return player.fallDistance > 0.0F
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.hasEffect(MobEffects.BLINDNESS)
                && !player.isPassenger();
    }
}
