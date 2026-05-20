package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.common.event.FloatingTurretEventHandler;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import com.github.nalamodikk.register.ModDataAttachments;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FloatingTurretEntity extends PathfinderMob {

    private static final EntityDataAccessor<Float> ORBIT_ANGLE =
            SynchedEntityData.defineId(FloatingTurretEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID_DATA =
            SynchedEntityData.defineId(FloatingTurretEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> SLOT_INDEX_DATA =
            SynchedEntityData.defineId(FloatingTurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IN_COMBAT_DATA =
            SynchedEntityData.defineId(FloatingTurretEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float ORBIT_RADIUS = 1.5F;
    private static final float ORBIT_HEIGHT = 1.0F;
    private static final float ORBIT_SPEED = 0.04F;
    private static final int PASSIVE_ATTACK_COOLDOWN = 40;
    private static final float PASSIVE_RANGE = 16.0F;
    private static final int MANA_PER_ATTACK = 50;
    private static final float ATTACK_DAMAGE = 5.0F;
    private static final long COMBAT_LINGER_TICKS = 100L;

    private int attackTimer = 0;

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
            serverTick();
        }
    }

    private void serverTick() {
        Player owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.isSpectator()) {
            this.discard();
            return;
        }

        int slotIdx = entityData.get(SLOT_INDEX_DATA);

        // 手持模式（slot 2 = 主手, 3 = 副手）：確認玩家還持有浮游砲
        if (slotIdx >= 2) {
            ItemStack handItem = slotIdx == 2
                    ? owner.getMainHandItem()
                    : owner.getOffhandItem();
            if (handItem.isEmpty() || !(handItem.getItem() instanceof FloatingTurretItem)) {
                this.discard();
                return;
            }
        }

        // 更新軌道角度（裝備槽模式使用）
        float angle = entityData.get(ORBIT_ANGLE);
        angle += ORBIT_SPEED;
        if (angle > (float) (2 * Math.PI)) angle -= (float) (2 * Math.PI);
        entityData.set(ORBIT_ANGLE, angle);

        double posX, posY, posZ;

        if (slotIdx < 2) {
            // 裝備槽：繞玩家軌道運轉，兩顆相差 180°
            float slotPhase = slotIdx == 0 ? 0.0F : (float) Math.PI;
            posX = owner.getX() + Math.cos(angle + slotPhase) * ORBIT_RADIUS;
            posY = owner.getY() + ORBIT_HEIGHT;
            posZ = owner.getZ() + Math.sin(angle + slotPhase) * ORBIT_RADIUS;
        } else {
            // 手持：固定在玩家右上角（主手）或左上角（副手），跟著玩家朝向旋轉
            float yawRad = owner.getYRot() * (float) (Math.PI / 180.0);
            double rightX   = -Math.cos(yawRad);
            double rightZ   = -Math.sin(yawRad);
            double forwardX = -Math.sin(yawRad);
            double forwardZ =  Math.cos(yawRad);
            boolean isLeftHanded = owner.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
            double mainHandSide = isLeftHanded ? -1.0 : 1.0;
            double side = (slotIdx == 2) ? mainHandSide : -mainHandSide;

            posX = owner.getX() + rightX * side * 1.8 + forwardX * 1.5;
            posY = owner.getEyeY() + 0.8;
            posZ = owner.getZ() + rightZ * side * 1.8 + forwardZ * 1.5;
        }

        this.setPos(posX, posY, posZ);

        // 更新戰鬥狀態
        long lastCombat = owner.getData(ModDataAttachments.LAST_COMBAT_TIME.get());
        boolean inCombat = lastCombat >= 0 && (level().getGameTime() - lastCombat) < COMBAT_LINGER_TICKS;
        entityData.set(IN_COMBAT_DATA, inCombat);

        // 自動攻擊：僅裝備槽模式（slot 0, 1），手持模式靠右鍵主動攻擊
        if (slotIdx < 2 && attackTimer == 0 && inCombat) {
            LivingEntity target = findNearestHostile();
            if (target != null) {
                performPassiveAttack(owner, target);
                attackTimer = PASSIVE_ATTACK_COOLDOWN;
            }
        }
        if (attackTimer > 0) attackTimer--;
    }

    @Nullable
    private LivingEntity findNearestHostile() {
        List<Monster> hostiles = level().getEntitiesOfClass(
                Monster.class,
                this.getBoundingBox().inflate(PASSIVE_RANGE),
                LivingEntity::isAlive
        );
        return hostiles.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(this)))
                .orElse(null);
    }

    private void performPassiveAttack(Player owner, LivingEntity target) {
        int dataIdx = 8 + entityData.get(SLOT_INDEX_DATA);
        NonNullList<ItemStack> equipment = owner.getData(ModDataAttachments.EXTRA_EQUIPMENT.get());
        if (dataIdx >= equipment.size()) return;

        ItemStack stack = equipment.get(dataIdx);
        if (stack.isEmpty() || !(stack.getItem() instanceof FloatingTurretItem)) return;

        // 檢查魔力
        int mana = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (mana < MANA_PER_ATTACK) return;

        // 造成傷害
        target.hurt(level().damageSources().mobAttack(this), ATTACK_DAMAGE);

        // 消耗魔力
        stack.set(ModDataComponents.MANA_STORED, mana - MANA_PER_ATTACK);

        // 消耗耐久
        consumeDurability(owner, equipment, dataIdx, stack);
    }

    private void consumeDurability(Player owner, NonNullList<ItemStack> equipment, int dataIdx, ItemStack stack) {
        if (!stack.isDamageableItem()) return;
        int newDamage = stack.getDamageValue() + 1;
        if (newDamage >= stack.getMaxDamage()) {
            equipment.set(dataIdx, ItemStack.EMPTY);
            owner.setData(ModDataAttachments.EXTRA_EQUIPMENT.get(), equipment);
            this.discard();
        } else {
            stack.setDamageValue(newDamage);
        }
    }

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
            returnItemToOwner();
        }
    }

    private void returnItemToOwner() {
        Player owner = getOwnerPlayer();
        if (owner == null) return;

        int dataIdx = 8 + entityData.get(SLOT_INDEX_DATA);
        NonNullList<ItemStack> equipment = owner.getData(ModDataAttachments.EXTRA_EQUIPMENT.get());
        if (dataIdx >= equipment.size()) return;

        ItemStack stack = equipment.get(dataIdx);
        if (stack.isEmpty()) return;

        equipment.set(dataIdx, ItemStack.EMPTY);
        owner.setData(ModDataAttachments.EXTRA_EQUIPMENT.get(), equipment);

        if (!owner.getInventory().add(stack.copy())) {
            owner.drop(stack.copy(), false);
        }
    }

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

    public float getOrbitAngle() {
        return entityData.get(ORBIT_ANGLE);
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
        if (source.getEntity() instanceof Player) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean shouldBeSaved() {
        return false; // 不存盤：靠 FloatingTurretEventHandler 根據 DataAttachment 重建，避免重載後重複生成
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
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
