package com.github.nalamodikk.common.block.blockentity.altar;

import com.github.nalamodikk.common.entity.SpaceCrackEntity;
import com.github.nalamodikk.common.network.packet.client.altar.RitualExplosionPacket;
import com.github.nalamodikk.dimension.ModDimensions;
import com.github.nalamodikk.register.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

/**
 * 儀式因魔力耗盡而失控時的爆炸效果：衝擊波封包、範圍魔法傷害、底座物品損毀，
 * 以及 T3+ 祭壇撕開通往鏡中世界的裂縫。純效果輸出，無祭壇狀態機。
 */
public final class AltarExplosionTrigger {

    private AltarExplosionTrigger() {}

    private static final int EXPLOSION_RADIUS = 64;
    private static final float ITEM_PERM_LOSS_CHANCE = 0.15f; // 15% 永久消失
    private static final float ITEM_DROP_CHANCE      = 0.50f; // 50% 掉落

    public static void trigger(Level level, BlockPos worldPosition, int upgradeTier,
                               UUID activatorUUID, List<AspectPedestalBlockEntity> activePedestals) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        Vec3 center = Vec3.atCenterOf(worldPosition);

        serverLevel.playSound(null, worldPosition, SoundEvents.WITHER_DEATH,
                SoundSource.BLOCKS, 1.5f, 0.6f);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y, center.z, 1, 0, 0, 0, 0);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                center.x, center.y + 1, center.z, 30, 1.5, 1.0, 1.5, 0.05);

        // 傳送衝擊波封包給 64 格內所有玩家，客戶端播放紅色擴散環
        for (ServerPlayer sp : serverLevel.players()) {
            if (sp.blockPosition().distSqr(worldPosition) <= EXPLOSION_RADIUS * EXPLOSION_RADIUS) {
                PacketDistributor.sendToPlayer(sp, new RitualExplosionPacket(worldPosition));
            }
        }

        // 50% 最大 HP 魔法傷害 + 緩速 II 10 秒，只影響玩家
        double dmgRadius = EXPLOSION_RADIUS;
        List<ServerPlayer> nearbyPlayers = serverLevel.getEntitiesOfClass(
                ServerPlayer.class,
                AABB.ofSize(center, dmgRadius * 2, dmgRadius * 2, dmgRadius * 2));
        for (ServerPlayer sp : nearbyPlayers) {
            sp.hurt(serverLevel.damageSources().magic(), sp.getMaxHealth() * 0.5f);
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
        }

        // 底座物品：15% 永久消失，50% 掉落，其餘保留
        for (AspectPedestalBlockEntity pedestal : activePedestals) {
            ItemStack held = pedestal.getHeldItem();
            if (held.isEmpty()) continue;
            float rand = serverLevel.random.nextFloat();
            if (rand < ITEM_PERM_LOSS_CHANCE) {
                pedestal.consumeItem();
            } else if (rand < ITEM_PERM_LOSS_CHANCE + ITEM_DROP_CHANCE) {
                ItemStack drop = held.copy();
                pedestal.consumeItem();
                Vec3 dropPos = Vec3.atCenterOf(pedestal.getBlockPos()).add(0, 0.5, 0);
                ItemEntity ie = new ItemEntity(serverLevel, dropPos.x, dropPos.y, dropPos.z, drop);
                ie.setDefaultPickUpDelay();
                serverLevel.addFreshEntity(ie);
            }
        }

        // T3+ 祭壇爆炸：在核心上方 10 格撕開一道通往鏡中世界的裂縫
        if (upgradeTier >= 3 && activatorUUID != null
                && !serverLevel.dimension().equals(ModDimensions.VOID_MIRROR)) {
            BlockPos crackPos = worldPosition.above(10);
            if (!SpaceCrackEntity.existsNear(serverLevel, crackPos)) {
                SpaceCrackEntity crack = new SpaceCrackEntity(ModEntities.SPACE_CRACK.get(), serverLevel);
                crack.setOwnerUUID(activatorUUID);
                crack.moveTo(crackPos.getX() + 0.5, crackPos.getY(), crackPos.getZ() + 0.5,
                        serverLevel.random.nextFloat() * 360F, 0F);
                serverLevel.addFreshEntity(crack);
            }
        }
    }
}
