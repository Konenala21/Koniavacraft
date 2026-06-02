package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.register.ModEntities;
import com.github.nalamodikk.research.skill.SkillEffectOp;
import com.github.nalamodikk.research.skill.SkillTargeting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * 靈魄 ORBITAL 載體生成的靈球:繞著施放者轉一段時間,週期性對附近敵人脈衝技能效果。
 * 跟投射不同,它持續存在、跟著主人。瞬時/不存檔(ops 設在 spawn 後,不同步不持久),
 * 視覺靠 tick 噴粒子(實體本身用 NoopRenderer 隱形)。
 */
public class OrbitalOrbEntity extends Entity {

    private UUID ownerUUID;
    private LivingEntity ownerCache;
    private List<SkillEffectOp> ops = List.of();
    private float damage = 4.0F;
    private int maxLifetime = 200;
    private int life = 0;
    private double radius = 2.2;
    private double angle = 0.0;
    private static final double ANGULAR_SPEED = 0.18; // rad/tick
    private static final int PULSE_INTERVAL = 20;

    public OrbitalOrbEntity(EntityType<? extends OrbitalOrbEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** server 端生成一顆靈球(startAngle 讓多顆均勻分佈)。 */
    public static OrbitalOrbEntity spawn(ServerLevel level, LivingEntity owner, float damage,
                                         List<SkillEffectOp> ops, double startAngle, int maxLifetime) {
        OrbitalOrbEntity orb = new OrbitalOrbEntity(ModEntities.ORBITAL_ORB.get(), level);
        orb.ownerUUID = owner.getUUID();
        orb.ownerCache = owner;
        orb.ops = List.copyOf(ops);
        orb.damage = damage;
        orb.maxLifetime = maxLifetime;
        orb.angle = startAngle;
        Vec3 p = owner.position();
        orb.setPos(p.x + Math.cos(startAngle) * orb.radius, p.y + 1.0, p.z + Math.sin(startAngle) * orb.radius);
        level.addFreshEntity(orb);
        return orb;
    }

    private LivingEntity owner() {
        if (ownerCache != null && !ownerCache.isRemoved()) return ownerCache;
        if (ownerUUID != null && level() instanceof ServerLevel sl && sl.getEntity(ownerUUID) instanceof LivingEntity le) {
            ownerCache = le;
            return le;
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        LivingEntity owner = owner();
        if (owner == null || ++life > maxLifetime) { discard(); return; }

        angle += ANGULAR_SPEED;
        Vec3 c = owner.position().add(0, owner.getBbHeight() * 0.6, 0);
        double x = c.x + Math.cos(angle) * radius;
        double z = c.z + Math.sin(angle) * radius;
        setPos(x, c.y, z);

        if (level() instanceof ServerLevel sl) {
            if (life % 2 == 0) { // 每 2 tick 噴一次粒子就夠順,省一半封包
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, c.y, z, 2, 0.04, 0.04, 0.04, 0.0);
                sl.sendParticles(ParticleTypes.END_ROD, x, c.y, z, 1, 0.02, 0.02, 0.02, 0.0);
            }

            if (life % PULSE_INTERVAL == 0) {
                AABB box = new AABB(x - 2.0, c.y - 2.0, z - 2.0, x + 2.0, c.y + 2.0, z + 2.0);
                for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != owner && e.isAlive())) {
                    Vec3 diff = le.position().subtract(position());
                    Vec3 dir = SkillTargeting.safeDir(diff);
                    le.invulnerableTime = 0;
                    le.hurt(damageSources().indirectMagic(this, owner), damage);
                    for (SkillEffectOp op : ops) op.applyTo(sl, le, owner, dir, damage);
                }
            }
        }
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
