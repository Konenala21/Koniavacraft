package com.github.nalamodikk.client.event;


import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.item.debug.ManaDebugToolItem;
import com.github.nalamodikk.common.network.packet.server.manatool.ModeChangePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID,value = Dist.CLIENT)
public class MouseScrollEvent {

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.isCrouching()) return;

        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(heldItem.getItem() instanceof ManaDebugToolItem)) return;

        boolean forward = event.getScrollDeltaY() > 0;
        ModeChangePacket.sendToServer(forward);
        event.setCanceled(true);
    }
}
