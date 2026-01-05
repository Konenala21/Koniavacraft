package com.github.nalamodikk.common.network.packet.client;

import com.github.nalamodikk.common.network.packet.server.rpg.SyncRPGDataPacket;
import com.github.nalamodikk.common.rpg.data.PlayerRPGData;
import com.github.nalamodikk.common.rpg.player.PlayerClass;
import com.github.nalamodikk.register.ModDataAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SyncRPGDataPacketClient {
    public static void handle(SyncRPGDataPacket packet, IPayloadContext context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        PlayerRPGData data = player.getData(ModDataAttachments.PLAYER_RPG_DATA.get());
        data.setLevel(packet.level());
        data.setExperience(packet.experience());
        data.setExperienceToNextLevel(packet.experienceToNextLevel());
        data.setPlayerClass(PlayerClass.fromId(packet.playerClassId()));
        data.getAttributes().setStrength(packet.strength());
        data.getAttributes().setIntelligence(packet.intelligence());
        data.getAttributes().setAgility(packet.agility());
        data.getAttributes().setVitality(packet.vitality());
        data.getAttributes().setPerception(packet.perception());
        data.setUnspentAttributePoints(packet.unspentAttributePoints());
    }
}
