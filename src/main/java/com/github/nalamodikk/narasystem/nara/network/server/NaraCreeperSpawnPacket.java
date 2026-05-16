package com.github.nalamodikk.narasystem.nara.network.server;

import com.github.nalamodikk.narasystem.nara.event.NaraServerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

public class NaraCreeperSpawnPacket {

    public static void spawnWardenNearPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 pos = player.position();

        Warden warden = EntityType.WARDEN.create(level);
        if (warden == null) return;

        warden.moveTo(pos.x, pos.y, pos.z, 0f, 0f);
        warden.getPersistentData().putBoolean("NaraEasterEgg", true);
        warden.increaseAngerAt(player, 150, false);

        level.addFreshEntity(warden);

        // 立刻對玩家造成 Warden 近戰傷害（不等 AI 動畫）
        float dmg = (float) warden.getAttributeValue(Attributes.ATTACK_DAMAGE);
        player.hurt(level.damageSources().mobAttack(warden), dmg);

        NaraServerEvents.scheduleWardenDespawn(warden.getUUID(), 80);
    }
}
