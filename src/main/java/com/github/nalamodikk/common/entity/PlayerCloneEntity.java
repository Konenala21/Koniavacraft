package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.register.ModItems;
import com.github.nalamodikk.common.dimension.VoidMirrorSavedData;
import com.github.nalamodikk.common.dimension.VoidMirrorTeleport;
import com.github.nalamodikk.common.event.VoidMirrorEvents;
import com.github.nalamodikk.common.item.upgrade.EquipmentUpgradeData;
import com.github.nalamodikk.common.item.weapon.FloatingTurretItem;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeBehavior;
import com.github.nalamodikk.common.item.weapon.turret.TurretUpgradeItem;
import com.github.nalamodikk.common.network.packet.client.Phase2TransitionPacket;
import com.github.nalamodikk.register.ModDataAttachments;
import com.github.nalamodikk.register.ModDataComponents;
import com.github.nalamodikk.register.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.tags.ItemTags;
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

    public enum Phase { NORMAL, WALLING, BERSERK }

    // 分身招牌技能：墊方塊衝撞，三種形態隨機輪用
    private enum PillarSkill { RAM_WALL, LIFT_UP, CHARGE_RAMP }

    private static final int WALL_CAP = 24;
    private static final int WALL_INTERVAL = 25;
    private static final int DRAIN_INTERVAL = 20;
    private static final int DRAIN_AMOUNT = 300;

    private static final ResourceLocation BERSERK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "clone_berserk_speed");
    private static final AttributeModifier BERSERK_SPEED =
            new AttributeModifier(BERSERK_SPEED_ID, 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    // 防風箏：目標離太遠時暫時加速追上（距離判斷用遲滯，見 customServerAiStep）
    private static final ResourceLocation ANTI_KITE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "clone_anti_kite_speed");
    private static final AttributeModifier ANTI_KITE_SPEED =
            new AttributeModifier(ANTI_KITE_SPEED_ID, 0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private Phase phase = Phase.NORMAL;
    private final Set<Long> placedWalls = new HashSet<>();
    private int wallCooldown = 0;
    private int drainCooldown = 0;
    private int breakCooldown = 0;
    private int antiPillarCooldown = 0;
    // 玩家高出分身達此格數 → 視為墊柱逃避，從腳下往下連拆整段支撐讓他摔回地面
    private static final int PILLAR_HEIGHT_TRIGGER = 3;

    // 墊方塊衝撞技能狀態
    private PillarSkill pendingSkill = null;
    private int skillChargeTicks = 0;
    private int skillCooldown = 100; // 開場緩衝，不一進場就放
    private static final int SKILL_COOLDOWN = 80; // 技能頻繁（~4s 一次），壓迫靠技能而非高傷
    private static final int SKILL_TELEGRAPH = 20;  // 前搖 1 秒：站定蓄力 + 漸強預警，給玩家反應/閃避
    private static final double SKILL_RANGE_SQR = 144.0; // 12 格內才發
    private static final double SKILL_MIN_SQR = 4.0;     // 太近不發
    private static final double SKILL_DODGE_RADIUS = 2.5; // 前搖內跑出鎖定點這個距離即閃過
    private BlockPos skillTargetPos = BlockPos.ZERO;      // 前搖時鎖定的地點（技能對此點生效，非鎖定玩家本人）
    // 技能墊的方塊：擊飛後 1 秒（20t）開始依序快速打掉，分身收拾自己墊的方塊
    private final List<Long> skillBlocks = new ArrayList<>();
    private int skillClearTimer = -1; // -1 閒置；>0 倒數；0 清理中（每 tick 打一格）
    private static final int SKILL_BLOCK_LIFETIME = 20;
    // 分段擊飛（RAM_WALL）：先把玩家拋飛離地，數 tick 後玩家在空中時再強水平轟飛（空中無摩擦才飛得誇張）
    @Nullable private Player pendingLaunchTarget = null;
    private int pendingLaunchTimer = 0;
    private Vec3 pendingLaunchDir = Vec3.ZERO;
    // 確保同源娜拉幻影恰好一個（娜拉不存盤，重載後由 boss 重建）
    private int naraCheckCooldown = 0;
    // 空中追擊墊的柱：追完落地就清掉，避免殘留把 arena 弄得坑坑疤疤讓 boss 走路卡頓
    private final List<Long> chaseBlocks = new ArrayList<>();
    // 進場結束後的緩衝：boss 站定不攻擊，給玩家準備時間（避免動畫剛完就被偷襲）
    private int graceTicks = 0;
    // 近戰攻擊冷卻（邊繞圈邊攻擊，取代 MeleeAttackGoal 的站定揮擊）
    private int attackCooldown = 0;
    // 鏡反狀態：週期進入，期間打 boss 才反傷（取代純機率反傷，玩家可學會停手）
    private int reflectCooldown = 200;
    private int reflectTicks = 0;
    private static final int REFLECT_INTERVAL = 160; // 每 ~8 秒一次
    private static final int REFLECT_DURATION = 30;  // 持續 1.5 秒

    // 進場演出（地底鑽出→飛高→浮動集氣→爆炸顯現裝備→降落→啟動），對齊過場時間軸（360t）
    private static final int INTRO_RISE_START = 430;
    private static final int INTRO_RISE_END = 480;
    private static final int INTRO_FLY_END = 530;
    private static final int INTRO_REVEAL_TICK = 570;
    private static final int INTRO_DESCEND_START = 575;
    private static final int INTRO_LEN = 620;
    private boolean introActive = false;
    private int introTicks = 0;
    private double introX, introBaseY, introZ;
    private final EnumMap<EquipmentSlot, ItemStack> pendingEquipment = new EnumMap<>(EquipmentSlot.class);
    // 鏡像的浮游砲（已過濾不鏡射的升級）。自走砲（繞行）vs 手持（雙持蓄力）分開
    private final List<ItemStack> mirroredTurrets = new ArrayList<>();      // 裝備槽/背包 → 繞行自走砲
    private final List<ItemStack> mirroredHandTurrets = new ArrayList<>();  // 主手/副手 → 手持蓄力
    private boolean turretsSpawned = false; // 不存盤：重載後為 false → 重新生成砲

    // ── 二階段方塊機甲（半血變身：以 BlockDisplay 偽裝方塊組成大型人形包住本體）──
    // 已送出 BGM 觸發 packet 的玩家 UUID（避免重複送）。Transient，重載 / 重啟自動重置 → 玩家重進時 BGM 會再觸發
    private final java.util.Set<UUID> bgmSentTo = new HashSet<>();
    private final java.util.List<Display.BlockDisplay> armorParts = new ArrayList<>();
    private final java.util.List<Vec3> armorSpawnOffsets = new ArrayList<>(); // 變身時方塊的初始飛來位置（local offset）
    private final java.util.List<Integer> armorAssembleDelay = new ArrayList<>(); // 每塊方塊飛入的起飛延遲 tick（順序漸快）
    private final java.util.List<Vec3> turretMountOffsets = new ArrayList<>(); // 結構模板的 LIME_WOOL 砲位（local offset，由左到右排序對應 slot index）
    private static final int ARMOR_TRAVEL_TICKS = 50; // 每塊方塊飛入移動時間（過場拉長後也跟著拉長，讓飛入動作更從容）
    private final java.util.List<Vec3> armorOffsets = new ArrayList<>();
    private boolean armorTriggered = false;   // 半血只觸發一次
    private boolean armoredDimensions = false; // 鏡像 ARMORED 給 getDimensions 用（避免在 entityData 尚未 define 時讀取）
    private boolean pendingArmorRebuild = false; // 重載時若仍在外殼狀態，首次 tick 重建外殼（接續二階段）
    private float armorHp = 0f;               // 外殼血量，與本體血量分離
    private static final float ARMOR_MAX_HP = 120f;
    // 二階段變身過場（server tick 倒數）：期間 boss 凍結 AI + 無敵，client 端鎖相機環繞
    private static final int PHASE2_TRANSITION_LEN = 220;
    private int phase2TransitionTicks = 0;
    private static final net.minecraft.world.entity.EntityDimensions ARMOR_DIMENSIONS =
            net.minecraft.world.entity.EntityDimensions.scalable(5.0F, 16.0F); // 涵蓋機甲外殼的碰撞箱
    private final ArrayList<Integer> armorLegSide = new ArrayList<>(); // 與 armorParts 平行：0=非腿 1=左腿 2=右腿
    private static final String ARMOR_TAG_PREFIX = "koniava_mecha_armor_"; // 後接 boss UUID，避免多 boss 場景下 cleanup 清掉別的 boss 的活 display

    private String armorTag() {
        return ARMOR_TAG_PREFIX + this.getStringUUID();
    }
    private double walkPhase = 0;                 // 走路動畫相位（移動時推進，讓雙腿前後擺動）
    private static final int LEG_TOP_ROW = 11;    // 剪影此列(含)以下視為腿
    private static final double LEG_HIP_Y = 3.0;  // 腿頂(髖)高度 = (rows-1) - LEG_TOP_ROW，繞此擺動
    // 本體鑲嵌深度：玩家模型整個藏進機甲，腳被胸甲擋住，只有頭剛好對齊 row 2 的眼縫從中露出
    // 玩家身高 ~1.8 → feet=11 head_center≈12.5 ≈ row 2 (oy 12-13) 中央
    public static final double ARMORED_BODY_OFFSET_Y = 11.0;
    // 機甲頭頂的世界座標 oy（rows-1 - row 0 = 14），浮游砲坐落在頭頂上方使用
    public static final double ARMORED_HEAD_TOP_Y = 14.0;
    // 機甲剪影（7 寬人形 + 寬肩窄腰）：o=外殼方塊（材質從 clonedInventory→環境取得）、b=本體核心(不放方塊)、'.'=空
    private static final String[] MECH_SHAPE = {
            "...o...",   // 0 頭頂天線
            "..ooo..",   // 1 頭頂
            ".o.b.o.",   // 2 眼+核心
            "..ooo..",   // 3 下顎
            ".ooooo.",   // 4 頸
            "ooooooo",   // 5 肩線 7 格
            "ooooooo",   // 6 肩甲
            ".ooooo.",   // 7 胸
            ".ooooo.",   // 8 腹
            "..ooo..",   // 9 腰收窄
            ".ooooo.",   // 10 髖
            ".oo.oo.",   // 11 大腿頂 (LEG_TOP_ROW)
            ".oo.oo.",   // 12 大腿
            ".oo.oo.",   // 13 小腿
            ".oo.oo.",   // 14 小腿（無腳板）
    };

    private static final EntityDataAccessor<Optional<UUID>> SOURCE_UUID =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> SOURCE_NAME =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.STRING);
    // 當前前搖中的招式（同步給 client 畫預兆）：0=無, 1=RAM_WALL, 2=LIFT_UP, 3=CHARGE_RAMP
    private static final EntityDataAccessor<Integer> TELEGRAPH_SKILL =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.INT);
    // 鏡反狀態（同步給 client 畫鏡面輪廓）：此狀態期間打 boss 會被反傷，給玩家學會停手
    private static final EntityDataAccessor<Boolean> REFLECTING =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.BOOLEAN);
    // 二階段方塊機甲狀態（同步給 client；機甲外殼由 BlockDisplay 組成、跟隨 boss）
    private static final EntityDataAccessor<Boolean> ARMORED =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.BOOLEAN);
    // 技能鎖定的地點（同步給 client 畫固定預兆；玩家在前搖內跑出此點即可閃避）
    private static final EntityDataAccessor<BlockPos> SKILL_TARGET =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.BLOCK_POS);

    // 鏡像玩家的整個主背包（快捷欄 0-8 + 背包 9-35），供疊方塊 AI 取用。不掉落、不同步。
    private final NonNullList<ItemStack> clonedInventory = NonNullList.withSize(36, ItemStack.EMPTY);
    // 鏡像玩家快捷欄（slot 0~8），戰鬥中定時切換主手；空槽跳過
    private final NonNullList<ItemStack> bossHotbar = NonNullList.withSize(9, ItemStack.EMPTY);
    private int hotbarIdx = 0;
    private int hotbarSwitchCooldown = 0;
    private static final int HOTBAR_SWITCH_INTERVAL = 80; // 4 秒切一次
    // Boss 主動發砲齊射技能：1 秒 telegraph + 同步所有 boss 砲對玩家蓄力齊射
    private int turretVolleyCooldown = 80; // 開戰初始 4 秒緩衝再開始算
    private int turretVolleyTelegraph = 0;
    private static final int TURRET_VOLLEY_INTERVAL = 200; // 10 秒一次齊射
    private static final int TURRET_VOLLEY_TELEGRAPH = 20; // 1 秒前搖
    private static final double TURRET_VOLLEY_RANGE_SQ = 32 * 32;

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
        builder.define(TELEGRAPH_SKILL, 0);
        builder.define(REFLECTING, false);
        builder.define(ARMORED, false);
        builder.define(SKILL_TARGET, BlockPos.ZERO);
    }

    public int getTelegraphSkill() {
        return entityData.get(TELEGRAPH_SKILL);
    }

    public boolean isReflecting() {
        return entityData.get(REFLECTING);
    }

    public boolean isArmored() {
        return entityData.get(ARMORED);
    }

    public BlockPos getSkillTarget() {
        return entityData.get(SKILL_TARGET);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HP)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        return armoredDimensions ? ARMOR_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        // 變身期間本體被 renderer 移到頭部、外殼又大，放寬剔除框避免某些視角整隻被 frustum culling 隱藏
        return armoredDimensions ? this.getBoundingBox().inflate(6.0) : super.getBoundingBoxForCulling();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        // ARMORED 同步到 client 時也鏡像給 armoredDimensions，否則 client 的 getDefaultDimensions/getBoundingBoxForCulling 永遠用小框
        if (ARMORED.equals(key)) {
            armoredDimensions = entityData.get(ARMORED);
            refreshDimensions();
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 近戰改由 customServerAiStep 的 tickMeleeStrafe 處理（邊繞圈邊攻擊，不站定揮擊）
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
        // 9 格儲存欄位（NINE_GRID 飾品背包）— 玩家會把武器藏這裡逃 boss 鏡像
        NonNullList<ItemStack> nineGrid = player.getData(ModDataAttachments.NINE_GRID.get());
        pool.addAll(nineGrid);
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
        // 重要物品（浮游砲/武器/盾）先排前面，避免主背包滿 36 格垃圾把 EXTRA_EQUIPMENT / 盒子內武器擠掉
        // 之前 bug：玩家把好武器藏進 EXTRA_EQUIPMENT 而主背包塞滿雜物 → boss 永遠選不到那把武器
        java.util.List<ItemStack> prioritized = new ArrayList<>(pool.size());
        for (ItemStack s : pool) if (!s.isEmpty() && s.getItem() instanceof FloatingTurretItem) prioritized.add(s);
        for (ItemStack s : pool) if (!s.isEmpty() && isProperWeapon(s)) prioritized.add(s);
        for (ItemStack s : pool) if (!s.isEmpty() && s.getItem() instanceof net.minecraft.world.item.ShieldItem) prioritized.add(s);
        // 其餘物品（包含工具、方塊等）尾隨在後
        for (ItemStack s : pool) {
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof FloatingTurretItem) continue;
            if (isProperWeapon(s)) continue;
            if (s.getItem() instanceof net.minecraft.world.item.ShieldItem) continue;
            prioritized.add(s);
        }
        int idx = 0;
        for (ItemStack s : prioritized) {
            if (idx >= clonedInventory.size()) break;
            clonedInventory.set(idx++, s.copy());
        }
        while (idx < clonedInventory.size()) clonedInventory.set(idx++, ItemStack.EMPTY);

        // 鏡像浮游砲：mirroredHandTurrets[main, off]，mirroredTurrets 對應 player 的兩個 EXTRA_EQUIPMENT 槽
        mirroredTurrets.clear();
        mirroredHandTurrets.clear();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        mirroredHandTurrets.add(mainHand.getItem() instanceof FloatingTurretItem ? filterMirroredTurret(mainHand) : ItemStack.EMPTY);
        mirroredHandTurrets.add(offHand.getItem() instanceof FloatingTurretItem ? filterMirroredTurret(offHand) : ItemStack.EMPTY);
        // 繞行砲：優先從 EXTRA_EQUIPMENT 的 2 個槽鏡像
        for (int i = 0; i < extra.size() && mirroredTurrets.size() < 2; i++) {
            ItemStack s = extra.get(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof FloatingTurretItem) {
                mirroredTurrets.add(filterMirroredTurret(s));
            }
        }
        // 補位：玩家把浮游砲藏在背包/盒子也要鏡像（避免「不裝就規避」），從 pool 補到 2 個
        // 跳過已經安排在主/副手/extra 那些浮游砲，避免重複鏡像
        java.util.Set<ItemStack> alreadyMirrored = new java.util.HashSet<>();
        if (mainHand.getItem() instanceof FloatingTurretItem) alreadyMirrored.add(mainHand);
        if (offHand.getItem() instanceof FloatingTurretItem) alreadyMirrored.add(offHand);
        for (ItemStack s : extra) if (s.getItem() instanceof FloatingTurretItem) alreadyMirrored.add(s);
        for (ItemStack s : pool) {
            if (mirroredTurrets.size() >= 2) break;
            if (s.isEmpty() || alreadyMirrored.contains(s)) continue;
            if (s.getItem() instanceof FloatingTurretItem) {
                mirroredTurrets.add(filterMirroredTurret(s));
            }
        }

        // 鏡像玩家快捷欄（前 9 格）供戰鬥中切換
        for (int i = 0; i < 9; i++) {
            ItemStack hot = i < player.getInventory().items.size() ? player.getInventory().items.get(i) : ItemStack.EMPTY;
            bossHotbar.set(i, hot.copy());
        }

        this.setHealth(this.getMaxHealth());
        this.bossEvent.setName(player.getDisplayName());
    }

    // Boss 主動發砲齊射：每 10 秒一次，前 1 秒在每門砲上噴 END_ROD 粒子當預兆，然後同時對玩家蓄力齊射
    private void tickTurretVolley() {
        if (!com.github.nalamodikk.common.config.ModCommonConfig.INSTANCE.bossTurretVolleyEnabled.get()) return;
        if (turretVolleyTelegraph > 0) {
            turretVolleyTelegraph--;
            if (level() instanceof ServerLevel sl) {
                for (FloatingTurretEntity t : findOwnedTurrets(sl)) {
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                            t.getX(), t.getY(), t.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
                }
            }
            if (turretVolleyTelegraph == 0) executeTurretVolley();
            return;
        }
        if (turretVolleyCooldown > 0) { turretVolleyCooldown--; return; }
        LivingEntity tgt = getTarget();
        if (tgt == null || !tgt.isAlive()) return;
        if (this.distanceToSqr(tgt) > TURRET_VOLLEY_RANGE_SQ) return;
        // 啟動前搖：播一個低沉蓄力音效讓玩家有反應時間
        turretVolleyTelegraph = TURRET_VOLLEY_TELEGRAPH;
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.0F, 1.4F);
        }
    }

    private void executeTurretVolley() {
        turretVolleyCooldown = TURRET_VOLLEY_INTERVAL;
        LivingEntity tgt = getTarget();
        if (tgt == null || !tgt.isAlive()) return;
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 targetPos = tgt.getBoundingBox().getCenter();
        for (FloatingTurretEntity t : findOwnedTurrets(sl)) {
            com.github.nalamodikk.common.entity.FloatingTurretProjectile proj =
                    com.github.nalamodikk.common.entity.FloatingTurretProjectile.shootAt(
                            sl, this, t.position(), targetPos, 1.0F); // charged
            proj.setNoBlockDamage(true);
            sl.addFreshEntity(proj);
        }
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.6F);
    }

    private java.util.List<FloatingTurretEntity> findOwnedTurrets(ServerLevel sl) {
        return sl.getEntitiesOfClass(FloatingTurretEntity.class,
                getBoundingBox().inflate(32.0),
                t -> t.getCloneOwner() == this);
    }

    // 戰鬥中切換主手：每 HOTBAR_SWITCH_INTERVAL tick 切到下一個有東西的快捷欄槽
    // 浮游砲跳過（已經是手持砲實體在打了），改換其他武器讓戰鬥節奏有變化
    private void tickHotbarSwitch() {
        if (!com.github.nalamodikk.common.config.ModCommonConfig.INSTANCE.bossHotbarSwitchEnabled.get()) return;
        if (hotbarSwitchCooldown > 0) { hotbarSwitchCooldown--; return; }
        hotbarSwitchCooldown = HOTBAR_SWITCH_INTERVAL;
        if (getTarget() == null || !getTarget().isAlive()) return;
        for (int i = 1; i <= 9; i++) {
            int next = (hotbarIdx + i) % 9;
            ItemStack candidate = bossHotbar.get(next);
            if (candidate.isEmpty()) continue;
            // 跳過浮游砲（已經是手持砲在打）；其他物品都可上手換打
            if (candidate.getItem() instanceof FloatingTurretItem) continue;
            // 跳過跟當下主手完全一樣的（無意義切換）
            if (ItemStack.isSameItemSameComponents(candidate, this.getMainHandItem())) continue;
            this.setItemSlot(EquipmentSlot.MAINHAND, candidate.copy());
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            hotbarIdx = next;
            return;
        }
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

    /** 玩家跳過過場：直接推進到進場結尾，下次 tickIntro 立即啟動 boss。 */
    public void skipIntro() {
        if (introActive) introTicks = INTRO_LEN;
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
                int count = 14 + (int) (prog * 26);
                double radius = 7.0 - prog * 2.5;
                for (int i = 0; i < count; i++) {
                    double a = sl.random.nextDouble() * Math.PI * 2;
                    double h = sl.random.nextDouble() * 3.5 - 1.4;
                    // count=0：(dx,dy,dz) 為單位方向，speed 為速度大小 → 緩慢往中心飄
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            introX + Math.cos(a) * radius, y + h, introZ + Math.sin(a) * radius,
                            0, -Math.cos(a), -h * 0.3, -Math.sin(a), 0.10 + prog * 0.14);
                }
                if (introTicks % 5 == 0) {
                    sl.sendParticles(ParticleTypes.ENCHANT, introX, y + 1.0, introZ, 14, 2.5, 1.8, 2.5, 0.4);
                }
                if (introTicks % 10 == 0) {
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, introX, y + 0.5, introZ, 6, 1.5, 1.2, 1.5, 0.02);
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
        equipBestOffhand();
        rebuildHandTurretsFromEquipped(); // 同步手持浮游砲列表（含 equipBestOffhand 從背包挑出的）
    }

    // 副手有盾且攻擊來自前方 (dot > 0.5) 就擋下：盾消耗耐久 + 播音效，不阻擋穿盾類型傷害
    private boolean tryShieldBlock(DamageSource source) {
        if (!com.github.nalamodikk.common.config.ModCommonConfig.INSTANCE.bossShieldBlockEnabled.get()) return false;
        if (!(this.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem)) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)) return false;
        net.minecraft.world.entity.Entity src = source.getDirectEntity();
        if (src == null) return false;
        Vec3 toSrc = src.position().subtract(this.position());
        if (toSrc.lengthSqr() < 1e-4) return false;
        toSrc = toSrc.normalize();
        Vec3 facing = Vec3.directionFromRotation(0, this.getYRot());
        if (facing.dot(toSrc) < 0.5) return false; // 不在前方 ~60° 錐形範圍 → 擋不到
        ItemStack shield = this.getOffhandItem();
        shield.hurtAndBreak(2, this, EquipmentSlot.OFFHAND);
        this.setItemSlot(EquipmentSlot.OFFHAND, shield);
        this.level().playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE,
                0.9F, 0.8F + this.random.nextFloat() * 0.4F);
        return true;
    }

    // 副手優先：浮游砲 → 盾牌；都找不到就保留 player 原本副手（不做去重，玩家手上有一堆一樣的物品很正常）
    private void equipBestOffhand() {
        ItemStack turret = ItemStack.EMPTY;
        ItemStack shield = ItemStack.EMPTY;
        for (ItemStack st : clonedInventory) {
            if (st.isEmpty()) continue;
            if (turret.isEmpty() && st.getItem() instanceof FloatingTurretItem) turret = st;
            else if (shield.isEmpty() && st.getItem() instanceof net.minecraft.world.item.ShieldItem) shield = st;
            if (!turret.isEmpty()) break;
        }
        ItemStack pick = !turret.isEmpty() ? turret : shield;
        if (!pick.isEmpty()) {
            this.setItemSlot(EquipmentSlot.OFFHAND, pick.copy());
            this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        }
    }

    // 拿背包裡最強武器到主手，三層優先級：
    //   tier 0: 浮游砲（本模組招牌武器）
    //   tier 1: vanilla 真武器（劍/斧/三叉戟）
    //   tier 2: vanilla 工具（鎬/鏟/任何有 ATTACK_DAMAGE）
    // 前一 tier 有東西就直接用，後 tier 高傷不會搶走
    private void equipBestWeapon() {
        ItemStack[] bestPerTier = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
        double[] bestAtkPerTier = { -1, -1, -1 };
        java.util.List<ItemStack> candidates = new ArrayList<>(clonedInventory.size() + 1);
        candidates.add(this.getMainHandItem());
        candidates.addAll(clonedInventory);
        for (ItemStack st : candidates) {
            if (st.isEmpty()) continue;
            int tier;
            if (st.getItem() instanceof FloatingTurretItem) tier = 0;
            else if (isProperWeapon(st)) tier = 1;
            else tier = 2;
            double a = weaponAttack(st);
            if (tier == 2 && a <= 0) continue; // 工具沒攻擊加成不考慮
            if (a > bestAtkPerTier[tier]) {
                bestAtkPerTier[tier] = a;
                bestPerTier[tier] = st;
            }
        }
        ItemStack best = ItemStack.EMPTY;
        for (int i = 0; i < bestPerTier.length; i++) {
            if (!bestPerTier[i].isEmpty()) { best = bestPerTier[i]; break; }
        }
        if (!best.isEmpty() && !ItemStack.isSameItemSameComponents(best, this.getMainHandItem())) {
            this.setItemSlot(EquipmentSlot.MAINHAND, best.copy());
            this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    // 真武器：劍 / 斧 / 三叉戟（不含鎬鋤鏟那種純工具）
    private static boolean isProperWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof net.minecraft.world.item.SwordItem
                || stack.getItem() instanceof net.minecraft.world.item.AxeItem
                || stack.getItem() instanceof net.minecraft.world.item.TridentItem;
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
        revealEquipment(); // 保險：跳過動畫時不會經過 REVEAL_TICK，這裡補套裝備（冪等）
        setNoAi(false);
        setNoGravity(false);
        setInvulnerable(false);
        setPos(introX, introBaseY, introZ);
        if (level() instanceof ServerLevel sl) {
            // 同源去重保險：清掉任何殘留的同源分身（退出再進入時 chunk 載入時序可能漏網）
            getSourceUUID().ifPresent(id -> {
                for (PlayerCloneEntity other : sl.getEntitiesOfClass(PlayerCloneEntity.class,
                        getBoundingBox().inflate(300),
                        e -> e != this && e.getSourceUUID().map(id::equals).orElse(false))) {
                    other.discard();
                }
            });
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(64))) {
                bossEvent.addPlayer(p);
            }
            // BGM 觸發改到 customServerAiStep 統一處理（per-player 一次性追蹤），這裡不直接送 packet
            graceTicks = 40; // 站定 2 秒給玩家準備，不立刻偷襲
            // 返回裂縫與娜拉幻影由 ensureCompanions（active tick）確保恰好一個
        }
        // 繞行砲由 customServerAiStep 的 turretsSpawned 守門生成（涵蓋啟動 + 重載後重生）
    }

    private void spawnMirroredTurrets() {
        if (!(level() instanceof ServerLevel sl)) return;
        // 自走砲：slotIndex 0/1（繞行）
        for (int i = 0; i < mirroredTurrets.size(); i++) {
            spawnCloneTurret(sl, mirroredTurrets.get(i), i);
        }
        // 手持砲：mirroredHandTurrets 永遠 [main, off]（可為 EMPTY），slotIndex 2=主手、3=副手
        for (int i = 0; i < mirroredHandTurrets.size(); i++) {
            ItemStack hs = mirroredHandTurrets.get(i);
            if (hs.isEmpty()) continue;
            spawnCloneTurret(sl, hs, i + 2);
        }
    }

    // 依目前裝備的主/副手浮游砲重建 mirroredHandTurrets（永遠 2 槽 [main, off]，可 EMPTY）
    // revealEquipment 後呼叫，讓 equipBestOffhand 從背包挑的浮游砲也能變成實體會射擊
    private void rebuildHandTurretsFromEquipped() {
        mirroredHandTurrets.clear();
        ItemStack main = this.getMainHandItem();
        ItemStack off = this.getOffhandItem();
        mirroredHandTurrets.add(main.getItem() instanceof FloatingTurretItem ? filterMirroredTurret(main) : ItemStack.EMPTY);
        mirroredHandTurrets.add(off.getItem() instanceof FloatingTurretItem ? filterMirroredTurret(off) : ItemStack.EMPTY);
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
        // 二階段變身過場期間完全無敵（玩家相機鎖住，沒理由還能繼續打）
        if (phase2TransitionTicks > 0) return false;
        // 免疫自己浮游砲造成的傷害（含蓄力彈爆炸），避免 boss 自爆
        if (source.getDirectEntity() instanceof FloatingTurretProjectile proj && proj.getOwner() == this) {
            return false;
        }
        // 副手有盾且攻擊來自前方 → 擋掉這次傷害，盾消耗耐久（玩家要學會繞背攻）
        if (!isArmored() && tryShieldBlock(source)) return false;
        // 變身期間：玩家攻擊打在方塊外殼上（與本體血量分離），其他傷害（環境/生物/自爆）走正常流程不被吞
        if (isArmored() && level() instanceof ServerLevel armorLevel
                && source.getEntity() instanceof Player attacker && attacker.isAlive()) {
            float dealt = attacker.getMainHandItem().is(ItemTags.PICKAXES) ? amount : amount * 0.2F; // tag 涵蓋 modded 鎬
            armorHp -= dealt;
            armorLevel.playSound(null, blockPosition(), SoundEvents.DEEPSLATE_BREAK, SoundSource.HOSTILE, 0.9F, 0.8F);
            armorLevel.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.0, getZ(), 6, 0.6, 1.0, 0.6, 0.1);
            if (armorHp <= 0F) breakArmor(armorLevel);
            return false; // 玩家攻擊：本體血量受外殼保護
        }
        boolean result = super.hurt(source, amount);
        // 只在鏡反狀態反傷（玩家看到鏡面輪廓就該停手），不再是純機率
        if (result && !level().isClientSide
                && isReflecting()
                && source.getEntity() instanceof Player attacker
                && attacker.isAlive()) {
            attacker.hurt(level().damageSources().magic(), amount);
        }
        return result;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        // 死亡視覺/聽覺回饋（boss bar 不立刻撤掉，留給 deathTime 動畫期間漸層淡出）
        playDeathFeedback(sl);
        // 停止戰鬥 BGM
        stopBgmForAll(sl);
        clearArmorParts(); // 死亡清掉殘留外殼方塊
        discardOwnedArmorDisplays(sl); // fallback：reload 後 pendingArmorRebuild 未消費就死的情況下，存盤孤兒仍會清掉
        MinecraftServer server = sl.getServer();
        boolean firstClear = true; // 無 source 的 boss（/summon 邊角）視為首次
        if (getSourceUUID().isPresent()) {
            UUID srcId = getSourceUUID().get();
            VoidMirrorSavedData saved = VoidMirrorSavedData.get(server);
            firstClear = !saved.isCleared(srcId); // 必須在 markCleared 前判斷
            saved.markCleared(srcId);
            // 同步立刻嘗試刪本維度（mirror）內的裂縫；overworld 入口裂縫 chunk 通常已卸載，
            // 改用 pendingCrackRemoval 旗標，讓裂縫自己 tick 時 self-discard
            SpaceCrackEntity.removeForOwner(sl, srcId);
            SpaceCrackEntity.removeForOwner(server.overworld(), srcId);
            saved.markCrackRemovalPending(srcId);
            // 勝利後娜拉消失
            for (NaraPhantomEntity nara : sl.getEntitiesOfClass(NaraPhantomEntity.class,
                    new AABB(BlockPos.ZERO).inflate(260),
                    n -> n.getSourceUUID().map(srcId::equals).orElse(false))) {
                nara.discard();
            }
        }
        // 維度內已無其他存活分身 → 開獎勵寶箱（鏡核碎片只首次擊敗才放，材料每次都有）
        boolean anyCloneLeft = !sl.getEntitiesOfClass(PlayerCloneEntity.class,
                new AABB(BlockPos.ZERO).inflate(260), e -> e != this && e.isAlive()).isEmpty();
        if (!anyCloneLeft) spawnRewardChest(sl, firstClear);
    }

    // 停掉所有收到 BGM 的玩家的戰鬥音樂
    private void stopBgmForAll(ServerLevel sl) {
        com.github.nalamodikk.common.network.packet.client.BossBgmPacket stop =
                com.github.nalamodikk.common.network.packet.client.BossBgmPacket.STOP;
        for (net.minecraft.server.level.ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
            if (bgmSentTo.contains(p.getUUID())) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, stop);
            }
        }
        bgmSentTo.clear();
    }

    // 死亡動畫回饋：爆破粒子環、震動音效、紫色靈魂消散
    private void playDeathFeedback(ServerLevel sl) {
        double x = getX(), y = getY() + 1.0, z = getZ();
        // 爆破粒子環（紫紅色魔力消散感）
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                x, y, z, 40, 0.6, 1.0, 0.6, 0.05);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                x, y, z, 30, 0.8, 1.2, 0.8, 0.10);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                x, y, z, 80, 1.2, 1.5, 1.2, 0.3);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                x, y, z, 20, 0.5, 0.8, 0.5, 0.05);
        // 死亡音效（雙重音：vanilla ender_dragon_death 低頻 + wither_death 高頻 + 玻璃碎裂感）
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_DEATH,
                SoundSource.HOSTILE, 1.4F, 1.2F);
        sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.HOSTILE, 1.8F, 0.55F);
    }

    private void spawnRewardChest(ServerLevel sl, boolean includeShard) {
        BlockPos chestPos = new BlockPos(0, 64, -3);
        // 先清空舊箱子內容物再 setBlock，避免 vanilla `setBlockAndUpdate` 觸發舊 chest 的
        // Block.onRemove → dropResources → 內容物噴一地。之前玩家重打 boss 時就因為這個 bug
        // 看到上次的獎勵掉在地上。
        if (sl.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity oldChest) {
            oldChest.clearContent();
        }
        sl.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        VoidMirrorEvents.addModifiedBlock(chestPos.asLong());
        if (sl.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            chest.clearContent();
            if (includeShard) {
                chest.setItem(13, new ItemStack(ModItems.MIRROR_CORE_SHARD.get())); // 中央：紀念物（限首次）
            }
            chest.setItem(10, new ItemStack(ModItems.MANA_INGOT.get(), 4));
            chest.setItem(11, new ItemStack(ModItems.MANA_DUST.get(), 8));
            chest.setItem(15, new ItemStack(ModItems.CORRUPTED_MANA_DUST.get(), 4));
            chest.setItem(16, new ItemStack(ModItems.MANA_INGOT.get(), 2));
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            this.bossEvent.removeAllPlayers();
            clearArmorParts();
            if (level() instanceof ServerLevel sl) discardOwnedArmorDisplays(sl); // fallback 同 die()
        }
        super.remove(reason);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (pendingArmorRebuild && level() instanceof ServerLevel rebuildLevel) {
            pendingArmorRebuild = false;
            rebuildArmor(rebuildLevel); // 重載後仍在外殼狀態 → 重建外殼接續二階段
        }
        // 變身期間血條顯示外殼血量（讓玩家看到挖外殼的進度），否則顯示本體血量
        this.bossEvent.setProgress(isArmored()
                ? Math.max(0F, armorHp / ARMOR_MAX_HP)
                : this.getHealth() / this.getMaxHealth());

        // BGM 觸發放在 graceTicks 早返之前，避免 2 秒延遲（玩家進範圍就播）
        if (level() instanceof ServerLevel bgmLevel) {
            com.github.nalamodikk.common.network.packet.client.BossBgmPacket bgm =
                    com.github.nalamodikk.common.network.packet.client.BossBgmPacket.INSTANCE;
            for (ServerPlayer p : bgmLevel.players()) {
                if (this.distanceToSqr(p) > 200.0 * 200.0) continue;
                if (bgmSentTo.add(p.getUUID())) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, bgm);
                }
            }
        }

        if (graceTicks > 0) {
            graceTicks--;
            setTarget(null); // 進場緩衝：站定不鎖定、不攻擊
            return;
        }
        // 二階段變身過場：boss 凍結（不追、不選技、不放招），方塊從遠處飛來組裝，讓 client 演出環繞鏡頭
        if (phase2TransitionTicks > 0) {
            phase2TransitionTicks--;
            setTarget(null);
            this.setDeltaMovement(Vec3.ZERO);
            tickArmorAssemble();
            return;
        }

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
            boolean has = speed.hasModifier(ANTI_KITE_SPEED_ID);
            if (tgt != null && tgt.isAlive()) {
                double d2 = this.distanceToSqr(tgt);
                // 遲滯：>12 格才開加速、<8 格才關，避免在邊界反覆開關造成走停抖動
                if (!has && d2 > 144.0) speed.addTransientModifier(ANTI_KITE_SPEED);
                else if (has && d2 < 64.0) speed.removeModifier(ANTI_KITE_SPEED_ID);
            } else if (has) {
                speed.removeModifier(ANTI_KITE_SPEED_ID);
            }
        }

        // 啟動後（或重載後）生成繞行砲，只做一次
        if (!turretsSpawned) {
            spawnMirroredTurrets();
            turretsSpawned = true;
        }

        // BGM 觸發已移到上方 graceTicks 之前（避免 2 秒延遲），這裡留空

        updatePhase();

        // 半血變身：召喚方塊機甲包住本體（只觸發一次）
        if (!armorTriggered && getHealth() <= getMaxHealth() * 0.5F
                && level() instanceof ServerLevel armorLevel) {
            armorTriggered = true;
            enterArmored(armorLevel);
            return;
        }
        // 變身期間：外殼面向玩家、緩慢逼近並擺腿走路（外殼被挖爆才解除）
        if (isArmored()) {
            LivingEntity tgt = getTarget();
            if (tgt != null && tgt.isAlive()) {
                double dx = tgt.getX() - getX();
                double dz = tgt.getZ() - getZ();
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)); // 面向玩家
                setYRot(yaw);
                this.yBodyRot = yaw;
                this.yHeadRot = yaw;
                if (dx * dx + dz * dz > 9.0) { // >3 格才逼近，太近就停讓玩家挖外殼
                    Vec3 step = new Vec3(dx, 0, dz).normalize().scale(0.08);
                    setDeltaMovement(step.x, getDeltaMovement().y, step.z);
                    hurtMarked = true;
                    walkPhase += 0.35; // 推進走路相位（擺腿）
                } else {
                    setDeltaMovement(0, getDeltaMovement().y, 0);
                }
            } else {
                setDeltaMovement(0, getDeltaMovement().y, 0);
            }
            tickArmorFollow(); // 設好朝向後再跟隨
            ensureCompanions(); // armored 期間也維護同源娜拉/返回裂縫（外殼期可能拉長，避免它們在這段消失）
            return;
        }

        tickAntiPillar(); // 全階段防墊高，開場就不給 cheese
        tickHotbarSwitch(); // 模擬玩家：戰鬥中定時切換主手快捷欄武器，讓 boss 戰鬥節奏有變化
        tickTurretVolley(); // 主動齊射：boss 親自下令所有浮游砲蓄力齊射玩家（10 秒一次）
        tickPillarSkill(); // 招牌技能：墊方塊衝撞擊飛（全階段）
        tickMeleeStrafe(); // 近戰：邊繞圈邊攻擊（取代 MeleeAttackGoal 的站定揮擊）
        tickReflect(); // 週期性鏡反狀態（取代純機率反傷）
        tickAirChase(); // 玩家飛高時墊方塊往上跳追擊（像玩家 pillar jump）
        tickPendingLaunch(); // RAM_WALL 橫向擊退後的第二段上彈
        tickSkillBlockClear(); // 擊飛後依序打掉技能墊的方塊
        ensureCompanions(); // 確保同源娜拉幻影 + 返回裂縫各恰好一個（重載/死亡重進入後重建、去重）
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

    // ── 招牌技能：墊方塊衝撞擊飛（三種形態隨機輪用，玩家可蹲下緩衝）──────────────
    private void tickPillarSkill() {
        if (skillCooldown > 0) skillCooldown--;
        if (!(level() instanceof ServerLevel sl)) return;
        if (!(getTarget() instanceof Player p) || !p.isAlive()) {
            pendingSkill = null;
            entityData.set(TELEGRAPH_SKILL, 0);
            return;
        }

        if (pendingSkill != null) {
            // 前置動作：站定蓄力、面向玩家，朝玩家噴漸強預警粒子，給玩家 1 秒反應/閃避
            this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
            this.getNavigation().stop();
            this.getLookControl().setLookAt(p);
            Vec3 d = horizUnit(p.position().subtract(this.position()));
            float prog = 1.0f - skillChargeTicks / (float) SKILL_TELEGRAPH;
            int count = 4 + (int) (prog * 10); // 越接近發動越密
            sl.sendParticles(ParticleTypes.CRIT,
                    getX() + d.x * 0.6, getY() + 1.0, getZ() + d.z * 0.6,
                    count, 0.3, 0.3, 0.3, 0.12 + prog * 0.1);
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    getX(), getY() + 1.2, getZ(), 2, 0.4, 0.4, 0.4, 0.0);
            if (--skillChargeTicks <= 0) {
                executeSkill(sl, pendingSkill, p);
                pendingSkill = null;
                entityData.set(TELEGRAPH_SKILL, 0);
                skillCooldown = SKILL_COOLDOWN;
            }
            return;
        }

        if (skillCooldown > 0) return;
        double d2 = this.distanceToSqr(p);
        if (d2 > SKILL_RANGE_SQR || d2 < SKILL_MIN_SQR) return;
        pendingSkill = PillarSkill.values()[this.random.nextInt(PillarSkill.values().length)];
        skillChargeTicks = SKILL_TELEGRAPH;
        skillTargetPos = p.blockPosition();                  // 鎖定當下地點：預兆固定於此，玩家跑出即可閃避
        entityData.set(SKILL_TARGET, skillTargetPos);
        entityData.set(TELEGRAPH_SKILL, pendingSkill.ordinal() + 1); // 同步給 client 畫預兆
        // 每招不同蓄力音，配合預兆讓玩家辨識
        net.minecraft.sounds.SoundEvent windup = switch (pendingSkill) {
            case RAM_WALL -> SoundEvents.WARDEN_SONIC_CHARGE;
            case LIFT_UP -> SoundEvents.PISTON_EXTEND;
            case CHARGE_RAMP -> SoundEvents.RAVAGER_ROAR;
        };
        sl.playSound(null, blockPosition(), windup, SoundSource.HOSTILE, 1.0F, 1.0F);
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
    private void tickPendingLaunch() {
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

    // 壓迫式徘徊：追到 2~3 格威脅距離就繞圈、保持距離（不貼臉），主要威脅靠頻繁技能
    private static final double KEEP_DISTANCE = 2.5;

    private void tickMeleeStrafe() {
        if (graceTicks > 0 || pendingSkill != null) return; // 緩衝期/技能前搖時不動
        if (!(getTarget() instanceof LivingEntity tgt) || !tgt.isAlive()) return;
        if (attackCooldown > 0) attackCooldown--;
        getLookControl().setLookAt(tgt, 30f, 30f);
        double dist = this.distanceTo(tgt);
        Vec3 toward = horizUnit(tgt.position().subtract(this.position()));
        Vec3 side = new Vec3(-toward.z, 0, toward.x); // 繞圈方向
        if (dist > KEEP_DISTANCE + 1.5) {
            this.getNavigation().moveTo(tgt, 1.15); // 太遠：追近
        } else {
            this.getNavigation().stop();
            Vec3 move = dist < KEEP_DISTANCE - 0.6
                    ? side.scale(0.6).add(toward.scale(-0.6)) // 太近：邊繞邊退開，不貼臉
                    : side.scale(0.7);                         // 距離剛好：繞圈徘徊（壓迫）
            Vec3 want = this.position().add(move);
            this.getMoveControl().setWantedPosition(want.x, this.getY(), want.z, 1.0);
        }
        // 真的貼到才近戰（低傷，主要威脅是技能）
        double reach = this.getBbWidth() * 2.0 + tgt.getBbWidth();
        if (dist <= reach && attackCooldown <= 0) {
            this.swing(InteractionHand.MAIN_HAND);
            this.doHurtTarget(tgt);
            attackCooldown = 16;
        }
    }

    // 鏡反狀態：每隔一段時間進入 1.5 秒，期間打 boss 才反傷（client 會畫鏡面輪廓提示）
    private void tickReflect() {
        if (reflectTicks > 0) {
            if (--reflectTicks == 0) entityData.set(REFLECTING, false);
            return;
        }
        if (reflectCooldown > 0) { reflectCooldown--; return; }
        reflectTicks = REFLECT_DURATION;
        reflectCooldown = REFLECT_INTERVAL;
        entityData.set(REFLECTING, true);
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 0.9F, 1.6F);
        }
    }

    // ── 二階段方塊機甲 ──────────────────────────────────────────────
    // 半血變身：放大碰撞箱涵蓋機甲（只大碰撞箱、不碰模型），用 BlockDisplay 組人形外殼；本體保持正常大小，由 renderer 移到機甲頭部
    private void enterArmored(ServerLevel sl) {
        if (isArmored()) return;
        entityData.set(ARMORED, true);
        armorHp = ARMOR_MAX_HP;
        this.bossEvent.setColor(BossEvent.BossBarColor.RED); // 血條轉紅，配合顯示外殼血量提示在打外殼
        // 清掉進行中的技能狀態，否則變身前正在前搖的招式會卡住、變身後一直重放同一招
        pendingSkill = null;
        skillChargeTicks = 0;
        skillCooldown = SKILL_COOLDOWN;
        entityData.set(TELEGRAPH_SKILL, 0);
        pendingLaunchTarget = null;
        pendingLaunchTimer = 0;
        armoredDimensions = true; // 放大碰撞箱（getDimensions），讓玩家打得到外殼
        refreshDimensions();
        // 啟動變身過場（先設 ticks 再 buildArmorShell，讓 spawnArmorPart 偵測到過場進行中、把方塊放在遠處等待飛入）
        // config 關閉時：略過過場，方塊直接到位、不鎖相機
        boolean cinematic = com.github.nalamodikk.common.config.ModCommonConfig.INSTANCE.phase2CinematicEnabled.get();
        phase2TransitionTicks = cinematic ? PHASE2_TRANSITION_LEN : 0;
        buildArmorShell(sl);
        if (cinematic) assignAssembleDelays(sl); // 每塊方塊隨機洗牌 + 後期更密的延遲（觀感從慢到快）
        sl.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 1.2F, 0.7F);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 60, 1.5, 2.0, 1.5, 0.02);
        // 廣播 client 端鎖相機環繞演出（過場開啟時才送）
        if (cinematic) {
            Phase2TransitionPacket payload = new Phase2TransitionPacket(this.getId());
            for (ServerPlayer p : sl.players()) {
                if (this.distanceToSqr(p) <= 300.0 * 300.0) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, payload);
                }
            }
        }
    }

    public void skipPhase2Transition() {
        phase2TransitionTicks = 0;
    }

    public boolean isPhase2Transitioning() {
        return phase2TransitionTicks > 0;
    }

    // 重載後仍在外殼狀態：清掉存盤殘留的孤兒外殼並重建，接續二階段（不重播變身演出，armorHp 維持讀回值）
    private void rebuildArmor(ServerLevel sl) {
        discardOwnedArmorDisplays(sl);
        entityData.set(ARMORED, true);
        armoredDimensions = true;
        refreshDimensions();
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        buildArmorShell(sl);
    }

    // 嘗試從結構模板（data/koniava/structure/mecha_shell.nbt）載入機甲形狀；若找不到回 false fallback 到 hardcoded MECH_SHAPE
    private static final ResourceLocation MECH_TEMPLATE_ID =
            ResourceLocation.fromNamespaceAndPath(KoniavacraftMod.MOD_ID, "mecha_shell");
    // 模板標記方塊：玩家蓋圖時用這些方塊定位
    // AMETHYST_BLOCK = 本體錨點（剛好對齊 boss 頭探出眼縫的位置），WHITE_WOOL = 一般外殼，RED_WOOL = 左腿（會擺動），BLUE_WOOL = 右腿（會擺動）
    private static final double ARMORED_ANCHOR_Y = 12.0; // 錨點對應的 world oy（boss 頭部會出現處）

    private boolean tryBuildArmorFromTemplate(ServerLevel sl) {
        var templateOpt = sl.getStructureManager().get(MECH_TEMPLATE_ID);
        if (templateOpt.isEmpty()) return false;
        net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template = templateOpt.get();
        net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings settings =
                new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
        var anchors = template.filterBlocks(BlockPos.ZERO, settings, Blocks.AMETHYST_BLOCK);
        if (anchors.isEmpty()) return false; // 必須有錨點才能用
        BlockPos anchorPos = anchors.get(0).pos();
        placeArmorMarker(sl, template, settings, anchorPos, Blocks.WHITE_WOOL, 0);
        placeArmorMarker(sl, template, settings, anchorPos, Blocks.RED_WOOL, 1);
        placeArmorMarker(sl, template, settings, anchorPos, Blocks.BLUE_WOOL, 2);
        placeFixedArmorMarker(sl, template, settings, anchorPos, Blocks.YELLOW_WOOL,
                Blocks.SEA_LANTERN.defaultBlockState(), 0); // 機甲眼：固定 SEA_LANTERN 不被 inventory 替換
        collectTurretMounts(template, settings, anchorPos);
        return true;
    }

    private void collectTurretMounts(net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template,
                                     net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings settings,
                                     BlockPos anchorPos) {
        turretMountOffsets.clear();
        var blocks = template.filterBlocks(BlockPos.ZERO, settings, Blocks.LIME_WOOL);
        // 由 X 升序排（左到右），對應到 turret slotIdx 0..N
        blocks.sort(java.util.Comparator.comparingInt(info -> info.pos().getX()));
        for (var info : blocks) {
            BlockPos p = info.pos();
            double ox = p.getX() - anchorPos.getX();
            double oy = (p.getY() - anchorPos.getY()) + ARMORED_ANCHOR_Y;
            double oz = p.getZ() - anchorPos.getZ();
            turretMountOffsets.add(new Vec3(ox, oy, oz));
        }
    }

    /** 提供給 FloatingTurretEntity：取得 slotIdx 對應的砲位 local offset（模板定義）。null = 沒有對應砲位，砲走 fallback 軌道。 */
    @Nullable
    public Vec3 getTurretMountOffset(int slotIdx) {
        if (slotIdx < 0 || slotIdx >= turretMountOffsets.size()) return null;
        return turretMountOffsets.get(slotIdx);
    }

    private void placeArmorMarker(ServerLevel sl,
                                  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template,
                                  net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings settings,
                                  BlockPos anchorPos, net.minecraft.world.level.block.Block marker, int legSide) {
        var blocks = template.filterBlocks(BlockPos.ZERO, settings, marker);
        for (var info : blocks) {
            BlockPos p = info.pos();
            double ox = p.getX() - anchorPos.getX();
            double oy = (p.getY() - anchorPos.getY()) + ARMORED_ANCHOR_Y;
            double oz = p.getZ() - anchorPos.getZ();
            BlockState state = pickShellBlockState(sl);
            CompoundTag stateTag = new CompoundTag();
            stateTag.put("block_state", NbtUtils.writeBlockState(state));
            spawnArmorPart(sl, ox, oy, oz, stateTag, legSide);
        }
    }

    // 固定材質標記：marker 位置 → 永遠生成 fixedState（不從 inventory/env 取代），用於「機甲眼」這類發光點綴
    private void placeFixedArmorMarker(ServerLevel sl,
                                       net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate template,
                                       net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings settings,
                                       BlockPos anchorPos, net.minecraft.world.level.block.Block marker,
                                       BlockState fixedState, int legSide) {
        var blocks = template.filterBlocks(BlockPos.ZERO, settings, marker);
        for (var info : blocks) {
            BlockPos p = info.pos();
            double ox = p.getX() - anchorPos.getX();
            double oy = (p.getY() - anchorPos.getY()) + ARMORED_ANCHOR_Y;
            double oz = p.getZ() - anchorPos.getZ();
            CompoundTag stateTag = new CompoundTag();
            stateTag.put("block_state", NbtUtils.writeBlockState(fixedState));
            spawnArmorPart(sl, ox, oy, oz, stateTag, legSide);
        }
    }

    private void buildArmorShell(ServerLevel sl) {
        if (tryBuildArmorFromTemplate(sl)) return; // 找得到模板就用，找不到 fallback 下面的 hardcoded 剪影
        int rows = MECH_SHAPE.length;
        for (int row = 0; row < rows; row++) {
            String line = MECH_SHAPE[row];
            int center = line.length() / 2; // 奇數寬度的中央 col (7→3, 5→2)
            for (int col = 0; col < line.length(); col++) {
                char ch = line.charAt(col);
                if (ch != 'o') continue; // 只有 'o' 是外殼，b/'.' 跳過
                BlockState state = pickShellBlockState(sl);
                CompoundTag stateTag = new CompoundTag();
                stateTag.put("block_state", NbtUtils.writeBlockState(state));
                double ox = col - center;                // 中央對齊本體
                double oy = (rows - 1 - row);            // 最後一列=腳=本體腳高
                int leg = (row >= LEG_TOP_ROW) ? (col < center ? 1 : (col > center ? 2 : 0)) : 0;
                spawnArmorPart(sl, ox, oy, 0, stateTag, leg);
            }
        }
    }

    // 機甲外殼方塊來源：優先從 clonedInventory（複製自玩家背包）扣除一個 BlockItem，否則從 boss 周圍環境取一格方塊（記入 modifiedBlocks 供離開時還原），最後 fallback 黑曜石
    private BlockState pickShellBlockState(ServerLevel sl) {
        for (ItemStack stack : clonedInventory) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof BlockItem bi) {
                stack.shrink(1);
                return bi.getBlock().defaultBlockState();
            }
        }
        BlockState env = takeNearbyTerrainBlock(sl);
        if (env != null) return env;
        return Blocks.OBSIDIAN.defaultBlockState();
    }

    // 從 boss 周圍掃描可用方塊（非空氣、非流體、非基岩、非已被本機改過），挖空並回傳狀態
    private BlockState takeNearbyTerrainBlock(ServerLevel sl) {
        BlockPos bp = blockPosition();
        int r = 10;
        for (int attempts = 0; attempts < 48; attempts++) {
            int dx = sl.random.nextInt(2 * r + 1) - r;
            int dy = sl.random.nextInt(r + 1) - (r / 2); // 偏向腰部高度
            int dz = sl.random.nextInt(2 * r + 1) - r;
            BlockPos p = bp.offset(dx, dy, dz);
            BlockState state = sl.getBlockState(p);
            if (state.isAir()) continue;
            if (!state.getFluidState().isEmpty()) continue;
            if (state.getDestroySpeed(sl, p) < 0) continue; // 不可破壞（基岩等）
            // 避開被自己已經放過或挖過的紀錄方塊（VoidMirrorEvents.addModifiedBlock 用同樣 long key）
            // 註：放/挖都會被加進來，所以這個 check 兼任避開自己組的外殼源頭
            VoidMirrorEvents.addMinedTerrain(p.asLong(), state); // 記錄原始狀態，離開維度時還原
            sl.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            return state;
        }
        return null;
    }

    // Display.BlockDisplay.setBlockState 是 private，只能透過 NBT（block_state）設定外觀
    private void spawnArmorPart(ServerLevel sl, double ox, double oy, double oz, CompoundTag stateTag, int legSide) {
        Display.BlockDisplay d = EntityType.BLOCK_DISPLAY.create(sl);
        if (d == null) return;
        d.load(stateTag.copy());
        d.addTag(armorTag());
        // 變身過場進行中：方塊先放在隨機遠處（上空 + 散布），tickArmorAssemble 插值飛到正位
        Vec3 spawnOff = phase2TransitionTicks > 0
                ? new Vec3(ox + (sl.random.nextDouble() - 0.5) * 14.0,
                           oy + 10.0 + sl.random.nextDouble() * 6.0,
                           oz + (sl.random.nextDouble() - 0.5) * 14.0)
                : new Vec3(ox, oy, oz);
        d.setPos(getX() + spawnOff.x - 0.5, getY() + spawnOff.y, getZ() + spawnOff.z - 0.5);
        sl.addFreshEntity(d);
        armorParts.add(d);
        armorOffsets.add(new Vec3(ox, oy, oz)); // local 偏移（右 x、上 y、前 z），跟隨時依朝向旋轉
        armorSpawnOffsets.add(spawnOff);
        armorAssembleDelay.add(0); // 暫填，buildArmorShell 結束後 assignAssembleDelays 改寫
        armorLegSide.add(legSide);
    }

    // 變身過場結束後或方塊組裝後，給每塊方塊指派飛入起飛 tick：順序隨機洗牌，間距用 sqrt 縮放 → 早期稀疏、後期密集（觀感由慢到快）
    private void assignAssembleDelays(ServerLevel sl) {
        int n = armorParts.size();
        if (n == 0) return;
        java.util.List<Integer> order = new ArrayList<>(n);
        for (int i = 0; i < n; i++) order.add(i);
        java.util.Collections.shuffle(order, new java.util.Random(sl.random.nextLong()));
        int span = Math.max(1, PHASE2_TRANSITION_LEN - ARMOR_TRAVEL_TICKS - 5); // 預留 5 tick 收尾
        for (int k = 0; k < n; k++) {
            float u = k / (float) Math.max(1, n - 1);
            float biased = (float) Math.pow(u, 1.8); // 後期更密 → 觀感加速
            int delay = Math.round(biased * span);
            armorAssembleDelay.set(order.get(k), delay);
        }
    }

    // 變身過場期間：每塊方塊各自有起飛延遲（從 spawnOffset 飛到 final offset），單塊用 ease-in（t²，慢→快）。tickArmorFollow 在過場結束後接手。
    private void tickArmorAssemble() {
        if (armorParts.isEmpty()) return;
        int elapsed = PHASE2_TRANSITION_LEN - phase2TransitionTicks;
        float yawRad = getYRot() * ((float) Math.PI / 180F);
        double sinY = Math.sin(yawRad), cosY = Math.cos(yawRad);
        for (int i = 0; i < armorParts.size(); i++) {
            Display.BlockDisplay d = armorParts.get(i);
            if (d == null || !d.isAlive()) continue;
            Vec3 f = armorOffsets.get(i);
            Vec3 sp = i < armorSpawnOffsets.size() ? armorSpawnOffsets.get(i) : f;
            int delay = i < armorAssembleDelay.size() ? armorAssembleDelay.get(i) : 0;
            float local = Mth.clamp((elapsed - delay) / (float) ARMOR_TRAVEL_TICKS, 0F, 1F);
            float s = local * local; // ease-in 單塊由慢加速到位
            double ox = Mth.lerp(s, sp.x, f.x);
            double oy = Mth.lerp(s, sp.y, f.y);
            double oz = Mth.lerp(s, sp.z, f.z);
            double wx = ox * cosY - oz * sinY;
            double wz = ox * sinY + oz * cosY;
            d.setPos(getX() + wx - 0.5, getY() + oy, getZ() + wz - 0.5);
        }
    }

    // 每 tick 讓外殼方塊跟著本體移動
    private void tickArmorFollow() {
        if (armorParts.isEmpty()) return;
        float yawRad = getYRot() * ((float) Math.PI / 180F);
        double sinY = Math.sin(yawRad), cosY = Math.cos(yawRad);
        for (int i = 0; i < armorParts.size(); i++) {
            Display.BlockDisplay d = armorParts.get(i);
            if (d == null || !d.isAlive()) continue;
            Vec3 o = armorOffsets.get(i);
            double lz = o.z;
            int leg = armorLegSide.get(i);
            if (leg != 0) { // 腿沿前後方向繞髖擺動（越靠腳擺幅越大），左右反相
                double phase = walkPhase + (leg == 2 ? Math.PI : 0.0);
                lz += (LEG_HIP_Y - o.y) * Math.sin(phase) * 0.35;
            }
            // 依本體朝向把 local 偏移（右 x、前 z）旋轉到世界座標
            double wx = o.x * cosY - lz * sinY;
            double wz = o.x * sinY + lz * cosY;
            d.setPos(getX() + wx - 0.5, getY() + o.y, getZ() + wz - 0.5);
        }
    }

    // 外殼被挖爆：剝落外殼、本體現身落地，回到一階段（玩家型態）行為繼續被攻擊
    private void breakArmor(ServerLevel sl) {
        // 剝落：每塊外殼位置噴深邃石碎裂粒子，再移除（比單一爆炸更像機甲崩解）
        net.minecraft.core.particles.BlockParticleOption crumble =
                new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                        Blocks.OBSIDIAN.defaultBlockState());
        for (Display.BlockDisplay d : armorParts) {
            if (d == null || !d.isAlive()) continue;
            sl.sendParticles(crumble, d.getX(), d.getY() + 0.5, d.getZ(), 6, 0.2, 0.2, 0.2, 0.12);
        }
        clearArmorParts();
        entityData.set(ARMORED, false);
        armoredDimensions = false; // 碰撞箱還原正常
        refreshDimensions();
        this.bossEvent.setColor(BossEvent.BossBarColor.WHITE); // 外殼破，血條恢復白色顯示本體血量
        sl.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_DEATH, SoundSource.HOSTILE, 1.3F, 0.8F);
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.0, getZ(), 12, 1.5, 2.0, 1.5, 0.0);
    }

    // 清掉殘留的外殼方塊（死亡 / 離開時呼叫，不留孤兒 display）
    private void clearArmorParts() {
        for (Display.BlockDisplay d : armorParts) {
            if (d != null) d.discard();
        }
        armorParts.clear();
        armorOffsets.clear();
        armorSpawnOffsets.clear();
        armorAssembleDelay.clear();
        armorLegSide.clear();
    }

    // 額外掃描場地，清掉「自己」tag 的孤兒 display（fallback：pendingArmorRebuild 未消費就 die、或存盤殘留）
    private void discardOwnedArmorDisplays(ServerLevel sl) {
        String myTag = armorTag();
        for (Display.BlockDisplay d : sl.getEntitiesOfClass(Display.BlockDisplay.class,
                this.getBoundingBox().inflate(24.0), e -> e.getTags().contains(myTag))) {
            d.discard();
        }
    }

    // 玩家飛高（被擊飛或自行升空）時，分身墊方塊往上跳追擊，像玩家 pillar jump
    private void tickAirChase() {
        if (!(level() instanceof ServerLevel sl)) return;
        if (!(getTarget() instanceof Player p) || !p.isAlive()) return;
        if (!this.onGround()) return; // 落在地面/方塊上才墊跳
        int dh = p.blockPosition().getY() - this.blockPosition().getY();
        if (dh < 3) {
            if (!chaseBlocks.isEmpty()) clearChaseBlocks(sl); // 追完落地就收掉墊的柱，不殘留卡地形
            return;
        }
        placeChaseBlock(sl, this.blockPosition(), takeWallBlock()); // 腳下放方塊
        this.setDeltaMovement(this.getDeltaMovement().x, 0.42, this.getDeltaMovement().z); // 跳起，落到新方塊上升一格
        this.hurtMarked = true;
    }

    // 追擊柱用的方塊：記進 chaseBlocks（追完清）+ placedWalls（anti-pillar 不拆自己）+ 重製清除
    private void placeChaseBlock(ServerLevel sl, BlockPos p, BlockState block) {
        if (!sl.getWorldBorder().isWithinBounds(p)) return;
        if (!sl.getBlockState(p).canBeReplaced()) return;
        sl.setBlockAndUpdate(p, block);
        long key = p.asLong();
        chaseBlocks.add(key);
        placedWalls.add(key);
        VoidMirrorEvents.addModifiedBlock(key);
    }

    private void clearChaseBlocks(ServerLevel sl) {
        for (long l : chaseBlocks) {
            BlockPos bp = BlockPos.of(l);
            if (sl.isLoaded(bp)) sl.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
            placedWalls.remove(l);
        }
        chaseBlocks.clear();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false; // 分身會墊方塊上下機動，不受墜落傷害
    }

    // 娜拉幻影 + 返回裂縫由活著的 boss 確保同源恰好一個（重建/去重），重載與死亡重進入都不殘留
    private void ensureCompanions() {
        if (naraCheckCooldown > 0) { naraCheckCooldown--; return; }
        if (!(level() instanceof ServerLevel sl)) return;
        naraCheckCooldown = 40;
        // 非變身狀態下清掉「自己」殘留的孤兒外殼方塊（重載後 armorParts 會清空但 BlockDisplay 存盤；owner-specific tag 避免清到別的 boss 的活外殼）
        if (!isArmored()) discardOwnedArmorDisplays(sl);
        UUID id = getSourceUUID().orElse(null);
        if (id == null) return;
        // 娜拉幻影（不存盤）
        List<NaraPhantomEntity> naras = sl.getEntitiesOfClass(NaraPhantomEntity.class,
                getBoundingBox().inflate(300),
                n -> n.getSourceUUID().map(id::equals).orElse(false));
        if (naras.isEmpty()) {
            VoidMirrorTeleport.spawnNaraPhantom(sl, id);
        } else {
            for (int i = 1; i < naras.size(); i++) naras.get(i).discard();
        }
        // 返回裂縫（boss 進攻中才有）
        List<SpaceCrackEntity> cracks = sl.getEntitiesOfClass(SpaceCrackEntity.class,
                getBoundingBox().inflate(300),
                c -> c.getOwnerUUID().map(id::equals).orElse(false));
        if (cracks.isEmpty()) {
            SpaceCrackEntity exit = ModEntities.SPACE_CRACK.get().create(sl);
            if (exit != null) {
                exit.moveTo(0.5, 64.0, 0.5, 0.0F, 0.0F);
                exit.setOwnerUUID(id);
                sl.addFreshEntity(exit);
            }
        } else {
            for (int i = 1; i < cracks.size(); i++) cracks.get(i).discard();
        }
    }

    private void executeSkill(ServerLevel sl, PillarSkill skill, Player p) {
        clearSkillBlocksNow(sl); // 先收掉上次技能殘留的方塊，避免堆積
        BlockPos target = skillTargetPos;                     // 前搖鎖定的地點（非玩家當下位置）
        Vec3 targetCenter = Vec3.atCenterOf(target);
        double dodgeRSq = SKILL_DODGE_RADIUS * SKILL_DODGE_RADIUS;
        // CHARGE_RAMP 是線狀攻擊（boss→鎖定點），用點到線段距離；其他用以鎖定點為中心的球體距離
        boolean dodged = skill == PillarSkill.CHARGE_RAMP
                ? pointToSegmentDistSqrXZ(p.getX(), p.getZ(),
                        this.getX(), this.getZ(), targetCenter.x, targetCenter.z) > dodgeRSq
                : (p.getX() - targetCenter.x) * (p.getX() - targetCenter.x)
                        + (p.getZ() - targetCenter.z) * (p.getZ() - targetCenter.z) > dodgeRSq;
        Vec3 awayFromClone = p.position().subtract(this.position()); // 命中時的擊退方向（玩家當下位置）
        Vec3 d = horizUnit(targetCenter.subtract(this.position()));
        BlockState block = takeWallBlock();
        switch (skill) {
            case RAM_WALL -> {
                // 從鎖定點朝分身方向水平排 3 格方塊，命中的玩家往反方向擊退
                Direction toClone = Direction.getNearest(this.getX() - targetCenter.x, 0, this.getZ() - targetCenter.z);
                int height = 1 + this.random.nextInt(2);
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
                    BlockPos bp = BlockPos.containing(getX() + d.x * i, getY() + 1, getZ() + d.z * i);
                    placeSkillBlock(sl, bp, block);
                }
                this.setDeltaMovement(d.x * 0.8, this.getDeltaMovement().y, d.z * 0.8); // 平滑衝刺逼近，不瞬移
                this.hurtMarked = true;
                if (!dodged && this.distanceToSqr(p) <= 25.0) knockbackPlayer(p, awayFromClone, 1.6, 0.6);
                sl.playSound(null, blockPosition(), SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 1.0F, 0.6F);
            }
        }
        entityData.set(SKILL_TARGET, BlockPos.ZERO); // 清鎖定點（client 停止畫預兆）
        skillClearTimer = SKILL_BLOCK_LIFETIME; // 擊飛後 1 秒開始依序打掉
    }

    private void placeSkillBlock(ServerLevel sl, BlockPos p, BlockState block) {
        if (!sl.getWorldBorder().isWithinBounds(p)) return;
        if (!sl.getBlockState(p).canBeReplaced()) return; // 只放在空氣/可替換處，不覆蓋地板或既有方塊
        sl.setBlockAndUpdate(p, block);
        long key = p.asLong();
        placedWalls.add(key);
        skillBlocks.add(key);
        VoidMirrorEvents.addModifiedBlock(key);
    }

    // 技能墊的方塊：倒數後每 tick 依序打掉一格（快速、按放置順序）
    private void tickSkillBlockClear() {
        if (skillClearTimer < 0) return;
        if (skillClearTimer > 0) { skillClearTimer--; return; }
        if (!(level() instanceof ServerLevel sl)) return;
        if (skillBlocks.isEmpty()) { skillClearTimer = -1; return; }
        long key = skillBlocks.remove(0);
        BlockPos bp = BlockPos.of(key);
        sl.destroyBlock(bp, false);
        placedWalls.remove(key);
        if (skillBlocks.isEmpty()) skillClearTimer = -1;
    }

    private void clearSkillBlocksNow(ServerLevel sl) {
        for (long key : skillBlocks) {
            BlockPos bp = BlockPos.of(key);
            if (sl.isLoaded(bp)) sl.setBlockAndUpdate(bp, Blocks.AIR.defaultBlockState());
            placedWalls.remove(key);
        }
        skillBlocks.clear();
        skillClearTimer = -1;
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
        tag.putBoolean("ArmorTriggered", armorTriggered); // 重載後不重複觸發變身
        tag.putBoolean("Armored", isArmored());           // 仍在外殼狀態 → 重載時重建外殼接續二階段
        tag.putFloat("ArmorHp", armorHp);
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
        armorTriggered = tag.getBoolean("ArmorTriggered");
        if (tag.getBoolean("Armored")) {
            armorHp = tag.getFloat("ArmorHp");
            pendingArmorRebuild = true;        // 首次 tick 重建外殼（此時 level 可能尚未 ready）
            entityData.set(ARMORED, true);     // 立刻設同步狀態：防 autosave 寫 Armored=false、防 reload 首 tick 攻擊繞過外殼直接扣本體血
            armoredDimensions = true;           // 立即套用大碰撞箱（onSyncedDataUpdated 也會設，這裡明示）
            refreshDimensions();
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
