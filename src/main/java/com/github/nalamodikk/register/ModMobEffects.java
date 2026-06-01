package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.effect.BleedEffect;
import com.github.nalamodikk.common.effect.ChillEffect;
import com.github.nalamodikk.common.effect.DazeEffect;
import com.github.nalamodikk.common.effect.RootMobEffect;
import com.github.nalamodikk.common.effect.SoulburnEffect;
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

    // 定身：浮游砲控制彈用，封鎖移動
    public static final DeferredHolder<MobEffect, MobEffect> ROOT =
            MOB_EFFECTS.register("root", () ->
                    new RootMobEffect(MobEffectCategory.HARMFUL, 0x66405A));

    // 技能自訂效果 ─────────────────────────────────────────────
    // 流血：每秒無視護甲真實傷害（會打死目標）
    public static final DeferredHolder<MobEffect, MobEffect> BLEED =
            MOB_EFFECTS.register("bleed", () ->
                    new BleedEffect(MobEffectCategory.HARMFUL, 0xAA0000));

    // 靈焰：更快更重的無視護甲 DoT（暗+火 煉獄火反應）
    public static final DeferredHolder<MobEffect, MobEffect> SOULBURN =
            MOB_EFFECTS.register("soulburn", () ->
                    new SoulburnEffect(MobEffectCategory.HARMFUL, 0x5A2A8A));

    // 易傷：受到的傷害放大（標記後爆發，見 SkillStatusEffectHandler）
    public static final DeferredHolder<MobEffect, MobEffect> VULNERABLE =
            MOB_EFFECTS.register("vulnerable", () ->
                    new MobEffect(MobEffectCategory.HARMFUL, 0xE0A030) {});

    // 冰緩：縮放實際速度的緩速，連飛行怪都拖得慢（vanilla 緩速做不到）
    public static final DeferredHolder<MobEffect, MobEffect> CHILL =
            MOB_EFFECTS.register("chill", () ->
                    new ChillEffect(MobEffectCategory.HARMFUL, 0x7FC8E0));

    // 迷盲：每 tick 清掉怪的攻擊目標，讓致盲真的對怪有用（vanilla 失明對怪 AI 無效）
    public static final DeferredHolder<MobEffect, MobEffect> DAZE =
            MOB_EFFECTS.register("daze", () ->
                    new DazeEffect(MobEffectCategory.HARMFUL, 0xE8E0B0));

    public static void register(IEventBus bus) {
        MOB_EFFECTS.register(bus);
    }
}
