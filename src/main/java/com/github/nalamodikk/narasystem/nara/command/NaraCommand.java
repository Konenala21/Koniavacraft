package com.github.nalamodikk.narasystem.nara.command;

import com.github.nalamodikk.narasystem.nara.event.NaraServerEvents;
import com.github.nalamodikk.narasystem.nara.network.client.NaraStartDialoguePacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraBindRequestPacket;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class NaraCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("koniava")
                .then(Commands.literal("nara")
                        .then(Commands.literal("replay")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                        ctx.getSource().sendFailure(Component.literal("必須由玩家執行"));
                                        return 0;
                                    }
                                    PacketDistributor.sendToPlayer(player, new NaraStartDialoguePacket());
                                    ctx.getSource().sendSuccess(() -> Component.literal("重新播放娜拉對話"), false);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("test_bind")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                        ctx.getSource().sendFailure(Component.literal("必須由玩家執行"));
                                        return 0;
                                    }
                                    NaraServerEvents.grantWelcomeAdvancement(player);
                                    NaraBindRequestPacket.spawnWelcomeFireworksPublic(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal("測試：成就 + 煙火"), false);
                                    return 1;
                                })
                        )
                )
        );
    }
}
