package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.entity.TrainingDummyEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * 把玩家對訓練假人造成的最終傷害累積進該假人的戰鬥 session。
 *
 * 用 LivingDamageEvent.Post（傷害減免後的最終值）保證累積總和與畫面上浮動傷害數字一致，
 * 浮動數字本身由 {@link DamageNumberEventHandler} 另外觸發，兩者讀同一個事件值。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class TrainingDummySessionHandler {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof TrainingDummyEntity dummy)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;

        float dmg = event.getNewDamage();
        if (dmg <= 0) return;
        dummy.recordHit(attacker, dmg);
    }
}
