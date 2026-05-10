package com.github.nalamodikk.register.client;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.NaraWatchItem;
import com.github.nalamodikk.register.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {
    public static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(
                    PoseStack poseStack, LocalPlayer player,
                    HumanoidArm arm, ItemStack itemInHand,
                    float partialTick, float equipProcess, float swingProcess) {
                float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
                // Flat held like a map/watch: angled toward the player
                poseStack.translate(side * 0.35f, -0.35f, 0.15f);
                poseStack.mulPose(Axis.XP.rotationDegrees(25f));
                poseStack.mulPose(Axis.YP.rotationDegrees(-side * 10f));
                return true;
            }
        }, ModItems.NARA_WATCH.get());
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parent) -> new ConfigurationScreen(container, parent, DeveloperModeConfigSectionScreen::new)
        );

        // 在 mod 主類的 FMLClientSetupEvent 中註冊 runtime handler

        // Some client setup code

        LOGGER.info("HELLO FROM CLIENT SETUP");
        LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }


}
