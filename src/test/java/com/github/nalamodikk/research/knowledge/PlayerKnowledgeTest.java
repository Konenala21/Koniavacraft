package com.github.nalamodikk.research.knowledge;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.research.aspect.ModAspects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerKnowledgeTest {

    private static final ResourceLocation RESEARCH_ID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mana_generation");
    private static final ResourceLocation ITEM_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "furnace_minecart");

    @Test
    void researchStateTracksLockedAvailableAndCompleted() {
        PlayerKnowledge knowledge = new PlayerKnowledge();

        assertEquals(PlayerKnowledge.ResearchState.LOCKED, knowledge.getResearchState(RESEARCH_ID));

        knowledge.setResearchState(RESEARCH_ID, PlayerKnowledge.ResearchState.FORCED_AVAILABLE);
        assertEquals(PlayerKnowledge.ResearchState.FORCED_AVAILABLE, knowledge.getResearchState(RESEARCH_ID));
        assertTrue(knowledge.isResearchForcedAvailable(RESEARCH_ID));
        assertFalse(knowledge.hasCompleted(RESEARCH_ID));

        knowledge.setResearchState(RESEARCH_ID, PlayerKnowledge.ResearchState.COMPLETED);
        assertEquals(PlayerKnowledge.ResearchState.COMPLETED, knowledge.getResearchState(RESEARCH_ID));
        assertFalse(knowledge.isResearchForcedAvailable(RESEARCH_ID));
        assertTrue(knowledge.hasCompleted(RESEARCH_ID));

        knowledge.setResearchState(RESEARCH_ID, PlayerKnowledge.ResearchState.LOCKED);
        assertEquals(PlayerKnowledge.ResearchState.LOCKED, knowledge.getResearchState(RESEARCH_ID));
        assertFalse(knowledge.isResearchForcedAvailable(RESEARCH_ID));
        assertFalse(knowledge.hasCompleted(RESEARCH_ID));
    }

    @Test
    void saveLoadPreservesAspectQuantitiesScansAndAvailableState() {
        PlayerKnowledge knowledge = new PlayerKnowledge();
        knowledge.discoverAspect(ModAspects.MANA, 4);
        knowledge.recordItemScan(ITEM_ID, java.util.List.of());
        knowledge.setResearchState(RESEARCH_ID, PlayerKnowledge.ResearchState.FORCED_AVAILABLE);
        knowledge.setTier(3);

        CompoundTag saved = knowledge.save();
        PlayerKnowledge loaded = PlayerKnowledge.load(saved);

        assertEquals(4, loaded.getAspectCount(ModAspects.MANA.getId()));
        assertTrue(loaded.getScannedTargets().containsKey(ITEM_ID));
        assertEquals(PlayerKnowledge.ResearchState.FORCED_AVAILABLE, loaded.getResearchState(RESEARCH_ID));
        assertEquals(3, loaded.getCurrentTier());
    }
}
