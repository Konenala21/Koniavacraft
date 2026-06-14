package com.github.nalamodikk.register.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.dimension.MoonDimensionEffects;
import com.github.nalamodikk.client.dimension.SeamlessTransitionScreen;
import com.github.nalamodikk.client.dimension.SpaceDimensionEffects;
import com.github.nalamodikk.client.dimension.VoidMirrorDimensionEffects;
import com.github.nalamodikk.dimension.ModDimensions;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {
    public static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRegisterDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ModDimensions.SPACE_EFFECTS, new SpaceDimensionEffects());
        event.register(ModDimensions.VOID_MIRROR_EFFECTS, new VoidMirrorDimensionEffects());
        event.register(ModDimensions.MOON_EFFECTS, new MoonDimensionEffects());
    }

    /**
     * 飛船 OVERWORLD <-> SPACE 轉場改用透明的「載入世界」畫面 → 無載入閃屏(世界 + 太空天空直接透出來)。
     * setLevel 和 startWaitingForNewLevel 都走 DimensionTransitionScreenManager,key 在維度上(SPACE 只有
     * 飛船能到),所以兩條路都蓋到、也不影響一般傳送。
     */
    @SubscribeEvent
    public static void onRegisterTransitionScreens(RegisterDimensionTransitionScreenEvent event) {
        event.registerConditionalEffect(Level.OVERWORLD, ModDimensions.SPACE, SeamlessTransitionScreen::new);
        event.registerConditionalEffect(ModDimensions.SPACE, Level.OVERWORLD, SeamlessTransitionScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parent) -> new ConfigurationScreen(container, parent, DeveloperModeConfigSectionScreen::new)
        );

        // 在 mod 主類的 FMLClientSetupEvent 中註冊 runtime handler

        LOGGER.info("HELLO FROM CLIENT SETUP");
        LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }


}
