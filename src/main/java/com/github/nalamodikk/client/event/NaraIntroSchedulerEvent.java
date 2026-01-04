package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.narasystem.nara.screen.NaraIntroScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, value = Dist.CLIENT)
public class NaraIntroSchedulerEvent {
    private static int ticksRemaining = -1;

    public static void schedule(int ticks) {
        ticksRemaining = ticks;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (ticksRemaining > 0) {
            ticksRemaining--;
            if (ticksRemaining == 0) {
                Minecraft.getInstance().setScreen(new NaraIntroScreen());
                ticksRemaining = -1;

            }
        }
    }

}
