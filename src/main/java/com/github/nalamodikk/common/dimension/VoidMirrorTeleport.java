package com.github.nalamodikk.common.dimension;

import com.github.nalamodikk.common.entity.NaraPhantomEntity;
import com.github.nalamodikk.common.entity.PlayerCloneEntity;
import com.github.nalamodikk.common.entity.SpaceCrackEntity;
import com.github.nalamodikk.common.network.packet.client.VoidMirrorIntroPacket;
import com.github.nalamodikk.dimension.BoundedFlatChunkGenerator;
import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.register.ModDataAttachments;
import com.github.nalamodikk.register.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class VoidMirrorTeleport {

    private static final double ARENA_X = 0.5;
    private static final double ARENA_Y = 64.0;
    private static final double ARENA_Z = 0.5;
    // 玩家從登場點後方此距離的裂縫往前走出（對齊 client VoidMirrorIntroManager.WALK_BACK）
    private static final double EMERGE_CRACK_BACK = 3.2;

    private VoidMirrorTeleport() {}

    public static void enter(ServerPlayer player) {
        MinecraftServer server = player.server;
        ServerLevel target = server.getLevel(ModDimensions.VOID_MIRROR);
        if (target == null) return;

        // 在玩家實際進入時設定邊界，避免被主世界邊界委派在啟動時覆蓋
        int size = BoundedFlatChunkGenerator.HALF_SIZE * 2;
        WorldBorder border = target.getWorldBorder();
        border.setCenter(0, 0);
        if (border.getSize() != size) border.setSize(size);

        GlobalPos returnPoint = GlobalPos.of(player.level().dimension(), player.blockPosition());
        player.setData(ModDataAttachments.RETURN_POINT.get(), Optional.of(returnPoint));

        // yaw=0 面向 +Z（boss 登場方向），過場結束交還鏡頭時玩家本來就面對 boss
        player.teleportTo(target, ARENA_X, ARENA_Y, ARENA_Z, Set.of(), 0.0F, 0.0F);

        // 先清掉該玩家殘留的 clone/娜拉/裂縫，確保「退出再進去」是乾淨的全新一場
        clearExistingFor(target, player.getUUID());
        // 清除上次 boss 死後遺留的「裂縫待清除」旗標 — 新一場 boss 開打要重新生成裂縫
        VoidMirrorSavedData.get(server).clearCrackRemovalPending(player.getUUID());

        // 娜拉一開始就在場（過場從她的視角開始）
        spawnNaraPhantom(target, player.getUUID());
        // 玩家走出用的裝飾裂縫（不傳送、會自己消失）
        spawnEmergeCrack(target);
        // 返回裂縫改由 boss 開始進攻時（PlayerCloneEntity.activateAfterIntro）生成，避免過場中與走出裂縫重複
        // 播放進場過場
        PacketDistributor.sendToPlayer(player, VoidMirrorIntroPacket.INSTANCE);
        // boss 以「進場演出」狀態生成，演出結束才啟動
        spawnClone(target, player);
    }

    // 清掉該玩家在鏡中世界殘留的 clone、娜拉幻影與舊裂縫（含上次的出口裂縫）
    private static void clearExistingFor(ServerLevel level, UUID id) {
        AABB box = new AABB(BlockPos.ZERO).inflate(300);
        for (PlayerCloneEntity c : level.getEntitiesOfClass(PlayerCloneEntity.class, box,
                e -> e.getSourceUUID().map(id::equals).orElse(false))) {
            c.discard();
        }
        for (NaraPhantomEntity n : level.getEntitiesOfClass(NaraPhantomEntity.class, box,
                e -> e.getSourceUUID().map(id::equals).orElse(false))) {
            n.discard();
        }
        for (Entity e : level.getAllEntities()) {
            if (e instanceof SpaceCrackEntity crack
                    && crack.getOwnerUUID().map(id::equals).orElse(false)) {
                crack.discard();
            }
        }
    }

    private static void spawnEmergeCrack(ServerLevel level) {
        SpaceCrackEntity crack = ModEntities.SPACE_CRACK.get().create(level);
        if (crack == null) return;
        crack.moveTo(ARENA_X, ARENA_Y, ARENA_Z - EMERGE_CRACK_BACK, 0.0F, 0.0F); // 玩家身後，供其走出
        crack.setDecorative(640); // 略長於過場(620t)，玩家走出後一段時間自動消失
        level.addFreshEntity(crack);
    }

    // public：供 boss（PlayerCloneEntity）在進攻中確保娜拉存在時重用
    public static void spawnNaraPhantom(ServerLevel level, UUID sourceId) {
        NaraPhantomEntity nara = ModEntities.NARA_PHANTOM.get().create(level);
        if (nara == null) return;
        // 站在遠處旁觀
        nara.moveTo(ARENA_X, ARENA_Y, ARENA_Z - 26.0, 0.0F, 0.0F);
        nara.setSourceUUID(sourceId);
        nara.setCustomName(Component.translatable("nara.phantom.name"));
        nara.setCustomNameVisible(true);
        level.addFreshEntity(nara);
    }

    private static void spawnClone(ServerLevel level, ServerPlayer source) {
        PlayerCloneEntity clone = ModEntities.PLAYER_CLONE.get().create(level);
        if (clone == null) return;
        // 登場點：玩家前方 20 格（對齊過場 D/E 段鏡頭看的位置）
        double ex = ARENA_X;
        double ey = ARENA_Y;
        double ez = ARENA_Z + 20.0;
        clone.moveTo(ex, ey, ez, 180.0F, 0.0F);
        clone.mirrorFrom(source);
        clone.startIntro(ex, ey, ez);
        level.addFreshEntity(clone);
    }

    public static void exit(ServerPlayer player) {
        MinecraftServer server = player.server;
        Optional<GlobalPos> returnPoint = player.getData(ModDataAttachments.RETURN_POINT.get());

        ServerLevel target;
        BlockPos pos;
        if (returnPoint.isPresent()) {
            target = server.getLevel(returnPoint.get().dimension());
            pos = returnPoint.get().pos();
        } else {
            target = server.overworld();
            pos = target.getSharedSpawnPos();
        }
        if (target == null) {
            target = server.overworld();
            pos = target.getSharedSpawnPos();
        }

        player.teleportTo(target, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                Set.of(), player.getYRot(), player.getXRot());
        player.removeData(ModDataAttachments.RETURN_POINT.get());
    }
}
