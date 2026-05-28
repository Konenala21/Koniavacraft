package com.github.nalamodikk.register;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.client.altar.AltarUpgradeAnimPacket;
import com.github.nalamodikk.common.network.packet.client.FormationStagePacket;
import com.github.nalamodikk.common.network.packet.client.altar.RitualExplosionPacket;
import com.github.nalamodikk.common.network.packet.client.Phase2TransitionPacket;
import com.github.nalamodikk.common.network.packet.client.VoidMirrorIntroPacket;
import com.github.nalamodikk.common.network.packet.server.Phase2SkipPacket;
import com.github.nalamodikk.common.network.packet.server.VoidMirrorSkipIntroPacket;
import com.github.nalamodikk.common.network.packet.client.armor.ManaShieldHitPacket;
import com.github.nalamodikk.common.network.packet.client.turret.DamageNumberPacket;
import com.github.nalamodikk.common.network.packet.client.turret.TurretHitPacket;
import com.github.nalamodikk.common.network.packet.server.OpenUpgradeGuiPacket;
import com.github.nalamodikk.common.network.packet.server.armor.ArmorUpgradeSwapPacket;
import com.github.nalamodikk.common.network.packet.server.inventory.SortContainerPacket;
import com.github.nalamodikk.common.network.packet.server.armor.DoubleJumpPacket;
import com.github.nalamodikk.common.network.packet.server.armor.ToggleNightVisionPacket;
import com.github.nalamodikk.common.network.packet.server.boots.BootsUpgradeSwapPacket;
import com.github.nalamodikk.common.network.packet.server.boots.DashPacket;
import com.github.nalamodikk.common.network.packet.server.wand.WandCoreSwapPacket;
import com.github.nalamodikk.common.network.packet.server.turret.TurretUpgradeSwapPacket;
import com.github.nalamodikk.common.network.packet.server.deployer.SetDeployerIntervalPacket;
import com.github.nalamodikk.common.network.packet.server.deployer.ToggleDeployerEnabledPacket;
import com.github.nalamodikk.common.network.packet.server.conduit.PriorityUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.conduit.ResetPrioritiesPacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ConfigDirectionBatchUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ConfigDirectionUpdatePacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ModeChangePacket;
import com.github.nalamodikk.common.network.packet.server.manatool.TechWandModePacket;
import com.github.nalamodikk.common.network.packet.server.manatool.ToggleModePacket;
import com.github.nalamodikk.common.network.packet.server.player.gui.OpenExtraEquipmentPacket;
import com.github.nalamodikk.narasystem.nara.network.client.NaraAngryPacket;
import com.github.nalamodikk.narasystem.nara.network.client.NaraCreeperPunishPacket;
import com.github.nalamodikk.narasystem.nara.network.client.NaraStartDialoguePacket;
import com.github.nalamodikk.narasystem.nara.network.client.NaraTauntPacket;
import com.github.nalamodikk.narasystem.nara.network.client.NaraTutorialPacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraBindRequestPacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraCloseDialoguePacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraSkipIntroPacket;
import com.github.nalamodikk.narasystem.nara.network.server.NaraTutorialSeenPacket;
import com.github.nalamodikk.research.network.AspectSynthesisPacket;
import com.github.nalamodikk.research.network.ResearchAspectPlacePacket;
import com.github.nalamodikk.research.network.ResearchCellRevertPacket;
import com.github.nalamodikk.research.network.ResearchCompletePacket;
import com.github.nalamodikk.research.network.ScanResultSyncPacket;
import com.github.nalamodikk.research.network.StartResearchPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID)
public class ModNetworking {
    public static final String VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        TechWandModePacket.registerTo(registrar);
        ModeChangePacket.registerTo(registrar);
        ConfigDirectionUpdatePacket.registerTo(registrar);
        ConfigDirectionBatchUpdatePacket.registerTo(registrar);
        ToggleModePacket.registerTo(registrar);
        OpenUpgradeGuiPacket.registerTo(registrar);
        OpenExtraEquipmentPacket.registerTo(registrar);
        PriorityUpdatePacket.registerTo(registrar);
        ResetPrioritiesPacket.registerTo(registrar);
        NaraBindRequestPacket.registerTo(registrar);
        NaraStartDialoguePacket.registerTo(registrar);  // 含 Punishment inner record
        NaraTutorialPacket.registerTo(registrar);
        NaraCloseDialoguePacket.registerTo(registrar);
        NaraCreeperPunishPacket.registerTo(registrar);
        NaraAngryPacket.registerTo(registrar);
        NaraSkipIntroPacket.registerTo(registrar);
        NaraTutorialSeenPacket.registerTo(registrar);

        SetDeployerIntervalPacket.registerTo(registrar);
        ToggleDeployerEnabledPacket.registerTo(registrar);

        ResearchCompletePacket.registerTo(registrar);
        ResearchAspectPlacePacket.registerTo(registrar);
        ResearchCellRevertPacket.registerToClient(registrar);
        ScanResultSyncPacket.registerToClient(registrar);
        StartResearchPacket.registerTo(registrar);
        AspectSynthesisPacket.registerTo(registrar);
        AltarUpgradeAnimPacket.registerToClient(registrar);
        RitualExplosionPacket.registerToClient(registrar);
        TurretHitPacket.registerToClient(registrar);
        DamageNumberPacket.registerToClient(registrar);
        WandCoreSwapPacket.registerTo(registrar);
        TurretUpgradeSwapPacket.registerTo(registrar);
        DashPacket.registerTo(registrar);
        BootsUpgradeSwapPacket.registerTo(registrar);
        ArmorUpgradeSwapPacket.registerTo(registrar);
        DoubleJumpPacket.registerTo(registrar);
        ToggleNightVisionPacket.registerTo(registrar);
        FormationStagePacket.registerToClient(registrar);
        SortContainerPacket.registerTo(registrar);
        ManaShieldHitPacket.registerToClient(registrar);
        VoidMirrorIntroPacket.registerToClient(registrar);
        VoidMirrorSkipIntroPacket.registerTo(registrar);
        Phase2TransitionPacket.registerToClient(registrar);
        Phase2SkipPacket.registerTo(registrar);
        NaraTauntPacket.registerToClient(registrar);
    }
}
