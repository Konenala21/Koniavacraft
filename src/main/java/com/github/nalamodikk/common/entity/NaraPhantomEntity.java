package com.github.nalamodikk.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * 娜拉幻影：在鏡中世界遠處旁觀玩家戰鬥，不介入、不可被攻擊。
 * 目前用玩家模型 + 來源玩家皮膚當佔位（娜拉尚無正式模型）。
 */
public class NaraPhantomEntity extends PathfinderMob {

    private static final EntityDataAccessor<Optional<UUID>> SOURCE_UUID =
            SynchedEntityData.defineId(NaraPhantomEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public NaraPhantomEntity(EntityType<? extends NaraPhantomEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SOURCE_UUID, Optional.empty());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void registerGoals() {
        // 只看著玩家，不移動、不攻擊
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 32.0F, 1.0F));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false; // 不存盤：由 boss（PlayerCloneEntity）在進攻中確保恰好一個同源娜拉，避免重載殘留/重複
    }

    @Override
    public void checkDespawn() {
        // 不自然消失
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public void setSourceUUID(@Nullable UUID uuid) {
        entityData.set(SOURCE_UUID, Optional.ofNullable(uuid));
    }

    public Optional<UUID> getSourceUUID() {
        return entityData.get(SOURCE_UUID);
    }

    // Boss 死亡時設定一個倒數 tick，倒數到 0 自我 discard（讓死亡演出有時間用她當對白主角）
    private int victoryFarewellTicks = -1;

    // 跟隨來源玩家：intro 結束後由 PlayerCloneEntity.activateAfterIntro 啟用，
    // 確保 boss 死亡時 Nara 仍在玩家附近 (BossDeathCameraManager.findNara 範圍 80 格內)。
    private boolean followingPlayer = false;
    private static final double FOLLOW_DISTANCE = 15.0;
    private static final double FOLLOW_DISTANCE_SQ = FOLLOW_DISTANCE * FOLLOW_DISTANCE;
    private static final double FOLLOW_SPEED = 0.3; // 每 tick 移動上限（≈6 b/s，足以跟上奔跑玩家）

    public void startVictoryFarewell(int ticks) {
        this.victoryFarewellTicks = ticks;
    }

    public void enablePlayerFollow() {
        this.followingPlayer = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (victoryFarewellTicks > 0) {
            victoryFarewellTicks--;
            if (victoryFarewellTicks == 0) this.discard();
            return; // 死亡演出期間凍結位置，配合相機鎖 Nara
        }
        if (followingPlayer) followSourcePlayer();
    }

    private void followSourcePlayer() {
        if (!(level() instanceof ServerLevel sl)) return;
        UUID src = entityData.get(SOURCE_UUID).orElse(null);
        if (src == null) return;
        Player player = sl.getPlayerByUUID(src);
        if (player == null) return;
        // 只用水平距離追蹤，Y 直接貼玩家高度。
        // 不可用 3D 方向 + setPos：setPos 不做碰撞檢測，dir.y 會把幻影沿直線往玩家腳底拉、穿過地形鑲進地底。
        Vec3 toPlayer = player.position().subtract(position());
        double horizSq = toPlayer.x * toPlayer.x + toPlayer.z * toPlayer.z;
        if (horizSq <= FOLLOW_DISTANCE_SQ) {
            // 水平已夠近：仍校正 Y 貼合玩家，消掉殘留的高度差（例如玩家爬上爬下後）
            if (Math.abs(toPlayer.y) > 0.05) setPos(getX(), player.getY(), getZ());
            return;
        }
        double horizDist = Math.sqrt(horizSq);
        double moveDist = Math.min(horizDist - FOLLOW_DISTANCE, FOLLOW_SPEED);
        double nx = toPlayer.x / horizDist;
        double nz = toPlayer.z / horizDist;
        setPos(getX() + nx * moveDist, player.getY(), getZ() + nz * moveDist);
        // body 朝向玩家（頭部視線由 LookAtPlayerGoal 處理）
        float yaw = (float) Math.toDegrees(Math.atan2(-toPlayer.x, toPlayer.z));
        setYRot(yaw);
        yBodyRot = yaw;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        entityData.get(SOURCE_UUID).ifPresent(uuid -> tag.putUUID("SourceUUID", uuid));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("SourceUUID")) {
            entityData.set(SOURCE_UUID, Optional.of(tag.getUUID("SourceUUID")));
        }
    }
}
