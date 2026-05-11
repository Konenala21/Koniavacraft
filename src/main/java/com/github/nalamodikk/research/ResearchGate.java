package com.github.nalamodikk.research;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maps machine IDs to their required research.
 * GUI should remain accessible; call {@link #canOperate} before recipes, ticking, or transfers.
 */
public final class ResearchGate {

    private static final Map<String, ResourceLocation> REQUIREMENTS = new HashMap<>();

    static {
        require("mana_generator",      "mana_generation");
        require("mana_grinder",        "mana_crystallisation");
        require("mana_crafting_table", "mana_crystallisation");
        require("mana_infuser",        "mana_infusion");
        require("basic_arcane_conduit","mana_flow");
        require("advanced_arcane_conduit", "advanced_conduits");
        require("elite_arcane_conduit","advanced_conduits");
        require("solar_mana_collector","mana_generation");
    }

    private static void require(String machineId, String researchPath) {
        REQUIREMENTS.put(machineId,
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, researchPath));
    }

    /**
     * Returns true if the player may perform a machine action.
     * If blocked, sends a chat hint and returns false.
     *
     * @param machineId  registry path of the machine block (e.g. "mana_generator")
     * @param player     the interacting player
     * @param level      current level
     */
    public static boolean canUse(String machineId, Player player, Level level) {
        ResourceLocation required = REQUIREMENTS.get(machineId);
        if (required == null) return true; // no requirement registered

        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            if (!hasCompleted(serverLevel, serverPlayer.getUUID(), required)) {
                sendLockedMessage(serverPlayer, required);
                return false;
            }
        }
        return true;
    }

    public static boolean hasResearch(String machineId, Player player, Level level) {
        ResourceLocation required = REQUIREMENTS.get(machineId);
        if (required == null) return true;
        if (!(level instanceof ServerLevel serverLevel)) return true;

        return hasCompleted(serverLevel, player.getUUID(), required);
    }

    public static boolean canOperate(String machineId, Level level, @Nullable UUID ownerId) {
        ResourceLocation required = REQUIREMENTS.get(machineId);
        if (required == null) return true;
        if (!(level instanceof ServerLevel serverLevel)) return true;
        if (ownerId == null) return true;

        return hasCompleted(serverLevel, ownerId, required);
    }

    public static @Nullable ResourceLocation getRequiredResearch(String machineId) {
        return REQUIREMENTS.get(machineId);
    }

    /**
     * Checks if the machine is unlocked for the local player on the client.
     */
    public static boolean isUnlockedOnClient(String machineId) {
        ResourceLocation required = REQUIREMENTS.get(machineId);
        if (required == null) return true;
        return com.github.nalamodikk.research.client.ClientResearchCache.hasCompleted(required);
    }

    private static boolean hasCompleted(ServerLevel serverLevel, UUID playerId, ResourceLocation required) {
        ResearchSavedData data = ResearchSavedData.get(serverLevel);
        return data.getOrCreate(playerId).hasCompleted(required);
    }

    private static void sendLockedMessage(ServerPlayer player, ResourceLocation required) {
        player.sendSystemMessage(Component.translatable(
                "research.koniava.locked",
                Component.translatable("research." + required.getNamespace() + "." + required.getPath())));
    }

    private ResearchGate() {}
}
