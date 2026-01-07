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
    private static final java.util.Queue<ScheduledMessage> queue = new java.util.LinkedList<>();
    private static int ticksRemaining = -1;

    /**
     * 排程一條訊息。
     * @param ticks 延遲的刻數
     */
    public static void schedule(int ticks) {
        queue.add(new ScheduledMessage(ticks));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (ticksRemaining < 0 && !queue.isEmpty()) {
            ticksRemaining = queue.poll().delayTicks;
        }

        if (ticksRemaining > 0) {
            ticksRemaining--;
            if (ticksRemaining == 0) {
                Minecraft.getInstance().setScreen(new NaraIntroScreen());
                ticksRemaining = -1;
            }
        }
    }

    private record ScheduledMessage(int delayTicks) {}
}
