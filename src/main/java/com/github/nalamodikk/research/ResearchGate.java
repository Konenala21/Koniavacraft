package com.github.nalamodikk.research;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps machine IDs to their required research.
 * Call {@link #canUse} in machine blocks before opening the GUI.
 */
public final class ResearchGate {

    private static final Map<String, ResourceLocation> REQUIREMENTS = new HashMap<>();

    static {
        require("mana_generator",      "mana_generation");
        require("mana_grinder",        "mana_crystallisation");
        require("mana_crafting_table", "mana_crystallisation");
        require("basic_arcane_conduit","mana_flow");
        require("advanced_arcane_conduit", "mana_flow");
        require("elite_arcane_conduit","mana_flow");
        require("solar_mana_collector","mana_generation");
    }

    private static void require(String machineId, String researchPath) {
        REQUIREMENTS.put(machineId,
                ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, researchPath));
    }

    /**
     * Returns true if the player may interact with this machine.
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
            ResearchSavedData data = ResearchSavedData.get(serverLevel);
            if (!data.getOrCreate(serverPlayer.getUUID()).hasCompleted(required)) {
                serverPlayer.sendSystemMessage(Component.translatable(
                        "research.koniava.locked",
                        Component.translatable("research." + required.getNamespace() + "." + required.getPath())));
                return false;
            }
        }
        return true;
    }

    private ResearchGate() {}
}
