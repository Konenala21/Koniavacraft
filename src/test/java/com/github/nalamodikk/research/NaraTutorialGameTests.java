package com.github.nalamodikk.research;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.narasystem.nara.hud.NaraTutorialFlow;
import com.github.nalamodikk.research.knowledge.PlayerKnowledge;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * GameTests for the Nara tutorial system.
 * Tests PlayerKnowledge tutorial state: seen/pending, NBT persistence, and multi-player isolation.
 */
@GameTestHolder(KoniavacraftMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class NaraTutorialGameTests {

    private static final String TUTORIAL_ID = NaraTutorialFlow.RESEARCH_TABLE;

    // ── Basic seen/pending logic ──────────────────────────────────────────────

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void tutorialNotSeenByDefault(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            PlayerKnowledge knowledge = new PlayerKnowledge();
            helper.assertTrue(!knowledge.hasSeenTutorial(TUTORIAL_ID),
                    "Tutorial should not be seen for a new player");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void markTutorialSeenReturnsTrueOnce(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            PlayerKnowledge knowledge = new PlayerKnowledge();
            boolean first  = knowledge.markTutorialSeen(TUTORIAL_ID);
            boolean second = knowledge.markTutorialSeen(TUTORIAL_ID);
            helper.assertTrue(first,   "First markTutorialSeen should return true");
            helper.assertTrue(!second, "Second markTutorialSeen should return false");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void addPendingIgnoredAfterSeen(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            PlayerKnowledge knowledge = new PlayerKnowledge();
            knowledge.markTutorialSeen(TUTORIAL_ID);
            knowledge.addPendingTutorial(TUTORIAL_ID);
            helper.assertTrue(knowledge.getPendingTutorials().isEmpty(),
                    "addPendingTutorial should be ignored once tutorial is already seen");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void markSeenClearsPending(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            PlayerKnowledge knowledge = new PlayerKnowledge();
            knowledge.addPendingTutorial(TUTORIAL_ID);
            helper.assertTrue(knowledge.getPendingTutorials().contains(TUTORIAL_ID),
                    "Tutorial should be in pending after addPendingTutorial");
            knowledge.markTutorialSeen(TUTORIAL_ID);
            helper.assertTrue(knowledge.getPendingTutorials().isEmpty(),
                    "Pending should be cleared after markTutorialSeen");
            helper.succeed();
        });
    }

    // ── NBT round-trip ────────────────────────────────────────────────────────

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void seenTutorialSurvivesNbtRoundTrip(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            PlayerKnowledge original = new PlayerKnowledge();
            original.markTutorialSeen(TUTORIAL_ID);

            PlayerKnowledge loaded = PlayerKnowledge.load(original.save());
            helper.assertTrue(loaded.hasSeenTutorial(TUTORIAL_ID),
                    "Seen tutorial should survive NBT save/load");
            helper.assertTrue(!loaded.markTutorialSeen(TUTORIAL_ID),
                    "markTutorialSeen after load should return false (already seen)");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void pendingTutorialSurvivesNbtRoundTrip(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            PlayerKnowledge original = new PlayerKnowledge();
            original.addPendingTutorial(TUTORIAL_ID);

            PlayerKnowledge loaded = PlayerKnowledge.load(original.save());
            helper.assertTrue(loaded.getPendingTutorials().contains(TUTORIAL_ID),
                    "Pending tutorial should survive NBT save/load");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void pendingNotRestoredIfAlreadySeen(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            // Simulate a corrupt/inconsistent save: both seen and pending contain the same id
            PlayerKnowledge original = new PlayerKnowledge();
            original.markTutorialSeen(TUTORIAL_ID);
            // Force pending into NBT by writing directly (simulates stale data)
            var tag = original.save();
            var pendingList = new net.minecraft.nbt.ListTag();
            pendingList.add(net.minecraft.nbt.StringTag.valueOf(TUTORIAL_ID));
            tag.put("PendingTutorials", pendingList);

            PlayerKnowledge loaded = PlayerKnowledge.load(tag);
            helper.assertTrue(loaded.getPendingTutorials().isEmpty(),
                    "Pending should not be restored if the tutorial is already in seenTutorials");
            helper.succeed();
        });
    }

    // ── Multi-player isolation ────────────────────────────────────────────────

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void tutorialStateIsolatedPerPlayer(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            ResearchSavedData data = ResearchSavedData.get(helper.getLevel());
            UUID playerA = UUID.randomUUID();
            UUID playerB = UUID.randomUUID();

            data.getOrCreate(playerA).markTutorialSeen(TUTORIAL_ID);

            helper.assertTrue(data.getOrCreate(playerA).hasSeenTutorial(TUTORIAL_ID),
                    "Player A should have seen the tutorial");
            helper.assertTrue(!data.getOrCreate(playerB).hasSeenTutorial(TUTORIAL_ID),
                    "Player B should NOT have seen the tutorial");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = KoniavacraftMod.MOD_ID, timeoutTicks = 40)
    public static void pendingStateIsolatedPerPlayer(GameTestHelper helper) {
        helper.runAtTickTime(2, () -> {
            ResearchSavedData data = ResearchSavedData.get(helper.getLevel());
            UUID playerA = UUID.randomUUID();
            UUID playerB = UUID.randomUUID();

            data.getOrCreate(playerA).addPendingTutorial(TUTORIAL_ID);

            helper.assertTrue(data.getOrCreate(playerA).getPendingTutorials().contains(TUTORIAL_ID),
                    "Player A should have pending tutorial");
            helper.assertTrue(data.getOrCreate(playerB).getPendingTutorials().isEmpty(),
                    "Player B's pending should be unaffected");
            helper.succeed();
        });
    }
}
