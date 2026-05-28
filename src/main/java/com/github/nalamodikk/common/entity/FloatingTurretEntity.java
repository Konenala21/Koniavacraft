package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.common.event.FloatingTurretEventHandler;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeBehavior;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeItem;
import com.github.nalamodikk.register.ModDataAttachments;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
    private static final int CLONE_HAND_CHARGE_COOLDOWN = 70; // 分身手持模式蓄力週期
    private static final float PASSIVE_RANGE = 16.0F;
    private static final int MANA_PER_ATTACK = 50;
    private static final float ATTACK_DAMAGE = 5.0F;
    public static final long COMBAT_LINGER_TICKS = 200L; // 10 秒無戰鬥後解除

    private int attackTimer = 0;

    // 控制彈各自獨立冷卻（自走砲模式；實體每次戰鬥重建故 transient）
    private final Map<TurretUpgradeBehavior, Integer> controlCooldowns =
            new EnumMap<>(TurretUpgradeBehavior.class);

    // 分身砲模式：owner 是 boss 分身（非玩家），自帶鏡像過的砲 stack
    @Nullable
    private LivingEntity cloneOwner = null;
    private ItemStack cloneTurretStack = ItemStack.EMPTY;
    private int cloneShotCount = 0;

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
        if (cloneOwner != null) {
            cloneServerTick();
            return;
        }
        Player owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.isSpectator()) {
            this.discard();
            return;
        }

        int slotIdx = entityData.get(SLOT_INDEX_DATA);

        // 手持模式：確認玩家還持有浮游砲
        if (slotIdx >= 2) {
            ItemStack handItem = slotIdx == 2 ? owner.getMainHandItem() : owner.getOffhandItem();
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
        float yawRad = owner.getYRot() * (float)(Math.PI / 180.0);

        if (slotIdx < 2) {
            // 保護使用者 Mk1：擋在玩家與最近敵人之間
            LivingEntity protectTarget = FloatingTurretItem.getUpgradeMk(
                    getSourceStack(owner), TurretUpgradeBehavior.PROTECT) >= 1 ? findNearestHostile() : null;
            if (protectTarget != null) {
                Vec3 dir = protectTarget.position().subtract(owner.position());
                if (dir.horizontalDistanceSqr() > 0.001) {
                    dir = dir.normalize();
                    posX = owner.getX() + dir.x * ORBIT_RADIUS;
                    posY = owner.getY() + ORBIT_HEIGHT;
                    posZ = owner.getZ() + dir.z * ORBIT_RADIUS;
                } else {
                    posX = owner.getX(); posY = owner.getY() + ORBIT_HEIGHT; posZ = owner.getZ();
                }
            } else {
                // 裝備槽：固定在玩家背後左右兩側
                float behindAngle = (float) Math.atan2(-Math.cos(yawRad), Math.sin(yawRad));
                float spread = (float)(Math.PI / 5);
                float finalAngle = behindAngle + (slotIdx == 0 ? -spread : spread);
                float bob = (float)(Math.sin(tickCount * 0.08) * 0.2);
                posX = owner.getX() + Math.cos(finalAngle) * ORBIT_RADIUS;
                posY = owner.getY() + ORBIT_HEIGHT + bob;
                posZ = owner.getZ() + Math.sin(finalAngle) * ORBIT_RADIUS;
            }
        } else {
            // 手持：側邊 1.8 + 前方 1.5（與 FloatingTurretPlayerRenderer 相同的數學）
            double rightX = -Math.cos(yawRad);
            double rightZ = -Math.sin(yawRad);
            double forwardX = -Math.sin(yawRad);
            double forwardZ =  Math.cos(yawRad);
            boolean isLeftHanded = owner.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
            double mainHandSide = isLeftHanded ? -1.0 : 1.0;
            double side = (slotIdx == 2) ? mainHandSide : -mainHandSide;
            double translateCorrect = (1.0 - 0.0843) * 0.35; // align hitbox to visual center (translate Z 0.0843→1.0)
            posX = owner.getX() + rightX * side * 1.8 + forwardX * 1.5 + rightX * translateCorrect;
            posY = owner.getEyeY() + 0.8;
            posZ = owner.getZ() + rightZ * side * 1.8 + forwardZ * 1.5 + rightZ * translateCorrect;
        }
        this.setPos(posX, posY, posZ);

        // 更新戰鬥狀態
        long lastCombat = owner.getData(ModDataAttachments.LAST_COMBAT_TIME.get());
        boolean inCombat = lastCombat >= 0 && (level().getGameTime() - lastCombat) < COMBAT_LINGER_TICKS;
        entityData.set(IN_COMBAT_DATA, inCombat);

        // 治療升級：定期回復擁有者（手持與自走砲皆適用）
        if (tickCount % FloatingTurretItem.HEAL_INTERVAL_TICKS == 0) {
            tryHealOwner(owner);
        }

        // 自走砲型態：戰鬥結束後立刻靜默消失，不等 EventHandler 20-tick 間隔
        if (slotIdx < 2 && !inCombat) {
            this.discard();
            return;
        }

        // 自動攻擊：僅裝備槽模式（slot 0, 1），手持模式靠右鍵主動攻擊
        if (slotIdx < 2 && inCombat) {
            LivingEntity target = findTarget(owner);
            if (target != null) {
                if (attackTimer == 0) {
                    performPassiveAttack(owner, target);
                    attackTimer = PASSIVE_ATTACK_COOLDOWN;
                }
                tickControlShots(owner, target);
            }
        }
        if (attackTimer > 0) attackTimer--;
    }

    // 控制彈：每種控制升級獨立冷卻，命中目標套用對應效果（普通彈照常）
    private void tickControlShots(Player owner, LivingEntity target) {
        ItemStack stack = getSourceStack(owner);
        if (!(stack.getItem() instanceof FloatingTurretItem)) return;
        if (!(level() instanceof ServerLevel sl)) return;
        for (ItemStack upg : FloatingTurretItem.getData(stack).upgrades().values()) {
            if (!(upg.getItem() instanceof TurretUpgradeItem tu)) continue;
            TurretUpgradeBehavior b = tu.getBehavior();
            if (!b.isControl() || b.getControlEffect() == null) continue;
            int cd = controlCooldowns.getOrDefault(b, 0);
            if (cd > 0) { controlCooldowns.put(b, cd - 1); continue; }
            Vec3 spawn = this.position().add(0, 0.2, 0);
            Vec3 tgt = target.getBoundingBox().getCenter();
            sl.addFreshEntity(FloatingTurretProjectile.shootControl(
                    sl, this, spawn, tgt, b.getControlEffect(), b.getControlDuration()));
            controlCooldowns.put(b, b.getControlCooldown());
        }
    }

    // 目標選取：預設最近敵對生物；裝有玩家鎖定升級時也納入鎖定的攻擊者玩家
    @Nullable
    private LivingEntity findTarget(Player owner) {
        LivingEntity nearest = findNearestHostile();
        ItemStack stack = getSourceStack(owner);
        if (stack.getItem() instanceof FloatingTurretItem
                && FloatingTurretItem.hasUpgrade(stack, TurretUpgradeBehavior.PLAYER_LOCK)
                && level() instanceof ServerLevel sl) {
            UUID lockedId = FloatingTurretEventHandler.getLockedAttacker(owner.getUUID());
            if (lockedId != null) {
                Player locked = sl.getPlayerByUUID(lockedId);
                if (locked != null && locked.isAlive() && locked != owner
                        && locked.distanceToSqr(this) <= PASSIVE_RANGE * PASSIVE_RANGE
                        && (nearest == null || locked.distanceToSqr(this) < nearest.distanceToSqr(this))) {
                    return locked;
                }
            }
        }
        return nearest;
    }

    // ── 分身砲模式 ────────────────────────────────────────────────────────────

    @javax.annotation.Nullable
    public LivingEntity getCloneOwner() {
        return cloneOwner;
    }

    public void setupAsCloneTurret(LivingEntity owner, ItemStack turretStack, int slotIndex) {
        this.cloneOwner = owner;
        this.cloneTurretStack = turretStack.copy();
        entityData.set(SLOT_INDEX_DATA, slotIndex);
        var attr = getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(100.0 + FloatingTurretItem.getHealthBonus(cloneTurretStack));
            setHealth(getMaxHealth());
        }
    }

    private void cloneServerTick() {
        if (cloneOwner == null || !cloneOwner.isAlive() || cloneOwner.isRemoved()) {
            this.discard();
            return;
        }
        // 二階段變身過場期間：boss 凍結 + 無敵，砲也跟著凍結不打、不繞、不射（畫面感一致）
        if (cloneOwner instanceof PlayerCloneEntity pc && pc.isPhase2Transitioning()) {
            return;
        }
        int slotIdx = entityData.get(SLOT_INDEX_DATA);

        float angle = entityData.get(ORBIT_ANGLE);
        angle += ORBIT_SPEED;
        if (angle > (float) (2 * Math.PI)) angle -= (float) (2 * Math.PI);
        entityData.set(ORBIT_ANGLE, angle);
        entityData.set(IN_COMBAT_DATA, true);

        if (slotIdx >= 2) {
            cloneHandTick(slotIdx);   // 鏡射「雙手持砲蓄力」
        } else {
            cloneOrbitTick(slotIdx);  // 鏡射「自走砲（繞行）」
        }
    }

    // 自走砲：繞行分身背後左右兩側，自動射擊（每 4 發蓄力）
    // 機甲狀態下，若結構模板有 LIME_WOOL 砲位則坐在那；否則 fallback 到頭頂繞行
    private void cloneOrbitTick(int slotIdx) {
        float yawRad = cloneOwner.getYRot() * (float) (Math.PI / 180.0);
        float bob = (float) (Math.sin(tickCount * 0.08) * 0.2);
        boolean mechaHead = cloneOwner instanceof PlayerCloneEntity pc && pc.isArmored();
        net.minecraft.world.phys.Vec3 mount = mechaHead
                ? ((PlayerCloneEntity) cloneOwner).getTurretMountOffset(slotIdx)
                : null;
        if (mount != null) {
            double sinY = Math.sin(yawRad), cosY = Math.cos(yawRad);
            double wx = mount.x * cosY - mount.z * sinY;
            double wz = mount.x * sinY + mount.z * cosY;
            this.setPos(
                    cloneOwner.getX() + wx,
                    cloneOwner.getY() + mount.y + bob * 0.5,
                    cloneOwner.getZ() + wz);
        } else {
            float behindAngle = (float) Math.atan2(-Math.cos(yawRad), Math.sin(yawRad));
            float spread = (float) (Math.PI / 5);
            float finalAngle = behindAngle + (slotIdx == 0 ? -spread : spread);
            float radius = mechaHead ? 0.9F : ORBIT_RADIUS;
            double yOffset = mechaHead ? (PlayerCloneEntity.ARMORED_HEAD_TOP_Y + 2.0) : ORBIT_HEIGHT;
            this.setPos(
                    cloneOwner.getX() + Math.cos(finalAngle) * radius,
                    cloneOwner.getY() + yOffset + bob,
                    cloneOwner.getZ() + Math.sin(finalAngle) * radius);
        }

        if (attackTimer == 0) {
            LivingEntity target = cloneTarget();
            if (target != null && level() instanceof ServerLevel sl) {
                cloneShotCount++;
                float charge = (cloneShotCount % 4 == 0) ? 1.0F : 0.0F;
                FloatingTurretProjectile proj = FloatingTurretProjectile.shootAt(
                        sl, cloneOwner, this.position(), target.getBoundingBox().getCenter(), charge);
                proj.setNoBlockDamage(true); // 分身砲不破壞 arena 地形
                sl.addFreshEntity(proj);
                attackTimer = PASSIVE_ATTACK_COOLDOWN;
            }
        }
        if (attackTimer > 0) attackTimer--;
    }

    // 手持模式：站在分身手邊，蓄力後發強化彈（鏡射玩家雙持蓄力）
    // 機甲狀態下，若結構模板有對應 LIME_WOOL 砲位則坐在那
    private void cloneHandTick(int slotIdx) {
        float yawRad = cloneOwner.getYRot() * (float) (Math.PI / 180.0);
        boolean mechaHead = cloneOwner instanceof PlayerCloneEntity pc && pc.isArmored();
        net.minecraft.world.phys.Vec3 mount = mechaHead
                ? ((PlayerCloneEntity) cloneOwner).getTurretMountOffset(slotIdx)
                : null;
        if (mount != null) {
            double sinY = Math.sin(yawRad), cosY = Math.cos(yawRad);
            double wx = mount.x * cosY - mount.z * sinY;
            double wz = mount.x * sinY + mount.z * cosY;
            this.setPos(
                    cloneOwner.getX() + wx,
                    cloneOwner.getY() + mount.y,
                    cloneOwner.getZ() + wz);
        } else {
            double rightX = -Math.cos(yawRad);
            double rightZ = -Math.sin(yawRad);
            double forwardX = -Math.sin(yawRad);
            double forwardZ = Math.cos(yawRad);
            boolean isLeftHanded = cloneOwner.getMainArm() == HumanoidArm.LEFT;
            double mainHandSide = isLeftHanded ? -1.0 : 1.0;
            double side = (slotIdx == 2) ? mainHandSide : -mainHandSide;
            this.setPos(
                    cloneOwner.getX() + rightX * side * 1.0 + forwardX * 1.2,
                    cloneOwner.getEyeY() + 0.3,
                    cloneOwner.getZ() + rightZ * side * 1.0 + forwardZ * 1.2);
        }

        if (attackTimer > 0) {
            // 蓄力末段的砲口集氣特效
            if (attackTimer <= 15 && level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 2, 0.08, 0.08, 0.08, 0.01);
            }
            attackTimer--;
        } else {
            LivingEntity target = cloneTarget();
            if (target != null && level() instanceof ServerLevel sl) {
                FloatingTurretProjectile proj = FloatingTurretProjectile.shootAt(
                        sl, cloneOwner, this.position(), target.getBoundingBox().getCenter(), 1.0F);
                proj.setNoBlockDamage(true); // 分身砲不破壞 arena 地形
                sl.addFreshEntity(proj);
                attackTimer = CLONE_HAND_CHARGE_COOLDOWN;
            }
        }
    }

    @Nullable
    private LivingEntity cloneTarget() {
        if (cloneOwner instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive()) {
            return mob.getTarget();
        }
        return level().getNearestPlayer(this, PASSIVE_RANGE);
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

        int mana = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (mana < MANA_PER_ATTACK) return;

        // 發射砲彈，同時重設戰鬥計時器（讓攻擊中的自走砲持續維持戰鬥狀態）
        owner.setData(ModDataAttachments.LAST_COMBAT_TIME.get(), level().getGameTime());
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            net.minecraft.world.phys.Vec3 targetPos = target.getBoundingBox().getCenter();
            FloatingTurretProjectile proj = FloatingTurretProjectile.shootAt(
                    sl, owner, this.position(), targetPos);
            sl.addFreshEntity(proj);
        }

        stack.set(ModDataComponents.MANA_STORED, mana - MANA_PER_ATTACK);
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

        int slotIdx = entityData.get(SLOT_INDEX_DATA);
        if (slotIdx >= 2) return; // 手持實體無裝備槽資料，不需要歸還物品

        int dataIdx = 8 + slotIdx;
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
        // 分身砲：玩家可以摧毀它（套用 DEFENSE 減傷）
        if (cloneOwner != null) {
            // 免疫同源浮游砲的傷害（含蓄力彈爆炸），不被自己或同伴的砲炸死
            if (source.getDirectEntity() instanceof FloatingTurretProjectile proj && proj.getOwner() == cloneOwner) {
                return false;
            }
            if (cloneTurretStack.getItem() instanceof FloatingTurretItem) {
                amount *= (1f - FloatingTurretItem.getDamageReduction(cloneTurretStack));
            }
            return super.hurt(source, amount);
        }
        if (source.getEntity() instanceof Player) return false;
        Player owner = getOwnerPlayer();
        if (owner != null) {
            ItemStack stack = getSourceStack(owner);
            if (stack.getItem() instanceof FloatingTurretItem) {
                amount *= (1f - FloatingTurretItem.getDamageReduction(stack));
            }
        }
        return super.hurt(source, amount);
    }

    // ── 升級插件套用 ──────────────────────────────────────────────────────────

    /** 取得本實體對應的浮游砲 ItemStack（裝備槽或手持）。 */
    public ItemStack getSourceStack(Player owner) {
        int slot = entityData.get(SLOT_INDEX_DATA);
        if (slot == FloatingTurretEventHandler.HAND_MAIN_SLOT) return owner.getMainHandItem();
        if (slot == FloatingTurretEventHandler.HAND_OFF_SLOT) return owner.getOffhandItem();
        NonNullList<ItemStack> equipment = owner.getData(ModDataAttachments.EXTRA_EQUIPMENT.get());
        int dataIdx = 8 + slot;
        return dataIdx < equipment.size() ? equipment.get(dataIdx) : ItemStack.EMPTY;
    }

    /** 依升級重算最大血量（生成時呼叫）。 */
    public void applyUpgradesFromOwner(Player owner) {
        ItemStack stack = getSourceStack(owner);
        if (stack.isEmpty() || !(stack.getItem() instanceof FloatingTurretItem)) return;
        var attr = getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(100.0 + FloatingTurretItem.getHealthBonus(stack));
            setHealth(getMaxHealth());
        }
    }

    private void tryHealOwner(Player owner) {
        ItemStack stack = getSourceStack(owner);
        if (stack.isEmpty() || !(stack.getItem() instanceof FloatingTurretItem)) return;
        int heal = FloatingTurretItem.getHealAmount(stack);
        if (heal <= 0) return;
        if (owner.getHealth() >= owner.getMaxHealth()) return;
        int mana = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (mana < FloatingTurretItem.HEAL_MANA_COST) return;
        owner.heal(heal);
        stack.set(ModDataComponents.MANA_STORED, mana - FloatingTurretItem.HEAL_MANA_COST);
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
