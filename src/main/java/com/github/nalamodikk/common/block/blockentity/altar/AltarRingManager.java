package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.client.altar.AltarUpgradeAnimPacket;
import com.github.nalamodikk.narasystem.nara.hud.NaraTutorialFlow;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.research.knowledge.ResearchSavedData;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 本源聚陣的升級環（resonance ring）狀態機：偵測環是否鋪滿、把魔力方塊替換成
 * RESONANCE_RING（或還原）、推進 upgradeTier 並發送升級動畫封包 / 推進研究 / 給成就。
 * tier 狀態 field 存放在 {@link AspectAltarBlockEntity}，本類別持有 BE reference 操作。
 */
final class AltarRingManager {

    private final AspectAltarBlockEntity altar;

    AltarRingManager(AspectAltarBlockEntity altar) {
        this.altar = altar;
    }

    void refreshUpgradeTier() {
        Level level = altar.getLevel();
        if (level == null || !altar.isFormed()) return;
        BlockPos worldPosition = altar.getBlockPos();
        // Restore tier silently when the core is rebuilt at a previously-upgraded position.
        // applyRingReplace is called here (not by the comparison below) so rings become RESONANCE_RING
        // immediately without triggering the upgrade animation packet.
        if (altar.upgradeTier == 0 && level instanceof ServerLevel sl) {
            AltarTierSavedData tierData = AltarTierSavedData.get(sl);
            int saved = tierData.peekTier(worldPosition);
            if (saved > 0) {
                int actualRings = 0;
                for (List<Vec3i> ring : AltarGeometry.ALL_RINGS) {
                    if (checkRingComplete(ring)) actualRings++;
                    else break;
                }
                int toRestore = Math.min(saved, Math.min(actualRings, AltarGeometry.ALL_RINGS.size()));
                if (toRestore > 0) {
                    for (int i = 0; i < toRestore; i++) applyRingReplace(AltarGeometry.ALL_RINGS.get(i));
                    altar.upgradeTier = toRestore;
                    tierData.clearTier(worldPosition);
                    altar.setChanged();
                    // Return here so the normal newTier-comparison segment below does not fire
                    // an upgrade animation packet in the same tick. Any extra rings the player
                    // placed will be detected on the next CHECK_INTERVAL and trigger a proper
                    // player-visible upgrade animation at that point.
                    return;
                }
            }
        }
        altar.upgradeTier = Math.min(altar.upgradeTier, AltarGeometry.ALL_RINGS.size());
        int newTier = 0;
        for (List<Vec3i> ring : AltarGeometry.ALL_RINGS) {
            if (checkRingComplete(ring)) newTier++;
            else break;
        }
        if (newTier != altar.upgradeTier) {
            if (newTier > altar.upgradeTier) {
                for (int i = altar.upgradeTier; i < newTier; i++) applyRingReplace(AltarGeometry.ALL_RINGS.get(i));
                if (level instanceof ServerLevel serverLevel) {
                    ResearchSavedData researchData = newTier == 6 ? ResearchSavedData.get(serverLevel) : null;
                    AdvancementHolder tierAdv = null;
                    if (newTier >= 5) {
                        String advId = newTier == 6 ? "altar_upgrade_t6" : "altar_upgrade_t5";
                        tierAdv = serverLevel.getServer().getAdvancements()
                                .get(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, advId));
                    }
                    for (ServerPlayer sp : serverLevel.players()) {
                        if (sp.blockPosition().distSqr(worldPosition) > 64 * 64) continue;
                        boolean triggerDialogue = false;
                        if (researchData != null) {
                            var knowledge = researchData.getOrCreate(sp.getUUID());
                            if (!knowledge.hasSeenTutorial(NaraTutorialFlow.ALTAR_T6)) {
                                triggerDialogue = true;
                                knowledge.markTutorialSeen(NaraTutorialFlow.ALTAR_T6);
                                researchData.setDirty();
                            }
                        }
                        PacketDistributor.sendToPlayer(sp, new AltarUpgradeAnimPacket(worldPosition, newTier, triggerDialogue));
                        if (tierAdv != null) {
                            var prog = sp.getAdvancements().getOrStartProgress(tierAdv);
                            if (!prog.isDone()) {
                                for (String c : prog.getRemainingCriteria()) sp.getAdvancements().award(tierAdv, c);
                            }
                        }
                    }
                }
            } else {
                for (int i = newTier; i < altar.upgradeTier; i++) restoreRingBlocks(AltarGeometry.ALL_RINGS.get(i));
            }
            altar.upgradeTier = newTier;
            altar.setChanged();
            altar.syncToClient();
        }
    }

    private boolean checkRingComplete(List<Vec3i> ring) {
        Level level = altar.getLevel();
        if (level == null) return false;
        BlockPos worldPosition = altar.getBlockPos();
        for (Vec3i offset : ring) {
            BlockState s = level.getBlockState(worldPosition.offset(offset));
            // 接受 MANA_BLOCK（尚未替換）或 RESONANCE_RING（已替換）
            if (!s.is(ModBlocks.MANA_BLOCK.get()) && !s.is(ModBlocks.RESONANCE_RING.get())) return false;
        }
        return true;
    }

    private void applyRingReplace(List<Vec3i> ring) {
        Level level = altar.getLevel();
        if (level == null) return;
        BlockPos worldPosition = altar.getBlockPos();
        for (Vec3i offset : ring) {
            BlockPos p = worldPosition.offset(offset);
            if (level.getBlockState(p).is(ModBlocks.MANA_BLOCK.get())) {
                level.setBlock(p, ModBlocks.RESONANCE_RING.get().defaultBlockState(), 3);
            }
        }
    }

    void restoreRingBlocks(List<Vec3i> ring) {
        Level level = altar.getLevel();
        if (level == null) return;
        BlockPos worldPosition = altar.getBlockPos();
        for (Vec3i offset : ring) {
            BlockPos p = worldPosition.offset(offset);
            if (level.getBlockState(p).is(ModBlocks.RESONANCE_RING.get())) {
                level.setBlock(p, ModBlocks.MANA_BLOCK.get().defaultBlockState(), 3);
            }
        }
    }
}
