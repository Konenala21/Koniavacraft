package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.common.event.VoidMirrorEvents;
import com.github.nalamodikk.common.loot.BossLootRegistry;
import com.github.nalamodikk.common.network.packet.client.BossBgmPacket;
import com.github.nalamodikk.register.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * Boss 死亡演出 controller（從 PlayerCloneEntity 抽出）。
 *
 * 覆寫 vanilla 20-tick 死亡 → 自製 400-tick 五階段演出：
 * Phase 1 (0-65):    Stagger      — 搖晃低頭、低吼，少量裂紋音
 * Phase 2 (66-165):  Glow Up      — 全身白熱化、緩慢旋轉、能量充能音
 * Phase 3 (166-275): Crack        — 胸口裂痕擴張、紫光外洩、玻璃碎裂連音
 * Phase 4 (276-365): Shatter      — 碎片噴飛 + 大爆破 + slow-mo
 * Phase 5 (366-400): Final Flash  — 全屏閃光、boss 消失、靈魂上飄
 *
 * sound/particle 一律走 {@link #playSound} / {@link #sendParticles}，多人模式下只發給 source player。
 * 持有 entity ref，phase 切換與 entityData 更新仍由 PlayerCloneEntity.tickDeath() 主導，這裡只負責演出內容。
 */
class PlayerCloneDeathSequence {

    private final PlayerCloneEntity clone;

    PlayerCloneDeathSequence(PlayerCloneEntity clone) {
        this.clone = clone;
    }

    static int computeDeathPhase(int t) {
        if (t <= PlayerCloneEntity.DEATH_PHASE_STAGGER_END) return 1;
        if (t <= PlayerCloneEntity.DEATH_PHASE_GLOW_END)    return 2;
        if (t <= PlayerCloneEntity.DEATH_PHASE_CRACK_END)   return 3;
        if (t <= PlayerCloneEntity.DEATH_PHASE_SHATTER_END) return 4;
        return 5;
    }

    // 取得這隻 boss 的 source player（多人模式下用來把死亡演出 sound/particle 只發給他）
    private @Nullable ServerPlayer getSourcePlayer(ServerLevel sl) {
        return clone.getSourceUUID().map(id -> sl.getServer().getPlayerList().getPlayer(id)).orElse(null);
    }

    // 只發給 source player 的音效（fallback：沒 source 就廣播給所有人，例 /summon 邊角）
    private void playSound(ServerLevel sl, SoundEvent event, float vol, float pitch) {
        ServerPlayer src = getSourcePlayer(sl);
        if (src == null) {
            sl.playSound(null, clone.blockPosition(), event, SoundSource.HOSTILE, vol, pitch);
            return;
        }
        src.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(event),
                SoundSource.HOSTILE, clone.getX(), clone.getY(), clone.getZ(), vol, pitch, sl.random.nextLong()));
    }

    // 只發給 source player 的粒子（fallback 同上）
    private void sendParticles(ServerLevel sl, ParticleOptions p, double x, double y, double z,
                               int count, double dx, double dy, double dz, double speed) {
        ServerPlayer src = getSourcePlayer(sl);
        if (src == null) {
            sl.sendParticles(p, x, y, z, count, dx, dy, dz, speed);
            return;
        }
        sl.sendParticles(src, p, true, x, y, z, count, dx, dy, dz, speed);
    }

    // 階段切換瞬間觸發 — 一次性大動作（音效、爆破粒子、slow-mo 開始等）
    // 所有 sound/particle 走 playSound / sendParticles，多人模式下只給 source player
    void onPhaseEnter(ServerLevel sl, int phase) {
        double x = clone.getX(), y = clone.getY() + 1.0, z = clone.getZ();
        switch (phase) {
            case 1 -> {
                playSound(sl, SoundEvents.GLASS_BREAK, 1.2F, 0.4F);
                playSound(sl, SoundEvents.WARDEN_HEARTBEAT, 2.0F, 0.5F);
            }
            case 2 -> {
                sendParticles(sl, ParticleTypes.END_ROD, x, y, z, 60, 0.8, 1.2, 0.8, 0.05);
                sendParticles(sl, ParticleTypes.PORTAL, x, y, z, 120, 1.0, 1.5, 1.0, 0.4);
                playSound(sl, SoundEvents.BEACON_ACTIVATE, 1.5F, 1.4F);
                playSound(sl, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 0.6F);
            }
            case 3 -> {
                sendParticles(sl, ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 50, 0.6, 0.8, 0.6, 0.10);
                playSound(sl, SoundEvents.GLASS_BREAK, 2.0F, 0.7F);
                playSound(sl, SoundEvents.AMETHYST_CLUSTER_BREAK, 2.0F, 0.5F);
                // 裂縫實體本身就是 entity，自然只有附近 client 看得到，不用特別過濾
                SpaceCrackEntity rift = ModEntities.SPACE_CRACK.get().create(sl);
                if (rift != null) {
                    rift.moveTo(x, y - 0.3, z, clone.getYRot(), 0F);
                    rift.setDecorative(60);
                    sl.addFreshEntity(rift);
                }
            }
            case 4 -> {
                sendParticles(sl, ParticleTypes.EXPLOSION_EMITTER, x, y, z, 2, 0.5, 0.5, 0.5, 0.0);
                sendParticles(sl, ParticleTypes.LARGE_SMOKE, x, y, z, 80, 1.5, 1.8, 1.5, 0.15);
                sendParticles(sl, ParticleTypes.FLASH, x, y, z, 2, 0.3, 0.3, 0.3, 0.0);
                playSound(sl, SoundEvents.ENDER_DRAGON_DEATH, 2.0F, 0.7F);
                playSound(sl, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.5F);
                spawnShards(sl);
            }
            case 5 -> {
                sendParticles(sl, ParticleTypes.FLASH, x, y, z, 4, 0.5, 0.5, 0.5, 0.0);
                sendParticles(sl, ParticleTypes.SOUL, x, y, z, 60, 1.2, 1.5, 1.2, 0.15);
                playSound(sl, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 1.8F);
                boolean anyCloneLeft = !sl.getEntitiesOfClass(PlayerCloneEntity.class,
                        new AABB(BlockPos.ZERO).inflate(260), e -> e != clone && e.isAlive()).isEmpty();
                if (!anyCloneLeft) spawnRewardChest(sl, clone.pendingRewardFirstClear);
            }
        }
    }

    // 階段內每 tick 持續觸發 — 連續粒子流、發光殘留等
    void tickPhase(ServerLevel sl, int t, int phase) {
        double x = clone.getX(), y = clone.getY() + 1.0, z = clone.getZ();
        switch (phase) {
            case 1 -> {
                // Stagger：少量灰煙從身上飄出
                if (t % 3 == 0) sendParticles(sl, ParticleTypes.SMOKE, x, y, z, 2, 0.3, 0.5, 0.3, 0.02);
            }
            case 2 -> {
                if (t % 2 == 0) sendParticles(sl, ParticleTypes.END_ROD, x, y, z, 3, 0.3, 0.5, 0.3, 0.06);
            }
            case 3 -> {
                if (t % 2 == 0) sendParticles(sl, ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 4, 0.5, 0.7, 0.5, 0.08);
                if (t % 5 == 0) playSound(sl, SoundEvents.GLASS_BREAK, 0.8F, 1.2F + clone.getRandom().nextFloat() * 0.6F);
            }
            case 4 -> {
                if (t % 3 == 0) {
                    sendParticles(sl, ParticleTypes.LARGE_SMOKE, x, y, z, 4, 1.0, 1.2, 1.0, 0.05);
                    sendParticles(sl, ParticleTypes.SOUL, x, y, z, 3, 0.8, 1.0, 0.8, 0.08);
                }
            }
            case 5 -> {
                if (t % 2 == 0) sendParticles(sl, ParticleTypes.SOUL, x, y + 1.5, z, 2, 0.6, 0.5, 0.6, 0.05);
            }
        }
    }

    // 碎片噴飛（Phase 4）— 16 個方向的 ITEM_SNOWBALL 用 motion 噴出（vanilla 粒子可帶速度）
    // 不另外做實體，因為 vanilla 粒子已能達到「碎屑往外飛 + 重力下墜」的視覺效果
    private void spawnShards(ServerLevel sl) {
        double x = clone.getX(), y = clone.getY() + 1.0, z = clone.getZ();
        for (int i = 0; i < 16; i++) {
            double ang = i * (Math.PI * 2 / 16);
            double vx = Math.cos(ang) * 0.6;
            double vz = Math.sin(ang) * 0.6;
            double vy = 0.3 + clone.getRandom().nextDouble() * 0.3;
            // sendParticles 的 xSpeed/ySpeed/zSpeed 在 count > 1 時是 random 散度，count = 0 時直接當速度
            // 這裡用 count=0 + ySpeed 當「初速」讓每個粒子都有方向性 ballistic
            sendParticles(sl, ParticleTypes.WHITE_ASH, x, y, z, 0, vx, vy, vz, 1.0);
            sendParticles(sl, ParticleTypes.POOF, x, y, z, 0, vx * 0.8, vy * 0.8, vz * 0.8, 0.8);
        }
        sendParticles(sl, ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
    }

    // 停 BGM：多人模式下，**場上還有其他 boss 活著就不真的停**，等所有 boss 都死才停
    // 不然兩人合作打 boss A、B，A 先死就把 B 的玩家 BGM 也停了，戰鬥下半場沒氣氛
    void stopBgm(ServerLevel sl) {
        boolean otherBossesAlive = !sl.getEntitiesOfClass(PlayerCloneEntity.class,
                new AABB(BlockPos.ZERO).inflate(260),
                e -> e != clone && e.isAlive() && !e.isDeathMarked()).isEmpty();
        if (otherBossesAlive) {
            // 留 BGM 給場上的玩家繼續聽，只清自己這隻的 sentTo 記錄
            clone.bgmSentTo.clear();
            return;
        }
        // 場上最後一隻 boss 了 → 真正停 BGM
        BossBgmPacket stop = BossBgmPacket.STOP;
        for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
            if (clone.bgmSentTo.contains(p.getUUID())) {
                PacketDistributor.sendToPlayer(p, stop);
            }
        }
        clone.bgmSentTo.clear();
    }

    void spawnRewardChest(ServerLevel sl, boolean includeShard) {
        BlockPos chestPos = new BlockPos(0, 64, -3);
        // 關鍵:若那格已經有寶箱(上次過關留下),就不要 setBlock 換掉它,直接在原箱清空 + 重設
        // loot table。換方塊在 loot table 模式下時序上仍會把舊內容噴一地,不換就根本不會掉。
        if (!(sl.getBlockEntity(chestPos) instanceof ChestBlockEntity)) {
            sl.setBlock(chestPos, Blocks.CHEST.defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
            VoidMirrorEvents.addModifiedBlock(chestPos.asLong());
        }
        if (sl.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            // 先清掉上次過關剩下的物品(只是清空,不會掉落),並清掉任何待填的 loot table,
            // 再「當場」roll 一次 loot table 直接填進去。不用懶填(setLootTable),所以開箱絕對
            // 只有這次的內容,不會有上一場殘留。內容定義 + JEI 仍是同一張 chests/mirror_boss_reward。
            chest.clearContent();
            chest.setLootTable(null);
            LootTable table = sl.getServer().reloadableRegistries().getLootTable(BossLootRegistry.MIRROR_BOSS.lootTable());
            LootParams params = new LootParams.Builder(sl).create(LootContextParamSets.EMPTY);
            table.fill(chest, params, sl.getRandom().nextLong());
            chest.setChanged();
        }
    }
}
