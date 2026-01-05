package com.github.nalamodikk.common.network.packet.server.rpg;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.network.packet.client.SyncRPGDataPacketClient;
import com.github.nalamodikk.common.rpg.data.PlayerRPGData;
import com.github.nalamodikk.common.rpg.player.PlayerClass;
import com.github.nalamodikk.register.ModDataAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record SyncRPGDataPacket(
        int level,
        int experience,
        int experienceToNextLevel,
        String playerClassId,
        int strength,
        int intelligence,
        int agility,
        int vitality,
        int perception,
        int unspentAttributePoints
) implements CustomPacketPayload {

    public static final Type<SyncRPGDataPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "sync_rpg_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRPGDataPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        ByteBufCodecs.VAR_INT.encode(buf, packet.level());
                        ByteBufCodecs.VAR_INT.encode(buf, packet.experience());
                        ByteBufCodecs.VAR_INT.encode(buf, packet.experienceToNextLevel());
                        ByteBufCodecs.stringUtf8(64).encode(buf, packet.playerClassId());
                        ByteBufCodecs.VAR_INT.encode(buf, packet.strength());
                        ByteBufCodecs.VAR_INT.encode(buf, packet.intelligence());
                        ByteBufCodecs.VAR_INT.encode(buf, packet.agility());
                        ByteBufCodecs.VAR_INT.encode(buf, packet.vitality());
                        ByteBufCodecs.VAR_INT.encode(buf, packet.perception());
                        ByteBufCodecs.VAR_INT.encode(buf, packet.unspentAttributePoints());
                    },
                    buf -> new SyncRPGDataPacket(
                            ByteBufCodecs.VAR_INT.decode(buf),
                            ByteBufCodecs.VAR_INT.decode(buf),
                            ByteBufCodecs.VAR_INT.decode(buf),
                            ByteBufCodecs.stringUtf8(64).decode(buf),
                            ByteBufCodecs.VAR_INT.decode(buf),
                            ByteBufCodecs.VAR_INT.decode(buf),
                            ByteBufCodecs.VAR_INT.decode(buf),
                            ByteBufCodecs.VAR_INT.decode(buf),
                            ByteBufCodecs.VAR_INT.decode(buf),
                            ByteBufCodecs.VAR_INT.decode(buf)
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void registerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(TYPE, STREAM_CODEC, (packet, context) -> context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                SyncRPGDataPacketClient.handle(packet, context);
            }
        }));
    }

    public static void sendToPlayer(ServerPlayer player) {
        if (player == null || !player.hasData(ModDataAttachments.PLAYER_RPG_DATA.get())) return;
        PlayerRPGData data = player.getData(ModDataAttachments.PLAYER_RPG_DATA.get());
        var attributes = data.getAttributes();
        String classId = data.getPlayerClass() != null ? data.getPlayerClass().getId() : PlayerClass.NONE.getId();

        SyncRPGDataPacket packet = new SyncRPGDataPacket(
                data.getLevel(),
                data.getExperience(),
                data.getExperienceToNextLevel(),
                classId,
                attributes.getStrength(),
                attributes.getIntelligence(),
                attributes.getAgility(),
                attributes.getVitality(),
                attributes.getPerception(),
                data.getUnspentAttributePoints()
        );
        PacketDistributor.sendToPlayer(player, packet);
    }
}
