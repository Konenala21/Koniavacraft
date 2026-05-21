package com.github.nalamodikk.common.item.weapon;

import com.github.nalamodikk.common.entity.FloatingTurretProjectile;
import com.github.nalamodikk.common.event.FloatingTurretEventHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import com.github.nalamodikk.common.player.equipment.EquipmentType;
import com.github.nalamodikk.common.player.equipment.ISpecificEquipment;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FloatingTurretItem extends Item implements ISpecificEquipment {

    public static final int DEFAULT_MAX_MANA = 15000;

    // 單手普通發射
    private static final int HAND_MANA_COST = 120;
    private static final float HAND_ATTACK_DAMAGE = 8.0F;

    // 雙持蓄力發射（各手各扣）
    private static final int CHARGED_MANA_COST_EACH = 500;
    private static final float CHARGED_DAMAGE_MIN = 16.0F;
    private static final float CHARGED_DAMAGE_MAX = 24.0F;
    public static final int MAX_CHARGE_TICKS = 40;    // 2 秒滿蓄
    private static final int QUICK_SHOT_THRESHOLD = 5; // 5 tick 內放開 = 雙普通彈

    public FloatingTurretItem(Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentType getEquipmentType() {
        return EquipmentType.TURRET;
    }

    private static boolean isDualWielding(Player player) {
        return player.getMainHandItem().getItem() instanceof FloatingTurretItem
                && player.getOffhandItem().getItem() instanceof FloatingTurretItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (isDualWielding(player)) {
            // 雙持：檢查最低魔力，進入蓄力狀態
            // 關鍵：必須明確呼叫 startUsingItem（同 BowItem），否則 releaseUsing 永遠不觸發
            int mainMana = player.getMainHandItem().getOrDefault(ModDataComponents.MANA_STORED, 0);
            int offMana  = player.getOffhandItem().getOrDefault(ModDataComponents.MANA_STORED, 0);
            if (mainMana < HAND_MANA_COST && offMana < HAND_MANA_COST) {
                return InteractionResultHolder.fail(stack);
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        } else {
            // 單手：立即發射
            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                fireNormalShot(serverLevel, player, stack, hand);
            }
            return InteractionResultHolder.success(stack);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_CHARGE_TICKS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;
        if (!(level instanceof ServerLevel sl)) return;

        int ticksHeld = MAX_CHARGE_TICKS - remainingUseDuration;
        float chargeRatio = Math.min(1.0F, (float) ticksHeld / MAX_CHARGE_TICKS);

        // 音調隨蓄力緩升：0.5 → 0.9
        float pitch = 0.5F + chargeRatio * 0.4F;
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.8F, pitch);

        // 雙持才有充能粒子（單手無蓄力狀態）
        if (!isDualWielding(player)) return;
        spawnChargingEffect(sl, player, FloatingTurretEventHandler.HAND_MAIN_SLOT, chargeRatio);
        spawnChargingEffect(sl, player, FloatingTurretEventHandler.HAND_OFF_SLOT, chargeRatio);
    }

    private static void spawnChargingEffect(ServerLevel sl, Player player, int slotIndex, float ratio) {
        Vec3 muzzle = turretPos(player, slotIndex, ratio);
        RandomSource rng = player.getRandom();

        // 砲口光球：spread 隨蓄力擴大，粒子往外微微發光
        double spread = 0.04 + ratio * 0.10;
        sl.sendParticles(ParticleTypes.END_ROD,
                muzzle.x, muzzle.y, muzzle.z,
                2 + (int)(ratio * 3), spread, spread, spread, 0.01);

        // 從周圍往砲口聚集：count=0 → deltaXYZ 是精確速度方向
        int pullCount = 2 + (int)(ratio * 3);
        double pullRadius = 0.7 + ratio * 0.6;
        for (int i = 0; i < pullCount; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            double b = rng.nextDouble() * Math.PI;
            double ox = Math.sin(b) * Math.cos(a) * pullRadius;
            double oy = Math.cos(b) * pullRadius * 0.6;
            double oz = Math.sin(b) * Math.sin(a) * pullRadius;
            Vec3 from = muzzle.add(ox, oy, oz);
            Vec3 dir  = muzzle.subtract(from).normalize();
            sl.sendParticles(ParticleTypes.END_ROD,
                    from.x, from.y, from.z,
                    0, dir.x, dir.y, dir.z, 0.10 + ratio * 0.10);
        }

        // 蓄力 70% 以上：砲口出現電弧閃爍
        if (ratio > 0.7F) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    muzzle.x, muzzle.y, muzzle.z,
                    3, 0.08, 0.08, 0.08, 0.04);
        }
    }

    private static void stopChargeSound(Player player) {
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundStopSoundPacket(
                    ResourceLocation.withDefaultNamespace("block.beacon.ambient"), SoundSource.PLAYERS));
        }
    }

    // 雙持：放開右鍵時依持續時間決定模式
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int ticksRemaining) {
        if (!(entity instanceof Player player) || level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        stopChargeSound(player);

        int mainMana = player.getMainHandItem().getOrDefault(ModDataComponents.MANA_STORED, 0);
        int offMana  = player.getOffhandItem().getOrDefault(ModDataComponents.MANA_STORED, 0);

        int ticksHeld = MAX_CHARGE_TICKS - ticksRemaining;
        if (ticksHeld < QUICK_SHOT_THRESHOLD) {
            // 快速點擊：兩把同時普通射
            if (mainMana < HAND_MANA_COST && offMana < HAND_MANA_COST) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5F, 1.2F);
                return;
            }
            fireDualNormalShot(serverLevel, player);
        } else {
            // 長按：依蓄力比例發射強化彈；魔力不足則降格為雙普通彈
            float chargeRatio = Math.min(1.0F, (float) ticksHeld / MAX_CHARGE_TICKS);
            if (mainMana >= CHARGED_MANA_COST_EACH && offMana >= CHARGED_MANA_COST_EACH) {
                fireChargedShot(serverLevel, player, chargeRatio);
            } else {
                fireDualNormalShot(serverLevel, player);
            }
        }
    }

    // 雙持：滿蓄力自動觸發
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !level.isClientSide && level instanceof ServerLevel serverLevel) {
            stopChargeSound(player);
            fireChargedShot(serverLevel, player, 1.0F);
        }
        return stack;
    }

    // 計算手持浮游砲的世界位置，chargeRatio 對應蓄力收攏動畫（與 FloatingTurretPlayerRenderer 相同的數學）
    private static Vec3 turretPos(Player player, int slotIndex, float chargeRatio) {
        boolean isMainHand = slotIndex == FloatingTurretEventHandler.HAND_MAIN_SLOT;
        boolean isLeftHanded = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
        double mainHandSide = isLeftHanded ? -1.0 : 1.0;
        double side = isMainHand ? mainHandSide : -mainHandSide;

        float yawRad = player.getYRot() * (float) (Math.PI / 180.0);
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);

        double sideMult = 1.0 - chargeRatio * 0.6;
        double fwdBoost = chargeRatio * 0.4;

        double translateCorrect = (1.0 - 0.0843) * 0.35; // align projectile origin to visual center
        return new Vec3(
                player.getX() + rightX * side * 1.8 * sideMult + forwardX * (1.5 + fwdBoost) + rightX * translateCorrect,
                player.getEyeY() + 0.8,
                player.getZ() + rightZ * side * 1.8 * sideMult + forwardZ * (1.5 + fwdBoost) + rightZ * translateCorrect);
    }

    // 雙持快按：各從自己砲管位置射出，方向 raycast 自動校準準心
    private void fireDualNormalShot(ServerLevel level, Player player) {
        ItemStack mainStack = player.getMainHandItem();
        ItemStack offStack  = player.getOffhandItem();
        boolean fired = false;

        int mainMana = mainStack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (mainMana >= HAND_MANA_COST) {
            level.addFreshEntity(FloatingTurretProjectile.shoot(level, player,
                    turretPos(player, FloatingTurretEventHandler.HAND_MAIN_SLOT, 0f)));
            mainStack.set(ModDataComponents.MANA_STORED, mainMana - HAND_MANA_COST);
            if (mainStack.isDamageableItem() && player instanceof ServerPlayer sp)
                mainStack.hurtAndBreak(1, sp, EquipmentSlot.MAINHAND);
            fired = true;
        }

        int offMana = offStack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (offMana >= HAND_MANA_COST) {
            level.addFreshEntity(FloatingTurretProjectile.shoot(level, player,
                    turretPos(player, FloatingTurretEventHandler.HAND_OFF_SLOT, 0f)));
            offStack.set(ModDataComponents.MANA_STORED, offMana - HAND_MANA_COST);
            if (offStack.isDamageableItem() && player instanceof ServerPlayer sp)
                offStack.hurtAndBreak(1, sp, EquipmentSlot.OFFHAND);
            fired = true;
        }

        if (fired) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
    }

    private void fireNormalShot(ServerLevel level, Player player, ItemStack stack, InteractionHand hand) {
        int mana = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (mana < HAND_MANA_COST) return;

        int slotIndex = hand == InteractionHand.MAIN_HAND
                ? FloatingTurretEventHandler.HAND_MAIN_SLOT
                : FloatingTurretEventHandler.HAND_OFF_SLOT;
        level.addFreshEntity(FloatingTurretProjectile.shoot(level, player, turretPos(player, slotIndex, 0f)));

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.6F, 1.6F);

        stack.set(ModDataComponents.MANA_STORED, mana - HAND_MANA_COST);

        if (stack.isDamageableItem() && player instanceof ServerPlayer sp) {
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, sp, slot);
        }
    }

    private void fireChargedShot(ServerLevel level, Player player, float chargeRatio) {
        ItemStack mainStack = player.getMainHandItem();
        ItemStack offStack  = player.getOffhandItem();

        int mainMana = mainStack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        int offMana  = offStack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (mainMana < CHARGED_MANA_COST_EACH || offMana < CHARGED_MANA_COST_EACH) return;

        mainStack.set(ModDataComponents.MANA_STORED, mainMana - CHARGED_MANA_COST_EACH);
        offStack.set(ModDataComponents.MANA_STORED, offMana - CHARGED_MANA_COST_EACH);

        if (player instanceof ServerPlayer sp) {
            if (mainStack.isDamageableItem()) mainStack.hurtAndBreak(2, sp, EquipmentSlot.MAINHAND);
            if (offStack.isDamageableItem())  offStack.hurtAndBreak(2, sp, EquipmentSlot.OFFHAND);
        }

        // 蓄力彈從兩砲收攏後的中心點出發，朝準心命中點
        Vec3 mainPos  = turretPos(player, FloatingTurretEventHandler.HAND_MAIN_SLOT, chargeRatio);
        Vec3 offPos   = turretPos(player, FloatingTurretEventHandler.HAND_OFF_SLOT, chargeRatio);
        Vec3 spawnPos = mainPos.add(offPos).scale(0.5);
        level.addFreshEntity(FloatingTurretProjectile.shootCharged(level, player, chargeRatio, spawnPos));

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F + chargeRatio * 0.6F);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        int mana    = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        int maxMana = stack.getOrDefault(ModDataComponents.MAX_MANA, DEFAULT_MAX_MANA);
        lines.add(Component.translatable("item.koniava.floating_turret.tooltip.mana", mana, maxMana));
        lines.add(Component.translatable("item.koniava.floating_turret.tooltip.hint"));
    }
}
