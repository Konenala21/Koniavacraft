package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.common.event.VoidMirrorEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Boss 主動戰術 controller（從 PlayerCloneEntity 抽出）。
 *
 * 涵蓋：反墊柱拆除、防禦工事拆除、俯衝重接戰（5 階段）、招牌墊方塊衝撞技能（RAM_WALL / LIFT_UP /
 * CHARGE_RAMP，含前搖預兆 + 閃避判定）、RAM_WALL 第二段上彈、技能墊方塊的依序清理。
 * 擁有所有技能 state field。
 *
 * 跨子系統借用（主人仍是本體）：placedWalls（牆系統，避免拆到自己疊的方塊）、takeWallBlock()（牆系統的取材）、
 * introActive / phase2TransitionTicks（過場期間不觸發俯衝）、entityData TELEGRAPH_SKILL / SKILL_TARGET（透過 clone 的 setter 同步給 client 畫預兆）。
 */
class PlayerCloneCombatSkills {

    enum PillarSkill { RAM_WALL, LIFT_UP, CHARGE_RAMP }

    private static final int PILLAR_HEIGHT_TRIGGER = 3;

    private static final int SKILL_COOLDOWN = 80; // 技能頻繁（~4s 一次），壓迫靠技能而非高傷
    private static final int SKILL_TELEGRAPH = 20;  // 前搖 1 秒：站定蓄力 + 漸強預警，給玩家反應/閃避
    private static final double SKILL_RANGE_SQR = 144.0; // 12 格內才發
    private static final double SKILL_MIN_SQR = 4.0;     // 太近不發
    private static final double SKILL_DODGE_RADIUS = 2.5; // 前搖內跑出鎖定點這個距離即閃過

    private static final int DIVE_COOLDOWN = 300; // 15 秒一次（不要太頻繁）
    private static final double DIVE_DIST_TRIGGER_SQ = 12.0 * 12.0; // 12 格外觸發
    private static final double DIVE_HEIGHT_TRIGGER = 3.0;          // 高度差 > 3 觸發

    private static final int SKILL_BLOCK_LIFETIME = 20;

    private final PlayerCloneEntity clone;

    PlayerCloneCombatSkills(PlayerCloneEntity clone) {
        this.clone = clone;
    }

    private int breakCooldown = 0;
    private int antiPillarCooldown = 0;

    // 墊方塊衝撞技能狀態
    private PillarSkill pendingSkill = null;
    private int skillChargeTicks = 0;
    private int skillCooldown = 100; // 開場緩衝，不一進場就放

    // 俯衝技能 Phase 0=inactive, 1=蓄力(20t), 2=跳起(15t), 3=頂點鎖定目標(10t), 4=俯衝(<=20t), 5=落地(10t)
    private int divePhase = 0;
    private int divePhaseTicks = 0;
    private Vec3 diveTarget = null;
    private boolean diveIsRangeTrigger = false; // 距離觸發 = 蓄力縮短、跳更高更快
    private int diveCooldown = 60; // 開場 3 秒緩衝
    private BlockPos skillTargetPos = BlockPos.ZERO;      // 前搖時鎖定的地點（技能對此點生效，非鎖定玩家本人）

    // 技能墊的方塊：擊飛後 1 秒（20t）開始依序快速打掉
    private final List<Long> skillBlocks = new ArrayList<>();
    private int skillClearTimer = -1; // -1 閒置；>0 倒數；0 清理中（每 tick 打一格）

    // 分段擊飛（RAM_WALL）：先把玩家拋飛離地，數 tick 後玩家在空中時再強水平轟飛
    @Nullable private Player pendingLaunchTarget = null;
    private int pendingLaunchTimer = 0;
    private Vec3 pendingLaunchDir = Vec3.ZERO;

    // 查詢 / 重置：給 PlayerCloneEntity 的 customServerAiStep / tickMeleeStrafe / enterArmored 用
    boolean isDiving() { return divePhase > 0; }

    boolean hasPendingSkill() { return pendingSkill != null; }

    // 半血變身時清掉進行中的技能狀態，否則變身前正在前搖的招式會卡住、變身後一直重放同一招
    void resetForArmored() {
        pendingSkill = null;
        skillChargeTicks = 0;
        skillCooldown = SKILL_COOLDOWN;
        clone.setTelegraphSkill(0);
        pendingLaunchTarget = null;
        pendingLaunchTimer = 0;
    }

    void tickAntiPillar() {
        if (antiPillarCooldown > 0) { antiPillarCooldown--; return; }
        if (!(clone.level() instanceof ServerLevel sl)) return;
        LivingEntity target = clone.getTarget();
        if (!(target instanceof Player)) return;

        BlockPos support = target.blockPosition().below();
        if (support.getY() < 64) return; // 站在地形上、沒墊高 → 不處理

        int heightAbove = target.blockPosition().getY() - clone.blockPosition().getY();
        if (heightAbove >= PILLAR_HEIGHT_TRIGGER) {
            // 墊柱逃避：從腳下往下連拆，一次解決整段支撐，讓玩家直接摔回分身高度
            BlockPos.MutableBlockPos p = support.mutable();
            int broken = 0;
            for (int i = 0; i < 8 && p.getY() >= 64; i++) {
                if (!clone.placedWalls.contains(p.asLong())) {
                    BlockState st = sl.getBlockState(p);
                    if (!st.isAir() && st.getDestroySpeed(sl, p) >= 0) {
                        sl.destroyBlock(p, false);
                        broken++;
                    }
                }
                p.move(Direction.DOWN);
            }
            antiPillarCooldown = broken > 0 ? 4 : 8;
            return;
        }

        // 一般情形：拆腳下支撐一格
        BlockState st = sl.getBlockState(support);
        if (!st.isAir() && st.getDestroySpeed(sl, support) >= 0 && !clone.placedWalls.contains(support.asLong())) {
            sl.destroyBlock(support, false);
            antiPillarCooldown = 6;
        } else {
            antiPillarCooldown = 8;
        }
    }

    // 拆玩家蓋的防禦工事 / 逃跑路線（地表 Y>=64 以上、非分身自己疊的方塊）
    void tickBreakSurroundings() {
        if (breakCooldown > 0) { breakCooldown--; return; }
        if (!(clone.level() instanceof ServerLevel sl)) return;
        LivingEntity target = clone.getTarget();
        if (!(target instanceof Player)) return;

        BlockPos center = target.blockPosition();
        BlockPos found = null;
        double best = Double.MAX_VALUE;
        int r = 5;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    p.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (p.getY() < 64) continue;                 // 不拆地形
                    if (clone.placedWalls.contains(p.asLong())) continue; // 不拆自己的牆
                    BlockState st = sl.getBlockState(p);
                    if (st.isAir()) continue;
                    if (st.getDestroySpeed(sl, p) < 0) continue;  // 不可破壞跳過
                    double d = p.distSqr(center);
                    if (d < best) { best = d; found = p.immutable(); }
                }
            }
        }
        if (found != null) {
            sl.destroyBlock(found, false); // 不掉落，避免玩家撿回
        }
        breakCooldown = 10;
    }

    // 俯衝重接戰：玩家躲太遠 / 太高 / 卡住打不到 → boss 跳起來俯衝回玩家面前
    // 5 階段：WINDUP 蓄力 → JUMP 跳起 → APEX 頂點鎖定 → DIVE 俯衝 → LAND 落地
    void tickDive() {
        if (diveCooldown > 0) diveCooldown--;
        if (!(clone.level() instanceof ServerLevel sl)) return;
        LivingEntity target = clone.getTarget();

        // === 進行中：分階段處理 ===
        if (divePhase > 0) {
            divePhaseTicks++;
            switch (divePhase) {
                case 1 -> { // WINDUP：距離觸發 5t (0.25s)，高度觸發 20t (1s)
                    int windup = diveIsRangeTrigger ? 5 : 20;
                    clone.setDeltaMovement(0, clone.getDeltaMovement().y, 0);
                    if (divePhaseTicks % 2 == 0) {
                        sl.sendParticles(ParticleTypes.FLAME, clone.getX(), clone.getY() + 0.5, clone.getZ(),
                                3, 0.5, 0.3, 0.5, 0.05);
                    }
                    if (divePhaseTicks >= windup) { divePhase = 2; divePhaseTicks = 0;
                        sl.playSound(null, clone.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP,
                                SoundSource.HOSTILE, 1.2F, 0.7F);
                        // 距離觸發 = 跳更高（飛得更遠才打得到），高度觸發 = 標準
                        clone.setDeltaMovement(0, diveIsRangeTrigger ? 2.5 : 1.8, 0);
                    }
                }
                case 2 -> { // JUMP 15t：飛上去，每 tick 補一點上升力
                    if (clone.getDeltaMovement().y < 0.5) clone.setDeltaMovement(clone.getDeltaMovement().x, 0.5, clone.getDeltaMovement().z);
                    if (divePhaseTicks >= 15) { divePhase = 3; divePhaseTicks = 0; }
                }
                case 3 -> { // APEX 10t：頂點靜止 + 鎖定玩家當下位置
                    clone.setDeltaMovement(0, 0, 0);
                    clone.fallDistance = 0; // 防 apex 期間累積墜落傷害
                    if (divePhaseTicks == 1 && target != null) {
                        diveTarget = target.position();
                    }
                    if (divePhaseTicks >= 10) { divePhase = 4; divePhaseTicks = 0;
                        sl.playSound(null, clone.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                                SoundSource.HOSTILE, 1.5F, 1.2F);
                    }
                }
                case 4 -> { // DIVE 最多 20t：往鎖定點俯衝
                    if (diveTarget != null) {
                        Vec3 dir = diveTarget.subtract(clone.position()).normalize();
                        clone.setDeltaMovement(dir.x * 1.2, -1.5, dir.z * 1.2); // 強下衝
                        if (divePhaseTicks % 2 == 0) {
                            sl.sendParticles(ParticleTypes.LARGE_SMOKE, clone.getX(), clone.getY(), clone.getZ(),
                                    2, 0.2, 0.2, 0.2, 0.05);
                        }
                    }
                    // 落地觸發：撞到地面 OR 接近鎖定點
                    if (clone.onGround() || divePhaseTicks >= 20) { divePhase = 5; divePhaseTicks = 0;
                        sl.playSound(null, clone.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                                SoundSource.HOSTILE, 1.5F, 0.8F);
                        sl.sendParticles(ParticleTypes.EXPLOSION, clone.getX(), clone.getY(), clone.getZ(),
                                1, 0, 0, 0, 0);
                        sl.sendParticles(ParticleTypes.CLOUD, clone.getX(), clone.getY() + 0.2, clone.getZ(),
                                30, 1.5, 0.3, 1.5, 0.2);
                        // 範圍擊飛
                        for (Player p : sl.getEntitiesOfClass(Player.class, clone.getBoundingBox().inflate(3))) {
                            if (p == target || target == null) {
                                Vec3 push = p.position().subtract(clone.position()).normalize();
                                p.push(push.x * 1.0, 0.6, push.z * 1.0);
                                p.hurt(clone.damageSources().mobAttack(clone), 4F); // 中等傷害
                            }
                        }
                        clone.fallDistance = 0; // 自己的墜落不算
                    }
                }
                case 5 -> { // LAND 10t：靜止恢復（給玩家也喘息）
                    clone.setDeltaMovement(0, clone.getDeltaMovement().y, 0);
                    if (divePhaseTicks >= 10) {
                        divePhase = 0;
                        diveTarget = null;
                        diveIsRangeTrigger = false;
                        diveCooldown = DIVE_COOLDOWN;
                    }
                }
            }
            return;
        }

        // === 觸發判定 ===
        if (diveCooldown > 0 || target == null || clone.phase2TransitionTicks > 0 || clone.introActive) return;
        double dsq = clone.distanceToSqr(target);
        double hdiff = target.getY() - clone.getY();
        boolean tooFar = dsq > DIVE_DIST_TRIGGER_SQ;
        boolean tooHigh = hdiff > DIVE_HEIGHT_TRIGGER && dsq > 16.0; // 不能太近就跳
        if (tooFar || tooHigh) {
            divePhase = 1;
            divePhaseTicks = 0;
            diveIsRangeTrigger = tooFar; // 距離觸發走快版（短蓄力 + 高跳），高度走標準版
            clone.setTelegraphSkill(4);
        }
    }

    void tickPillarSkill() {
        if (skillCooldown > 0) skillCooldown--;
        if (!(clone.level() instanceof ServerLevel sl)) return;
        if (!(clone.getTarget() instanceof Player p) || !p.isAlive()) {
            pendingSkill = null;
            clone.setTelegraphSkill(0);
            return;
        }

        if (pendingSkill != null) {
            // 前置動作：站定蓄力、面向玩家，朝玩家噴漸強預警粒子，給玩家 1 秒反應/閃避
            clone.setDeltaMovement(0.0, clone.getDeltaMovement().y, 0.0);
            clone.getNavigation().stop();
            clone.getLookControl().setLookAt(p);
            Vec3 d = horizUnit(p.position().subtract(clone.position()));
            float prog = 1.0f - skillChargeTicks / (float) SKILL_TELEGRAPH;
            int count = 4 + (int) (prog * 10); // 越接近發動越密
            sl.sendParticles(ParticleTypes.CRIT,
                    clone.getX() + d.x * 0.6, clone.getY() + 1.0, clone.getZ() + d.z * 0.6,
                    count, 0.3, 0.3, 0.3, 0.12 + prog * 0.1);
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    clone.getX(), clone.getY() + 1.2, clone.getZ(), 2, 0.4, 0.4, 0.4, 0.0);
            if (--skillChargeTicks <= 0) {
                executeSkill(sl, pendingSkill, p);
                pendingSkill = null;
                clone.setTelegraphSkill(0);
                skillCooldown = SKILL_COOLDOWN;
            }
            return;
        }

        if (skillCooldown > 0) return;
        double d2 = clone.distanceToSqr(p);
        if (d2 > SKILL_RANGE_SQR || d2 < SKILL_MIN_SQR) return;
        pendingSkill = PillarSkill.values()[clone.getRandom().nextInt(PillarSkill.values().length)];
        skillChargeTicks = SKILL_TELEGRAPH;
        skillTargetPos = p.blockPosition();                  // 鎖定當下地點：預兆固定於此，玩家跑出即可閃避
        clone.setSkillTarget(skillTargetPos);
        clone.setTelegraphSkill(pendingSkill.ordinal() + 1); // 同步給 client 畫預兆
        // 每招不同蓄力音，配合預兆讓玩家辨識
        SoundEvent windup = switch (pendingSkill) {
            case RAM_WALL -> SoundEvents.WARDEN_SONIC_CHARGE;
            case LIFT_UP -> SoundEvents.PISTON_EXTEND;
            case CHARGE_RAMP -> SoundEvents.RAVAGER_ROAR;
        };
        sl.playSound(null, clone.blockPosition(), windup, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private static Vec3 horizUnit(Vec3 v) {
        double len = Math.sqrt(v.x * v.x + v.z * v.z);
        if (len < 1.0e-4) return new Vec3(0, 0, 1);
        return new Vec3(v.x / len, 0, v.z / len);
    }

    // 水平面點(px,pz) 到線段 (ax,az)→(bx,bz) 的最短距離平方（CHARGE_RAMP 閃避用）
    private static double pointToSegmentDistSqrXZ(double px, double pz,
                                                  double ax, double az,
                                                  double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        double lenSq = dx * dx + dz * dz;
        if (lenSq < 1.0e-6) {
            double rx = px - ax, rz = pz - az;
            return rx * rx + rz * rz;
        }
        double t = ((px - ax) * dx + (pz - az) * dz) / lenSq;
        if (t < 0) t = 0;
        else if (t > 1) t = 1;
        double projX = ax + dx * t, projZ = az + dz * t;
        double rx = px - projX, rz = pz - projZ;
        return rx * rx + rz * rz;
    }

    // 對玩家施加擊退（起飛弧線）。蹲下大幅緩衝。複用 LeggingsDoubleJumpHandler 的速度同步寫法。
    private void knockbackPlayer(Player p, Vec3 awayDir, double horizPower, double vertPower) {
        double mult = p.isCrouching() ? 0.35 : 1.0;
        Vec3 h = horizUnit(awayDir);
        p.setDeltaMovement(h.x * horizPower * mult, vertPower * mult, h.z * horizPower * mult);
        p.hurtMarked = true; // server 同步速度給 client
    }

    // RAM_WALL 的第二段：拋飛後玩家在空中時，再強力水平轟飛（空中無摩擦才飛得遠，蹲下緩衝）
    void tickPendingLaunch() {
        if (pendingLaunchTimer <= 0) return;
        if (--pendingLaunchTimer > 0) return;
        Player p = pendingLaunchTarget;
        pendingLaunchTarget = null;
        if (p == null || !p.isAlive()) return;
        double mult = p.isCrouching() ? 0.35 : 1.0;
        Vec3 h = horizUnit(pendingLaunchDir);
        Vec3 m = p.getDeltaMovement();
        p.setDeltaMovement(h.x * 3.0 * mult, Math.max(m.y, 0.2), h.z * 3.0 * mult);
        p.hurtMarked = true;
    }

    private void executeSkill(ServerLevel sl, PillarSkill skill, Player p) {
        clearSkillBlocksNow(sl); // 先收掉上次技能殘留的方塊，避免堆積
        BlockPos target = skillTargetPos;                     // 前搖鎖定的地點（非玩家當下位置）
        Vec3 targetCenter = Vec3.atCenterOf(target);
        double dodgeRSq = SKILL_DODGE_RADIUS * SKILL_DODGE_RADIUS;
        // CHARGE_RAMP 是線狀攻擊（boss→鎖定點），用點到線段距離；其他用以鎖定點為中心的球體距離
        boolean dodged = skill == PillarSkill.CHARGE_RAMP
                ? pointToSegmentDistSqrXZ(p.getX(), p.getZ(),
                        clone.getX(), clone.getZ(), targetCenter.x, targetCenter.z) > dodgeRSq
                : (p.getX() - targetCenter.x) * (p.getX() - targetCenter.x)
                        + (p.getZ() - targetCenter.z) * (p.getZ() - targetCenter.z) > dodgeRSq;
        Vec3 awayFromClone = p.position().subtract(clone.position()); // 命中時的擊退方向（玩家當下位置）
        Vec3 d = horizUnit(targetCenter.subtract(clone.position()));
        BlockState block = clone.takeWallBlock();
        switch (skill) {
            case RAM_WALL -> {
                // 從鎖定點朝分身方向水平排 3 格方塊，命中的玩家往反方向擊退
                Direction toClone = Direction.getNearest(clone.getX() - targetCenter.x, 0, clone.getZ() - targetCenter.z);
                int height = 1 + clone.getRandom().nextInt(2);
                for (int i = 1; i <= 3; i++) {
                    BlockPos col = target.relative(toClone, i).above(1);
                    for (int dy = 0; dy < height; dy++) placeSkillBlock(sl, col.above(dy), block);
                }
                if (!dodged) {
                    knockbackPlayer(p, awayFromClone, 0.3, 0.85);
                    pendingLaunchTarget = p;
                    pendingLaunchDir = awayFromClone;
                    pendingLaunchTimer = 6;
                }
                sl.playSound(null, target, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 1.0F, 0.8F);
            }
            case LIFT_UP -> {
                // 鎖定點冒方塊往上頂飛命中的玩家
                placeSkillBlock(sl, target, block);
                placeSkillBlock(sl, target.above(), block);
                if (!dodged) knockbackPlayer(p, awayFromClone, 0.5, 1.5);
                sl.playSound(null, target, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 1.0F, 1.2F);
            }
            case CHARGE_RAMP -> {
                // 從分身身體高度往鎖定點方向排空中方塊 + 逼近一步，撞到的玩家強水平擊飛
                for (int i = 1; i <= 3; i++) {
                    BlockPos bp = BlockPos.containing(clone.getX() + d.x * i, clone.getY() + 1, clone.getZ() + d.z * i);
                    placeSkillBlock(sl, bp, block);
                }
                clone.setDeltaMovement(d.x * 0.8, clone.getDeltaMovement().y, d.z * 0.8); // 平滑衝刺逼近，不瞬移
                clone.hurtMarked = true;
                if (!dodged && clone.distanceToSqr(p) <= 25.0) knockbackPlayer(p, awayFromClone, 1.6, 0.6);
                sl.playSound(null, clone.blockPosition(), SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 1.0F, 0.6F);
            }
        }
        clone.setSkillTarget(BlockPos.ZERO); // 清鎖定點（client 停止畫預兆）
        skillClearTimer = SKILL_BLOCK_LIFETIME; // 擊飛後 1 秒開始依序打掉
    }

    private void placeSkillBlock(ServerLevel sl, BlockPos p, BlockState block) {
        if (!sl.getWorldBorder().isWithinBounds(p)) return;
        if (!sl.getBlockState(p).canBeReplaced()) return; // 只放在空氣/可替換處，不覆蓋地板或既有方塊
        sl.setBlockAndUpdate(p, block);
        long key = p.asLong();
        clone.placedWalls.add(key);
        skillBlocks.add(key);
        VoidMirrorEvents.addModifiedBlock(key);
    }

    // 技能墊的方塊：倒數後每 tick 依序打掉一格（快速、按放置順序）
    void tickSkillBlockClear() {
        if (skillClearTimer < 0) return;
        if (skillClearTimer > 0) { skillClearTimer--; return; }
        if (!(clone.level() instanceof ServerLevel sl)) return;
        if (skillBlocks.isEmpty()) { skillClearTimer = -1; return; }
        long key = skillBlocks.remove(0);
        BlockPos bp = BlockPos.of(key);
        sl.destroyBlock(bp, false);
        clone.placedWalls.remove(key);
        if (skillBlocks.isEmpty()) skillClearTimer = -1;
    }

    private void clearSkillBlocksNow(ServerLevel sl) {
        for (long key : skillBlocks) {
            BlockPos bp = BlockPos.of(key);
            if (sl.isLoaded(bp)) sl.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
            clone.placedWalls.remove(key);
        }
        skillBlocks.clear();
        skillClearTimer = -1;
    }
}
