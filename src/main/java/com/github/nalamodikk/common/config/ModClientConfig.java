package com.github.nalamodikk.common.config;

import com.github.nalamodikk.KoniavacraftMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ModClientConfig {
    public static final ModClientConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    static {
        Pair<ModClientConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(ModClientConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    public final ModConfigSpec.IntValue fullAnimationDistance;
    public final ModConfigSpec.IntValue reducedAnimationDistance;
    public final ModConfigSpec.DoubleValue reducedAnimationScale;
    public final ModConfigSpec.DoubleValue naraVoiceVolume;
    public final ModConfigSpec.DoubleValue bossMusicVolume;
    public final ModConfigSpec.BooleanValue sortButtonEnabled;
    public final ModConfigSpec.BooleanValue customTitleScreenEnabled;
    public final ModConfigSpec.ConfigValue<String> customTitleText;
    public final ModConfigSpec.BooleanValue showTitleToggleButton;
    public final ModConfigSpec.BooleanValue deferToOtherMenuMods;

    private ModClientConfig(ModConfigSpec.Builder builder) {
        builder.push("inventory");

        sortButtonEnabled = builder
                .comment("Show sort buttons in container GUIs (left-click sorts, right-click cycles mode)")
                .translation("koniava.config.inventory.sortButtonEnabled")
                .define("sortButtonEnabled", true);

        builder.pop();
        builder.push("titleScreen");

        customTitleScreenEnabled = builder
                .comment("Replace vanilla Minecraft logo on title screen with the mod's floating title")
                .translation("koniava.config.titleScreen.customTitleScreenEnabled")
                .define("customTitleScreenEnabled", true);

        customTitleText = builder
                .comment("Text shown as the floating title (replaces the vanilla Minecraft logo)")
                .translation("koniava.config.titleScreen.customTitleText")
                .define("customTitleText", "Koniavacraft");

        showTitleToggleButton = builder
                .comment("Show the modded/vanilla title toggle button in the title screen's top-right corner")
                .comment("Modpack authors who want a totally clean vanilla title can set this to false")
                .translation("koniava.config.titleScreen.showTitleToggleButton")
                .define("showTitleToggleButton", true);

        deferToOtherMenuMods = builder
                .comment("If another title-screen-customizing mod is present (FancyMenu, CustomMainMenu, BetterTitleScreen, TitleTweaks)")
                .comment("automatically defer to them and skip our custom title. Set to false to force ours.")
                .translation("koniava.config.titleScreen.deferToOtherMenuMods")
                .define("deferToOtherMenuMods", true);

        builder.pop();
        builder.push("nara");

        naraVoiceVolume = builder
                .comment("Volume for Nara's voice dialogue (0.0 = mute, 1.0 = 100%)")
                .translation("koniava.config.nara.voiceVolume")
                .defineInRange("naraVoiceVolume", 1.0D, 0.0D, 1.0D);

        builder.pop();
        builder.push("bossMusic");

        bossMusicVolume = builder
                .comment("Volume for boss battle BGM (Mirror Image and future boss music) (0.0 = mute, 1.0 = 100%)")
                .translation("koniava.config.boss.musicVolume")
                .defineInRange("bossMusicVolume", 1.0D, 0.0D, 1.0D);

        builder.pop();
        builder.push("render");

        fullAnimationDistance = builder
                .comment("Block entity render animation: full-detail distance in blocks")
                .translation("koniava.config.render.fullAnimationDistance")
                .defineInRange("fullAnimationDistance", 24, 1, 256);

        reducedAnimationDistance = builder
                .comment("Block entity render animation: reduced-detail distance in blocks")
                .translation("koniava.config.render.reducedAnimationDistance")
                .defineInRange("reducedAnimationDistance", 48, 1, 512);

        reducedAnimationScale = builder
                .comment("Block entity render animation: animation speed/amplitude scale at reduced distance")
                .translation("koniava.config.render.reducedAnimationScale")
                .defineInRange("reducedAnimationScale", 0.35D, 0.0D, 1.0D);

        builder.pop();
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        KoniavacraftMod.LOGGER.info(
                "載入客戶端設定: fullDistance={}, reducedDistance={}, reducedScale={}, naraVoiceVolume={}, sortButton={}",
                INSTANCE.fullAnimationDistance.get(),
                INSTANCE.reducedAnimationDistance.get(),
                INSTANCE.reducedAnimationScale.get(),
                INSTANCE.naraVoiceVolume.get(),
                INSTANCE.sortButtonEnabled.get()
        );
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        KoniavacraftMod.LOGGER.info(
                "重新載入客戶端設定: fullDistance={}, reducedDistance={}, reducedScale={}, naraVoiceVolume={}, sortButton={}",
                INSTANCE.fullAnimationDistance.get(),
                INSTANCE.reducedAnimationDistance.get(),
                INSTANCE.reducedAnimationScale.get(),
                INSTANCE.naraVoiceVolume.get(),
                INSTANCE.sortButtonEnabled.get()
        );
    }
}
