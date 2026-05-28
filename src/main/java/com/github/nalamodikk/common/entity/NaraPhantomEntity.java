package com.github.nalamodikk.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

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

    public void startVictoryFarewell(int ticks) {
        this.victoryFarewellTicks = ticks;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && victoryFarewellTicks > 0) {
            victoryFarewellTicks--;
            if (victoryFarewellTicks == 0) {
                this.discard();
            }
        }
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
