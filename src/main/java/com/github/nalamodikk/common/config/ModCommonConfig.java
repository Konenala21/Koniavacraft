package com.github.nalamodikk.common.config;

import com.github.nalamodikk.KoniavacraftMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ModCommonConfig {

    // 持有實例與規格
    public static final ModCommonConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    static {
        // 使用官方推薦的 configure 方法建立
        Pair<ModCommonConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ModCommonConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    // ===============================
    // 🎯 原有設定值
    // ===============================
    public final ModConfigSpec.IntValue manaRecipeRefreshInterval;
    public final ModConfigSpec.BooleanValue showIntroAnimation;
    public final ModConfigSpec.IntValue manaGeneratorSignificantManaChange;
    public final ModConfigSpec.IntValue manaGeneratorSignificantEnergyChange;
    public final ModConfigSpec.IntValue calculateThreadCount;
    public final ModConfigSpec.BooleanValue developerModeEnabled;
    public final ModConfigSpec.BooleanValue autoEnableDeveloperModeInDevEnvironment;
    // 鏡中世界 boss 行為開關（預設全開，玩家覺得太難或不想看演出可關）
    public final ModConfigSpec.BooleanValue bossTurretVolleyEnabled;
    public final ModConfigSpec.BooleanValue bossHotbarSwitchEnabled;
    public final ModConfigSpec.BooleanValue bossShieldBlockEnabled;
    public final ModConfigSpec.BooleanValue phase2CinematicEnabled;



    private ModCommonConfig(ModConfigSpec.Builder builder) {
        // ===============================
        // 🎯 原有配置項目
        // ===============================
        calculateThreadCount = builder
                .comment("粒子運算線程池大小（建議：CPU核心數 - 1）")
                .comment("Thread pool size for particle calculation (Recommended: CPU cores - 1)")
                .translation("koniava.config.calculateThreadCount")
                .defineInRange("calculateThreadCount", 4, 1, 32);

        manaRecipeRefreshInterval = builder
                .comment("每幾 tick 更新一次魔力合成配方結果（建議值：2～10）")
                .comment("How many ticks to refresh the mana crafting recipe result (Recommended value: 2-10)")
                .translation("koniava.config.manaRecipeRefreshInterval")
                .defineInRange("manaRecipeRefreshInterval", 2, 1, 40);

        showIntroAnimation = builder
                .comment("是否啟用登入動畫（預設開啟）")
                .comment("Enable intro animation on player login (default: true)")
                .translation("koniava.config.showIntroAnimation")
                .define("showIntroAnimation", true);

        manaGeneratorSignificantManaChange = builder
                .comment("魔力發電機：同步所需的魔力顯著變化量")
                .comment("Mana generator: significant mana delta to trigger sync")
                .translation("koniava.config.manaGeneratorSignificantManaChange")
                .defineInRange("manaGeneratorSignificantManaChange", 100, 0, 100000);

        manaGeneratorSignificantEnergyChange = builder
                .comment("魔力發電機：同步所需的能量顯著變化量")
                .comment("Mana generator: significant energy delta to trigger sync")
                .translation("koniava.config.manaGeneratorSignificantEnergyChange")
                .defineInRange("manaGeneratorSignificantEnergyChange", 100, 0, 100000);

        developerModeEnabled = builder
                .comment("開發者模式手動開關（預設關閉）")
                .comment("Manual developer mode switch (default: false)")
                .translation("koniava.config.developerModeEnabled")
                .define("developerModeEnabled", false);

        autoEnableDeveloperModeInDevEnvironment = builder
                .comment("在開發環境自動啟用開發者模式（正式環境不生效）")
                .comment("Auto-enable developer mode in development environment only")
                .translation("koniava.config.autoEnableDeveloperModeInDevEnvironment")
                .define("autoEnableDeveloperModeInDevEnvironment", true);

        // ===============================
        // ⚔️ 鏡中世界 boss 行為開關
        // ===============================
        builder.push("voidMirrorBoss");

        bossTurretVolleyEnabled = builder
                .comment("Enable the boss's active turret volley skill (4 turrets fire charged shots together every 10s after a 1s telegraph)")
                .translation("koniava.config.boss.turretVolleyEnabled")
                .define("turretVolleyEnabled", true);

        bossHotbarSwitchEnabled = builder
                .comment("Enable the boss switching its main-hand weapon mid-combat from its mirrored hotbar (every 4s)")
                .translation("koniava.config.boss.hotbarSwitchEnabled")
                .define("hotbarSwitchEnabled", true);

        bossShieldBlockEnabled = builder
                .comment("Enable the boss blocking frontal attacks with a shield in its offhand (60-degree cone, consumes durability)")
                .translation("koniava.config.boss.shieldBlockEnabled")
                .define("shieldBlockEnabled", true);

        phase2CinematicEnabled = builder
                .comment("Enable the phase 2 transformation cinematic (camera lock, shell pieces flying in over 11s). If disabled, the mecha appears instantly.")
                .translation("koniava.config.boss.phase2CinematicEnabled")
                .define("phase2CinematicEnabled", true);

        builder.pop();
    }


    // ===============================
    // 🎧 配置事件處理器
    // ===============================

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            // 原有配置日誌
            KoniavacraftMod.LOGGER.info("Loaded mana settings: manaRecipeRefreshInterval={}",
                    INSTANCE.manaRecipeRefreshInterval.get());
            KoniavacraftMod.LOGGER.info("Loaded animation settings: showIntroAnimation={}",
                    INSTANCE.showIntroAnimation.get());
            KoniavacraftMod.LOGGER.info("Loaded mana generator sync settings: manaChange={}, energyChange={}",
                    INSTANCE.manaGeneratorSignificantManaChange.get(),
                    INSTANCE.manaGeneratorSignificantEnergyChange.get());
            KoniavacraftMod.LOGGER.info("Loaded developer mode settings: enabled={}, autoInDev={}, active={}",
                    INSTANCE.developerModeEnabled.get(),
                    INSTANCE.autoEnableDeveloperModeInDevEnvironment.get(),
                    isDeveloperModeActive());


        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            // 原有配置日誌
            KoniavacraftMod.LOGGER.info("Reloaded mana settings: manaRecipeRefreshInterval={}",
                    INSTANCE.manaRecipeRefreshInterval.get());
            KoniavacraftMod.LOGGER.info("Reloaded animation settings: showIntroAnimation={}",
                    INSTANCE.showIntroAnimation.get());
            KoniavacraftMod.LOGGER.info("Reloaded mana generator sync settings: manaChange={}, energyChange={}",
                    INSTANCE.manaGeneratorSignificantManaChange.get(),
                    INSTANCE.manaGeneratorSignificantEnergyChange.get());
            KoniavacraftMod.LOGGER.info("Reloaded developer mode settings: enabled={}, autoInDev={}, active={}",
                    INSTANCE.developerModeEnabled.get(),
                    INSTANCE.autoEnableDeveloperModeInDevEnvironment.get(),
                    isDeveloperModeActive());

            KoniavacraftMod.LOGGER.info("Reloaded biome processing settings.");
        }
    }

    // ===============================
    // 🛠️ 配置工具方法
    // ===============================

    /**
     * 📋 獲取當前配置摘要
     */


    /**
     * 🚀 一鍵性能模式設定
     */
    public static class PerformancePresets {

        public static void applyLowEndServer() {
            KoniavacraftMod.LOGGER.info("Applying low-end server preset.");
            // 這個功能需要配合配置重載機制，這裡只是示例
        }

        public static void applyHighEndServer() {
            KoniavacraftMod.LOGGER.info("Applying high-end server preset.");
            // 這個功能需要配合配置重載機制，這裡只是示例
        }

        public static void applyBalanced() {
            KoniavacraftMod.LOGGER.info("Applying balanced preset.");
            // 這個功能需要配合配置重載機制，這裡只是示例
        }
    }

    public static boolean isDeveloperModeActive() {
        boolean autoInDev = INSTANCE.autoEnableDeveloperModeInDevEnvironment.get() && KoniavacraftMod.IS_DEV;
        boolean manualEnabled = INSTANCE.developerModeEnabled.get();
        return autoInDev || manualEnabled;
    }
}
