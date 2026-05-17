package com.github.nalamodikk.common.command;

import com.github.nalamodikk.research.aspect.Aspect;
import com.github.nalamodikk.research.aspect.ModAspects;
import com.github.nalamodikk.research.knowledge.PlayerKnowledge;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.research.network.KnowledgeSyncPacket;
import com.github.nalamodikk.research.template.ResearchRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResearchCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("koniava")
                        .then(Commands.literal("research")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("unlock")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                                        .suggests((context, builder) -> {
                                                            suggestResearchIds(builder);
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(ResearchCommand::unlockResearch))))
                                .then(Commands.literal("lock")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                                        .suggests((context, builder) -> {
                                                            suggestResearchIds(builder);
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(ResearchCommand::lockResearch))))
                                .then(Commands.literal("set_state")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("id", ResourceLocationArgument.id())
                                                        .suggests((context, builder) -> {
                                                            suggestResearchIds(builder);
                                                            return builder.buildFuture();
                                                        })
                                                        .then(Commands.argument("state", StringArgumentType.word())
                                                                .suggests((context, builder) -> {
                                                                    builder.suggest("locked");
                                                                    builder.suggest("forced_available");
                                                                    builder.suggest("available");
                                                                    builder.suggest("completed");
                                                                    return builder.buildFuture();
                                                                })
                                                                .executes(ResearchCommand::setResearchState)))))
                                .then(Commands.literal("set_tier")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("tier", IntegerArgumentType.integer(1, 12))
                                                        .executes(ResearchCommand::setTier))))
                                .then(Commands.literal("set_aspect")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("aspect", ResourceLocationArgument.id())
                                                        .suggests((context, builder) -> {
                                                            for (Aspect aspect : ModAspects.all()) {
                                                                builder.suggest(aspect.getId().toString());
                                                            }
                                                            return builder.buildFuture();
                                                        })
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                                .executes(ResearchCommand::setAspect)))))
                                .then(Commands.literal("add_aspect")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("aspect", ResourceLocationArgument.id())
                                                        .suggests((context, builder) -> {
                                                            for (Aspect aspect : ModAspects.all()) {
                                                                builder.suggest(aspect.getId().toString());
                                                            }
                                                            return builder.buildFuture();
                                                        })
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                                .executes(ResearchCommand::addAspect)))))
                                .then(Commands.literal("add_all_aspects")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(context -> addAllAspects(context, 1))
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(context -> addAllAspects(context, IntegerArgumentType.getInteger(context, "amount"))))))
                                .then(Commands.literal("reset_non_primary_aspects")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ResearchCommand::resetNonPrimaryAspects)))
                                .then(Commands.literal("list_aspects")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ResearchCommand::listAspects)))
                                .then(Commands.literal("unlock_all")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ResearchCommand::unlockAllResearch)))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ResearchCommand::clearResearch)))));
    }

    private static int unlockResearch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        ResourceLocation id = ResourceLocationArgument.getId(context, "id");
        Component idText = Component.literal(id.toString());
        if (!researchExists(context, id)) {
            return 0;
        }

        int changed = 0;
        for (ServerPlayer player : players) {
            PlayerKnowledge knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
            if (knowledge.completeResearch(id)) {
                syncKnowledge(player);
                ResearchRegistry.get(id).ifPresent(t -> {
                    List<RecipeHolder<?>> toAward = player.serverLevel().getRecipeManager().getRecipes()
                            .stream().filter(h -> t.getUnlockedRecipes().contains(h.id())).toList();
                    if (!toAward.isEmpty()) player.awardRecipes(toAward);
                });
                changed++;
            }
        }

        int finalChanged = changed;
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.koniava.research.unlock.success", idText, finalChanged),
                true);
        return changed;
    }

    private static int lockResearch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        ResourceLocation id = ResourceLocationArgument.getId(context, "id");
        Component idText = Component.literal(id.toString());
        if (!researchExists(context, id)) {
            return 0;
        }

        for (ServerPlayer player : players) {
            PlayerKnowledge knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
            knowledge.setResearchState(id, PlayerKnowledge.ResearchState.LOCKED);
            syncKnowledge(player);
        }

        int playerCount = players.size();
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.koniava.research.lock.success", idText, playerCount),
                true);
        return playerCount;
    }

    private static int setResearchState(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        ResourceLocation id = ResourceLocationArgument.getId(context, "id");
        Component idText = Component.literal(id.toString());
        if (!researchExists(context, id)) {
            return 0;
        }

        PlayerKnowledge.ResearchState state = parseState(context, StringArgumentType.getString(context, "state"));
        if (state == null) {
            return 0;
        }

        for (ServerPlayer player : players) {
            PlayerKnowledge knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
            knowledge.setResearchState(id, state);
            syncKnowledge(player);
        }

        int playerCount = players.size();
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.koniava.research.set_state.success",
                        idText, state.name().toLowerCase(), playerCount),
                true);
        return playerCount;
    }

    private static int setTier(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        int tier = IntegerArgumentType.getInteger(context, "tier");

        for (ServerPlayer player : players) {
            PlayerKnowledge knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
            knowledge.setTier(tier);
            syncKnowledge(player);
        }

        int playerCount = players.size();
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.koniava.research.set_tier.success", tier, playerCount),
                true);
        return playerCount;
    }

    private static int setAspect(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        ResourceLocation aspectId = ResourceLocationArgument.getId(context, "aspect");
        Component aspectText = Component.literal(aspectId.toString());
        int amount = IntegerArgumentType.getInteger(context, "amount");

        for (ServerPlayer player : players) {
            PlayerKnowledge knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
            knowledge.setAspectAmount(aspectId, amount);
            syncKnowledge(player);
        }

        int playerCount = players.size();
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.koniava.research.set_aspect.success",
                        aspectText, amount, playerCount),
                true);
        return playerCount;
    }

    private static int addAspect(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        ResourceLocation aspectId = ResourceLocationArgument.getId(context, "aspect");
        Component aspectText = Component.literal(aspectId.toString());
        int amount = IntegerArgumentType.getInteger(context, "amount");
        Aspect aspect = ModAspects.get(aspectId);

        if (aspect == null) {
            context.getSource().sendFailure(Component.translatable("commands.koniava.research.aspect.unknown", aspectText));
            return 0;
        }

        for (ServerPlayer player : players) {
            PlayerKnowledge knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
            knowledge.discoverAspect(aspect, amount);
            syncKnowledge(player);
        }

        int playerCount = players.size();
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.koniava.research.add_aspect.success",
                        aspectText, amount, playerCount),
                true);
        return playerCount;
    }

    private static int addAllAspects(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        int aspectCount = ModAspects.all().size();

        for (ServerPlayer player : players) {
            PlayerKnowledge knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
            for (Aspect aspect : ModAspects.all()) {
                knowledge.discoverAspect(aspect, amount);
            }
            syncKnowledge(player);
        }

        int playerCount = players.size();
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.koniava.research.add_all_aspects.success",
                        amount, aspectCount, playerCount),
                true);
        return playerCount * aspectCount;
    }

    private static int resetNonPrimaryAspects(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        int resetCount = 0;

        for (ServerPlayer player : players) {
            PlayerKnowledge knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
            resetCount += knowledge.clearNonPrimaryAspects();
            restorePrimaryAspectAmounts(knowledge);
            syncKnowledge(player);
        }

        final int resetAspectCount = resetCount;
        final int playerCount = players.size();
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.koniava.research.reset_non_primary_aspects.success",
                        resetAspectCount, playerCount),
                true);
        return resetAspectCount;
    }

    private static void restorePrimaryAspectAmounts(PlayerKnowledge knowledge) {
        for (Aspect aspect : ModAspects.all()) {
            if (!aspect.isPrimary()) {
                continue;
            }

            int currentAmount = knowledge.getAspectCount(aspect.getId());
            int delta = 5 - currentAmount;
            if (delta != 0) {
                knowledge.discoverAspect(aspect, delta);
            }
        }
    }

    private static int listAspects(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "target");
        PlayerKnowledge knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
        Map<ResourceLocation, Integer> aspects = knowledge.getDiscoveredAspectsMap();

        context.getSource().sendSuccess(
                () -> Component.translatable("commands.koniava.research.list_aspects.header", player.getName()),
                false);

        for (Map.Entry<ResourceLocation, Integer> entry : aspects.entrySet()) {
            Aspect aspect = ModAspects.get(entry.getKey());
            Component name = aspect != null
                    ? aspect.getName()
                    : Component.translatable("commands.koniava.research.aspect.missing", Component.literal(entry.getKey().toString()));
            context.getSource().sendSuccess(
                    () -> Component.translatable("commands.koniava.research.list_aspects.entry",
                            name, entry.getValue()).withStyle(ChatFormatting.AQUA),
                    false);
        }

        return aspects.size();
    }

    private static int unlockAllResearch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        int total = 0;

        for (ServerPlayer player : players) {
            PlayerKnowledge knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
            for (var research : ResearchRegistry.all()) {
                knowledge.completeResearch(research.getId());
                total++;
            }
            syncKnowledge(player);
            Set<ResourceLocation> allRecipeIds = new HashSet<>();
            for (var r : ResearchRegistry.all()) allRecipeIds.addAll(r.getUnlockedRecipes());
            if (!allRecipeIds.isEmpty()) {
                List<RecipeHolder<?>> toAward = player.serverLevel().getRecipeManager().getRecipes()
                        .stream().filter(h -> allRecipeIds.contains(h.id())).toList();
                if (!toAward.isEmpty()) player.awardRecipes(toAward);
            }
        }

        int researchCount = ResearchRegistry.all().size();
        int playerCount = players.size();
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.koniava.research.unlock_all.success", researchCount, playerCount),
                true);
        return total;
    }

    private static int clearResearch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");

        for (ServerPlayer player : players) {
            PlayerKnowledge knowledge = ResearchSavedData.get(player.serverLevel()).getOrCreate(player.getUUID());
            knowledge.clearProgress();
            syncKnowledge(player);
        }

        int playerCount = players.size();
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.koniava.research.clear.success", playerCount),
                true);
        return playerCount;
    }

    private static void syncKnowledge(ServerPlayer player) {
        ResearchSavedData.get(player.serverLevel()).setDirty();
        KnowledgeSyncPacket.sendTo(player);
    }

    private static SuggestionsBuilder suggestResearchIds(SuggestionsBuilder builder) {
        for (var research : ResearchRegistry.all()) {
            builder.suggest(research.getId().toString());
        }
        return builder;
    }

    private static boolean researchExists(CommandContext<CommandSourceStack> context, ResourceLocation id) {
        if (ResearchRegistry.get(id).isPresent()) {
            return true;
        }
        context.getSource().sendFailure(Component.translatable("commands.koniava.research.unknown", Component.literal(id.toString())));
        return false;
    }

    private static PlayerKnowledge.ResearchState parseState(CommandContext<CommandSourceStack> context, String value) {
        return switch (value.toLowerCase()) {
            case "locked", "lock", "unlocked_false" -> PlayerKnowledge.ResearchState.LOCKED;
            case "available", "forced_available", "unlockable" -> PlayerKnowledge.ResearchState.FORCED_AVAILABLE;
            case "completed", "complete", "done" -> PlayerKnowledge.ResearchState.COMPLETED;
            default -> {
                context.getSource().sendFailure(Component.translatable("commands.koniava.research.state.unknown", value));
                yield null;
            }
        };
    }
}
