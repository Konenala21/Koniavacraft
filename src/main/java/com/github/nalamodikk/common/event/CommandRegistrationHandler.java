package com.github.nalamodikk.common.event;

import com.github.nalamodikk.common.command.ResearchCommand;
import com.github.nalamodikk.common.command.TestCommand;
import com.github.nalamodikk.common.network.packet.client.SpaceTimeScalePacket;
import com.github.nalamodikk.narasystem.nara.command.NaraCommand;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 */
@EventBusSubscriber(modid = com.github.nalamodikk.KoniavacraftMod.MOD_ID)
public class CommandRegistrationHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TestCommand.register(event.getDispatcher());
        ResearchCommand.register(event.getDispatcher());
        NaraCommand.register(event.getDispatcher());

        event.getDispatcher().register(
            Commands.literal("koniava")
                .requires(src -> src.hasPermission(2))
                // /koniava tickrate <1-999999> — 突破 vanilla /tick rate 的 10000 上限
                .then(Commands.literal("tickrate")
                    .then(Commands.argument("rate", FloatArgumentType.floatArg(1.0f, 999999.0f))
                        .executes(ctx -> {
                            float rate = FloatArgumentType.getFloat(ctx, "rate");
                            ctx.getSource().getServer().tickRateManager().setTickRate(rate);
                            ctx.getSource().sendSuccess(
                                () -> Component.literal("Tick rate: " + rate + " tps"), true);
                            return 1;
                        })
                    )
                )
                // /koniava timescale <0-100000> — 太空行星公轉/自轉時間倍率
                .then(Commands.literal("timescale")
                    .then(Commands.argument("scale", FloatArgumentType.floatArg(0.0f, 100000.0f))
                        .executes(ctx -> {
                            float scale = FloatArgumentType.getFloat(ctx, "scale");
                            // 廣播給所有玩家（client 渲染才生效）
                            for (ServerPlayer p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                                PacketDistributor.sendToPlayer(p, new SpaceTimeScalePacket(scale));
                            }
                            ctx.getSource().sendSuccess(
                                () -> Component.literal("Space time scale: " + scale + "x"), true);
                            return 1;
                        })
                    )
                )
        );
    }
}
