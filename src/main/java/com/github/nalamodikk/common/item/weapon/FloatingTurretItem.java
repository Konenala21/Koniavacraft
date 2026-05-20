package com.github.nalamodikk.common.item.weapon;

import com.github.nalamodikk.common.entity.FloatingTurretProjectile;
import com.github.nalamodikk.common.event.FloatingTurretEventHandler;
import com.github.nalamodikk.common.player.equipment.EquipmentType;
import com.github.nalamodikk.common.player.equipment.ISpecificEquipment;
import com.github.nalamodikk.register.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;
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
    private static final int MAX_CHARGE_TICKS = 40;    // 2 秒滿蓄
    private static final int QUICK_SHOT_THRESHOLD = 5; // 5 tick 內放開 = 雙普通彈

    public FloatingTurretItem(Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentType getEquipmentType() {
        return EquipmentType.TURRET;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.github.nalamodikk.client.renderer.item.FloatingTurretBEWLR.getInstance();
            }
        });
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

    // 充能中每 4 tick 播一次烽火台嗡嗡音效（vanilla onUseTick 固定每 4 tick 呼叫一次）
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide) return;
        int ticksHeld = MAX_CHARGE_TICKS - remainingUseDuration;
        // 音調低沉，隨蓄力緩升：0.5 → 0.9
        float pitch = 0.5F + (float) ticksHeld / MAX_CHARGE_TICKS * 0.4F;
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.8F, pitch);
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

    // 計算手持浮游砲的世界位置（與 FloatingTurretPlayerRenderer 相同的數學）
    private static Vec3 turretPos(Player player, int slotIndex) {
        boolean isMainHand = slotIndex == FloatingTurretEventHandler.HAND_MAIN_SLOT;
        boolean isLeftHanded = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
        double mainHandSide = isLeftHanded ? -1.0 : 1.0;
        double side = isMainHand ? mainHandSide : -mainHandSide;

        float yawRad = player.getYRot() * (float) (Math.PI / 180.0);
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);

        return new Vec3(
                player.getX() + rightX * side * 1.8 + forwardX * 1.5,
                player.getEyeY() + 0.8,
                player.getZ() + rightZ * side * 1.8 + forwardZ * 1.5);
    }

    // 雙持快按：各從自己砲管位置射出，方向 raycast 自動校準準心
    private void fireDualNormalShot(ServerLevel level, Player player) {
        ItemStack mainStack = player.getMainHandItem();
        ItemStack offStack  = player.getOffhandItem();
        boolean fired = false;

        int mainMana = mainStack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (mainMana >= HAND_MANA_COST) {
            level.addFreshEntity(FloatingTurretProjectile.shoot(level, player,
                    turretPos(player, FloatingTurretEventHandler.HAND_MAIN_SLOT)));
            mainStack.set(ModDataComponents.MANA_STORED, mainMana - HAND_MANA_COST);
            if (mainStack.isDamageableItem() && player instanceof ServerPlayer sp)
                mainStack.hurtAndBreak(1, sp, EquipmentSlot.MAINHAND);
            fired = true;
        }

        int offMana = offStack.getOrDefault(ModDataComponents.MANA_STORED, 0);
        if (offMana >= HAND_MANA_COST) {
            level.addFreshEntity(FloatingTurretProjectile.shoot(level, player,
                    turretPos(player, FloatingTurretEventHandler.HAND_OFF_SLOT)));
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
        level.addFreshEntity(FloatingTurretProjectile.shoot(level, player, turretPos(player, slotIndex)));

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

        // 蓄力彈從兩砲中心點出發，朝準心命中點
        Vec3 mainPos  = turretPos(player, FloatingTurretEventHandler.HAND_MAIN_SLOT);
        Vec3 offPos   = turretPos(player, FloatingTurretEventHandler.HAND_OFF_SLOT);
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
