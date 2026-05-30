package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.event.VoidMirrorEvents;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.github.nalamodikk.common.entity.PlayerCloneEntity.Phase;

/**
 * Boss 血量階段狀態機 + 階段限定行為 controller（從 PlayerCloneEntity 抽出）。
 *
 * 涵蓋：血量驅動的 NORMAL/WALLING/BERSERK 階段切換、暴走時拆牆 + 加速、
 * WALLING 階段封路築牆、BERSERK 階段吸取玩家魔力。
 * 擁有 phase + 築牆/吸魔的冷卻 state。
 *
 * 跨子系統借用（主人仍是本體）：placedWalls（牆系統共享，PlayerCloneCombatSkills 也讀）、
 * takeWallBlock()（從 clonedInventory 取材的共用 helper）。
 */
class PlayerClonePhaseAI {

    private static final int WALL_CAP = 24;
    private static final int WALL_INTERVAL = 25;
    private static final int DRAIN_INTERVAL = 20;
    private static final int DRAIN_AMOUNT = 300;

    private static final ResourceLocation BERSERK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "clone_berserk_speed");
    private static final AttributeModifier BERSERK_SPEED =
            new AttributeModifier(BERSERK_SPEED_ID, 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private final PlayerCloneEntity clone;

    private Phase phase = Phase.NORMAL;
    private int wallCooldown = 0;
    private int drainCooldown = 0;

    PlayerClonePhaseAI(PlayerCloneEntity clone) {
        this.clone = clone;
    }

    boolean isWalling() {
        return phase == Phase.WALLING;
    }

    boolean isBerserk() {
        return phase == Phase.BERSERK;
    }

    int phaseOrdinal() {
        return phase.ordinal();
    }

    void loadPhaseOrdinal(int ord) {
        Phase[] values = Phase.values();
        phase = ord >= 0 && ord < values.length ? values[ord] : Phase.NORMAL;
    }

    void updatePhase() {
        float r = clone.getHealth() / clone.getMaxHealth();
        Phase next = r > 0.6F ? Phase.NORMAL : (r > 0.3F ? Phase.WALLING : Phase.BERSERK);
        if (next != phase) {
            phase = next;
            if (next == Phase.BERSERK) onEnterBerserk();
        }
    }

    private void onEnterBerserk() {
        // 全力進攻：拆掉自己建的所有牆，並加速
        removeAllWalls();
        AttributeInstance speed = clone.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(BERSERK_SPEED_ID)) {
            speed.addPermanentModifier(BERSERK_SPEED);
        }
    }

    void tickWallBuilding() {
        if (wallCooldown > 0) { wallCooldown--; return; }
        if (!(clone.level() instanceof ServerLevel sl)) return;
        if (clone.placedWalls.size() >= WALL_CAP) return;
        LivingEntity target = clone.getTarget();
        if (!(target instanceof Player)) return;
        if (clone.distanceToSqr(target) > 64.0) return; // 8 格內才封路

        Vec3 toTarget = target.position().subtract(clone.position());
        Direction dir = Direction.getNearest(toTarget.x, 0.0, toTarget.z);
        // 封住玩家遠離分身那一側的退路
        BlockPos base = target.blockPosition().relative(dir);

        BlockState wall = clone.takeWallBlock();
        boolean placed = false;
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos p = base.above(dy);
            if (!sl.getWorldBorder().isWithinBounds(p)) continue;
            if (!sl.getBlockState(p).canBeReplaced()) continue;
            sl.setBlockAndUpdate(p, wall);
            clone.placedWalls.add(p.asLong());
            VoidMirrorEvents.addModifiedBlock(p.asLong());
            placed = true;
        }
        if (placed) {
            sl.playSound(null, base, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 0.6F, 1.0F);
        }
        wallCooldown = WALL_INTERVAL;
    }

    private void removeAllWalls() {
        if (!(clone.level() instanceof ServerLevel sl)) return;
        for (long l : clone.placedWalls) {
            BlockPos p = BlockPos.of(l);
            if (sl.isLoaded(p)) {
                sl.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            }
        }
        clone.placedWalls.clear();
    }

    void tickManaDrain() {
        if (drainCooldown > 0) { drainCooldown--; return; }
        drainCooldown = DRAIN_INTERVAL;
        if (!(clone.level() instanceof ServerLevel sl)) return;
        LivingEntity target = clone.getTarget();
        if (!(target instanceof Player player)) return;
        if (clone.distanceToSqr(target) > 256.0) return; // 16 格內

        boolean drained = false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack st = player.getItemBySlot(slot);
            int mana = st.getOrDefault(ModDataComponents.MANA_STORED, 0);
            if (mana > 0) {
                st.set(ModDataComponents.MANA_STORED, Math.max(0, mana - DRAIN_AMOUNT));
                drained = true;
            }
        }
        if (drained) {
            sl.sendParticles(ParticleTypes.SCULK_SOUL,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    8, 0.3, 0.5, 0.3, 0.02);
        }
    }
}
