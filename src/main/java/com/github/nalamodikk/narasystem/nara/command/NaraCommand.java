package com.github.nalamodikk.narasystem.nara.command;

import com.github.nalamodikk.narasystem.nara.event.NaraServerEvents;
import com.github.nalamodikk.narasystem.nara.hud.NaraTutorialFlow;
import com.github.nalamodikk.narasystem.nara.network.client.NaraStartDialoguePacket;
import com.github.nalamodikk.narasystem.nara.network.client.NaraTutorialPacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraBindRequestPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;

public class NaraCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("koniava")
                .then(Commands.literal("nara")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("replay")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                        ctx.getSource().sendFailure(Component.translatable("command.koniava.nara.player_only"));
                                        return 0;
                                    }
                                    PacketDistributor.sendToPlayer(player, new NaraStartDialoguePacket());
                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.koniava.nara.replay_success"), false);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("test_bind")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                        ctx.getSource().sendFailure(Component.translatable("command.koniava.nara.player_only"));
                                        return 0;
                                    }
                                    NaraServerEvents.grantWelcomeAdvancement(player);
                                    NaraBindRequestPacket.spawnWelcomeFireworksPublic(player);
                                    ctx.getSource().sendSuccess(() -> Component.translatable("command.koniava.nara.test_bind_success"), false);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("tutorial")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (String id : TUTORIAL_IDS) builder.suggest(id);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> triggerTutorial(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "id")))
                                )
                        )
                        .then(Commands.literal("detect")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
                                        ctx.getSource().sendFailure(Component.translatable("command.koniava.nara.player_only"));
                                        return 0;
                                    }
                                    NaraServerEvents.restorePendingTutorials(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Nara tutorial detection run for " + player.getName().getString()), false);
                                    return 1;
                                })
                        )
                )
        );
    }

    private static int triggerTutorial(CommandSourceStack source, String id) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.koniava.nara.player_only"));
            return 0;
        }
        NaraServerEvents.scheduleTestTutorial(player, id);
        source.sendSuccess(() -> Component.translatable("command.koniava.nara.tutorial_success", id), false);
        return 1;
    }

    private static final String[] TUTORIAL_IDS = Arrays.stream(NaraTutorialFlow.class.getDeclaredFields())
            .filter(f -> f.getType() == String.class
                    && Modifier.isStatic(f.getModifiers())
                    && Modifier.isFinal(f.getModifiers()))
            .map(f -> { try { return (String) f.get(null); } catch (Exception e) { return null; } })
            .filter(Objects::nonNull)
            .toArray(String[]::new);
}
