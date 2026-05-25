package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, KoniavacraftMod.MOD_ID);

    // 戰鬥狀態指示效果：純 HUD 顯示，不影響遊戲
    public static final DeferredHolder<MobEffect, MobEffect> COMBAT_STATE =
            MOB_EFFECTS.register("combat_state", () ->
                    new MobEffect(MobEffectCategory.NEUTRAL, 0x4FC3F7) {});

    // 衝刺冷卻效果：純 HUD 顯示，用於魔力衝刺靴冷卻計時
    public static final DeferredHolder<MobEffect, MobEffect> SPRINT_COOLDOWN =
            MOB_EFFECTS.register("sprint_cooldown", () ->
                    new MobEffect(MobEffectCategory.NEUTRAL, 0x44AAFF) {});

    public static void register(IEventBus bus) {
        MOB_EFFECTS.register(bus);
    }
}
