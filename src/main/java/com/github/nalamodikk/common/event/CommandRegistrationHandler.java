package com.github.nalamodikk.common.event;

import com.github.nalamodikk.common.command.ResearchCommand;
import com.github.nalamodikk.common.command.TestCommand;
import com.github.nalamodikk.narasystem.nara.command.NaraCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 */
@EventBusSubscriber(modid = com.github.nalamodikk.KoniavacraftMod.MOD_ID)
public class CommandRegistrationHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TestCommand.register(event.getDispatcher());
        ResearchCommand.register(event.getDispatcher());
        NaraCommand.register(event.getDispatcher());
    }
}
