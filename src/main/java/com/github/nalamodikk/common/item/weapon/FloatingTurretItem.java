package com.github.nalamodikk.common.item.weapon;

import com.github.nalamodikk.common.entity.FloatingTurretProjectile;
import com.github.nalamodikk.common.event.FloatingTurretEventHandler;
import com.github.nalamodikk.common.item.upgrade.EquipmentUpgradeData;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeBehavior;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import com.github.nalamodikk.common.player.equipment.EquipmentType;
import com.github.nalamodikk.common.player.equipment.ISpecificEquipment;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.github.nalamodikk.register.ModSounds;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FloatingTurretItem extends Item implements ISpecificEquipment {

    public static final int DEFAULT_MAX_MANA = 15000;
    public static final int MAX_UPGRADE_SLOTS = 6;
    public static final int HEAL_INTERVAL_TICKS = 40;
    public static final int HEAL_MANA_COST = 300;

    // 單手普通發射
    private static final int HAND_MANA_COST = 120;
    private static final float HAND_ATTACK_DAMAGE = 8.0F;

    // 雙持蓄力發射（各手各扣）
    private static final int CHARGED_MANA_COST_EACH = 500;
    private static final float CHARGED_DAMAGE_MIN = 16.0F;
    private static final float CHARGED_DAMAGE_MAX = 24.0F;
    public static final int MAX_CHARGE_TICKS = 40;    // 2 秒滿蓄
    private static final int QUICK_SHOT_THRESHOLD = 5; // 5 tick 內放開 = 雙普通彈

    // 雙持蓄力鬆手三段：[0,MIN)=弱蓄力彈, [MIN,MAX)=控制彈（依裝的控制插件數等分選效果）, [MAX,1]=強蓄力彈
    // public：client 蓄力表 overlay 共用同一組閾值，顯示才會跟 server 判定一致
    public static final float CONTROL_BAND_MIN = 0.40f;
    public static final float CONTROL_BAND_MAX = 0.75f;

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
            // 單手：立即發射，加最小冷卻防止連點器濫用
            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                fireNormalShot(serverLevel, player, stack, hand);
                player.getCooldowns().addCooldown(stack.getItem(), 4);
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

    // 雙持：放開右鍵時依持續時間決定模式
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int ticksRemaining) {
        if (!(entity instanceof Player player) || level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        int mainMana = player.getMainHandItem().getOrDefault(ModDataComponents.MANA_STORED, 0);
        int offMana  = player.getOffhandItem().getOrDefault(ModDataComponents.MANA_STORED, 0);

        int ticksHeld = MAX_CHARGE_TICKS - ticksRemaining;
        if (ticksHeld < QUICK_SHOT_THRESHOLD) {
            // 快速點擊：兩把同時普通射
            if (mainMana < HAND_MANA_COST && offMana < HAND_MANA_COST) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.TURRET_NO_MANA.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
                return;
            }
            fireDualNormalShot(serverLevel, player);
        } else {
            // 長按：依蓄力鬆手位置三段發射（弱蓄力彈 / 控制彈 / 強蓄力彈）；魔力不足則降格雙普通彈
            float chargeRatio = Math.min(1.0F, (float) ticksHeld / MAX_CHARGE_TICKS);
            if (mainMana >= CHARGED_MANA_COST_EACH && offMana >= CHARGED_MANA_COST_EACH) {
                List<TurretUpgradeBehavior> controls =
                        installedControls(player.getMainHandItem(), player.getOffhandItem());
                // 鬆手落在控制區且有裝控制插件：依插件數等分，落在哪格就放那個控制效果
                if (chargeRatio >= CONTROL_BAND_MIN && chargeRatio < CONTROL_BAND_MAX && !controls.isEmpty()) {
                    int n = controls.size();
                    int idx = (int) ((chargeRatio - CONTROL_BAND_MIN) / (CONTROL_BAND_MAX - CONTROL_BAND_MIN) * n);
                    idx = Math.max(0, Math.min(n - 1, idx));
                    fireControlShot(serverLevel, player, controls.get(idx));
                } else {
                    fireChargedShot(serverLevel, player, chargeRatio);
                }
            } else {
                fireDualNormalShot(serverLevel, player);
            }
        }
    }

    // 雙持：滿蓄力自動觸發
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !level.isClientSide && level instanceof ServerLevel serverLevel) {
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
            level.addFreshEntity(makeShot(level, player, mainStack,
                    turretPos(player, FloatingTurretEventHandler.HAND_MAIN_SLOT, 0f), 0f));
            mainStack.set(ModDataComponents.MANA_STORED, mainMana - HAND_MANA_COST);
            if (mainStack.isDamageableItem() && player instanceof ServerPlayer sp)
                mainStack.hurtAndBreak(1, sp, EquipmentSlot.MAINHAND);
            fired = true;
        }

        int offMana = offStack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (offMana >= HAND_MANA_COST) {
            level.addFreshEntity(makeShot(level, player, offStack,
                    turretPos(player, FloatingTurretEventHandler.HAND_OFF_SLOT, 0f), 0f));
            offStack.set(ModDataComponents.MANA_STORED, offMana - HAND_MANA_COST);
            if (offStack.isDamageableItem() && player instanceof ServerPlayer sp)
                offStack.hurtAndBreak(1, sp, EquipmentSlot.OFFHAND);
            fired = true;
        }

        if (fired) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.TURRET_SHOOT.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    // ── 自動瞄準輔助 ──────────────────────────────────────────────────────────

    private static final double AUTO_AIM_RANGE = 32.0;
    private static final double AUTO_AIM_CONE = 0.5; // cos(60°)

    @org.jetbrains.annotations.Nullable
    private static Vec3 findAutoAimTarget(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        net.minecraft.world.entity.LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        var box = player.getBoundingBox().inflate(AUTO_AIM_RANGE);
        for (var m : level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, box,
                net.minecraft.world.entity.LivingEntity::isAlive)) {
            Vec3 toTarget = m.getBoundingBox().getCenter().subtract(eye);
            double dist = toTarget.length();
            if (dist > AUTO_AIM_RANGE || dist < 0.001) continue;
            if (look.dot(toTarget.scale(1.0 / dist)) < AUTO_AIM_CONE) continue;
            if (dist < bestDist) { bestDist = dist; best = m; }
        }
        return best != null ? best.getBoundingBox().getCenter() : null;
    }

    private static FloatingTurretProjectile makeShot(ServerLevel level, Player player, ItemStack stack,
                                                     Vec3 spawnPos, float chargeRatio) {
        Vec3 target = hasUpgrade(stack, TurretUpgradeBehavior.AUTO_AIM)
                ? findAutoAimTarget(level, player) : null;
        FloatingTurretProjectile proj = target != null
                ? FloatingTurretProjectile.shootAt(level, player, spawnPos, target, chargeRatio)
                : (chargeRatio > 0
                        ? FloatingTurretProjectile.shootCharged(level, player, chargeRatio, spawnPos)
                        : FloatingTurretProjectile.shoot(level, player, spawnPos));
        if (hasUpgrade(stack, TurretUpgradeBehavior.NO_BLOCK_DAMAGE)) proj.setNoBlockDamage(true);
        return proj;
    }

    private void fireNormalShot(ServerLevel level, Player player, ItemStack stack, InteractionHand hand) {
        int mana = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (mana < HAND_MANA_COST) return;

        int slotIndex = hand == InteractionHand.MAIN_HAND
                ? FloatingTurretEventHandler.HAND_MAIN_SLOT
                : FloatingTurretEventHandler.HAND_OFF_SLOT;
        level.addFreshEntity(makeShot(level, player, stack, turretPos(player, slotIndex, 0f), 0f));

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TURRET_SHOOT.get(), SoundSource.PLAYERS, 0.6F, 1.0F);

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
        Vec3 aimTarget = (hasUpgrade(mainStack, TurretUpgradeBehavior.AUTO_AIM)
                || hasUpgrade(offStack, TurretUpgradeBehavior.AUTO_AIM))
                ? findAutoAimTarget(level, player) : null;
        FloatingTurretProjectile chargedProj = aimTarget != null
                ? FloatingTurretProjectile.shootAt(level, player, spawnPos, aimTarget, chargeRatio)
                : FloatingTurretProjectile.shootCharged(level, player, chargeRatio, spawnPos);
        if (hasUpgrade(mainStack, TurretUpgradeBehavior.NO_BLOCK_DAMAGE)
                || hasUpgrade(offStack, TurretUpgradeBehavior.NO_BLOCK_DAMAGE)) {
            chargedProj.setNoBlockDamage(true);
        }
        level.addFreshEntity(chargedProj);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TURRET_SHOOT_CHARGED.get(), SoundSource.PLAYERS, 1.2F, 0.7F + chargeRatio * 0.4F);
    }

    // 兩手已裝的控制升級聯集，按 enum ordinal 固定排序。
    // 順序固定（不隨槽位變）才能讓蓄力表左→右穩定，玩家靠肌肉記憶選效果。
    public static List<TurretUpgradeBehavior> installedControls(ItemStack mainStack, ItemStack offStack) {
        List<TurretUpgradeBehavior> result = new ArrayList<>();
        collectControls(mainStack, result);
        collectControls(offStack, result);
        result.sort(Comparator.comparingInt(Enum::ordinal));
        return result;
    }

    private static void collectControls(ItemStack stack, List<TurretUpgradeBehavior> out) {
        if (!(stack.getItem() instanceof FloatingTurretItem)) return;
        for (ItemStack upg : getData(stack).upgrades().values()) {
            if (upg.getItem() instanceof TurretUpgradeItem tu) {
                TurretUpgradeBehavior b = tu.getBehavior();
                if (b.isControl() && b.getControlEffect() != null && !out.contains(b)) out.add(b);
            }
        }
    }

    // 控制彈：消耗與蓄力彈同級（雙手各扣魔力 + 耐久），單發中心彈帶指定控制效果，朝準心/自動瞄準目標
    private void fireControlShot(ServerLevel level, Player player, TurretUpgradeBehavior control) {
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

        Vec3 mainPos  = turretPos(player, FloatingTurretEventHandler.HAND_MAIN_SLOT, 1.0F);
        Vec3 offPos   = turretPos(player, FloatingTurretEventHandler.HAND_OFF_SLOT, 1.0F);
        Vec3 spawnPos = mainPos.add(offPos).scale(0.5);
        // 控制彈朝準心：優先自動瞄準目標，否則玩家視線前方
        Vec3 aimTarget = (hasUpgrade(mainStack, TurretUpgradeBehavior.AUTO_AIM)
                || hasUpgrade(offStack, TurretUpgradeBehavior.AUTO_AIM))
                ? findAutoAimTarget(level, player) : null;
        if (aimTarget == null) {
            aimTarget = player.getEyePosition().add(player.getLookAngle().scale(AUTO_AIM_RANGE));
        }
        FloatingTurretProjectile proj = FloatingTurretProjectile.shootControl(
                level, player, spawnPos, aimTarget, control.getControlEffect(), control.getControlDuration());
        if (hasUpgrade(mainStack, TurretUpgradeBehavior.NO_BLOCK_DAMAGE)
                || hasUpgrade(offStack, TurretUpgradeBehavior.NO_BLOCK_DAMAGE)) {
            proj.setNoBlockDamage(true);
        }
        level.addFreshEntity(proj);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.TURRET_SHOOT_CHARGED.get(), SoundSource.PLAYERS, 1.0F, 1.3F);
    }

    // ── 升級插件 ──────────────────────────────────────────────────────────────

    public static EquipmentUpgradeData getData(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.TURRET_UPGRADE_DATA, EquipmentUpgradeData.empty());
    }

    public static void setData(ItemStack stack, EquipmentUpgradeData data) {
        stack.set(ModDataComponents.TURRET_UPGRADE_DATA, data);
    }

    public int getMaxUpgradeSlots() {
        return MAX_UPGRADE_SLOTS;
    }

    public boolean isValidUpgradeItem(ItemStack stack) {
        return stack.getItem() instanceof TurretUpgradeItem;
    }

    public String getUpgradeBehaviorKey(ItemStack upgradeStack) {
        if (upgradeStack.getItem() instanceof TurretUpgradeItem tu) return tu.getBehavior().name();
        return "";
    }

    private static int sumBonus(ItemStack turret, TurretUpgradeBehavior behavior) {
        int total = 0;
        for (ItemStack upg : getData(turret).upgrades().values()) {
            if (upg.getItem() instanceof TurretUpgradeItem tu && tu.getBehavior() == behavior) {
                total += tu.getBehavior().getBonusForMk(tu.getMk());
            }
        }
        return total;
    }

    public static void recalculateMaxMana(ItemStack turret) {
        int max = DEFAULT_MAX_MANA + sumBonus(turret, TurretUpgradeBehavior.CAPACITY);
        turret.set(ModDataComponents.MAX_MANA, max);
        int stored = turret.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (stored > max) turret.set(ModDataComponents.MANA_STORED, max);
    }

    /** 自走砲實體額外血量（HEALTH 升級總和）。 */
    public static int getHealthBonus(ItemStack turret) {
        return sumBonus(turret, TurretUpgradeBehavior.HEALTH);
    }

    /** 傷害減免比例 0..1（DEFENSE 升級總和，上限 90%）。 */
    public static float getDamageReduction(ItemStack turret) {
        int pct = sumBonus(turret, TurretUpgradeBehavior.DEFENSE);
        return Math.min(0.90f, pct / 100f);
    }

    /** 每個治療週期回復的血量（HEALING 升級總和），0 表示未安裝。 */
    public static int getHealAmount(ItemStack turret) {
        return sumBonus(turret, TurretUpgradeBehavior.HEALING);
    }

    public static boolean hasUpgrade(ItemStack turret, TurretUpgradeBehavior behavior) {
        for (ItemStack upg : getData(turret).upgrades().values()) {
            if (upg.getItem() instanceof TurretUpgradeItem tu && tu.getBehavior() == behavior) return true;
        }
        return false;
    }

    /** 已安裝該升級的最高 Mk，未安裝回 -1。 */
    public static int getUpgradeMk(ItemStack turret, TurretUpgradeBehavior behavior) {
        int mk = -1;
        for (ItemStack upg : getData(turret).upgrades().values()) {
            if (upg.getItem() instanceof TurretUpgradeItem tu && tu.getBehavior() == behavior) {
                mk = Math.max(mk, tu.getMk());
            }
        }
        return mk;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        int mana    = stack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        int maxMana = stack.getOrDefault(ModDataComponents.MAX_MANA, DEFAULT_MAX_MANA);
        lines.add(Component.translatable("item.koniava.floating_turret.tooltip.mana", mana, maxMana));
        int upgrades = (int) getData(stack).upgrades().values().stream().filter(s -> !s.isEmpty()).count();
        if (upgrades > 0) {
            lines.add(Component.translatable("tooltip.koniava.floating_turret.upgrades", upgrades, MAX_UPGRADE_SLOTS));
        }
        lines.add(Component.translatable("item.koniava.floating_turret.tooltip.hint"));
    }
}
