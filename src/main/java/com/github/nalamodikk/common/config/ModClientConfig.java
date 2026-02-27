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

    private ModClientConfig(ModConfigSpec.Builder builder) {
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
                "載入客戶端渲染設定: fullDistance={}, reducedDistance={}, reducedScale={}",
                INSTANCE.fullAnimationDistance.get(),
                INSTANCE.reducedAnimationDistance.get(),
                INSTANCE.reducedAnimationScale.get()
        );
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        KoniavacraftMod.LOGGER.info(
                "重新載入客戶端渲染設定: fullDistance={}, reducedDistance={}, reducedScale={}",
                INSTANCE.fullAnimationDistance.get(),
                INSTANCE.reducedAnimationDistance.get(),
                INSTANCE.reducedAnimationScale.get()
        );
    }
}
