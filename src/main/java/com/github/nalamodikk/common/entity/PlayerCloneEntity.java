package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.common.dimension.VoidMirrorSavedData;
import com.github.nalamodikk.common.event.VoidMirrorEvents;
import com.github.nalamodikk.common.item.upgrade.EquipmentUpgradeData;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeBehavior;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeItem;
import com.github.nalamodikk.register.ModDataAttachments;
import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.register.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PlayerCloneEntity extends Monster {

    public static final float MAX_HP = 300.0F;
    private static final float REFLECT_CHANCE = 0.25F;

    public enum Phase { NORMAL, WALLING, BERSERK }

    private static final int WALL_CAP = 24;
    private static final int WALL_INTERVAL = 25;
    private static final int DRAIN_INTERVAL = 20;
    private static final int DRAIN_AMOUNT = 300;

    private static final ResourceLocation BERSERK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "clone_berserk_speed");
    private static final AttributeModifier BERSERK_SPEED =
            new AttributeModifier(BERSERK_SPEED_ID, 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    // 防風箏：目標離太遠時暫時加速追上
    private static final double ANTI_KITE_DIST_SQR = 100.0; // >10 格
    private static final ResourceLocation ANTI_KITE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "clone_anti_kite_speed");
    private static final AttributeModifier ANTI_KITE_SPEED =
            new AttributeModifier(ANTI_KITE_SPEED_ID, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private Phase phase = Phase.NORMAL;
    private final Set<Long> placedWalls = new HashSet<>();
    private int wallCooldown = 0;
    private int drainCooldown = 0;
    private int breakCooldown = 0;
    private int antiPillarCooldown = 0;
    // 玩家高出分身達此格數 → 視為墊柱逃避，從腳下往下連拆整段支撐讓他摔回地面
    private static final int PILLAR_HEIGHT_TRIGGER = 3;

    // 進場演出（地底鑽出→飛高→浮動集氣→爆炸顯現裝備→降落→啟動），對齊過場時間軸（360t）
    private static final int INTRO_RISE_START = 255;
    private static final int INTRO_RISE_END = 290;
    private static final int INTRO_FLY_END = 320;
    private static final int INTRO_REVEAL_TICK = 335;
    private static final int INTRO_DESCEND_START = 338;
    private static final int INTRO_LEN = 360;
    private boolean introActive = false;
    private int introTicks = 0;
    private double introX, introBaseY, introZ;
    private final EnumMap<EquipmentSlot, ItemStack> pendingEquipment = new EnumMap<>(EquipmentSlot.class);
    // 鏡像的浮游砲（已過濾不鏡射的升級）。自走砲（繞行）vs 手持（雙持蓄力）分開
    private final List<ItemStack> mirroredTurrets = new ArrayList<>();      // 裝備槽/背包 → 繞行自走砲
    private final List<ItemStack> mirroredHandTurrets = new ArrayList<>();  // 主手/副手 → 手持蓄力
    private boolean turretsSpawned = false; // 不存盤：重載後為 false → 重新生成砲

    private static final EntityDataAccessor<Optional<UUID>> SOURCE_UUID =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> SOURCE_NAME =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.STRING);

    // 鏡像玩家的整個主背包（快捷欄 0-8 + 背包 9-35），供疊方塊 AI 取用。不掉落、不同步。
    private final NonNullList<ItemStack> clonedInventory = NonNullList.withSize(36, ItemStack.EMPTY);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);

    public PlayerCloneEntity(EntityType<? extends PlayerCloneEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SOURCE_UUID, Optional.empty());
        builder.define(SOURCE_NAME, "");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HP)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void mirrorFrom(Player player) {
        setSourceUUID(player.getUUID());
        setSourceName(player.getGameProfile().getName());

        // 收集玩家「擁有的全部物品」：穿戴 + 主副手 + 背包 + 額外裝備槽，再遞迴展開容器內容
        // （界伏盒/潛影盒/收納袋/巢狀），杜絕把裝備藏進盒子讓 boss 鏡像不到的逃課
        List<ItemStack> pool = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) pool.add(player.getItemBySlot(slot));
        pool.addAll(player.getInventory().items);
        NonNullList<ItemStack> extra = player.getData(ModDataAttachments.EXTRA_EQUIPMENT.get());
        pool.addAll(extra);
        List<ItemStack> contained = new ArrayList<>();
        for (ItemStack s : pool) collectContained(s, contained, 0);
        pool.addAll(contained);

        // 裝備：每個防具槽鏡像「擁有的最強」（藏在盒子裡的也算進來），進場爆炸那刻才顯現
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack best = ItemStack.EMPTY;
                double bestScore = -1;
                for (ItemStack s : pool) {
                    double sc = armorScore(s, slot);
                    if (sc > bestScore) { bestScore = sc; best = s; }
                }
                pendingEquipment.put(slot, best.copy());
            } else {
                // 主/副手先放玩家當下手持（供下方手持砲判定），武器最強由 equipBestWeapon 補
                pendingEquipment.put(slot, player.getItemBySlot(slot).copy());
            }
            this.setDropChance(slot, 0.0F);
        }

        // clonedInventory：把擁有的物品填進去（供疊牆 AI 取方塊、equipBestWeapon 選到盒裡的武器）
        int idx = 0;
        for (ItemStack s : pool) {
            if (idx >= clonedInventory.size()) break;
            if (s.isEmpty()) continue;
            clonedInventory.set(idx++, s.copy());
        }
        while (idx < clonedInventory.size()) clonedInventory.set(idx++, ItemStack.EMPTY);

        // 鏡像浮游砲：手持(主副手) + 自走(其餘來源，含盒子內)
        mirroredTurrets.clear();
        mirroredHandTurrets.clear();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (mainHand.getItem() instanceof FloatingTurretItem) mirroredHandTurrets.add(filterMirroredTurret(mainHand));
        if (offHand.getItem() instanceof FloatingTurretItem) mirroredHandTurrets.add(filterMirroredTurret(offHand));
        for (ItemStack s : pool) {
            if (s == mainHand || s == offHand) continue; // 主副手已算手持
            addMirroredTurret(s);
        }

        this.setHealth(this.getMaxHealth());
        this.bossEvent.setName(player.getDisplayName());
    }

    private static final int CONTAINER_DEPTH_CAP = 4;

    // 遞迴展開容器物品內容：界伏盒/潛影盒等（CONTAINER component）+ 收納袋（BUNDLE_CONTENTS）
    private static void collectContained(ItemStack stack, List<ItemStack> out, int depth) {
        if (stack.isEmpty() || depth >= CONTAINER_DEPTH_CAP) return;
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
            for (ItemStack inner : container.nonEmptyItems()) {
                out.add(inner);
                collectContained(inner, out, depth + 1);
            }
        }
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            bundle.itemCopyStream().forEach(inner -> {
                out.add(inner);
                collectContained(inner, out, depth + 1);
            });
        }
    }

    // 該物品作為指定防具槽的防護評分（ARMOR + ARMOR_TOUGHNESS），非該槽可穿戴回 -1
    private static double armorScore(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return -1;
        Equipable eq = Equipable.get(stack);
        if (eq == null || eq.getEquipmentSlot() != slot) return -1;
        double v = 0;
        for (var e : stack.getAttributeModifiers().modifiers()) {
            if (!e.slot().test(slot)) continue;
            if (e.attribute().equals(Attributes.ARMOR) || e.attribute().equals(Attributes.ARMOR_TOUGHNESS)) {
                v += e.modifier().amount();
            }
        }
        return v;
    }

    private void addMirroredTurret(ItemStack stack) {
        if (mirroredTurrets.size() >= 2) return;
        if (stack.isEmpty() || !(stack.getItem() instanceof FloatingTurretItem)) return;
        mirroredTurrets.add(filterMirroredTurret(stack));
    }

    // 不鏡射治療升級（避免分身自我回血變消耗戰）；其餘升級保留
    private static ItemStack filterMirroredTurret(ItemStack original) {
        ItemStack copy = original.copy();
        EquipmentUpgradeData data = FloatingTurretItem.getData(copy);
        if (data.upgrades().isEmpty()) return copy;
        EquipmentUpgradeData filtered = data;
        for (var e : new HashMap<>(data.upgrades()).entrySet()) {
            if (e.getValue().getItem() instanceof TurretUpgradeItem tu
                    && tu.getBehavior() == TurretUpgradeBehavior.HEALING) {
                filtered = filtered.withUpgrade(e.getKey(), ItemStack.EMPTY);
            }
        }
        FloatingTurretItem.setData(copy, filtered);
        return copy;
    }

    public NonNullList<ItemStack> getClonedInventory() {
        return clonedInventory;
    }

    // ── 進場演出 ──────────────────────────────────────────────────────────────

    /** 在登場點啟動演出：埋在地底、無敵、無 AI，等過場推進到此再鑽出。 */
    public void startIntro(double x, double baseY, double z) {
        this.introActive = true;
        this.introTicks = 0;
        this.introX = x;
        this.introBaseY = baseY;
        this.introZ = z;
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setPos(x, baseY - 3.0, z);
    }

    public boolean isIntroActive() {
        return introActive;
    }

    private void tickIntro() {
        introTicks++;
        double y;
        if (introTicks < INTRO_RISE_START) {
            y = introBaseY - 3.0;                                  // 埋在地底
        } else if (introTicks < INTRO_RISE_END) {
            y = Mth.lerp(smooth(frac(introTicks, INTRO_RISE_START, INTRO_RISE_END)), introBaseY - 3.0, introBaseY); // 鑽出
        } else if (introTicks < INTRO_FLY_END) {
            y = Mth.lerp(smooth(frac(introTicks, INTRO_RISE_END, INTRO_FLY_END)), introBaseY, introBaseY + 4.0);    // 飛高
        } else if (introTicks < INTRO_DESCEND_START) {
            y = introBaseY + 4.0 + Math.sin((introTicks - INTRO_FLY_END) * 0.25) * 0.25;                            // 浮動集氣
        } else if (introTicks < INTRO_LEN) {
            y = Mth.lerp(smooth(frac(introTicks, INTRO_DESCEND_START, INTRO_LEN)), introBaseY + 4.0, introBaseY);   // 降落
        } else {
            activateAfterIntro();
            return;
        }
        this.setPos(introX, y, introZ);
        this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        this.setYRot(180.0F);
        this.yBodyRot = 180.0F;
        this.yHeadRot = 180.0F;

        if (level() instanceof ServerLevel sl) {
            if (introTicks == INTRO_RISE_START) {
                // 鑽出瞬間：地面崩裂音 + 一圈塵土
                sl.playSound(null, introX, introBaseY, introZ, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.2F, 0.7F);
                sl.sendParticles(ParticleTypes.EXPLOSION, introX, introBaseY, introZ, 1, 0, 0, 0, 0);
            }
            if (introTicks >= INTRO_RISE_START && introTicks < INTRO_RISE_END) {
                // 地底鑽出：塵土 + 靈魂煙
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, introX, introBaseY, introZ, 8, 0.5, 0.1, 0.5, 0.01);
                sl.sendParticles(ParticleTypes.SCULK_SOUL, introX, y + 0.5, introZ, 4, 0.4, 0.6, 0.4, 0.02);
            } else if (introTicks >= INTRO_FLY_END && introTicks < INTRO_REVEAL_TICK) {
                // 集氣：粒子從四周遠處慢慢飄向中心（越接近爆炸越密越快）
                float prog = frac(introTicks, INTRO_FLY_END, INTRO_REVEAL_TICK);
                int count = 8 + (int) (prog * 14);
                double radius = 5.5 - prog * 2.0;
                for (int i = 0; i < count; i++) {
                    double a = sl.random.nextDouble() * Math.PI * 2;
                    double h = sl.random.nextDouble() * 3.0 - 1.2;
                    // count=0：(dx,dy,dz) 為單位方向，speed 為速度大小 → 緩慢往中心飄
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            introX + Math.cos(a) * radius, y + h, introZ + Math.sin(a) * radius,
                            0, -Math.cos(a), -h * 0.3, -Math.sin(a), 0.10 + prog * 0.12);
                }
                if (introTicks % 8 == 0) {
                    sl.sendParticles(ParticleTypes.ENCHANT, introX, y + 1.0, introZ, 8, 2.0, 1.5, 2.0, 0.3);
                }
            }
            if (introTicks == INTRO_REVEAL_TICK) {
                // 爆炸：裝備此刻顯現
                revealEquipment();
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, introX, y, introZ, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.FLASH, introX, y + 0.5, introZ, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, introX, y + 0.5, introZ, 60, 0.1, 0.1, 0.1, 0.6);
                sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5F, 0.7F);
                sl.playSound(null, blockPosition(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.0F, 0.6F);
            }
        }
    }

    private void revealEquipment() {
        for (var e : pendingEquipment.entrySet()) {
            this.setItemSlot(e.getKey(), e.getValue());
        }
        equipBestWeapon();
    }

    // 拿背包裡攻擊力最高的武器到主手（避免好武器在快捷欄沒拿在手上時 boss 空手）
    private void equipBestWeapon() {
        ItemStack best = this.getMainHandItem();
        double bestAtk = weaponAttack(best);
        for (ItemStack st : clonedInventory) {
            if (st.isEmpty()) continue;
            double a = weaponAttack(st);
            if (a > bestAtk) { bestAtk = a; best = st; }
        }
        if (!best.isEmpty() && !ItemStack.isSameItemSameComponents(best, this.getMainHandItem())) {
            this.setItemSlot(EquipmentSlot.MAINHAND, best.copy());
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    private static double weaponAttack(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        double atk = 0;
        for (var e : stack.getAttributeModifiers().modifiers()) {
            if (e.attribute().equals(Attributes.ATTACK_DAMAGE) && e.slot().test(EquipmentSlot.MAINHAND)) {
                atk += e.modifier().amount();
            }
        }
        return atk;
    }

    private void activateAfterIntro() {
        introActive = false;
        introTicks = 0;
        setNoAi(false);
        setNoGravity(false);
        setInvulnerable(false);
        setPos(introX, introBaseY, introZ);
        if (level() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(64))) {
                bossEvent.addPlayer(p);
                setTarget(p);
            }
        }
        // 繞行砲由 customServerAiStep 的 turretsSpawned 守門生成（涵蓋啟動 + 重載後重生）
    }

    private void spawnMirroredTurrets() {
        if (!(level() instanceof ServerLevel sl)) return;
        // 自走砲：slotIndex 0/1（繞行）
        for (int i = 0; i < mirroredTurrets.size(); i++) {
            spawnCloneTurret(sl, mirroredTurrets.get(i), i);
        }
        // 手持砲：slotIndex 2/3（站手邊、蓄力）
        for (int i = 0; i < mirroredHandTurrets.size(); i++) {
            spawnCloneTurret(sl, mirroredHandTurrets.get(i), i + 2);
        }
    }

    private void spawnCloneTurret(ServerLevel sl, ItemStack stack, int slotIndex) {
        FloatingTurretEntity turret = ModEntities.FLOATING_TURRET.get().create(sl);
        if (turret == null) return;
        turret.moveTo(getX(), getY() + 1.0, getZ(), 0f, 0f);
        turret.setupAsCloneTurret(this, stack, slotIndex);
        sl.addFreshEntity(turret);
    }

    private static float frac(int t, int start, int end) {
        return (t - start) / (float) (end - start);
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && introActive) {
            tickIntro();
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return !introActive && super.shouldBeSaved();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !level().isClientSide
                && source.getEntity() instanceof Player attacker
                && attacker.isAlive()
                && this.random.nextFloat() < REFLECT_CHANCE) {
            attacker.hurt(level().damageSources().magic(), amount);
        }
        return result;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        this.bossEvent.removeAllPlayers();
        MinecraftServer server = sl.getServer();
        getSourceUUID().ifPresent(srcId -> {
            VoidMirrorSavedData.get(server).markCleared(srcId);
            SpaceCrackEntity.removeForOwner(server.overworld(), srcId);
            // 勝利後娜拉消失
            for (NaraPhantomEntity nara : sl.getEntitiesOfClass(NaraPhantomEntity.class,
                    new AABB(BlockPos.ZERO).inflate(260),
                    n -> n.getSourceUUID().map(srcId::equals).orElse(false))) {
                nara.discard();
            }
        });
        // 維度內已無其他存活分身 → 開出獎勵寶箱（內容待定）
        boolean anyCloneLeft = !sl.getEntitiesOfClass(PlayerCloneEntity.class,
                new AABB(BlockPos.ZERO).inflate(260), e -> e != this && e.isAlive()).isEmpty();
        if (!anyCloneLeft) {
            spawnRewardChest(sl);
        }
    }

    private void spawnRewardChest(ServerLevel sl) {
        BlockPos chestPos = new BlockPos(0, 64, -3);
        if (sl.getBlockState(chestPos).is(Blocks.CHEST)) return;
        sl.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        VoidMirrorEvents.addModifiedBlock(chestPos.asLong());
        // TODO: 寶箱內容待決定
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            this.bossEvent.removeAllPlayers();
        }
        super.remove(reason);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        // 永遠鎖定附近玩家（含創造模式，只排除旁觀），確保 boss 會主動追打
        if (getTarget() == null || !getTarget().isAlive()) {
            Player nearest = null;
            double best = 48.0 * 48.0;
            for (Player p : level().players()) {
                if (p.isSpectator() || !p.isAlive()) continue;
                double d = p.distanceToSqr(this);
                if (d < best) { best = d; nearest = p; }
            }
            if (nearest != null) setTarget(nearest);
        }

        // 防風箏：目標離太遠就加速追上，靠近就解除
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            LivingEntity tgt = getTarget();
            boolean far = tgt != null && tgt.isAlive() && this.distanceToSqr(tgt) > ANTI_KITE_DIST_SQR;
            boolean has = speed.hasModifier(ANTI_KITE_SPEED_ID);
            if (far && !has) speed.addTransientModifier(ANTI_KITE_SPEED);
            else if (!far && has) speed.removeModifier(ANTI_KITE_SPEED_ID);
        }

        // 啟動後（或重載後）生成繞行砲，只做一次
        if (!turretsSpawned) {
            spawnMirroredTurrets();
            turretsSpawned = true;
        }

        updatePhase();
        tickAntiPillar(); // 全階段防墊高，開場就不給 cheese
        if (phase == Phase.WALLING) {
            tickWallBuilding();
            tickBreakSurroundings();
        } else if (phase == Phase.BERSERK) {
            tickManaDrain();
            tickBreakSurroundings();
        }
    }

    // 反 pillar：玩家墊方塊升高 → 拆腳下支撐讓他掉下來；墊太高就一次連拆整段柱摔回地面
    private void tickAntiPillar() {
        if (antiPillarCooldown > 0) { antiPillarCooldown--; return; }
        if (!(level() instanceof ServerLevel sl)) return;
        LivingEntity target = getTarget();
        if (!(target instanceof Player)) return;

        BlockPos support = target.blockPosition().below();
        if (support.getY() < 64) return; // 站在地形上、沒墊高 → 不處理

        int heightAbove = target.blockPosition().getY() - this.blockPosition().getY();
        if (heightAbove >= PILLAR_HEIGHT_TRIGGER) {
            // 墊柱逃避：從腳下往下連拆，一次解決整段支撐，讓玩家直接摔回分身高度
            BlockPos.MutableBlockPos p = support.mutable();
            int broken = 0;
            for (int i = 0; i < 8 && p.getY() >= 64; i++) {
                if (!placedWalls.contains(p.asLong())) {
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
        if (!st.isAir() && st.getDestroySpeed(sl, support) >= 0 && !placedWalls.contains(support.asLong())) {
            sl.destroyBlock(support, false);
            antiPillarCooldown = 6;
        } else {
            antiPillarCooldown = 8;
        }
    }

    // 拆玩家蓋的防禦工事 / 逃跑路線（地表 Y>=64 以上、非分身自己疊的方塊）
    private void tickBreakSurroundings() {
        if (breakCooldown > 0) { breakCooldown--; return; }
        if (!(level() instanceof ServerLevel sl)) return;
        LivingEntity target = getTarget();
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
                    if (placedWalls.contains(p.asLong())) continue; // 不拆自己的牆
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

    private void updatePhase() {
        float r = this.getHealth() / this.getMaxHealth();
        Phase next = r > 0.6F ? Phase.NORMAL : (r > 0.3F ? Phase.WALLING : Phase.BERSERK);
        if (next != phase) {
            phase = next;
            if (next == Phase.BERSERK) onEnterBerserk();
        }
    }

    private void onEnterBerserk() {
        // 全力進攻：拆掉自己建的所有牆，並加速
        removeAllWalls();
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(BERSERK_SPEED_ID)) {
            speed.addPermanentModifier(BERSERK_SPEED);
        }
    }

    private void tickWallBuilding() {
        if (wallCooldown > 0) { wallCooldown--; return; }
        if (!(level() instanceof ServerLevel sl)) return;
        if (placedWalls.size() >= WALL_CAP) return;
        LivingEntity target = getTarget();
        if (!(target instanceof Player)) return;
        if (this.distanceToSqr(target) > 64.0) return; // 8 格內才封路

        Vec3 toTarget = target.position().subtract(this.position());
        Direction dir = Direction.getNearest(toTarget.x, 0.0, toTarget.z);
        // 封住玩家遠離分身那一側的退路
        BlockPos base = target.blockPosition().relative(dir);

        BlockState wall = takeWallBlock();
        boolean placed = false;
        for (int dy = 0; dy <= 1; dy++) {
            BlockPos p = base.above(dy);
            if (!sl.getWorldBorder().isWithinBounds(p)) continue;
            if (!sl.getBlockState(p).canBeReplaced()) continue;
            sl.setBlockAndUpdate(p, wall);
            placedWalls.add(p.asLong());
            VoidMirrorEvents.addModifiedBlock(p.asLong());
            placed = true;
        }
        if (placed) {
            sl.playSound(null, base, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 0.6F, 1.0F);
        }
        wallCooldown = WALL_INTERVAL;
    }

    private BlockState takeWallBlock() {
        for (ItemStack st : clonedInventory) {
            if (!st.isEmpty() && st.getItem() instanceof BlockItem bi) {
                st.shrink(1);
                return bi.getBlock().defaultBlockState();
            }
        }
        return Blocks.STONE.defaultBlockState();
    }

    private void removeAllWalls() {
        if (!(level() instanceof ServerLevel sl)) return;
        for (long l : placedWalls) {
            BlockPos p = BlockPos.of(l);
            if (sl.isLoaded(p)) {
                sl.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            }
        }
        placedWalls.clear();
    }

    private void tickManaDrain() {
        if (drainCooldown > 0) { drainCooldown--; return; }
        drainCooldown = DRAIN_INTERVAL;
        if (!(level() instanceof ServerLevel sl)) return;
        LivingEntity target = getTarget();
        if (!(target instanceof Player player)) return;
        if (this.distanceToSqr(target) > 256.0) return; // 16 格內

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

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!introActive) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void checkDespawn() {
        // Boss never despawns (ignores peaceful / distance rules).
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public void setSourceUUID(@Nullable UUID uuid) {
        entityData.set(SOURCE_UUID, Optional.ofNullable(uuid));
    }

    public Optional<UUID> getSourceUUID() {
        return entityData.get(SOURCE_UUID);
    }

    public void setSourceName(String name) {
        entityData.set(SOURCE_NAME, name);
    }

    public String getSourceName() {
        return entityData.get(SOURCE_NAME);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        entityData.get(SOURCE_UUID).ifPresent(uuid -> tag.putUUID("SourceUUID", uuid));
        tag.putString("SourceName", getSourceName());
        CompoundTag invTag = new CompoundTag();
        ContainerHelper.saveAllItems(invTag, clonedInventory, this.registryAccess());
        tag.put("ClonedInventory", invTag);
        tag.putInt("Phase", phase.ordinal());
        long[] walls = new long[placedWalls.size()];
        int wi = 0;
        for (long l : placedWalls) walls[wi++] = l;
        tag.putLongArray("PlacedWalls", walls);
        ListTag turretList = new ListTag();
        for (ItemStack s : mirroredTurrets) {
            if (!s.isEmpty()) turretList.add(s.save(this.registryAccess()));
        }
        tag.put("MirroredTurrets", turretList);
        ListTag handList = new ListTag();
        for (ItemStack s : mirroredHandTurrets) {
            if (!s.isEmpty()) handList.add(s.save(this.registryAccess()));
        }
        tag.put("MirroredHandTurrets", handList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("SourceUUID")) {
            entityData.set(SOURCE_UUID, Optional.of(tag.getUUID("SourceUUID")));
        }
        if (tag.contains("SourceName")) {
            setSourceName(tag.getString("SourceName"));
            if (!getSourceName().isEmpty()) {
                this.bossEvent.setName(Component.literal(getSourceName()));
            }
        }
        if (tag.contains("ClonedInventory")) {
            ContainerHelper.loadAllItems(tag.getCompound("ClonedInventory"), clonedInventory, this.registryAccess());
        }
        if (tag.contains("Phase")) {
            Phase[] values = Phase.values();
            int ord = tag.getInt("Phase");
            phase = ord >= 0 && ord < values.length ? values[ord] : Phase.NORMAL;
        }
        placedWalls.clear();
        for (long l : tag.getLongArray("PlacedWalls")) placedWalls.add(l);
        mirroredTurrets.clear();
        ListTag turretList = tag.getList("MirroredTurrets", Tag.TAG_COMPOUND);
        for (int i = 0; i < turretList.size(); i++) {
            ItemStack.parse(this.registryAccess(), turretList.getCompound(i))
                    .ifPresent(s -> { if (!s.isEmpty()) mirroredTurrets.add(s); });
        }
        mirroredHandTurrets.clear();
        ListTag handList = tag.getList("MirroredHandTurrets", Tag.TAG_COMPOUND);
        for (int i = 0; i < handList.size(); i++) {
            ItemStack.parse(this.registryAccess(), handList.getCompound(i))
                    .ifPresent(s -> { if (!s.isEmpty()) mirroredHandTurrets.add(s); });
        }
    }
}
