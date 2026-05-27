package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.client.cinematic.VoidMirrorIntroManager;
import com.github.nalamodikk.client.screenAPI.test.UIPreviewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class ClientCommandHandler {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("koniava")
                .then(Commands.literal("ui_test")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    // 打開測試介面
                    // 必須在主執行緒執行
                    Minecraft.getInstance().execute(() -> {
                        Minecraft.getInstance().setScreen(new UIPreviewScreen());
                    });
                    return 1;
                })
                )
                .then(Commands.literal("voidmirror")
                .then(Commands.literal("intro")
                .executes(context -> {
                    // 重複觸發進場運鏡（搭配 hotswap 調手感）
                    Minecraft.getInstance().execute(VoidMirrorIntroManager::start);
                    return 1;
                })
                ))
        );
    }
}
