package com.github.nalamodikk.common.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * 技能自訂狀態效果的全域處理。
 *
 * 易傷 (VULNERABLE)：標記目標,讓它受到的傷害放大 (+20% 起,每層 +10%)。
 * 配合先掛標記再爆發的組合玩法 (例:感知標記 → 大招收尾)。
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class SkillStatusEffectHandler {

    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        MobEffectInstance vulnerable = victim.getEffect(ModMobEffects.VULNERABLE);
        if (vulnerable == null) return;

        float mult = 1.2F + 0.1F * vulnerable.getAmplifier();
        event.setNewDamage(event.getNewDamage() * mult);
    }
}
