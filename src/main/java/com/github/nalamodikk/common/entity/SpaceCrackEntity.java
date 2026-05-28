package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.common.dimension.VoidMirrorTeleport;
import com.github.nalamodikk.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Optional;
import java.util.UUID;

public class SpaceCrackEntity extends Entity {

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(SpaceCrackEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    // 裝飾用裂縫（進場演出玩家鑽出處）：不傳送、不存盤、到壽命自動消失
    private boolean decorative = false;
    private int lifespan = -1;

    public SpaceCrackEntity(EntityType<? extends SpaceCrackEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** 標記為裝飾用裂縫：不傳送、不存盤、lifespan tick 後自動消失。 */
    public void setDecorative(int lifespanTicks) {
        this.decorative = true;
        this.lifespan = lifespanTicks;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_UUID, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0, 0, 0);
        if (!level().isClientSide) {
            if (decorative && lifespan >= 0 && this.tickCount >= lifespan) {
                this.discard();
                return;
            }
            // boss 死亡後 die() 標記 pendingCrackRemoval，這裡每 20 tick 檢查並自我 discard
            // 之所以用 tick-based 而不是 die() 直接刪除：玩家在 mirror dim 打 boss 時 overworld
            // 的入口裂縫所在 chunk 通常已卸載，從 die() 那邊 getEntities 找不到。讓裂縫自己 tick 解決
            if (!decorative && this.tickCount % 20 == 0 && level() instanceof ServerLevel sl) {
                UUID owner = getOwnerUUID().orElse(null);
                if (owner != null && com.github.nalamodikk.common.dimension.VoidMirrorSavedData
                        .get(sl.getServer()).hasPendingCrackRemoval(owner)) {
                    this.discard();
                }
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (decorative) return InteractionResult.PASS; // 裝飾裂縫不傳送
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            if (level().dimension().equals(ModDimensions.VOID_MIRROR)) {
                VoidMirrorTeleport.exit(sp); // 鏡中世界內的裂縫 = 出口，傳回主世界返回點
            } else {
                VoidMirrorTeleport.enter(sp); // 主世界的裂縫 = 入口
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean isPickable() {
        return !decorative && !this.isRemoved();
    }

    @Override
    public boolean shouldBeSaved() {
        return !decorative && super.shouldBeSaved();
    }

    public void setOwnerUUID(UUID uuid) {
        entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    public Optional<UUID> getOwnerUUID() {
        return entityData.get(OWNER_UUID);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            entityData.set(OWNER_UUID, Optional.of(tag.getUUID("OwnerUUID")));
        } else {
            entityData.set(OWNER_UUID, Optional.empty());
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        entityData.get(OWNER_UUID).ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // Client 端追蹤：加入 / 移除世界時通知 SpaceCrackStageRenderer.ACTIVE set
    // 避免每幀掃 64 格 spatial 查找（perf 修正）
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (this.level().isClientSide) {
            com.github.nalamodikk.client.renderer.entity.SpaceCrackStageRenderer.register(this);
        }
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        if (this.level().isClientSide) {
            com.github.nalamodikk.client.renderer.entity.SpaceCrackStageRenderer.unregister(this);
        }
    }

    public static boolean existsNear(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(2.0);
        return !level.getEntitiesOfClass(SpaceCrackEntity.class, box).isEmpty();
    }

    public static void removeForOwner(ServerLevel level, UUID owner) {
        for (Entity e : level.getAllEntities()) {
            if (e instanceof SpaceCrackEntity crack
                    && crack.getOwnerUUID().map(owner::equals).orElse(false)) {
                crack.discard();
            }
        }
    }
}
