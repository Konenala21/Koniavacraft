package com.github.nalamodikk.narasystem.nara.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlock;
import com.github.nalamodikk.common.block.blockentity.conduit.ArcaneConduitBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.github.nalamodikk.research.ResearchGate;

import java.util.UUID;

/**
 * 娜拉系統 server-side 事件樞紐（facade）。
 * 所有 state 與邏輯都放在三個 manager：
 * <ul>
 *     <li>{@link NaraTutorialManager} 教程派發主力（schedule* / item crafted / block placed / boots equip / 大部分 tick 邏輯 / Warden cleanup）</li>
 *     <li>{@link NaraPunishmentManager} 懲罰對話倒數 + death/respawn</li>
 *     <li>{@link NaraGhostTutorialManager} dev /koniava nara tutorial 的 ghost block 預覽</li>
 * </ul>
 *
 * <p>本類只負責三件事：
 * <ol>
 *     <li>掛 @SubscribeEvent → 委派給對應 manager</li>
 *     <li>保留所有外部呼叫的 public static API 簽名（callers 不用改）</li>
 *     <li>跨 manager 共用的小工具（grantAdvancement 系列）</li>
 * </ol>
 */
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class NaraServerEvents {

    // ── Event subscribers（委派層） ────────────────────────────────────────

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        NaraPunishmentManager.clearAll();
        NaraTutorialManager.clearAll();
        NaraGhostTutorialManager.clearAll(event.getServer());
        ArcaneConduitBlockEntity.clearAllStaticCachesGracefully();
        ArcaneConduitBlock.clearStaticCaches();
        ResearchGate.clearCache();
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        NaraPunishmentManager.clearForPlayer(uuid);
        NaraTutorialManager.clearForPlayer(uuid);
        NaraGhostTutorialManager.clearForPlayer(player);
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        NaraTutorialManager.handleItemCrafted(sp, event.getCrafting());
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        NaraTutorialManager.handleBlockPlaced(player, level, event.getPos(), event.getPlacedBlock());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        NaraTutorialManager.tick(event);
        NaraGhostTutorialManager.tick(event);
        NaraPunishmentManager.tick(event);
        NaraTutorialManager.tickWardenDespawn(event);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        NaraPunishmentManager.handleDeath(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        NaraPunishmentManager.handleRespawn(player);
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        NaraTutorialManager.handleEquipmentChange(player, event.getSlot(), event.getFrom(), event.getTo());
    }

    // ── 對外 public API（thin delegate，外部 caller 簽名零變動） ─────────

    public static void scheduleFirstScanTutorial(ServerPlayer player) {
        NaraTutorialManager.scheduleFirstScan(player);
    }

    public static void scheduleFirstWatchOpenTutorial(ServerPlayer player) {
        NaraTutorialManager.scheduleFirstWatchOpen(player);
    }

    public static void scheduleFirstResearchTutorial(ServerPlayer player) {
        NaraTutorialManager.scheduleFirstResearch(player);
    }

    public static void scheduleFirstAltarFormedTutorial(ServerPlayer player) {
        NaraTutorialManager.scheduleFirstAltarFormed(player);
    }

    public static void scheduleWardenDespawn(UUID wardenUUID, int delayTicks) {
        NaraTutorialManager.scheduleWardenDespawn(wardenUUID, delayTicks);
    }

    public static void schedulePunishmentDialogue(UUID playerUUID, int delayTicks) {
        NaraPunishmentManager.schedule(playerUUID, delayTicks);
    }

    public static void cancelPunishmentState(UUID uuid) {
        NaraPunishmentManager.cancel(uuid);
    }

    public static void scheduleTestTutorial(ServerPlayer player, String tutorialId) {
        NaraGhostTutorialManager.schedule(player, tutorialId);
    }

    public static boolean isGhostBlock(BlockPos pos) {
        return NaraGhostTutorialManager.isGhostBlock(pos);
    }

    public static void restorePendingTutorials(ServerPlayer player) {
        NaraTutorialManager.restorePendingTutorials(player);
    }

    // ── Advancement 小工具（跨子系統共用） ──────────────────────────────

    public static void grantWelcomeAdvancement(ServerPlayer player) {
        grantAdvancement(player, "nara_welcome");
    }

    public static void grantAngryAdvancement(ServerPlayer player) {
        grantAdvancement(player, "nara_angry");
    }

    private static void grantAdvancement(ServerPlayer player, String id) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, id);
        var holder = player.server.getAdvancements().get(loc);
        if (holder != null) player.getAdvancements().award(holder, "obtained");
    }
}
