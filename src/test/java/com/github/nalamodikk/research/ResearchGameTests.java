package com.github.nalamodikk.research;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import com.github.nalamodikk.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * GameTests for the Research system.
 * Verifies machine locking logic for different research states.
 */
@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class ResearchGameTests {

    private static final BlockPos MACHINE_POS = new BlockPos(1, 2, 1);
    private static final UUID PLAYER_A = UUID.randomUUID();
    private static final UUID PLAYER_B = UUID.randomUUID();

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 50)
    public static void machineLockedWithoutResearch(GameTestHelper helper) {
        helper.setBlock(MACHINE_POS, ModBlocks.MANA_GENERATOR.get().defaultBlockState());
        
        // Machine should be locked by default for a new player ID
        helper.runAtTickTime(5, () -> {
            boolean canOperate = ResearchGate.canOperate("mana_generator", helper.getLevel(), PLAYER_A);
            helper.assertTrue(!canOperate, "Machine should be locked without research");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 50)
    public static void machineUnlockedAfterResearch(GameTestHelper helper) {
        helper.setBlock(MACHINE_POS, ModBlocks.MANA_GENERATOR.get().defaultBlockState());
        
        helper.runAtTickTime(5, () -> {
            // Give player A the research
            ResearchSavedData data = ResearchSavedData.get(helper.getLevel());
            data.completeResearch(PLAYER_A, ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_generation"));
            
            boolean canOperate = ResearchGate.canOperate("mana_generator", helper.getLevel(), PLAYER_A);
            helper.assertTrue(canOperate, "Machine should be unlocked after research");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 50)
    public static void multiPlayerResearchIsolation(GameTestHelper helper) {
        helper.setBlock(MACHINE_POS, ModBlocks.MANA_GENERATOR.get().defaultBlockState());
        
        helper.runAtTickTime(5, () -> {
            ResearchSavedData data = ResearchSavedData.get(helper.getLevel());
            // Give only Player A the research
            data.completeResearch(PLAYER_A, ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_generation"));
            
            helper.assertTrue(ResearchGate.canOperate("mana_generator", helper.getLevel(), PLAYER_A), 
                    "Player A should be unlocked");
            helper.assertTrue(!ResearchGate.canOperate("mana_generator", helper.getLevel(), PLAYER_B), 
                    "Player B should remain locked");
            helper.succeed();
        });
    }
}
