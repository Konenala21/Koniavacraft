package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.common.event.FloatingTurretEventHandler;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 浮游砲實體本體：負責 entity 框架（attribute / synced data / save+load）與兩個模式的 controller 委派。
 * 邏輯細節：
 *   {@link PlayerTurretController} 玩家手持/裝備槽模式
 *   {@link CloneTurretController}  boss 鏡像模式
 */
public class FloatingTurretEntity extends PathfinderMob {

    private static final EntityDataAccessor<Float> ORBIT_ANGLE =
            SynchedEntityData.defineId(FloatingTurretEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID_DATA =
            SynchedEntityData.defineId(FloatingTurretEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> SLOT_INDEX_DATA =
            SynchedEntityData.defineId(FloatingTurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IN_COMBAT_DATA =
            SynchedEntityData.defineId(FloatingTurretEntity.class, EntityDataSerializers.BOOLEAN);

    // package-private：controller 共用
    static final float ORBIT_RADIUS = 1.5F;
    static final float ORBIT_HEIGHT = 1.0F;
    static final float ORBIT_SPEED = 0.04F;
    static final int PASSIVE_ATTACK_COOLDOWN = 40;
    static final float PASSIVE_RANGE = 16.0F;

    private static final float ATTACK_DAMAGE = 5.0F;
    public static final long COMBAT_LINGER_TICKS = 200L; // 10 秒無戰鬥後解除

    // package-private：兩個模式都會 mutate
    int attackTimer = 0;

    private final PlayerTurretController playerCtrl = new PlayerTurretController(this);
    private final CloneTurretController cloneCtrl = new CloneTurretController(this);

    public FloatingTurretEntity(EntityType<? extends FloatingTurretEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ORBIT_ANGLE, 0.0F);
        builder.define(OWNER_UUID_DATA, Optional.empty());
        builder.define(SLOT_INDEX_DATA, 0);
        builder.define(IN_COMBAT_DATA, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, PASSIVE_RANGE);
    }

    @Override
    protected void registerGoals() {
        // 不使用 pathfinding goals，由 serverTick() 控制
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (cloneCtrl.isActive()) {
                cloneCtrl.serverTick();
            } else {
                playerCtrl.serverTick();
            }
        }
    }

    // ── 分身砲模式：委派給 CloneTurretController ──────────────────────────────

    @Nullable
    public LivingEntity getCloneOwner() {
        return cloneCtrl.getCloneOwner();
    }

    public void setupAsCloneTurret(LivingEntity owner, ItemStack turretStack, int slotIndex) {
        cloneCtrl.setup(owner, turretStack, slotIndex);
    }

    // ── 共用：最近敵對生物搜尋（兩個 controller 都用） ──────────────────────

    @Nullable
    LivingEntity findNearestHostile() {
        // 用 Enemy 介面而非 Monster 類別：包含 Slime、Ghast、Phantom 等 Mob implements Enemy 的敵對生物
        List<Mob> hostiles = level().getEntitiesOfClass(
                Mob.class,
                this.getBoundingBox().inflate(PASSIVE_RANGE),
                e -> e.isAlive() && e instanceof Enemy
        );
        return hostiles.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(this)))
                .orElse(null);
    }

    // ── Entity lifecycle ──────────────────────────────────────────────────────

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            entityData.get(OWNER_UUID_DATA).ifPresent(uuid ->
                    FloatingTurretEventHandler.unregisterTurret(uuid, entityData.get(SLOT_INDEX_DATA)));
        }
        super.remove(reason);
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!level().isClientSide) {
            playerCtrl.returnItemToOwner();
        }
    }

    // ── 擁有者/位置/狀態 accessor ──────────────────────────────────────────

    @Nullable
    public Player getOwnerPlayer() {
        Optional<UUID> uuid = entityData.get(OWNER_UUID_DATA);
        if (uuid.isEmpty()) return null;
        if (level() instanceof ServerLevel sl) {
            return sl.getPlayerByUUID(uuid.get());
        }
        return null;
    }

    public void setOwnerUUID(UUID uuid) {
        entityData.set(OWNER_UUID_DATA, Optional.of(uuid));
    }

    public Optional<UUID> getOwnerUUID() {
        return entityData.get(OWNER_UUID_DATA);
    }

    public void setSlotIndex(int slot) {
        entityData.set(SLOT_INDEX_DATA, slot);
    }

    public int getSlotIndex() {
        return entityData.get(SLOT_INDEX_DATA);
    }

    public boolean isInCombat() {
        return entityData.get(IN_COMBAT_DATA);
    }

    /** package-private：controller 用來強制設定戰鬥狀態。 */
    void setInCombat(boolean v) {
        entityData.set(IN_COMBAT_DATA, v);
    }

    public float getOrbitAngle() {
        return entityData.get(ORBIT_ANGLE);
    }

    /** package-private：controller 用來推進軌道角度。 */
    void setOrbitAngle(float angle) {
        entityData.set(ORBIT_ANGLE, angle);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        entityData.get(OWNER_UUID_DATA).ifPresent(uuid -> tag.putUUID("OwnerUUID", uuid));
        tag.putInt("SlotIndex", entityData.get(SLOT_INDEX_DATA));
        tag.putFloat("OrbitAngle", entityData.get(ORBIT_ANGLE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUUID")) {
            entityData.set(OWNER_UUID_DATA, Optional.of(tag.getUUID("OwnerUUID")));
        }
        entityData.set(SLOT_INDEX_DATA, tag.getInt("SlotIndex"));
        if (tag.contains("OrbitAngle")) {
            entityData.set(ORBIT_ANGLE, tag.getFloat("OrbitAngle"));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (cloneCtrl.isActive()) {
            CloneTurretController.HurtResult r = cloneCtrl.handleHurt(source, amount);
            if (r.blocked()) return false;
            return super.hurt(source, r.adjustedAmount());
        }
        if (source.getEntity() instanceof Player) return false;
        return super.hurt(source, playerCtrl.applyDamageReduction(amount));
    }

    // ── 對外 public API delegate ──────────────────────────────────────────────

    /** 取得本實體對應的浮游砲 ItemStack（裝備槽或手持）。 */
    public ItemStack getSourceStack(Player owner) {
        return playerCtrl.getSourceStack(owner);
    }

    /** 依升級重算最大血量（生成時呼叫）。 */
    public void applyUpgradesFromOwner(Player owner) {
        playerCtrl.applyUpgradesFromOwner(owner);
    }

    // ── 雜項覆寫 ──────────────────────────────────────────────────────────────

    @Override
    public boolean shouldBeSaved() {
        return false; // 不存盤：靠 FloatingTurretEventHandler 根據 DataAttachment 重建
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        // clone 模式對玩家不可選取：箭/砲彈/近戰 ray-cast 直接穿過
        return !cloneCtrl.isActive();
    }

    @Override
    @Nullable
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    @Nullable
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public boolean isNoAi() {
        return true;
    }
}
