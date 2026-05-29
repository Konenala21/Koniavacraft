package com.github.nalamodikk.common.entity;

import com.github.nalamodikk.KoniavacraftMod;
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
import com.github.nalamodikk.register.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

public class PlayerCloneEntity extends Monster {

    public static final float MAX_HP = 500.0F;

    public enum Phase { NORMAL, WALLING, BERSERK }

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
    final Set<Long> placedWalls = new HashSet<>(); // package-private：牆系統主人，PlayerCloneCombatSkills 借讀（避免拆到自己疊的方塊）
    private int wallCooldown = 0;
    private int drainCooldown = 0;

    // 主動戰術 controller（反墊柱 / 拆工事 / 俯衝 / 招牌墊方塊技能 / 上彈 / 技能方塊清理；擁有技能 state）
    private final PlayerCloneCombatSkills skills = new PlayerCloneCombatSkills(this);
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
    boolean introActive = false; // package-private：intro 系統主人，PlayerCloneCombatSkills.tickDive 讀（過場期間不觸發俯衝）
    private int introTicks = 0;
    private double introX, introBaseY, introZ;
    private final EnumMap<EquipmentSlot, ItemStack> pendingEquipment = new EnumMap<>(EquipmentSlot.class);
    // 鏡像的浮游砲（已過濾不鏡射的升級）。自走砲（繞行）vs 手持（雙持蓄力）分開
    private final List<ItemStack> mirroredTurrets = new ArrayList<>();      // 裝備槽/背包 → 繞行自走砲
    private final List<ItemStack> mirroredHandTurrets = new ArrayList<>();  // 主手/副手 → 手持蓄力
    private boolean turretsSpawned = false; // 不存盤：重載後為 false → 重新生成砲

    // ── 二階段方塊機甲（半血變身：以 BlockDisplay 偽裝方塊組成大型人形包住本體）──
    // 已送出 BGM 觸發 packet 的玩家 UUID（避免重複送）。Transient，重載 / 重啟自動重置 → 玩家重進時 BGM 會再觸發
    final java.util.Set<UUID> bgmSentTo = new HashSet<>(); // package-private：啟動時在此寫入、死亡演出由 PlayerCloneDeathSequence.stopBgm 讀取
    // 機甲視覺 controller（外殼建構 / 飛入動畫 / 跟隨擺腿 / 剝落 / 清理；擁有 display 清單）
    private final PlayerCloneArmorRig armorRig = new PlayerCloneArmorRig(this);

    private boolean armorTriggered = false;   // 半血只觸發一次
    private boolean armoredDimensions = false; // 鏡像 ARMORED 給 getDimensions 用（避免在 entityData 尚未 define 時讀取）
    private boolean pendingArmorRebuild = false; // 重載時若仍在外殼狀態，首次 tick 重建外殼（接續二階段）
    private float armorHp = 0f;               // 外殼血量，與本體血量分離
    private static final float ARMOR_MAX_HP = 200f;
    private boolean armorWasBroken = false;   // 三階段判斷：曾經破過外殼回到本體型態
    // 二階段變身過場（server tick 倒數）：期間 boss 凍結 AI + 無敵，client 端鎖相機環繞
    // package-private：PlayerCloneArmorRig 的飛入動畫需讀過場進度與總長
    static final int PHASE2_TRANSITION_LEN = 220;
    int phase2TransitionTicks = 0;
    private static final EntityDimensions ARMOR_DIMENSIONS =
            EntityDimensions.scalable(5.0F, 16.0F); // 涵蓋機甲外殼的碰撞箱
    double walkPhase = 0;                 // package-private：走路動畫相位（本體移動時推進，PlayerCloneArmorRig 跟隨時讀來擺腿）
    // 本體鑲嵌深度：玩家模型整個藏進機甲，腳被胸甲擋住，只有頭剛好對齊 row 2 的眼縫從中露出
    // 玩家身高 ~1.8 → feet=11 head_center≈12.5 ≈ row 2 (oy 12-13) 中央
    public static final double ARMORED_BODY_OFFSET_Y = 11.0;
    // 機甲頭頂的世界座標 oy（rows-1 - row 0 = 14），浮游砲坐落在頭頂上方使用
    public static final double ARMORED_HEAD_TOP_Y = 14.0;

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
    // 死亡演出階段（0=未死亡, 1=stagger, 2=glow_up, 3=crack, 4=shatter, 5=final_flash）
    // 自製 120-tick 死亡動畫，覆寫 vanilla tickDeath。Renderer 讀此狀態切換視覺。
    private static final EntityDataAccessor<Integer> DEATH_PHASE =
            SynchedEntityData.defineId(PlayerCloneEntity.class, EntityDataSerializers.INT);

    // 死亡演出時序常量（tick 為單位），總共 400t = 20 秒
    public static final int DEATH_TOTAL_TICKS = 400;
    public static final int DEATH_PHASE_STAGGER_END = 65;    // 65t  搖晃 (3.25s)
    public static final int DEATH_PHASE_GLOW_END    = 165;   // 100t 白熱化 (5s)
    public static final int DEATH_PHASE_CRACK_END   = 275;   // 110t 裂痕 (5.5s)
    public static final int DEATH_PHASE_SHATTER_END = 365;   // 90t  碎裂 (4.5s)
    // 365-400 = 35t final flash (1.75s)

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

    // 死亡演出 controller（五階段動畫 / 音效 / 粒子 / 停 BGM / 獎勵箱）
    private final PlayerCloneDeathSequence deathSequence = new PlayerCloneDeathSequence(this);

    // package-private 探針：暴露 LivingEntity.dead（protected）給 PlayerCloneDeathSequence 判斷其他 boss 是否已進入死亡
    boolean isDeathMarked() {
        return this.dead;
    }

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
        builder.define(DEATH_PHASE, 0);
    }

    public int getDeathPhase() {
        return entityData.get(DEATH_PHASE);
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

    // package-private setter：PlayerCloneCombatSkills 透過這兩個把技能預兆 / 鎖定點同步給 client（entityData 是 protected）
    void setTelegraphSkill(int v) {
        entityData.set(TELEGRAPH_SKILL, v);
    }

    void setSkillTarget(BlockPos pos) {
        entityData.set(SKILL_TARGET, pos);
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
    protected EntityDimensions getDefaultDimensions(Pose pose) {
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

        // HurtByTargetGoal 設定要忽略其他 PlayerCloneEntity 的傷害 — 不然多人同場 boss
        // 互相打 AoE 會啟動 retaliate，boss-vs-boss 互砍把 fight 變太簡單
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        // 只鎖定「自己這隻 boss 的 source player」— 多人模式下不會去抓別的玩家
        // 沒有 sourceUUID（/summon 出來的）就 fallback 抓任何玩家
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> getSourceUUID().map(id -> p.getUUID().equals(id)).orElse(true)));
    }

    // 多人同場：忽略其他 PlayerCloneEntity 之間的傷害（不然 AoE / 砲彈會誤傷
    // 隔壁的 boss，那邊 retaliate 回打，雙方 source player 都看戲，戰鬥變得太簡單）
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.getEntity() instanceof PlayerCloneEntity other && other != this) return true;
        return super.isInvulnerableTo(source);
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
        for (ItemStack s : pool) if (!s.isEmpty() && s.getItem() instanceof ShieldItem) prioritized.add(s);
        // 其餘物品（包含工具、方塊等）尾隨在後
        for (ItemStack s : pool) {
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof FloatingTurretItem) continue;
            if (isProperWeapon(s)) continue;
            if (s.getItem() instanceof ShieldItem) continue;
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
        updateBossBarName();
        // 進場過場期間先不發光（保留神秘感），由 customServerAiStep 在「正常戰鬥」狀態才開
        this.setGlowingTag(false);
    }

    // 血條標題：「鏡中的自己 - PlayerName [二階段/三階段]」
    private void updateBossBarName() {
        String srcName = getSourceName();
        if (srcName == null || srcName.isEmpty()) srcName = "???";
        Component base = Component.translatable("entity.koniava.player_clone.bossbar", srcName);
        Component phaseSuffix = isArmored()
                ? Component.translatable("entity.koniava.player_clone.phase2")
                : armorWasBroken
                    ? Component.translatable("entity.koniava.player_clone.phase3")
                    : null;
        this.bossEvent.setName(phaseSuffix == null ? base
                : base.copy().append(Component.literal(" ")).append(phaseSuffix));
    }

    // Boss 主動發砲齊射：每 10 秒一次，前 1 秒在每門砲上噴 END_ROD 粒子當預兆，然後同時對玩家蓄力齊射
    private void tickTurretVolley() {
        if (!com.github.nalamodikk.common.config.ModCommonConfig.INSTANCE.bossTurretVolleyEnabled.get()) return;
        if (turretVolleyTelegraph > 0) {
            turretVolleyTelegraph--;
            if (level() instanceof ServerLevel sl) {
                for (FloatingTurretEntity t : findOwnedTurrets(sl)) {
                    sl.sendParticles(ParticleTypes.END_ROD,
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
        this.setDeltaMovement(Vec3.ZERO);
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
        if (!(this.getOffhandItem().getItem() instanceof ShieldItem)) return false;
        if (source.is(DamageTypeTags.BYPASSES_SHIELD)) return false;
        Entity src = source.getDirectEntity();
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
            else if (shield.isEmpty() && st.getItem() instanceof ShieldItem) shield = st;
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
        return stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof AxeItem
                || stack.getItem() instanceof TridentItem;
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
            // 啟動 Nara 跟隨玩家：intro 結束後 Nara 不再固定在 spawn 點，
            // 避免玩家追 boss 走遠後 boss 死亡演出抓不到她（findNara 範圍 80 格）
            getSourceUUID().ifPresent(srcId -> {
                for (NaraPhantomEntity nara : sl.getEntitiesOfClass(NaraPhantomEntity.class,
                        new AABB(BlockPos.ZERO).inflate(300),
                        n -> n.getSourceUUID().map(srcId::equals).orElse(false))) {
                    nara.enablePlayerFollow();
                }
            });
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
        // 變身期間：只有鎬 或 浮游砲蓄力彈（爆破力）能打破外殼，其他武器完全免疫
        // 這樣玩家有近戰（鎬）+ 遠程（蓄力彈）兩個選項，不會被綁死在近戰
        if (isArmored() && level() instanceof ServerLevel armorLevel
                && source.getEntity() instanceof Player attacker && attacker.isAlive()) {
            boolean isPickaxe = attacker.getMainHandItem().is(ItemTags.PICKAXES);
            boolean isChargedTurret = source.getDirectEntity() instanceof FloatingTurretProjectile proj
                    && proj.getChargeRatio() > 0F;
            if (!isPickaxe && !isChargedTurret) {
                // 既不是鎬也不是蓄力彈：完全擋下，給聲音 + 火星粒子但不掉血、不顯示傷害
                armorLevel.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6F, 1.6F);
                armorLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.0, getZ(), 4, 0.4, 0.6, 0.4, 0.05);
                return false;
            }
            // 蓄力彈額外 +50% 因為爆破破甲合理，玩家也才有動機切換武器
            float dealt = isChargedTurret ? amount * 1.5F : amount;
            armorHp -= dealt;
            armorLevel.playSound(null, blockPosition(), SoundEvents.DEEPSLATE_BREAK, SoundSource.HOSTILE, 0.9F, 0.8F);
            armorLevel.sendParticles(ParticleTypes.CRIT, getX(), getY() + 1.0, getZ(), 6, 0.6, 1.0, 0.6, 0.1);
            // 顯示傷害數字（CRIT 色 = 黃色，提示「鎬有效」）
            for (ServerPlayer p : armorLevel.players()) {
                if (this.distanceToSqr(p) < 64 * 64) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,
                            new com.github.nalamodikk.common.network.packet.client.turret.DamageNumberPacket(
                                    getX(), getY() + getBbHeight() * 0.8, getZ(), amount,
                                    com.github.nalamodikk.common.network.packet.client.turret.DamageNumberPacket.CRIT,
                                    getId()));
                }
            }
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
        // 偵測指令殺死 / out_of_world：跳過 20s 演出直接 remove（管理員/開發 debug 方便）
        boolean isCommandKill = cause.is(DamageTypes.GENERIC_KILL)
                || cause.is(DamageTypes.FELL_OUT_OF_WORLD);
        super.die(cause);
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        // 死亡演出：120-tick 五階段動畫由 tickDeath() 接手，這裡只負責停 BGM 與清理任務
        this.setGlowingTag(false); // 死亡演出期間不發光（保留戲劇 / 神秘感）
        deathSequence.stopBgm(sl);
        armorRig.clearParts(); // 死亡清掉殘留外殼方塊
        armorRig.discardOwnedDisplays(sl); // fallback：reload 後 pendingArmorRebuild 未消費就死的情況下，存盤孤兒仍會清掉
        MinecraftServer server = sl.getServer();
        boolean firstClear = true; // 無 source 的 boss（/summon 邊角）視為首次
        if (getSourceUUID().isPresent()) {
            UUID srcId = getSourceUUID().get();
            VoidMirrorSavedData saved = VoidMirrorSavedData.get(server);
            firstClear = !saved.isCleared(srcId); // 必須在 markCleared 前判斷
            saved.markCleared(srcId);
            // 只清 overworld 入口裂縫（boss 已過）；mirror dim 的返回裂縫保留給玩家當出口
            // pendingCrackRemoval 旗標也只影響非 mirror 維度（見 SpaceCrackEntity.tick）
            SpaceCrackEntity.removeForOwner(server.overworld(), srcId);
            saved.markCrackRemovalPending(srcId);
            // 勝利後娜拉延後消失：boss 死亡演出 20s + Nara 結語 outro ~10s + 緩衝
            // outro 期間 client camera 會切到 Nara 視角播放台詞
            for (NaraPhantomEntity nara : sl.getEntitiesOfClass(NaraPhantomEntity.class,
                    new AABB(BlockPos.ZERO).inflate(260),
                    n -> n.getSourceUUID().map(srcId::equals).orElse(false))) {
                nara.startVictoryFarewell(DEATH_TOTAL_TICKS + 300); // 20s 死亡 + 15s outro/fade 緩衝
            }
        }
        // 寶箱生成延到 Phase 5（演出尾聲）才開，跟視覺節奏對齊（玩家看完爆破才看到獎勵）
        pendingRewardFirstClear = firstClear;

        // 指令殺死：跳過動畫，立刻生成寶箱 + 撤回 Nara + 立即 remove，相機/輸入不會被鎖
        if (isCommandKill) {
            boolean anyCloneLeft = !sl.getEntitiesOfClass(PlayerCloneEntity.class,
                    new AABB(BlockPos.ZERO).inflate(260), e -> e != this && e.isAlive()).isEmpty();
            if (!anyCloneLeft) deathSequence.spawnRewardChest(sl, firstClear);
            // 立刻撤掉 Nara（沒人要看 outro）
            for (NaraPhantomEntity nara : sl.getEntitiesOfClass(NaraPhantomEntity.class,
                    new AABB(BlockPos.ZERO).inflate(260),
                    n -> getSourceUUID().map(srcId -> n.getSourceUUID().map(srcId::equals).orElse(false)).orElse(true))) {
                nara.discard();
            }
            this.remove(RemovalReason.KILLED);
        }
    }

    boolean pendingRewardFirstClear = false; // package-private：die() 寫入、死亡演出 Phase 5 由 PlayerCloneDeathSequence 讀取

    // 覆寫 vanilla 20-tick 死亡 → 自製 400-tick 五階段演出（內容見 PlayerCloneDeathSequence）
    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (level().isClientSide) return;
        if (!(level() instanceof ServerLevel sl)) return;

        int phase = PlayerCloneDeathSequence.computeDeathPhase(this.deathTime);
        int currentPhase = entityData.get(DEATH_PHASE);
        if (phase != currentPhase) {
            entityData.set(DEATH_PHASE, phase);
            deathSequence.onPhaseEnter(sl, phase);
        }
        deathSequence.tickPhase(sl, this.deathTime, phase);

        if (this.deathTime >= DEATH_TOTAL_TICKS) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            this.bossEvent.removeAllPlayers();
            armorRig.clearParts();
            if (level() instanceof ServerLevel sl) armorRig.discardOwnedDisplays(sl); // fallback 同 die()
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
        // glow outline 只在「正常戰鬥」狀態開（多人混戰可見性）；過場/變身/死亡不開保留神秘感
        boolean inCinematic = introActive || phase2TransitionTicks > 0 || this.deathTime > 0;
        if (this.hasGlowingTag() == inCinematic) {
            this.setGlowingTag(!inCinematic);
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
            armorRig.tickAssemble();
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
            armorRig.tickFollow(); // 設好朝向後再跟隨
            ensureCompanions(); // armored 期間也維護同源娜拉/返回裂縫（外殼期可能拉長，避免它們在這段消失）
            return;
        }

        skills.tickAntiPillar(); // 全階段防墊高，開場就不給 cheese
        tickHotbarSwitch(); // 模擬玩家：戰鬥中定時切換主手快捷欄武器，讓 boss 戰鬥節奏有變化
        tickTurretVolley(); // 主動齊射：boss 親自下令所有浮游砲蓄力齊射玩家（10 秒一次）
        skills.tickDive(); // 玩家躲高處 / 太遠 → boss 跳起來俯衝重接戰
        if (skills.isDiving()) return; // 俯衝期間不跑其他 AI（不然會卡技能或停下來）
        skills.tickPillarSkill(); // 招牌技能：墊方塊衝撞擊飛（全階段）
        tickMeleeStrafe(); // 近戰：邊繞圈邊攻擊（取代 MeleeAttackGoal 的站定揮擊）
        tickReflect(); // 週期性鏡反狀態（取代純機率反傷）
        tickAirChase(); // 玩家飛高時墊方塊往上跳追擊（像玩家 pillar jump）
        skills.tickPendingLaunch(); // RAM_WALL 橫向擊退後的第二段上彈
        skills.tickSkillBlockClear(); // 擊飛後依序打掉技能墊的方塊
        ensureCompanions(); // 確保同源娜拉幻影 + 返回裂縫各恰好一個（重載/死亡重進入後重建、去重）
        if (phase == Phase.WALLING) {
            tickWallBuilding();
            skills.tickBreakSurroundings();
        } else if (phase == Phase.BERSERK) {
            tickManaDrain();
            skills.tickBreakSurroundings();
        }
    }

    // 壓迫式徘徊：追到 2~3 格威脅距離就繞圈、保持距離（不貼臉），主要威脅靠頻繁技能
    private static final double KEEP_DISTANCE = 2.5;

    // 水平單位向量（tickMeleeStrafe 用）。PlayerCloneCombatSkills 也有一份同樣的 helper。
    private static Vec3 horizUnit(Vec3 v) {
        double len = Math.sqrt(v.x * v.x + v.z * v.z);
        if (len < 1.0e-4) return new Vec3(0, 0, 1);
        return new Vec3(v.x / len, 0, v.z / len);
    }

    private void tickMeleeStrafe() {
        if (graceTicks > 0 || skills.hasPendingSkill()) return; // 緩衝期/技能前搖時不動
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
        updateBossBarName(); // 顯示「二階段」尾標
        // 清掉進行中的技能狀態，否則變身前正在前搖的招式會卡住、變身後一直重放同一招
        skills.resetForArmored();
        armoredDimensions = true; // 放大碰撞箱（getDimensions），讓玩家打得到外殼
        refreshDimensions();
        // 啟動變身過場（先設 ticks 再 buildArmorShell，讓 spawnArmorPart 偵測到過場進行中、把方塊放在遠處等待飛入）
        // config 關閉時：略過過場，方塊直接到位、不鎖相機
        boolean cinematic = com.github.nalamodikk.common.config.ModCommonConfig.INSTANCE.phase2CinematicEnabled.get();
        phase2TransitionTicks = cinematic ? PHASE2_TRANSITION_LEN : 0;
        armorRig.buildShell(sl);
        if (cinematic) armorRig.assignAssembleDelays(sl); // 每塊方塊隨機洗牌 + 後期更密的延遲（觀感從慢到快）
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
    // Reload 後重建外殼（接續二階段，不重播變身演出）
    private void rebuildArmor(ServerLevel sl) {
        armorRig.discardOwnedDisplays(sl);
        entityData.set(ARMORED, true);
        armoredDimensions = true;
        refreshDimensions();
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        armorRig.buildShell(sl);
        updateBossBarName(); // reload 後也要顯示「二階段」尾標
        this.setGlowingTag(true); // 多人模式可見性，reload 後也要重新開
    }

    /** 提供給 FloatingTurretEntity：取得 slotIdx 對應的砲位 local offset（模板定義）。null = 沒有對應砲位，砲走 fallback 軌道。 */
    @Nullable
    public Vec3 getTurretMountOffset(int slotIdx) {
        return armorRig.getTurretMountOffset(slotIdx);
    }

    // 外殼被挖爆：剝落外殼、本體現身落地，回到一階段（玩家型態）行為繼續被攻擊
    private void breakArmor(ServerLevel sl) {
        armorRig.breakVisual(sl); // 剝落：噴碎裂粒子 + 移除外殼方塊
        entityData.set(ARMORED, false);
        armoredDimensions = false; // 碰撞箱還原正常
        refreshDimensions();
        armorWasBroken = true;   // 三階段：標記曾破殼
        this.bossEvent.setColor(BossEvent.BossBarColor.WHITE); // 外殼破，血條恢復白色顯示本體血量
        updateBossBarName(); // 顯示「三階段」尾標
        sl.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_DEATH, SoundSource.HOSTILE, 1.3F, 0.8F);
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.0, getZ(), 12, 1.5, 2.0, 1.5, 0.0);
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
        if (!isArmored()) armorRig.discardOwnedDisplays(sl);
        UUID id = getSourceUUID().orElse(null);
        if (id == null) return;
        // 娜拉幻影（不存盤）
        List<NaraPhantomEntity> naras = sl.getEntitiesOfClass(NaraPhantomEntity.class,
                getBoundingBox().inflate(300),
                n -> n.getSourceUUID().map(id::equals).orElse(false));
        if (naras.isEmpty()) {
            VoidMirrorTeleport.spawnNaraPhantom(sl, id);
            // 重載後重生：若 intro 已結束（boss 進攻中）直接啟用跟隨，銜接 activateAfterIntro 漏掉的初始化
            if (!introActive) {
                for (NaraPhantomEntity nara : sl.getEntitiesOfClass(NaraPhantomEntity.class,
                        new AABB(BlockPos.ZERO).inflate(300),
                        n -> n.getSourceUUID().map(id::equals).orElse(false))) {
                    nara.enablePlayerFollow();
                }
            }
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

    BlockState takeWallBlock() { // package-private：PlayerCloneCombatSkills.executeSkill 借此取外殼/牆材
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
        tag.putBoolean("ArmorWasBroken", armorWasBroken); // 三階段判斷
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
        armorWasBroken = tag.getBoolean("ArmorWasBroken");
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
