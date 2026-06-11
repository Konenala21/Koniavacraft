package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.common.network.packet.client.ship.ShipBlockUpdatePacket;
import com.github.nalamodikk.common.network.packet.client.ship.ShipChestLidPacket;
import com.github.nalamodikk.common.block.blockentity.altar.AltarGeometry;
import com.github.nalamodikk.common.item.tool.StructureBuildWandItem;
import com.github.nalamodikk.register.ModBlocks;
import com.github.nalamodikk.common.capability.mana.ManaAction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.ContainerHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.ArrayVoxelShape;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.github.nalamodikk.space.ship.collision.CollisionList;
import com.github.nalamodikk.space.ship.collision.ContinuousOBBCollider;
import com.github.nalamodikk.space.ship.collision.Matrix3d;
import com.github.nalamodikk.space.ship.collision.OrientedBB;
import com.github.nalamodikk.space.ship.virtualworld.VirtualRenderWorld;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 飛船實體：承載一個 ShipContraption（一組方塊），組裝後方塊從世界移除、改由這個實體渲染/移動。
 *
 * M2a：只負責「持有 contraption + 存讀 + spawn 同步到 client」。還沒渲染（隱形）、還沒移動。
 *   渲染 = M2b（ShipEntityRenderer + 假世界）；六方向飛行 = M3。
 *
 * contraption 透過 IEntityWithComplexSpawn 在 spawn 時整包傳到 client（M2b 渲染要用）。
 */
public class ShipEntity extends Entity implements IEntityWithComplexSpawn {

    /** 找出 box 附近的飛船。用 level 自己的實體查詢(per-level 正確：client 查詢只回 client 的船，不會像共用
     *  static registry 在單機把 server 的船混進來/被 level 濾掉)。inflate 大(128)解決長船：船只登記在中心 section，
     *  玩家走到離 anchor 遠的尾端時搜尋盒要夠大才涵蓋到 anchor 那格。細部相交由呼叫端自己判。 */
    public static List<ShipEntity> nearbyShips(Level level, AABB box) {
        return level.getEntitiesOfClass(ShipEntity.class, box.inflate(128.0), s -> s.getContraption() != null);
    }


    private static final double MAX_SPEED = 0.4;  // 每 tick 最大速度
    private static final double ACCEL = 0.1;       // 朝目標速度的 lerp（慣性感）
    private static final float YAW_LERP = 0.15f;   // 船頭轉向駕駛視角的平滑度
    private static final float ROLL_RATE = 2.5f;   // A/D 翻滾速度（度/tick）
    public static final int MAX_DRIVERS = 2;        // 駕駛位數量（離核心最近的 N 張椅子）。其餘椅子=乘客
    private static final double SEAT_SIT_HEIGHT = 0.0; // 坐進椅子的高度（坐姿臀部還會往上，0.4 浮太高，可微調）

    @Nullable private ShipContraption contraption;
    private boolean intentionalDisassembly; // 正常拆解時 true，讓 remove() 的散架保險不重複寫回

    // 駕駛輸入（由 ShipInputPacket 每 tick 設；server 端用）。每個駕駛位一份，tick 時合併。
    private final float[] inForward = new float[MAX_DRIVERS];
    private final float[] inStrafe = new float[MAX_DRIVERS];
    private final int[] inVertical = new int[MAX_DRIVERS];
    private final float[] inYaw = new float[MAX_DRIVERS];
    private final float[] inPitch = new float[MAX_DRIVERS]; // 駕駛 look pitch → 船頭俯仰
    private Vec3 shipVel = Vec3.ZERO;

    // 座位指派：乘客 UUID(字串) → 座位 index。同步給 client（positionRider 兩端都要用）。
    // 點哪張椅子就坐哪張，不再照上船順序。
    private static final EntityDataAccessor<CompoundTag> DATA_SEATS =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.COMPOUND_TAG);
    // roll 不在 vanilla 旋轉同步裡，自己用 synched data 傳給 client（六面飛行的翻滾）
    private static final EntityDataAccessor<Float> DATA_ROLL =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);
    // 船頭在 local 框的 yaw 偏移(駕駛座朝向)。pitch/roll 要繞「船頭相對軸」轉，不然蓋的方向不同 pitch/roll 會反。
    private static final EntityDataAccessor<Float> DATA_BOW =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);
    // 油門 0~1：玩家調，實際速度 = 引擎決定的上限 × 油門。同步給 client(HUD + 之後客戶端顯示)。
    private static final EntityDataAccessor<Float> DATA_THROTTLE =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);
    // 目前總燃料(影子各燃料槽魔力和)。server 算、同步給 client 畫 HUD(client 沒影子查不到)。
    private static final EntityDataAccessor<Integer> DATA_FUEL =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    // 目前曲速能量(各進料口能量和)。同理同步給 HUD。
    private static final EntityDataAccessor<Integer> DATA_WARP_FUEL =
            SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);

    // client 端平滑跟隨 server 廣播位置用（lerpSteps 在 Entity 是 private 無 getter，自己存一份）
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYRot, lerpXRot;

    // 燃料/引擎(1b)：引擎數=速度上限、燃料槽 local=飛行時抽魔力的對象。contraption 變動時 recomputeFuelSystem() 重算。
    private int engineCount = 0;
    private final java.util.List<BlockPos> fuelTankLocals = new java.util.ArrayList<>();
    // 曲速引擎(#2,多方塊):核心 local(偵測輸入)、完整結構數、完整結構的進料口 local(曲速燃料從這抽)。
    private final java.util.List<BlockPos> warpCoreLocals = new java.util.ArrayList<>();
    private int warpDriveCount = 0;
    private final java.util.List<BlockPos> warpIntakeLocals = new java.util.ArrayList<>();
    private static final double SPEED_PER_ENGINE = 0.2;  // 每引擎貢獻的每 tick 速度上限(20 b/s=1.0/tick;~50 引擎到頂 200)
    private static final double SPEED_CAP = 10.0;         // 一般引擎天花板 10.0/tick=200 b/s。碰撞已子步進防穿牆;這麼快只適合高空/太空,貼地面 chunk 載入跟不上會穿插
    private static final int FUEL_PER_ENGINE_MOVE = 12;   // 移動時「每引擎」每 tick 耗魔力(滿油門);加速 ×2;隨油門縮放。引擎越多越快也越耗
    private static final double SPEED_PER_WARP = 4.0;     // 每「座」完整曲速結構貢獻(~6 座到頂 600)
    private static final double WARP_CAP = 30.0;          // 有曲速結構時的天花板 30.0/tick=600 b/s
    private static final int FUEL_PER_WARP_MOVE = 200;    // 每座曲速結構每 tick 從進料口抽的能量(很兇,要高密度燃料/魔力網路撐)

    public ShipEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;     // 自己用 contraption 邏輯處理，不走 vanilla 方塊碰撞
        this.setNoGravity(true);   // 飛船不受重力（太空/停在原地）
    }

    /** 設定某個駕駛位(seatIndex ∈ [0,MAX_DRIVERS))的輸入。由 ShipInputPacket 每 tick 從對應駕駛 client 設。 */
    public void setControlInput(int seatIndex, float forward, float strafe, int vertical, float yaw, float pitch) {
        if (seatIndex < 0 || seatIndex >= MAX_DRIVERS) return;
        this.inForward[seatIndex] = forward;
        this.inStrafe[seatIndex] = strafe;
        this.inVertical[seatIndex] = vertical;
        this.inYaw[seatIndex] = yaw;
        this.inPitch[seatIndex] = pitch;
    }

    /** 玩家坐在第幾張座位（依 DATA_SEATS 指派，對應 getSeats 的 index）。沒指派回上船順序，非乘客回 -1。 */
    public int seatIndexOf(Entity passenger) {
        CompoundTag tag = getEntityData().get(DATA_SEATS);
        String key = passenger.getUUID().toString();
        if (tag.contains(key)) return tag.getInt(key);
        return getPassengers().indexOf(passenger);
    }

    private void assignSeat(Entity passenger, int seatIndex) {
        CompoundTag tag = getEntityData().get(DATA_SEATS).copy();
        tag.putInt(passenger.getUUID().toString(), seatIndex);
        getEntityData().set(DATA_SEATS, tag);
    }

    private void unassignSeat(Entity passenger) {
        CompoundTag tag = getEntityData().get(DATA_SEATS).copy();
        tag.remove(passenger.getUUID().toString());
        getEntityData().set(DATA_SEATS, tag);
    }

    /** 某座位是否已被佔。 */
    private boolean isSeatOccupied(int seatIndex) {
        for (Entity p : getPassengers()) if (seatIndexOf(p) == seatIndex) return true;
        return false;
    }

    /** 第一個沒被佔的座位 index（找不到回 -1）。 */
    private int firstFreeSeat() {
        int n = getSeats().size();
        for (int i = 0; i < n; i++) if (!isSeatOccupied(i)) return i;
        return -1;
    }

    /** 主駕駛座位 = 最小的「有人坐的駕駛位」(index < MAX_DRIVERS)。沒有回 -1。 */
    private int primaryDriverSeat() {
        for (int i = 0; i < MAX_DRIVERS; i++) if (isSeatOccupied(i)) return i;
        return -1;
    }

    // client 渲染用：從 contraption NBT 還原的臨時 BlockEntity（箱子等 BER 方塊），建一次快取
    @Nullable private Map<BlockPos, BlockEntity> renderBEs;
    @Nullable private VirtualRenderWorld renderWorld; // BER 查方塊的假 Level
    // client：剛編輯放的方塊 → 時間戳。VBO 烤好前每幀先畫它們(不然先透明)。超過此時間視為已烤進 VBO。
    private final Map<BlockPos, Long> pendingVisualBlocks = new HashMap<>();
    private static final long PENDING_VISUAL_MS = 1500;

    /** client 渲染用：剛放、還沒進 VBO 的方塊(每幀先畫)。順便剪掉過期(已烤進 VBO)的。 */
    public Map<BlockPos, Long> getPendingVisualBlocks() {
        if (!pendingVisualBlocks.isEmpty()) {
            long now = System.currentTimeMillis();
            pendingVisualBlocks.values().removeIf(t -> now - t > PENDING_VISUAL_MS);
        }
        return pendingVisualBlocks;
    }

    /** VBO 烤好接棒了 → 清掉每幀先畫的集合(不重畫造成 z-fighting)。 */
    public void clearPendingVisualBlocks() {
        if (!pendingVisualBlocks.isEmpty()) pendingVisualBlocks.clear();
    }
    // client 渲染用：靜態方塊烤好的 VBO 快取（型別 Object 避免 server 載入 client 類別）
    @Nullable private Object meshCache;
    private boolean meshFailed;

    public void setContraption(ShipContraption contraption) {
        this.contraption = contraption;
        this.renderBEs = null;   // contraption 換了，BE 快取作廢
        this.meshCache = null;   // 烤好的 VBO 也作廢（contraption 只在 spawn 設一次，實務上不會走到）
        this.localCollisionShapeCache = null;
        this.collisionListCache = null;
        this.hullBlocksCache = null;
        this.dynamicMirrorCache = null;
        recomputeFuelSystem();   // 引擎數/燃料槽
        refreshDimensions();     // 依新 contraption 撐大 hitbox
    }

    // ── 靜態方塊 VBO 快取（client）──────────────────────────────────────────
    @Nullable public Object getMeshCache() { return meshCache; }
    public void setMeshCache(@Nullable Object cache) { this.meshCache = cache; }
    public boolean isMeshFailed() { return meshFailed; }
    public void markMeshFailed() { this.meshFailed = true; }

    /**
     * 換掉 contraption 裡某 local 方塊的 blockstate（互動方塊切狀態用，例如門開關）。
     * 失效碰撞形狀 + 渲染 VBO/BER 快取，門才會視覺打開、碰撞跟著變。server/client 都呼叫；
     * server 端另外送 ShipBlockUpdatePacket 給 client。
     */
    public void updateContraptionBlock(BlockPos local, BlockState state) {
        if (contraption == null) return;
        // 舊方塊(改之前)：給粒子用，也判斷要不要重算燃料系統(只有動到引擎/燃料槽才掃)
        var oldInfo = contraption.getBlocks().get(local);
        Block oldBlock = oldInfo != null ? oldInfo.state().getBlock() : null;
        // client：挖掉方塊(state=air)時噴破壞粒子（移除前用舊 state）
        if (level().isClientSide && state.isAir() && oldInfo != null && !oldInfo.state().isAir()) {
            spawnBreakParticles(local, oldInfo.state());
        }
        // state=air → 移除；已存在 → 換 state（保留 NBT）；不存在 → 新增。涵蓋 Phase2 切狀態 + 停船編輯加/挖
        if (state.isAir()) {
            contraption.removeBlock(local);
        } else if (contraption.getBlocks().containsKey(local)) {
            contraption.setBlockState(local, state);
        } else {
            contraption.addBlock(local, state, null);
        }
        // client：剛放的方塊每幀先 tesselate 顯示，避免等 debounce+背景烤(~0.5s)才出現(先透明)。烤好就清。
        if (level().isClientSide && !state.isAir()) {
            pendingVisualBlocks.put(local.immutable(), System.currentTimeMillis());
        }
        localCollisionShapeCache = null; collisionListCache = null;
        hullBlocksCache = null;
        dynamicMirrorCache = null;
        renderBEs = null;
        if (meshCache instanceof ShipMeshHandle h) {
            h.markDirty(); // 保留舊 VBO，debounce 後背景重烤一次(off-thread，不凍主執行緒)
        } else {
            meshCache = null; // 還沒建/烤失敗 → 讓渲染器重建
            meshFailed = false;
        }
        // 增量更新引擎數/燃料槽，不掃全船 → 大量放引擎/燃料槽逐個放也不卡(每次 O(1))。
        // 全船掃描只在組裝/載入(recomputeFuelSystem)做一次；這裡只調整這一格的差異。
        Block newBlock = state.getBlock();
        boolean oldEngine = oldBlock instanceof ManaEngineBlock, newEngine = newBlock instanceof ManaEngineBlock;
        if (oldEngine != newEngine) engineCount += newEngine ? 1 : -1;
        boolean oldTank = oldBlock instanceof ManaFuelTankBlock, newTank = newBlock instanceof ManaFuelTankBlock;
        if (oldTank && !newTank) fuelTankLocals.remove(local);
        else if (newTank && !oldTank) fuelTankLocals.add(local.immutable());
        // 曲速核心 list 增量維護
        boolean oldWarp = oldBlock instanceof ManaWarpEngineBlock, newWarp = newBlock instanceof ManaWarpEngineBlock;
        if (oldWarp && !newWarp) warpCoreLocals.remove(local);
        else if (newWarp && !oldWarp) warpCoreLocals.add(local.immutable());
        // 動到曲速相關方塊(核心/外殼魔力合金/進料口) → 重掃完整結構(結構完整性靠多方塊，不能增量)
        Block alloy = ModBlocks.MANA_ALLOY_BLOCK.get();
        if (oldWarp || newWarp || oldBlock == alloy || newBlock == alloy
                || oldBlock instanceof ManaWarpInputBlock || newBlock instanceof ManaWarpInputBlock) {
            recomputeWarpDrives();
        }
        refreshDimensions(); // bounds 可能變了，更新 hitbox
    }

    /** Client：收到影子鏡射過來的 BE NBT(物品底座的 item / 機器內容)→ 更新 contraption NBT + 只重建那一格的
     *  render BE(不整批 renderBEs=null 重建,太貴)。BER 之後就拿得到新 item 畫出漂浮物品。 */
    public void updateContraptionBlockEntityData(BlockPos local, CompoundTag nbt) {
        if (contraption == null) return;
        contraption.setBlockNbt(local, nbt);
        if (renderBEs != null && level() != null) {
            var info = contraption.getBlocks().get(local);
            if (info != null && info.state().getBlock() instanceof EntityBlock) {
                BlockEntity existing = renderBEs.get(local);
                if (existing != null && existing.getType().isValid(info.state())) {
                    // 套 NBT 到既有 render BE，保留 transient 動畫狀態(箱子 openness)。
                    // 重建(loadStatic)會把 openness 歸零，跟 ShipChestAnimator 的開蓋每 16 tick 打架 → 抽搐。
                    existing.loadWithComponents(nbt, level().registryAccess());
                } else {
                    BlockEntity be = BlockEntity.loadStatic(local, info.state(), nbt, level().registryAccess());
                    if (be != null) {
                        be.setLevel(renderWorld != null ? renderWorld : level());
                        renderBEs.put(local, be);
                        if (renderWorld != null) renderWorld.setBlockEntity(be);
                    }
                }
            }
        }
    }

    /** 結構杖等工具透過 forwardUseToShadow 在影子裡新蓋的方塊(祭壇柱子/底座/升級環)不在 contraption →
     *  掃 center(local 祭壇)±radius，把「影子有、視覺船沒有或不同」的方塊收進 contraption + 發給 client。
     *  一次性(杖少用)，範圍掃可接受。server 端呼叫。 */
    private void syncNewShadowBlocksToContraption(BlockPos center, int radius) {
        ServerLevel shadow = getShadow();
        if (shadow == null || contraption == null) {
            com.github.nalamodikk.KoniavacraftMod.LOGGER.info("[shipwand] no shadow/contraption, skip");
            return;
        }
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            BlockPos shadowPos = shadowAnchor.offset(p);
            BlockState ss = shadow.getBlockState(shadowPos);
            var info = contraption.getBlocks().get(p);
            BlockState cur = info != null ? info.state() : Blocks.AIR.defaultBlockState();
            if (ss.isAir() || ss.equals(cur)) continue;     // 影子空 / 沒變 → 跳過
            BlockPos lp = p.immutable();
            updateContraptionBlock(lp, ss);                 // server 本地:加進 contraption + 失效碰撞/mesh 快取 + refreshDimensions
            ShipBlockUpdatePacket.sendToClients(this, lp, ss); // 客戶端:沒這個 client 看不到方塊(只有 server 有 → 拆解才出現)
            structureMirror.add(lp);                        // 持久鏡射:祭壇成形/升級會把這些(非 EntityBlock)換成 resonance_ring 等,要持續同步
            if (ss.getBlock() instanceof EntityBlock) {
                BlockEntity sbe = shadow.getBlockEntity(shadowPos);
                if (sbe != null) {
                    CompoundTag nbt = sbe.saveWithFullMetadata(shadow.registryAccess());
                    nbt.remove("x"); nbt.remove("y"); nbt.remove("z");
                    contraption.setBlockNbt(lp, nbt);
                    com.github.nalamodikk.common.network.packet.client.ship.ShipBlockEntityDataPacket
                            .sendToClients(this, lp, nbt);
                }
            }
            n++;
        }
        com.github.nalamodikk.KoniavacraftMod.LOGGER.info("[shipwand] synced {} new blocks from shadow to ship (0=杖沒蓋成/沒跑)", n);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide) ShipShadowManager.unregisterActive(this); // 音效橋接:登出活躍船
        // 保險：被 /kill（KILLED）而非正常拆解時，把方塊寫回世界（船散架在原地），不要讓整艘船無聲蒸發。
        // DISCARDED 是「靜默移除」(vanilla 語意=不掉落，例如指令/清理/gametest 框架收尾)，不寫回，否則會把核心灑回原地污染。
        // 區塊卸載/維度切換也不觸發（會存檔/搬移）。
        if (!level().isClientSide && contraption != null && !intentionalDisassembly && reason == Entity.RemovalReason.KILLED) {
            recoverContraptionToWorld();
        }
        // VM4：船被移除但非 KILLED 回收（卸載/換維度/靜默 discard）時，解除影子 force-load 讓機器暫停省效能；方塊保留，重載再開
        if (!level().isClientSide && reason != Entity.RemovalReason.KILLED && shadowAnchor != null && contraption != null) {
            var server = level().getServer();
            if (server != null) {
                ServerLevel shadow = ShipShadowManager.shadowLevel(server);
                if (shadow != null) ShipShadowManager.setForceLoad(shadow, shadowAnchor, contraption.bounds(), false);
            }
            shadowForceLoaded = false;
        }
        // 釋放烤好的 VBO（GL 資源）。client 端、render thread 上做
        if (level().isClientSide && meshCache instanceof AutoCloseable ac) {
            try { ac.close(); } catch (Exception ignored) {}
            meshCache = null;
        }
        super.remove(reason);
    }

    /** 清掉影子維度裡這台船的方塊 + 解除 force-load + 釋放 slot（拆解/移除時）。 */
    void clearShadow() {
        if (shadowAnchor == null || contraption == null || level().isClientSide) return;
        var server = level().getServer();
        if (server == null) return;
        ServerLevel shadow = ShipShadowManager.shadowLevel(server);
        if (shadow != null) {
            ShipShadowManager.clearShadow(shadow, shadowAnchor, contraption);
            ShipShadowManager.setForceLoad(shadow, shadowAnchor, contraption.bounds(), false);
        }
        ShipShadowManager.get(server).free(getUUID());
        shadowAnchor = null;
    }

    /** 異常移除時把 contraption 寫回世界；放不下就掉落方塊與容器物品（寧可掉也不蒸發）。 */
    private void recoverContraptionToWorld() {
        clearShadow(); // 影子那份也要清，不然 slot 洩漏 + 機器繼續在影子跑
        Rotation rotation = snapRotation(getYRot());
        Vec3 coreWorld = rotatedWorldCorner(0, 0, 0);
        BlockPos target = new BlockPos(
                Mth.floor(coreWorld.x + 0.5), Mth.floor(coreWorld.y + 0.5), Mth.floor(coreWorld.z + 0.5));
        if (contraption.addToWorld(level(), target, rotation)) return;
        // 被擋：掉落方塊物品 + 容器內容（避免整艘材料消失）
        for (StructureBlockInfo info : contraption.getBlocks().values()) {
            ItemStack block = new ItemStack(info.state().getBlock().asItem());
            if (!block.isEmpty()) spawnAtLocation(block);
            if (info.nbt() != null && info.nbt().contains("Items")) {
                NonNullList<ItemStack> items = NonNullList.withSize(256, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(info.nbt(), items, level().registryAccess());
                for (ItemStack it : items) if (!it.isEmpty()) spawnAtLocation(it);
            }
        }
    }

    /**
     * client 渲染用：盤點 contraption 裡需要 BER 的方塊（EntityBlock + 有 NBT，例如箱子），
     * 從 NBT 還原成臨時 BlockEntity 並快取（不每幀重建）。setLevel 用真實 level 讓 BER 能查時間/光照。
     */
    public Map<BlockPos, BlockEntity> getRenderBlockEntities() {
        if (renderBEs == null) {
            renderBEs = new HashMap<>();
            if (contraption != null && level() != null) {
                // 假 Level：放進所有 contraption 方塊的 blockstate + EntityBlock 的 BE。BER 渲染時會查
                // level.getBlockState/getBlockEntity(鄰居)驗方塊/做箱子 double 合併 → 必須回 contraption 的方塊，
                // 不是主世界(船方塊已從主世界移除)。否則自訂 BER 不畫、箱子畫成單箱。
                renderWorld = new VirtualRenderWorld(level(), level().getMinBuildHeight(), level().getHeight(),
                        net.minecraft.core.Vec3i.ZERO, () -> {});
                for (var e : contraption.getBlocks().entrySet()) {
                    renderWorld.setBlock(e.getKey(), e.getValue().state(), 3);
                }
                for (var e : contraption.getBlocks().entrySet()) {
                    var info = e.getValue();
                    if (!(info.state().getBlock() instanceof EntityBlock eb)) continue;
                    BlockEntity be;
                    if (info.nbt() != null) {
                        be = BlockEntity.loadStatic(e.getKey(), info.state(), info.nbt(), level().registryAccess());
                    } else {
                        be = eb.newBlockEntity(e.getKey(), info.state());
                    }
                    if (be != null) {
                        be.setLevel(renderWorld); // BER 查的 level = 假 Level，不是主世界
                        renderWorld.setBlockEntity(be);
                        renderBEs.put(e.getKey(), be);
                    }
                }
                renderWorld.runLightEngine();
            }
        }
        return renderBEs;
    }

    @Nullable
    public ShipContraption getContraption() {
        return contraption;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && contraption == null) {
            discard(); // 沒有 contraption 的飛船無意義
            return;
        }
        // 記住上一 tick 的位置與角度，渲染對位置/yaw 的 partialTick 插值才不會跟碰撞分家（轉向歪）。
        this.setOldPosAndRot();
        this.rollO = getRoll(); // roll 不在 vanilla rot，自記上一 tick 供渲染插值

        if (level().isClientSide) {
            tickLerp(); // server 權威：client 只平滑跟隨 server 廣播的位置，不自己算移動
        } else {
            ensureShadowForceLoaded();                       // VM4：載入後確保影子 force-load（機器才 tick）
            if (shadowAnchor != null) ShipShadowManager.registerActive(this); // 音效橋接:登記活躍船(idempotent)
            tickServerMovement(); // server 是唯一真相，依駕駛輸入算移動
            if ((tickCount & 15) == 0) tickServerMirror(); // VM2：每 16 tick 把影子狀態鏡射回視覺船
        }
        setDeltaMovement(0, 0, 0); // 自己用 setPos 移動，不靠 vanilla 速度
    }

    // ── VM2：影子(真相)→視覺船(鏡像) 狀態鏡射 ─────────────────────────────────
    @Nullable
    private ServerLevel getShadow() {
        if (shadowAnchor == null || level().isClientSide) return null;
        var server = level().getServer();
        return server == null ? null : ShipShadowManager.shadowLevel(server);
    }

    /**
     * VM3：把對船上 BE 方塊的右鍵轉發給影子維度裡的真 BE（開真機器/容器 GUI）。
     * 影子 block 的 use 會 player.openMenu，menu 綁影子 BE；stillValid 由 MenuShipShadowMixin 放行。
     */
    private boolean forwardUseToShadow(ServerPlayer player, BlockPos local, BlockState expected, InteractionHand hand) {
        ServerLevel shadow = getShadow();
        if (shadow == null) return false;
        BlockPos sp = shadowAnchor.offset(local);
        BlockState ss = shadow.getBlockState(sp);
        if (ss.isAir() || ss.getBlock() != expected.getBlock()) return false; // 影子沒這方塊(放置失敗) → 退回
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(sp), Direction.UP, sp, false);
        ItemStack held = player.getItemInHand(hand);
        // 複製 vanilla 的方塊互動順序：很多機器(發電機/充能台...)是用 useItemOn 開選單，不是 useWithoutItem。
        // 潛行+手持物 → 跳過方塊互動，直接 item.useOn(設定工具之類對機器用)。
        boolean hasItem = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
        boolean sneakWithItem = player.isSecondaryUseActive() && hasItem;
        if (!sneakWithItem) {
            ItemInteractionResult ir = ss.useItemOn(held, shadow, player, hand, hit);
            if (ir.consumesAction()) return true;
            if (ir == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
                if (ss.useWithoutItem(shadow, player, hit).consumesAction()) return true;
            } else {
                return true; // SKIP_DEFAULT/FAIL：物品已明確處理，不再開 GUI
            }
        }
        if (!held.isEmpty()) {
            held.useOn(new UseOnContext(shadow, player, hand, held, hit));
        }
        return true;
    }

    /** 編輯飛船時把改動也寫進影子（不然下一輪鏡射會把編輯蓋回去；機器網路也要看到新方塊）。 */
    private void writeToShadow(BlockPos local, BlockState state, @Nullable CompoundTag nbt) {
        ServerLevel shadow = getShadow();
        if (shadow == null) return;
        BlockPos sp = shadowAnchor.offset(local);
        // 影子是隱藏維度:不用 UPDATE_ALL(flag 1 鄰居連鎖在大船每次編輯就是凍的元兇)。只用 CLIENTS(影子無玩家=近乎 no-op)
        // → 不觸發鄰居更新連鎖。方塊/BE 照樣放、capability 照樣失效通知(魔力網路每 tick push 偵測,不靠鄰居更新)。
        shadow.setBlock(sp, state, Block.UPDATE_CLIENTS);
        if (nbt != null) {
            BlockEntity be = shadow.getBlockEntity(sp);
            if (be != null) be.loadWithComponents(nbt, shadow.registryAccess());
        }
    }

    // 鏡射只需掃「會變」的方塊：機器(EntityBlock，運轉/亮/進度) + 隨機刻(作物生長)。靜態方塊永不變。
    // 編輯(門/拉桿/放挖)已在 writeToShadow 兩端同寫，不靠鏡射，所以不必納入。快取，加/減方塊才失效。
    @Nullable private java.util.List<BlockPos> dynamicMirrorCache;
    // 杖蓋的結構方塊(柱子/環)即使不是 EntityBlock 也要每 tick 鏡射:祭壇成形/升級會把 mana_block 換成
    // resonance_ring 等,那是非 EntityBlock 的方塊替換,不在這集合就不會同步回視覺船。
    private final java.util.Set<BlockPos> structureMirror = new java.util.HashSet<>();
    private java.util.List<BlockPos> dynamicMirrorBlocks() {
        if (dynamicMirrorCache == null) {
            java.util.List<BlockPos> dyn = new java.util.ArrayList<>();
            if (contraption != null) {
                for (var e : contraption.getBlocks().entrySet()) {
                    BlockState st = e.getValue().state();
                    if (st.getBlock() instanceof EntityBlock || st.isRandomlyTicking()
                            || structureMirror.contains(e.getKey())) dyn.add(e.getKey());
                }
            }
            dynamicMirrorCache = dyn;
        }
        return dynamicMirrorCache;
    }

    /** 把影子裡的 blockstate/BE 變化(機器運轉、作物生長、熔爐亮)鏡射回視覺 contraption + 同步 client。 */
    private void tickServerMirror() {
        ServerLevel shadow = getShadow();
        if (shadow == null || contraption == null) return;
        // 只掃「會變」的方塊(機器 BE / 作物等隨機刻)；靜態石頭外殼永遠不變，不必每 16 tick 掃。
        // 大船(2048 方塊,大半石頭)從掃全部 → 只掃機器+作物，大幅省 server tick。
        for (BlockPos local : dynamicMirrorBlocks()) {
            StructureBlockInfo info = contraption.getBlocks().get(local);
            if (info == null) continue;
            BlockPos sp = shadowAnchor.offset(local);
            BlockState ss = shadow.getBlockState(sp);
            // 影子讀到空氣:區分兩種情況。
            //  1) 那格區塊沒載入 → 不可信，保護視覺方塊別誤抹(force-load 前/載入時序)。
            //  2) 區塊載入著卻是空 = 那格真的被移除了(結構 deform / 機器 / 先前的 break)→ 視覺也要移除，
            //     否則 contraption 留著方塊但影子沒了 = ghost：打不到(pick 找不到對應實 BE)、拆解才現形。
            if (ss.isAir() && !info.state().isAir()) {
                if (!shadow.hasChunkAt(sp)) continue;          // 沒載入 → 保護
                updateContraptionBlock(local, ss);             // 載入著卻空 → 真移除，同步抹掉視覺
                ShipBlockUpdatePacket.sendToClients(this, local, ss);
                continue;
            }
            if (!ss.equals(info.state())) {
                contraption.setBlockState(local, ss);          // blockstate 變(作物/熔爐亮/機器 active)
                ShipBlockUpdatePacket.sendToClients(this, local, ss);
                localCollisionShapeCache = null; collisionListCache = null; // 形狀可能變(作物長/門開)
            }
            if (ss.getBlock() instanceof EntityBlock) {        // BE NBT 鏡射(機器內容/進度/物品底座 item)
                BlockEntity sbe = shadow.getBlockEntity(sp);
                if (sbe != null) {
                    CompoundTag tag = sbe.saveWithFullMetadata(shadow.registryAccess());
                    tag.remove("x"); tag.remove("y"); tag.remove("z");
                    if (!tag.equals(info.nbt())) { // 變了才寫 + 發給 client(否則 client render BE 拿不到新 item → BER 不畫漂浮物品)
                        contraption.setBlockNbt(local, tag);
                        com.github.nalamodikk.common.network.packet.client.ship.ShipBlockEntityDataPacket
                                .sendToClients(this, local, tag);
                    }
                }
            }
            // 祭壇核心(EntityBlock)在影子端成形/升級時，會就地把角落 mana_block 換成 altar_pillar、把環換成 resonance_ring
            // (兩者都不是 EntityBlock，不會自己進 dynamicMirror)。核心被鏡射到這裡時，把它的柱子/環位置納入 structureMirror，
            // 下一輪鏡射就把這些轉換結果同步到視覺船(否則船上柱子停在 mana_block 的樣子)。
            if (ss.is(ModBlocks.ASPECT_ALTAR.get())) {
                boolean added = false;
                for (Vec3i off : AltarGeometry.PILLAR_OFFSETS) added |= structureMirror.add(local.offset(off));
                for (List<Vec3i> ring : AltarGeometry.ALL_RINGS)
                    for (Vec3i off : ring) added |= structureMirror.add(local.offset(off));
                if (added) dynamicMirrorCache = null; // 有新追蹤位置 → 失效快取，下輪 dynamicMirrorBlocks 才掃得到柱子/環
            }
        }
    }

    /**
     * 某張座椅的坐姿方向在 local 座標的 yaw（你坐上去面對的方向 = 視覺椅子開口方向 = FACING）。
     * 之前用 FACING.getOpposite() 是錯的，害船頭與視覺椅子差 180（駕駛面向椅背=視角反）。非座椅退回北。
     */
    private float sitYawLocal(BlockPos seatLocal) {
        if (contraption != null) {
            var info = contraption.getBlocks().get(seatLocal);
            if (info != null && info.state().getBlock() instanceof ShipSeatBlock
                    && info.state().hasProperty(ShipSeatBlock.FACING)) {
                return info.state().getValue(ShipSeatBlock.FACING).toYRot();
            }
        }
        return 180f;
    }

    /** 船頭在 local 座標的 yaw = 駕駛椅（座位0）的坐姿方向。沒座椅退回北。 */
    private float bowLocalYaw() {
        List<BlockPos> seats = getSeats();
        return seats.isEmpty() ? 180f : sitYawLocal(seats.get(0));
    }

    /** server 端：合併各駕駛位輸入算移動並 setPos/setYRot。位置經 entity tracking 自動廣播給所有 client。 */
    private void tickServerMovement() {
        // 只有「有人坐的駕駛位」(index<MAX_DRIVERS)才算輸入；空的清零（離座殘留不該推船）
        for (int i = 0; i < MAX_DRIVERS; i++) {
            if (!isSeatOccupied(i)) { inForward[i] = 0; inStrafe[i] = 0; inVertical[i] = 0; }
        }
        // 油門合併：所有駕駛位的前後/左右/升降相加後夾到 [-1,1]
        float f = 0, s = 0; int v = 0;
        for (int i = 0; i < MAX_DRIVERS; i++) { f += inForward[i]; s += inStrafe[i]; v += inVertical[i]; }
        f = Mth.clamp(f, -1f, 1f); s = Mth.clamp(s, -1f, 1f); v = Mth.clamp(v, -1, 1);

        // 船頭 = 主駕駛那張椅子的朝向（主駕駛=最小的有人駕駛位，可能是座位1而非座位0）。
        int primary = primaryDriverSeat();
        float bowLocal = (primary >= 0) ? sitYawLocal(getSeats().get(primary)) : 180f;
        if (primary >= 0) {
            // 主駕駛視角定船頭：yaw 讓 bow 轉到指向視角；pitch 直接跟駕駛抬/低頭(夾範圍避免翻過頭)。
            setYRot(Mth.rotLerp(YAW_LERP, getYRot(), inYaw[primary] - bowLocal));
            setXRot(Mth.lerp(YAW_LERP, getXRot(), Mth.clamp(inPitch[primary], -75f, 75f)));
            setBowLocal(bowLocal); // pitch/roll 要繞船頭軸轉，把船頭偏移同步給渲染/碰撞
        }
        // A/D → 翻滾(roll，繞船頭軸)。換掉側移：看哪轉哪，側移較少用；全姿勢不自動回正。
        if (primary >= 0 && s != 0f) setRoll(Mth.wrapDegrees(getRoll() + s * ROLL_RATE));
        // 前進=船頭(含 pitch+roll)，用完整姿勢轉 local 前向；上下=世界垂直(跳/疾跑恆定升降)。
        Quaternionf q = orientation();
        double bowR = Math.toRadians(bowLocal);
        Vector3f wf = q.transform(new Vector3f(-(float) Math.sin(bowR), 0, (float) Math.cos(bowR)));
        Vec3 forwardDir = new Vec3(wf.x, wf.y, wf.z);
        Vec3 target = forwardDir.scale(f).add(0, v, 0);
        if (target.lengthSqr() > 1) target = target.normalize();
        // 速度上限：一般引擎吃燃料槽、曲速結構吃進料口能量,各自有燃料才貢獻;有曲速結構通電 → 天花板拉到 600。× 油門。
        int fuel = getFuel();
        getEntityData().set(DATA_FUEL, fuel); // 同步給 client 畫 HUD(燃料槽)
        boolean hasFuel = fuel > 0;
        int warpFuel = getWarpFuel();
        getEntityData().set(DATA_WARP_FUEL, warpFuel); // 同步給 HUD
        boolean hasWarp = warpDriveCount > 0 && warpFuel > 0;
        double engineSpeed = hasFuel ? engineCount * SPEED_PER_ENGINE : 0.0;
        double warpSpeed = hasWarp ? warpDriveCount * SPEED_PER_WARP : 0.0;
        double cap = hasWarp ? WARP_CAP : SPEED_CAP;
        double maxSpeed = Math.min(engineSpeed + warpSpeed, cap) * getThrottle();
        target = target.scale(maxSpeed);

        shipVel = shipVel.add(target.subtract(shipVel).scale(ACCEL));
        if (shipVel.lengthSqr() < 1e-6) shipVel = Vec3.ZERO;
        Vec3 allowed = resolveTerrain(shipVel); // 撞地形的軸歸零（會沿牆滑）
        if (allowed.lengthSqr() > 0) {
            setPos(getX() + allowed.x, getY() + allowed.y, getZ() + allowed.z);
        }
        shipVel = allowed; // 撞到的軸不保留動量

        // 燃料消耗：移動才耗(停滯不耗)、加速(有前進/升降輸入)×2、隨油門縮放。一般引擎抽燃料槽、曲速結構抽進料口。
        if (shipVel.lengthSqr() > 1e-6) {
            int mult = (f != 0f || v != 0) ? 2 : 1;
            double thr = getThrottle();
            if (hasFuel && engineCount > 0)
                drainFuel((int) Math.ceil(FUEL_PER_ENGINE_MOVE * engineCount * thr * mult));
            if (hasWarp)
                drainWarpFuel((int) Math.ceil(FUEL_PER_WARP_MOVE * warpDriveCount * thr * mult));
        }
    }

    /** client 端：朝 server 廣播的目標位置/yaw 每 tick 步進一步，平滑跟隨（像 vanilla 船）。 */
    private void tickLerp() {
        if (lerpSteps > 0) {
            lerpPositionAndRotationStep(lerpSteps, lerpX, lerpY, lerpZ, lerpYRot, lerpXRot);
            lerpSteps--;
        }
    }

    // server 權威：回 false 讓駕駛 client 不送 vanilla ServerboundMoveVehiclePacket。否則 client 會
    // 每 tick 把「停在原地的船位置」推給 server，蓋掉 server 算的移動 → 按 WSAD 船動不了。
    @Override
    public boolean isControlledByLocalInstance() {
        return false;
    }

    // server 經 entity tracking 廣播位置 → client 收到後呼叫此處設定 lerp 目標（Entity.lerpSteps 私有，
    // 自存一份）。所有 client（含駕駛）都跟隨 server，不再自己預測 → 不抖、不分家、不彈回。
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x; this.lerpY = y; this.lerpZ = z;
        this.lerpYRot = yRot; this.lerpXRot = xRot;
        this.lerpSteps = steps;
    }

    /**
     * 實體原點放在「船中心」(X/Z 中心、Y 底)，所以對稱的 EntityDimensions 盒能貼合船。
     * centerOffset = 從核心(local 原點)到該中心的位移。
     */
    public Vec3 centerOffset() {
        if (contraption == null) return Vec3.ZERO;
        AABB b = contraption.bounds();
        return new Vec3((b.minX + b.maxX) / 2.0, b.minY, (b.minZ + b.maxZ) / 2.0);
    }

    /** 組裝後把實體放到船中心（= 核心 + centerOffset）。 */
    public void placeAtShipCenter(BlockPos corePos) {
        Vec3 c = centerOffset();
        setPos(corePos.getX() + c.x, corePos.getY() + c.y, corePos.getZ() + c.z);
    }

    /**
     * 把 local 方塊角落（相對核心）轉成世界座標：先減去 centerOffset（相對船中心），再依 yaw
     * 繞船中心(=實體位置)旋轉。yaw=0 時 = 實體 + (local - centerOffset) = 核心 + local（原位）。
     */
    public Vec3 rotatedWorldCorner(int lx, int ly, int lz) {
        return rotatedWorldPoint(lx, ly, lz);
    }

    // 六面飛行：roll(繞船頭軸)。yaw=getYRot, pitch=getXRot(vanilla 已有), roll 走 synched data(不在 vanilla lerp)。
    public float getRoll() { return getEntityData().get(DATA_ROLL); }
    public void setRoll(float r) { getEntityData().set(DATA_ROLL, r); }
    public float getBowLocal() { return getEntityData().get(DATA_BOW); }
    public void setBowLocal(float b) { getEntityData().set(DATA_BOW, b); }

    // ── 燃料/引擎(1b) ──────────────────────────────────────────────────────────
    public float getThrottle() { return getEntityData().get(DATA_THROTTLE); }
    public void setThrottle(float t) { getEntityData().set(DATA_THROTTLE, Mth.clamp(t, 0f, 1f)); }
    public int getEngineCount() { return engineCount; }
    public int getWarpDriveCount() { return warpDriveCount; }
    /** HUD 用：同步過來的目前燃料(server 算)。 */
    public int getDisplayFuel() { return getEntityData().get(DATA_FUEL); }
    /** HUD 用：總燃料容量 = 燃料槽數 × 單槽容量(client 端 fuelTankLocals 在 readSpawnData 算好)。 */
    public int getMaxFuel() { return fuelTankLocals.size() * ManaFuelTankBlockEntity.CAPACITY; }
    /** HUD 用：曲速能量(同步) + 總容量 = 進料口數 × 單口容量。 */
    public int getDisplayWarpFuel() { return getEntityData().get(DATA_WARP_FUEL); }
    public int getMaxWarpFuel() { return warpIntakeLocals.size() * ManaWarpInputBlockEntity.CAPACITY; }

    /** contraption 變動時重算引擎數 + 燃料槽/曲速核心 local，再掃完整曲速結構。只在組裝/載入跑一次。 */
    private void recomputeFuelSystem() {
        engineCount = 0;
        fuelTankLocals.clear();
        warpCoreLocals.clear();
        if (contraption == null) { warpDriveCount = 0; warpIntakeLocals.clear(); return; }
        for (var e : contraption.getBlocks().entrySet()) {
            Block b = e.getValue().state().getBlock();
            if (b instanceof ManaWarpEngineBlock) warpCoreLocals.add(e.getKey().immutable());
            else if (b instanceof ManaEngineBlock) engineCount++;
            else if (b instanceof ManaFuelTankBlock) fuelTankLocals.add(e.getKey().immutable());
        }
        recomputeWarpDrives();
    }

    /** 用 warpCoreLocals 掃完整 3×4×3 沙漏結構，更新 warpDriveCount + warpIntakeLocals(曲速燃料從這些進料口抽)。 */
    private void recomputeWarpDrives() {
        warpIntakeLocals.clear();
        if (contraption == null || warpCoreLocals.isEmpty()) { warpDriveCount = 0; return; }
        var blocks = contraption.getBlocks();
        var found = WarpDriveStructure.detect(warpCoreLocals,
                lp -> { var i = blocks.get(lp); return i != null ? i.state() : null; });
        warpDriveCount = found.size();
        for (var f : found) warpIntakeLocals.add(f.intakeLocal());
    }

    /** 目前總燃料 = 影子裡各燃料槽的魔力總和。沒影子(gametest)回 0。 */
    public int getFuel() {
        ServerLevel shadow = getShadow();
        if (shadow == null || shadowAnchor == null) return 0;
        int total = 0;
        for (BlockPos lp : fuelTankLocals) {
            if (shadow.getBlockEntity(shadowAnchor.offset(lp)) instanceof ManaFuelTankBlockEntity tank)
                total += tank.getManaStorage().getManaStored();
        }
        return total;
    }

    /** 從影子的燃料槽抽 amount 魔力（飛行消耗）。逐槽抽到夠為止。 */
    private void drainFuel(int amount) {
        ServerLevel shadow = getShadow();
        if (shadow == null || shadowAnchor == null || amount <= 0) return;
        for (BlockPos lp : fuelTankLocals) {
            if (amount <= 0) break;
            if (shadow.getBlockEntity(shadowAnchor.offset(lp)) instanceof ManaFuelTankBlockEntity tank)
                amount -= tank.getManaStorage().extractMana(amount, ManaAction.EXECUTE);
        }
    }

    /** 曲速燃料 = 完整結構各進料口的能量總和(影子)。 */
    private int getWarpFuel() {
        ServerLevel shadow = getShadow();
        if (shadow == null || shadowAnchor == null) return 0;
        int total = 0;
        for (BlockPos lp : warpIntakeLocals) {
            if (shadow.getBlockEntity(shadowAnchor.offset(lp)) instanceof ManaWarpInputBlockEntity in)
                total += in.getEnergy().getManaStored();
        }
        return total;
    }

    /** 從各進料口抽 amount 曲速能量。 */
    private void drainWarpFuel(int amount) {
        ServerLevel shadow = getShadow();
        if (shadow == null || shadowAnchor == null || amount <= 0) return;
        for (BlockPos lp : warpIntakeLocals) {
            if (amount <= 0) break;
            if (shadow.getBlockEntity(shadowAnchor.offset(lp)) instanceof ManaWarpInputBlockEntity in)
                amount -= in.getEnergy().extractMana(amount, ManaAction.EXECUTE);
        }
    }
    private float rollO; // 上一 tick，渲染插值用

    /**
     * 船的完整 3D 姿勢(四元數)。pitch/roll 繞「船頭相對軸」(bow)轉，故蓋的方向不同也一致。
     * pitch=roll=0 時 bow 抵銷 = 純 yaw 旋轉(rotateY(-yawRad)，與舊式一致)，不破壞既有水平行為。
     */
    public Quaternionf orientation() {
        return orientationOf(getYRot(), getXRot(), getRoll(), getBowLocal());
    }

    /**
     * 由 yaw/pitch/roll + bow 組四元數。yaw=getYRot=lookYaw-bow，所以 -yaw-bow = -lookYaw。
     * = rotateY(-lookYaw)·rotateX(pitch)·rotateZ(roll)·rotateY(bow)：尾端 rotateY(bow) 把 local 對齊到船頭框，
     * 故 pitch/roll 繞船頭相對軸轉(蓋的方向不同也一致)。pitch=roll=0 時 bow 抵銷 = rotateY(-yaw)，與舊式一致。
     */
    private static Quaternionf orientationOf(float yaw, float pitch, float roll, float bow) {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-yaw - bow))
                .rotateX((float) Math.toRadians(pitch))
                .rotateZ((float) Math.toRadians(roll))
                .rotateY((float) Math.toRadians(bow));
    }

    /** 插值姿勢(渲染用，避免轉動頓)。bow 穩定不需插值。 */
    public Quaternionf orientation(float partialTick) {
        float yaw   = Mth.rotLerp(partialTick, yRotO, getYRot());
        float pitch = Mth.lerp(partialTick, xRotO, getXRot());
        float rl    = Mth.rotLerp(partialTick, rollO, getRoll());
        return orientationOf(yaw, pitch, rl, getBowLocal());
    }

    /** 插值後的 roll(渲染/鏡頭用)。鏡頭 roll 跟玩家傾斜要用這個,不能用 getRoll() tick 值,否則 20Hz 跳、船卻 60Hz 平滑轉 → 抖。 */
    public float getRoll(float partialTick) {
        return Mth.rotLerp(partialTick, rollO, getRoll());
    }

    /** 任意 local 點（可含 +0.5 之類的偏移）轉世界座標：偏移依完整姿勢繞船中心旋轉。 */
    public Vec3 rotatedWorldPoint(double lx, double ly, double lz) {
        Vec3 c = centerOffset();
        Vector3f o = new Vector3f((float) (lx - c.x), (float) (ly - c.y), (float) (lz - c.z));
        orientation().transform(o);
        return new Vec3(getX() + o.x, getY() + o.y, getZ() + o.z);
    }

    // ── 甲板碰撞（外部實體站上船、跟船走）地基 ───────────────────────────────
    // 玩家在世界、船方塊在實體 local 框（繞船中心旋轉 yaw）。把玩家轉進 local 框跟「沒旋轉的方塊表」
    // 做單純逐軸 AABB 碰撞，再轉回世界。只有 yaw 旋轉，玩家盒近似軸對齊（誤差小）。

    @Nullable private VoxelShape localCollisionShapeCache;
    @Nullable private CollisionList collisionListCache; // OBB 碰撞用的方塊盒清單，跟 shape 一起失效(別每 tick 重建)

    private CollisionList collisionList(VoxelShape shape) {
        if (collisionListCache != null) return collisionListCache;
        CollisionList list = new CollisionList();
        shape.forAllBoxes(new CollisionList.Populate(list));
        return collisionListCache = list;
    }

    /**
     * contraption 所有方塊的碰撞盒合併成 local 框的一個 VoxelShape（靜態，快取一次）。
     * 滿格方塊(絕大多數)直接填一個 block 解析度的 bitset(O(N))，只有非滿格(階梯/半磚等)才 Shapes.or。
     * 避免舊式 N 次 Shapes.or 的 O(N^2)（2000 方塊船 92ms → ~1ms）。
     */
    private VoxelShape localCollisionShape() {
        if (localCollisionShapeCache != null) return localCollisionShapeCache;
        if (contraption == null || contraption.getBlocks().isEmpty()) {
            return localCollisionShapeCache = Shapes.empty();
        }
        AABB b = contraption.bounds();
        int minX = Mth.floor(b.minX), minY = Mth.floor(b.minY), minZ = Mth.floor(b.minZ);
        int sx = Mth.floor(b.maxX) - minX, sy = Mth.floor(b.maxY) - minY, sz = Mth.floor(b.maxZ) - minZ;
        BitSetDiscreteVoxelShape grid = new BitSetDiscreteVoxelShape(sx, sy, sz);
        VoxelShape partial = Shapes.empty();
        for (var e : contraption.getBlocks().entrySet()) {
            BlockPos lp = e.getKey();
            BlockState st = e.getValue().state();
            VoxelShape cs = st.getCollisionShape(EmptyBlockGetter.INSTANCE, lp);
            if (cs.isEmpty()) {
                // 有些方塊碰撞依賴 BE/Level，用 EmptyBlockGetter 查到空殼 → 站上去穿模掉下去、也指不到。
                // 任何非空氣方塊(都有渲染)空碰撞就補滿格 = 你看得到的方塊就站得上去。
                grid.fill(lp.getX() - minX, lp.getY() - minY, lp.getZ() - minZ);
                continue;
            }
            if (st.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, lp)) {
                grid.fill(lp.getX() - minX, lp.getY() - minY, lp.getZ() - minZ);
            } else {
                partial = Shapes.or(partial, cs.move(lp.getX(), lp.getY(), lp.getZ()));
            }
        }
        double[] xs = new double[sx + 1], ys = new double[sy + 1], zs = new double[sz + 1];
        for (int i = 0; i <= sx; i++) xs[i] = minX + i;
        for (int i = 0; i <= sy; i++) ys[i] = minY + i;
        for (int i = 0; i <= sz; i++) zs[i] = minZ + i;
        VoxelShape full = new ArrayVoxelShape(grid, xs, ys, zs);
        return localCollisionShapeCache = (partial.isEmpty() ? full : Shapes.or(full, partial));
    }

    /** 世界座標點 → local 框座標（rotatedWorldPoint 的逆：用姿勢的共軛）。 */
    public Vec3 worldToLocalPoint(double wx, double wy, double wz) {
        Vec3 c = centerOffset();
        Vector3f d = new Vector3f((float) (wx - getX()), (float) (wy - getY()), (float) (wz - getZ()));
        orientation().conjugate().transform(d);
        return new Vec3(d.x + c.x, d.y + c.y, d.z + c.z);
    }

    private Vec3 rotateVec(double x, double y, double z, double deg) {
        double rad = Math.toRadians(deg);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        return new Vec3(x * cos - z * sin, y, x * sin + z * cos);
    }

    /**
     * 把世界移動向量依船的方塊碰撞限制（站甲板=Y 撐住、撞牆=X/Z 擋）。回傳限制後的世界移動。
     * worldBox = 實體移動「前」的世界 AABB。
     */
    public Vec3 restrictMotion(AABB worldBox, Vec3 worldMotion) {
        VoxelShape shape = localCollisionShape();
        if (shape.isEmpty()) return worldMotion;
        Vec3 cen = worldBox.getCenter();
        Vec3 lc = worldToLocalPoint(cen.x, cen.y, cen.z);
        AABB lb = AABB.ofSize(lc, worldBox.getXsize(), worldBox.getYsize(), worldBox.getZsize());
        Vec3 lm = rotateVec(worldMotion.x, worldMotion.y, worldMotion.z, -getYRot());
        double my = lm.y; if (my != 0) my = shape.collide(Direction.Axis.Y, lb, my); lb = lb.move(0, my, 0);
        double mx = lm.x; if (mx != 0) mx = shape.collide(Direction.Axis.X, lb, mx); lb = lb.move(mx, 0, 0);
        double mz = lm.z; if (mz != 0) mz = shape.collide(Direction.Axis.Z, lb, mz);
        return rotateVec(mx, my, mz, getYRot());
    }

    /**
     * 相對運動碰撞（移動平台正解）：把實體轉進船「上一 tick」的 local 框，在那裡對靜止方塊逐軸碰撞它
     * 自己的移動，再用船「這一 tick」的變換轉回世界。回傳的世界位移 = 跟船走(平移+旋轉) + 撞牆擋住，合一。
     * 取代舊的「restrictMotion + carry 分開」。實體不在船範圍內則原樣回傳。
     */
    public Vec3 applyContraptionMovement(Entity e, Vec3 worldMotion) {
        VoxelShape shape = localCollisionShape();
        if (shape.isEmpty()) return worldMotion;
        AABB box = e.getBoundingBox();
        // (曾試過「人工重力 phase 1」把重力沿甲板法線重導向防滑，但它在停著的微斜船上造成上下彈跳(抽搐/陷下去)。
        //  實測：站甲板 onGround 會把下墜速度歸零，重力不累積，5° 滑移只 0.02 格，根本不需要重導向。已移除。
        //  真正大幅傾斜要防滑是 phase 2/3(deck-is-down)的事，不是這個半套。)
        // T0 = 上一 tick 姿勢(yRotO/xRotO/rollO)，T1 = 這一 tick。用完整姿勢(含 pitch/roll)，碰撞才跟傾斜視覺對齊。
        Quaternionf q0 = orientationOf(yRotO, xRotO, rollO, getBowLocal());
        Quaternionf q1 = orientation();
        // 外接盒(玩家 8 角轉進 local 取 AABB)只用來「廣相位 gate」(判斷接近船)。傾斜時它會放大不漏。
        AABB lbEnclose = localEnclosingBox(box, xOld, yOld, zOld, q0);
        Vector3f lmv = q0.conjugate(new Quaternionf()).transform(
                new Vector3f((float) worldMotion.x, (float) worldMotion.y, (float) worldMotion.z));
        // 用「掃掠盒」(含這 tick 移動)判斷是否接近船：快速摔落時 tick 開始玩家還在船上方 >1 格，
        // 只看當前盒會判定「不在船範圍」直接放行 → 一個 tick 摔穿過去。掃掠盒會碰到甲板 → 正常接管擋住。
        if (!lbEnclose.expandTowards(lmv.x, lmv.y, lmv.z).intersects(contraption.bounds().inflate(1.0))) return worldMotion;

        Vec3 P = e.position();
        Vec3 L0 = worldToLocalAt(P.x, P.y, P.z, xOld, yOld, zOld, q0);
        // 實際碰撞用「玩家原尺寸盒」(放在 local 中心)，不是外接盒：外接盒在 yaw 旋轉時比玩家大(45°大42%)，
        // 每 tick 把玩家往外推 → 走路抽搐/被慢慢推走。原尺寸盒不過度推。傾斜的精準碰撞留給 OBB(階段3)。
        AABB lb = AABB.ofSize(lbEnclose.getCenter(), box.getXsize(), box.getYsize(), box.getZsize());
        Vec3 lm = new Vec3(lmv.x, lmv.y, lmv.z);
        // 傾斜船(>2°)走 OBB：collideBoundingBox 處理不了傾斜的「牆」(世界對齊玩家 vs local 框轉過的牆 → 穿)。
        // 之前提到 10° 是為了避開 OBB 在斜甲板的 onGround 跳；但有了人工重力(重力沿 local-down)後，垂直移動是
        // 純 local-down → OBB 垂直擋住一致、不跳。只有近乎水平(<2°)才用 collideBoundingBox(對樓梯/半磚 robust)。
        final float TILT_DEG = 2f;
        boolean tilted = Math.abs(getXRot()) > TILT_DEG || Math.abs(getRoll()) > TILT_DEG
                || Math.abs(xRotO) > TILT_DEG || Math.abs(rollO) > TILT_DEG;
        Vec3 collided;
        if (tilted) {
            collided = obbCollideLocal(e, box, lb, lm, q0, shape); // OBB 已含穿入推出
        } else {
            // 直立:collideBoundingBox 對樓梯/半磚 robust，但「不會把已重疊的盒子推出」(從外面走進牆會直接穿)。
            // 補一個零移動 MTV 推出 pass：只在玩家已穿進方塊時才推(沒穿入=collisionResponse 0=不動，不影響正常走/站)。
            collided = Entity.collideBoundingBox(e, lm, lb, level(), java.util.List.of(shape));
            collided = pushOutPenetration(lb, collided, shape, q0, box);
        }
        Vec3 newLocal = L0.add(collided.x, collided.y, collided.z);
        Vec3 newWorld = localToWorldAt(newLocal.x, newLocal.y, newLocal.z, getX(), getY(), getZ(), q1);
        Vec3 result = newWorld.subtract(P);
        // 旋轉 round-trip 在沒實際被擋的軸留下 ~1e-10 epsilon；vanilla Entity.move 用精確 != 判碰撞，
        // 會把這 epsilon 當成撞牆 → 每 tick 歸零玩家速度 → 走/跳「黏黏的」。差距極小的軸 snap 回 desired。
        // 水平容差放寬到 0.005：微傾斜的船(<2° 走 collideBoundingBox)把 local-Y 歸零會洩漏到世界 X/Z ~0.003，
        // 容差太小會讓 result.x≠輸入 → vanilla move 每 tick 歸零水平速度 = 走路阻力。真撞牆的減速 >>0.005 不受影響。
        // Y 維持嚴格(1e-4)：甲板擋住下墜本來就該讓 vanilla 判垂直碰撞、歸零下墜速度。
        double rx = Math.abs(result.x - worldMotion.x) < 5.0e-3 ? worldMotion.x : result.x;
        double ry = Math.abs(result.y - worldMotion.y) < 1.0e-4 ? worldMotion.y : result.y;
        double rz = Math.abs(result.z - worldMotion.z) < 5.0e-3 ? worldMotion.z : result.z;
        return new Vec3(rx, ry, rz);
    }

    /**
     * 傾斜船的 OBB 連續碰撞(用移植自 Create 的 ContinuousOBBCollider)：colliders = 船形狀的軸對齊方塊盒，
     * OBB = 玩家盒在 local 框被船姿勢逆轉(q0.conjugate)的有向盒。回傳 local 框的碰撞後位移(collide-and-slide)。
     * lmv 已是 local 框的移動。doHorizontalPass=false(Create 對有垂直旋轉的 contraption 就傳 false)。
     */
    private Vec3 obbCollideLocal(Entity e, AABB box, AABB lb, Vec3 lmv, Quaternionf q0, VoxelShape shape) {
        CollisionList colliders = collisionList(shape); // 快取，不每 tick 重建
        CollisionList dense = new CollisionList();
        Vec3 extents = new Vec3(box.getXsize() / 2, box.getYsize() / 2, box.getZsize() / 2);
        Matrix3d rot = new Matrix3d().set(q0.conjugate(new Quaternionf()));
        OrientedBB obb = new OrientedBB(lb.getCenter(), extents, rot);
        // maxStep=0：OBB 的 step-up 會把「微斜的斜面」當成一連串小台階一直往上踩 → 重力拉下 → 站著上下彈(抽搐)，
        // 低角度(3~5°)最明顯。飛船甲板用不到 step-up(連續斜面靠 collide-and-slide 走得上去，不是離散台階)，
        // 關掉它抽搐全消(tiltsweep 各角度 bob=0)且 110 gametest 全過(牆/樓梯/穿入推出都不靠它)。
        float maxStep = 0f;
        // 子步進：把這 tick 的移動切 N 小段，每段 連續碰撞+沿面滑+穿入推出。角落「切角」是因為一步斜跨過角點，
        // 小步走會在跨過前先重疊到 → 抓得住。每段後再做穿入解析 pass 把殘餘穿入推回表面。
        // 子步數依速度自適應：每步 ~0.35 格，快速墜落(終端速度 ~3.9/tick)才不會一步跨過甲板穿透。
        final int N = Math.min(16, Math.max(4, (int) Math.ceil(lmv.length() / 0.35)));
        Vec3 sub = lmv.scale(1.0 / N);
        Vec3 collided = Vec3.ZERO;
        for (int s = 0; s < N; s++) {
            obb.setCenter(lb.getCenter().add(collided));
            ContinuousOBBCollider.CollisionResponse res =
                    ContinuousOBBCollider.collideMany(colliders, dense, obb, sub, maxStep, false);
            double t = res.temporalResponse;
            Vec3 toImpact = sub.scale(t);
            Vec3 remaining = sub.scale(1.0 - t);
            Vec3 slid = remaining;
            Vec3 n = res.normal;
            if (n.lengthSqr() > 1.0e-9) {
                Vec3 nn = n.normalize();
                double into = remaining.dot(nn);
                if (into < 0) slid = remaining.subtract(nn.scale(into));
            }
            collided = collided.add(toImpact).add(slid).add(res.collisionResponse);
            // 穿入解析：零移動 collideMany 拿 MTV 推出殘餘穿入
            for (int i = 0; i < 3; i++) {
                obb.setCenter(lb.getCenter().add(collided));
                ContinuousOBBCollider.CollisionResponse pr =
                        ContinuousOBBCollider.collideMany(colliders, dense, obb, Vec3.ZERO, 0f, false);
                if (pr.collisionResponse.lengthSqr() < 1.0e-10) break;
                collided = collided.add(pr.collisionResponse);
            }
        }
        return collided;
    }

    /** 零移動 MTV 推出:把已穿進船方塊的盒子推回最近表面。collideBoundingBox 不推已重疊的盒子(從船外走進牆 →
     *  gate 延遲讓人先穿入 → 之後 collideBoundingBox 對「已重疊」沒表面可擋 → 直接穿過去)，這個補上。
     *  只在真的穿入時推(collisionResponse=0=沒穿入=不動)，所以不影響正常走/站(不會浮、不抖)。 */
    private Vec3 pushOutPenetration(AABB lb, Vec3 collided, VoxelShape shape, Quaternionf q0, AABB box) {
        CollisionList colliders = collisionList(shape);
        CollisionList dense = new CollisionList();
        Vec3 extents = new Vec3(box.getXsize() / 2, box.getYsize() / 2, box.getZsize() / 2);
        Matrix3d rot = new Matrix3d().set(q0.conjugate(new Quaternionf()));
        OrientedBB obb = new OrientedBB(lb.getCenter(), extents, rot);
        for (int i = 0; i < 4; i++) {
            obb.setCenter(lb.getCenter().add(collided));
            ContinuousOBBCollider.CollisionResponse pr =
                    ContinuousOBBCollider.collideMany(colliders, dense, obb, Vec3.ZERO, 0f, false);
            if (pr.collisionResponse.lengthSqr() < 1.0e-10) break;
            collided = collided.add(pr.collisionResponse);
        }
        return collided;
    }

    /** 把世界軸對齊盒的 8 個角轉進指定姿勢的 local 框，取外接 AABB。傾斜時盒會放大 → 碰撞不漏抓(不穿模)。 */
    private AABB localEnclosingBox(AABB world, double px, double py, double pz, Quaternionf q) {
        Vec3 c = centerOffset();
        Quaternionf inv = q.conjugate(new Quaternionf());
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        double[] xs = {world.minX, world.maxX}, ys = {world.minY, world.maxY}, zs = {world.minZ, world.maxZ};
        Vector3f d = new Vector3f();
        for (double wx : xs) for (double wy : ys) for (double wz : zs) {
            d.set((float) (wx - px), (float) (wy - py), (float) (wz - pz));
            inv.transform(d);
            double lx = d.x + c.x, ly = d.y + c.y, lz = d.z + c.z;
            if (lx < minX) minX = lx; if (lx > maxX) maxX = lx;
            if (ly < minY) minY = ly; if (ly > maxY) maxY = ly;
            if (lz < minZ) minZ = lz; if (lz > maxZ) maxZ = lz;
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** 世界點 → 指定變換(pos + 姿勢 q)的 local 框。 */
    private Vec3 worldToLocalAt(double wx, double wy, double wz, double px, double py, double pz, Quaternionf q) {
        Vec3 c = centerOffset();
        Vector3f r = q.conjugate(new Quaternionf()).transform(
                new Vector3f((float) (wx - px), (float) (wy - py), (float) (wz - pz)));
        return new Vec3(r.x + c.x, r.y + c.y, r.z + c.z);
    }

    /** local 框 → 指定變換(pos + 姿勢 q)的世界點。 */
    private Vec3 localToWorldAt(double lx, double ly, double lz, double px, double py, double pz, Quaternionf q) {
        Vec3 c = centerOffset();
        Vector3f r = q.transform(new Vector3f((float) (lx - c.x), (float) (ly - c.y), (float) (lz - c.z)));
        return new Vec3(px + r.x, py + r.y, pz + r.z);
    }

    /** 世界某點(玩家大小的盒)是否卡在船的方塊裡。下船找不會穿模的位置用。 */
    public boolean isInsideShip(double wx, double wy, double wz) {
        VoxelShape shape = localCollisionShape();
        if (shape.isEmpty()) return false;
        Vec3 lc = worldToLocalPoint(wx, wy, wz);
        AABB probe = AABB.ofSize(new Vec3(lc.x, lc.y + 0.9, lc.z), 0.6, 1.8, 0.6); // 玩家盒(腳在 wy)
        return Shapes.joinIsNotEmpty(shape, Shapes.create(probe), BooleanOp.AND);
    }

    /**
     * 把卡進船方塊裡的實體推出來（移動平台碰撞的務實補救：船掃進你 / 你穿進牆 都靠這個推出）。
     * 在 local 框試 上/水平四向 找最近的脫離方向，轉回世界推。
     */
    public void resolveOverlap(Entity e) {
        VoxelShape shape = localCollisionShape();
        if (shape.isEmpty()) return;
        AABB box = e.getBoundingBox();
        Vec3 cen = box.getCenter();
        Vec3 lc = worldToLocalPoint(cen.x, cen.y, cen.z);
        // 只用「半尺寸的中心盒」判是否真的深陷方塊：縮 0.05 太貼邊，正常站/走時腳碰甲板就被判卡 →
        // 每 tick 往上推 → 走路像在水裡有阻力。半尺寸盒只在中心嵌進方塊(真卡死)才觸發，表面接觸不推。
        AABB lb = AABB.ofSize(lc, box.getXsize() * 0.5, box.getYsize() * 0.5, box.getZsize() * 0.5);
        if (!Shapes.joinIsNotEmpty(shape, Shapes.create(lb), BooleanOp.AND)) return; // 沒深陷
        Direction[] order = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (Direction d : order) {
            for (double dist = 0.05; dist <= 1.3; dist += 0.05) {
                AABB moved = lb.move(d.getStepX() * dist, d.getStepY() * dist, d.getStepZ() * dist);
                if (!Shapes.joinIsNotEmpty(shape, Shapes.create(moved), BooleanOp.AND)) {
                    Vec3 w = rotateVec(d.getStepX() * dist, d.getStepY() * dist, d.getStepZ() * dist, getYRot());
                    e.setPos(e.getX() + w.x, e.getY() + w.y, e.getZ() + w.z);
                    return;
                }
            }
        }
    }

    /**
     * 防穿安全網（照 Create savePlayerFromClipping）：從實體腳部四角往「世界下方」射線打船的碰撞形狀，
     * 找到甲板表面。實體若已穿到表面下(每-tick 碰撞漏抓:漂移/角落/部分方塊)，就拉回表面上、歸零下墜速度。
     * 這是 catch-all,補碰撞的所有漏洞。回傳是否有拉回。
     */
    public boolean snapToDeckSurface(Entity e) {
        VoxelShape shape = localCollisionShape();
        if (shape.isEmpty()) return false;
        AABB box = e.getBoundingBox().deflate(0.2, 0, 0.2); // 縮水平避免邊緣誤判(照 Create deflate 1/4)
        // 射線從腳上方 0.5 起(不是 maxStep+0.5)：太高會掃到腳邊/上方的牆方塊、把人往上彈 → 抽搐。
        // 只涵蓋「這 tick 可能穿入的深度」。每-tick 穿入 < 0.5。
        double up = 0.5;
        double feetY = box.minY;
        double bestSurfaceY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 4; i++) {
            double cx = (i / 2 == 0) ? box.minX : box.maxX;
            double cz = (i % 2 == 0) ? box.minZ : box.maxZ;
            Vec3 sL = worldToLocalPoint(cx, feetY + up, cz);   // 腳上方(世界 up)轉進 local
            Vec3 eL = worldToLocalPoint(cx, feetY - 1.0, cz);  // 腳下方 1 格
            BlockHitResult hit = shape.clip(sL, eL, BlockPos.ZERO);
            if (hit == null || hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) continue;
            Vec3 hw = rotatedWorldPoint(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
            if (hw.y > bestSurfaceY) bestSurfaceY = hw.y;
        }
        if (bestSurfaceY > feetY + 0.1) { // 表面明顯在腳上方(>0.1)=真穿入才拉；微沉不管(否則每 tick 拉=阻力)
            e.setPos(e.getX(), bestSurfaceY, e.getZ());
            if (e.getDeltaMovement().y < 0) e.setDeltaMovement(e.getDeltaMovement().x, 0, e.getDeltaMovement().z);
            return true;
        }
        return false;
    }

    /** 實體是否「站在」這艘船上（腳底正下方有船的方塊）。用來決定要不要 carry。 */
    public boolean isSupporting(Entity e) {
        VoxelShape shape = localCollisionShape();
        if (shape.isEmpty()) return false;
        AABB b = e.getBoundingBox();
        Vec3 cen = b.getCenter();
        // 查腳下 0.5 內有沒有船方塊(放寬：太緊的話傾斜/微浮一點就判沒站在船上 → 人工重力/onGround 失效)。
        Vec3 lc = worldToLocalPoint(cen.x, b.minY - 0.25, cen.z);
        AABB probe = AABB.ofSize(lc, Math.max(b.getXsize(), 0.1), 0.5, Math.max(b.getZsize(), 0.1));
        return Shapes.joinIsNotEmpty(shape, Shapes.create(probe), BooleanOp.AND);
    }

    /**
     * 把站在船上的實體「帶著走」：套用船這 tick 的平移 + 繞船中心的 yaw 旋轉，實體才會跟船一起動。
     * 用 xOld/yRotO（tick 開頭 setOldPosAndRot 記的上一 tick 值）算這 tick 的位移/轉角。
     */
    public void carry(Entity e) {
        double tdy = getY() - yOld;
        float dyaw = getYRot() - yRotO;
        // 水平：實體相對「舊船中心」的位移依 dyaw 旋轉，貼回新船中心（同時涵蓋平移與繞心轉）
        double relx = e.getX() - xOld, relz = e.getZ() - zOld;
        double rad = Math.toRadians(dyaw);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        double newX = getX() + (relx * cos - relz * sin);
        double newZ = getZ() + (relx * sin + relz * cos);
        double newY = e.getY() + tdy;
        if (newX == e.getX() && newY == e.getY() && newZ == e.getZ() && dyaw == 0) return;
        e.setPos(newX, newY, newZ);
        if (dyaw != 0) {
            e.setYRot(e.getYRot() + dyaw);
            if (e instanceof LivingEntity living) {
                living.setYHeadRot(living.getYHeadRot() + dyaw);
                living.setYBodyRot(living.yBodyRot + dyaw);
            }
        }
    }

    /**
     * 船撞地形：逐軸檢查移動後有沒有船方塊會卡進世界固體方塊，會的話該軸歸零（沿牆滑）。
     * 船方塊不在世界裡（在 entity），所以 noCollision 只會撞到真實地形，船自己不互撞。
     */
    Vec3 resolveTerrain(Vec3 move) { // package-visible 給 GameTest 直接測碰撞
        if (contraption == null) return move;
        // 子步進：高速(>~0.5格/tick)時 blockedBy 只看目的地會穿過薄牆。切成 ≤0.5 格的小段逐段擋(per-axis 沿牆滑)。
        // ≤0.5 格走原本單段邏輯，保住既有碰撞 gametest 的行為。
        int steps = (int) Math.ceil(move.length() / 0.5);
        if (steps <= 1) {
            double mx = move.x != 0 && blockedBy(move.x, 0, 0) ? 0 : move.x;
            double my = move.y != 0 && blockedBy(0, move.y, 0) ? 0 : move.y;
            double mz = move.z != 0 && blockedBy(0, 0, move.z) ? 0 : move.z;
            return new Vec3(mx, my, mz);
        }
        double ax = 0, ay = 0, az = 0;
        double sx = move.x / steps, sy = move.y / steps, sz = move.z / steps;
        for (int i = 0; i < steps; i++) {
            if (sx != 0 && !blockedBy(ax + sx, ay, az)) ax += sx;
            if (sy != 0 && !blockedBy(ax, ay + sy, az)) ay += sy;
            if (sz != 0 && !blockedBy(ax, ay, az + sz)) az += sz;
        }
        return new Vec3(ax, ay, az);
    }

    // 地形碰撞只需檢查外殼方塊：被其他船方塊六面全包的內部方塊，不可能比外殼先撞到世界地形。
    // 實心船省很多(內部佔多)；空心船全是殼，不減也不錯。快取，contraption 變了失效。
    @Nullable private java.util.List<BlockPos> hullBlocksCache;
    private java.util.List<BlockPos> hullBlocks() {
        if (hullBlocksCache == null) {
            java.util.List<BlockPos> hull = new java.util.ArrayList<>();
            if (contraption != null) {
                var blocks = contraption.getBlocks();
                for (BlockPos lp : blocks.keySet()) {
                    boolean interior = true;
                    for (Direction d : Direction.values()) {
                        if (!blocks.containsKey(lp.relative(d))) { interior = false; break; }
                    }
                    if (!interior) hull.add(lp);
                }
            }
            hullBlocksCache = hull;
        }
        return hullBlocksCache;
    }

    private boolean blockedBy(double dx, double dy, double dz) {
        for (BlockPos local : hullBlocks()) {
            // 用旋轉後的世界角落（船會轉），碰撞用軸對齊 1x1 近似（夠用，不做 OBB）
            Vec3 c = rotatedWorldCorner(local.getX(), local.getY(), local.getZ());
            AABB newBox = new AABB(c.x + dx, c.y + dy, c.z + dz, c.x + dx + 1, c.y + dy + 1, c.z + dz + 1).deflate(0.06);
            if (!boxHitsTerrain(newBox)) continue;
            // 新位置撞到地形。但若這方塊「現在」就已嵌在地形裡(剛組裝卡地板/嵌進山)，
            // 不算擋 → 讓船能脫離，否則任何方向都撞、永遠卡死。只擋「本來沒撞、移過去才撞」的真碰撞。
            AABB curBox = new AABB(c.x, c.y, c.z, c.x + 1, c.y + 1, c.z + 1).deflate(0.06);
            if (boxHitsTerrain(curBox)) continue;
            return true;
        }
        return false;
    }

    /** 軸對齊盒是否撞到世界實心方塊(忽略自己的發射台結構)。 */
    private boolean boxHitsTerrain(AABB box) {
        int minX = Mth.floor(box.minX), maxX = Mth.floor(box.maxX);
        int minY = Mth.floor(box.minY), maxY = Mth.floor(box.maxY);
        int minZ = Mth.floor(box.minZ), maxZ = Mth.floor(box.maxZ);
        for (int bx = minX; bx <= maxX; bx++)
            for (int by = minY; by <= maxY; by++)
                for (int bz = minZ; bz <= maxZ; bz++) {
                    BlockPos bp = new BlockPos(bx, by, bz);
                    BlockState s = level().getBlockState(bp);
                    if (s.isAir() || isShipScaffolding(s)) continue; // 自己的發射台結構不擋船
                    var shape = s.getCollisionShape(level(), bp);
                    if (shape.isEmpty()) continue;
                    if (box.intersects(shape.bounds().move(bx, by, bz))) return true;
                }
        return false;
    }

    /**
     * 只有組裝架(gantry)不算地形：船要能從框架往上升起飛，所以穿過 gantry。
     * 底座/組裝台維持實心（會擋船=玩家預期）。核心/座椅組裝後已從世界移除，不在世界裡，不必列。
     */
    private static boolean isShipScaffolding(BlockState s) {
        return s.getBlock() instanceof ShipAssemblyGantryBlock;
    }

    // getDimensions 仍給一個合理尺寸(部分 vanilla 邏輯會用)，但真正的 hitbox 由 makeBoundingBox 決定(下方)。
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (contraption == null) return super.getDimensions(pose);
        AABB b = contraption.bounds();
        double xs = b.maxX - b.minX, zs = b.maxZ - b.minZ;
        float w = (float) Math.max(Math.sqrt(xs * xs + zs * zs), 1.0);
        float h = (float) Math.max(b.maxY - b.minY, 1.0);
        return EntityDimensions.scalable(w, h);
    }

    // hitbox 用「以船 3D 中心為中心、邊長 = 3D 對角線」的立方體：軸對齊盒在船 yaw/pitch/roll 傾斜時包不住
    // 旋轉後的角落(實體 AABB 是 pick 與碰撞 mixin 找船的依據) → 點不到駕駛位、角落方塊穿過去。立方體任何姿勢都覆蓋。
    // (寬鬆只是廣相位；準心真正打哪個方塊由 pickLocal、碰撞由 applyContraptionMovement 的窄相位決定。)
    @Override
    protected AABB makeBoundingBox() {
        if (contraption == null) return super.makeBoundingBox();
        AABB b = contraption.bounds();
        double xs = b.maxX - b.minX, ys = b.maxY - b.minY, zs = b.maxZ - b.minZ;
        double half = Math.max(Math.sqrt(xs * xs + ys * ys + zs * zs), 1.0) / 2.0;
        double cy = getY() + ys / 2.0; // 實體在船底(minY)，船的垂直中心要 +yspan/2
        return new AABB(getX() - half, cy - half, getZ() - half, getX() + half, cy + half, getZ() + half);
    }

    // ── 騎乘 ────────────────────────────────────────────────────────────────

    /**
     * 右鍵分流：從玩家視線 raycast 打進船的 local 方塊，看指到哪個方塊再決定行為。
     * 指到座椅=上船；指到船身其他地方=沒反應（達成「大碰撞箱不互動、座位才上船」）。
     * Phase 2+ 會在這裡接「指到門/箱子/機器=轉發互動」。
     */
    @Override
    public InteractionResult interactAt(Player player, Vec3 hitVec, InteractionHand hand) {
        // 互動全走 client EntityInteractSpecific → ShipInteractPacket → interactWithPick(準心 pick)。
        // 這裡 server 端不再自己做:client 的 cancel 偶爾擋不住 vanilla 這條,會跟封包雙重觸發
        //(基座物品放了又被 useWithoutItem 拿走、門開了又關)。PASS = 不做,封包是唯一互動路徑。
        return InteractionResult.PASS;
    }

    /** interactAt 的本體,但用指定的 pick。client 透過 ShipInteractPacket 傳準心 pick,server 不自己 raycast。 */
    public InteractionResult interactWithPick(Player player, InteractionHand hand, @Nullable Pick pick) {
        if (contraption == null || pick == null) return InteractionResult.PASS;
        BlockPos local = pick.local();
        if (local == null) return InteractionResult.PASS; // 指到空隙=船身，不反應
        var info = contraption.getBlocks().get(local);
        if (info == null) return InteractionResult.PASS;

        if (info.state().getBlock() instanceof ShipSeatBlock) {
            if (!level().isClientSide) {
                int clicked = getSeats().indexOf(local);            // 點到的是第幾張椅子
                int target = (clicked >= 0 && !isSeatOccupied(clicked)) ? clicked : firstFreeSeat();
                if (target >= 0 && player.startRiding(this)) {
                    assignSeat(player, target); // 坐到指定座位（不再照上船順序）
                    if (player instanceof ServerPlayer sp) {
                        // 上船瞬間視角 snap 到該座位的船頭方向，船不會為了對齊你的視線而甩動。
                        float bowWorld = sitYawLocal(getSeats().get(target)) + getYRot();
                        sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), bowWorld, sp.getXRot());
                    }
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        // Phase 2：門/活板門/柵欄門/拉桿/按鈕 → 切 blockstate（船上沒紅石，拉桿/按鈕純視覺切換）
        if (!level().isClientSide && tryToggleBlock(local, info.state())) {
            return InteractionResult.sidedSuccess(false);
        }
        if (level().isClientSide && isToggleable(info.state())) {
            return InteractionResult.sidedSuccess(true);
        }
        // VM3：箱子/機器等有 BlockEntity 的方塊 → 把互動轉發給「影子維度裡的真 BE」，
        // 開的是真正在運轉的機器/容器 GUI（stillValid mixin 讓跨維度選單不秒關）。
        // 影子不存在(沒 runData)時退回 Phase3 的鏡像容器(只箱子)。
        // 潛行 + 手持方塊 + 停船 = 編輯放置，讓它落到下面的放置分支（不開 GUI），其餘一律開互動方塊的 GUI
        boolean sneakPlacing = isParked() && player.isSecondaryUseActive()
                && player.getItemInHand(hand).getItem() instanceof BlockItem;
        if (info.state().getBlock() instanceof EntityBlock && !sneakPlacing) {
            if (!level().isClientSide && player instanceof ServerPlayer sp) {
                boolean opened = forwardUseToShadow(sp, local, info.state(), hand); // 影子真 BE 的 GUI
                // 結構杖在影子裡蓋出了祭壇柱子/底座/升級環(forwardUseToShadow 跑了 wand.useOn)，
                // 但那些是影子的新方塊、不在 contraption 裡 → 掃祭壇周圍(最大偏移 13)把新方塊收回視覺船。
                if (sp.getItemInHand(hand).getItem() instanceof StructureBuildWandItem) {
                    syncNewShadowBlocksToContraption(local, 14);
                }
                if (!opened && isContainer(info.state())) { openContainer(sp, local, info); opened = true; } // 退回鏡像容器
                // 箱子開了 GUI → 權威開蓋訊號(計人數，關閉由 ShipChestTracker 的 container close 事件處理)
                if (opened && info.state().getBlock() instanceof ChestBlock) {
                    onChestOpened(local);
                    ShipChestTracker.track(sp, this, local);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        // 停船編輯：手持方塊 + 指到非互動方塊的面 → 放方塊到相鄰空位（互動方塊優先，所以放在這後面）
        if (isParked() && player.getItemInHand(hand).getItem() instanceof BlockItem bi) {
            BlockPos newLocal = pick.local().relative(pick.face());
            if (!contraption.getBlocks().containsKey(newLocal)) {
                if (!level().isClientSide) {
                    if (placeBlock(player, hand, pick, newLocal, bi.getBlock())
                            && !player.getAbilities().instabuild) {
                        player.getItemInHand(hand).shrink(1);
                    }
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        // TODO Phase 4：機器 → 虛擬世界 tick
        return InteractionResult.PASS;
    }

    /** 船是否停著（沒人在駕駛）→ 才允許編輯。 */
    public boolean isParked() {
        return getControllingPassenger() == null;
    }

    /** 不受傷。左鍵挖停著的船的方塊改由 client 發 ShipBreakBlockPacket 處理(準心一致,不用 server raycast 延遲位置)。 */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /** 挖飛船上指定的 local 方塊(掉落 + 拆同步 + 門另一半)。哪一格由 client 的 ShipBreakBlockPacket 指定,
     *  跟 client 準心一致;不在 server 自己 raycast(會用到延遲的玩家位置 → 挖到別格)。 */
    public void breakLocalBlock(Player player, BlockPos local) {
        if (contraption == null) return;
        if (local == null || local.equals(BlockPos.ZERO)) return; // 核心(0,0,0)是錨點，不可挖
        var info = contraption.getBlocks().get(local);
        if (info == null) return;
        if (!player.getAbilities().instabuild) {
            ItemStack block = new ItemStack(info.state().getBlock().asItem());
            if (!block.isEmpty()) spawnAtLocation(block);
            if (info.nbt() != null && info.nbt().contains("Items")) {
                NonNullList<ItemStack> items = NonNullList.withSize(256, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(info.nbt(), items, level().registryAccess());
                for (ItemStack it : items) if (!it.isEmpty()) spawnAtLocation(it);
            }
        }
        playInteractSound(local, info.state().getSoundType().getBreakSound());
        removeBlockAndSync(local);
        // 門是雙方塊：另一半也要拆，否則留半截
        if (info.state().getBlock() instanceof DoorBlock && info.state().hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            BlockPos other = info.state().getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                    ? local.above() : local.below();
            if (contraption.getBlocks().containsKey(other)) removeBlockAndSync(other);
        }
    }

    private void removeBlockAndSync(BlockPos local) {
        updateContraptionBlock(local, Blocks.AIR.defaultBlockState());
        ShipBlockUpdatePacket.sendToClients(this, local, Blocks.AIR.defaultBlockState());
        writeToShadow(local, Blocks.AIR.defaultBlockState(), null); // 影子也挖掉（機器網路同步）
    }

    /**
     * 放方塊：用 vanilla getStateForPlacement 算朝向（把你的朝向/點擊面轉進 local 框），多方塊(門/床/
     * 高植物)放兩格。回傳是否有放。server 端。
     */
    boolean placeBlock(Player player, InteractionHand hand, Pick pick, BlockPos local, Block block) {
        if (isStructuralBlock(block)) return false;                 // 核心/發射台方塊不該放上船
        if (contraption.size() >= ShipContraption.MAX_BLOCKS) return false; // 方塊數上限（渲染/效能）
        return placeState(local, computePlacementState(player, hand, pick, local, block));
    }

    /** 核心與發射台方塊不可放到船上（會出現怪狀態/重複錨點）。 */
    private static boolean isStructuralBlock(Block b) {
        return b instanceof ShipCoreBlock || b instanceof ShipAssemblyBaseBlock
                || b instanceof ShipAssemblyGantryBlock || b instanceof ShipAssemblyPadBlock;
    }

    /** 把算好的 state 放進 contraption，多方塊(門/高植物上下、床頭腳)放兩格。回傳是否有放。package-visible 給 GameTest。 */
    boolean placeState(BlockPos local, BlockState state) {
        Block b = state.getBlock();
        // 箱子 double：computePlacementState 用真實世界 context 算 → 看不到 contraption 裡的相鄰箱子 → 預設 SINGLE。
        // 這裡照 vanilla 幾何在 contraption 內找同 facing 的相鄰單箱連成 LEFT/RIGHT(否則船上箱子開起來都是 27)。
        // LEFT 的夥伴在 facing.clockwise、RIGHT 在 facing.counterclockwise。
        if (b instanceof net.minecraft.world.level.block.ChestBlock
                && state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE) == ChestType.SINGLE) {
            Direction f = state.getValue(net.minecraft.world.level.block.ChestBlock.FACING);
            var cw = contraption.getBlocks().get(local.relative(f.getClockWise()));
            var ccw = contraption.getBlocks().get(local.relative(f.getCounterClockWise()));
            if (cw != null && isSingleChestFacing(cw.state(), f)) {
                state = state.setValue(net.minecraft.world.level.block.ChestBlock.TYPE, ChestType.LEFT);
                placeOne(local.relative(f.getClockWise()), cw.state().setValue(net.minecraft.world.level.block.ChestBlock.TYPE, ChestType.RIGHT));
            } else if (ccw != null && isSingleChestFacing(ccw.state(), f)) {
                state = state.setValue(net.minecraft.world.level.block.ChestBlock.TYPE, ChestType.RIGHT);
                placeOne(local.relative(f.getCounterClockWise()), ccw.state().setValue(net.minecraft.world.level.block.ChestBlock.TYPE, ChestType.LEFT));
            }
        }
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) { // 門/高植物：上下
            BlockPos up = local.above();
            if (contraption.getBlocks().containsKey(up)) return false;
            placeOne(local, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
            placeOne(up, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
        } else if (b instanceof BedBlock && state.hasProperty(BedBlock.PART)) { // 床：腳 local、頭 FACING 那格
            BlockPos head = local.relative(state.getValue(BedBlock.FACING));
            if (contraption.getBlocks().containsKey(head)) return false;
            placeOne(local, state.setValue(BedBlock.PART, BedPart.FOOT));
            placeOne(head, state.setValue(BedBlock.PART, BedPart.HEAD));
        } else {
            placeOne(local, state);
        }
        playInteractSound(local, state.getSoundType().getPlaceSound());
        return true;
    }

    private void placeOne(BlockPos local, BlockState state) {
        updateContraptionBlock(local, state);
        ShipBlockUpdatePacket.sendToClients(this, local, state);
        writeToShadow(local, state, null); // 影子也放（機器網路同步）
    }

    /** contraption 內某格是不是「同 facing 的單箱」(可連成 double 的對象)。 */
    private static boolean isSingleChestFacing(BlockState s, Direction f) {
        return s.getBlock() instanceof net.minecraft.world.level.block.ChestBlock
                && s.getValue(net.minecraft.world.level.block.ChestBlock.TYPE) == ChestType.SINGLE
                && s.getValue(net.minecraft.world.level.block.ChestBlock.FACING) == f;
    }

    /** 用 vanilla getStateForPlacement 算放置後的 state，方向轉進 local 框。失敗退回 defaultBlockState。 */
    private BlockState computePlacementState(Player player, InteractionHand hand, Pick pick, BlockPos local, Block block) {
        var stack = player.getItemInHand(hand);
        Direction localFace = worldToLocalDir(pick.face());
        BlockHitResult hit = new BlockHitResult(pick.hitLocal(), localFace, local, false);
        BlockPlaceContext ctx = new BlockPlaceContext(level(), player, hand, stack, hit) {
            @Override public BlockPos getClickedPos() { return local; }
            @Override public boolean canPlace() { return true; }
            @Override public boolean replacingClickedOnBlock() { return false; }
            @Override public Direction getClickedFace() { return localFace; }
            @Override public Direction getHorizontalDirection() { return worldToLocalDir(player.getDirection()); }
            @Override public Direction getNearestLookingDirection() { return worldToLocalDir(super.getNearestLookingDirection()); }
            @Override public Direction[] getNearestLookingDirections() {
                Direction[] w = super.getNearestLookingDirections();
                Direction[] l = new Direction[w.length];
                for (int i = 0; i < w.length; i++) l[i] = worldToLocalDir(w[i]);
                return l;
            }
        };
        BlockState s = block.getStateForPlacement(ctx);
        return s != null ? s : block.defaultBlockState();
    }

    /** 把世界方向轉進船的 local 框（繞 -shipYaw 的 90° 整步）。水平方向才轉，上下不動。 */
    private Direction worldToLocalDir(Direction d) {
        if (d.getAxis() == Direction.Axis.Y) return d;
        int steps = Math.floorMod(Math.round(getYRot() / 90f), 4);
        Direction r = d;
        for (int i = 0; i < steps; i++) r = r.getCounterClockWise();
        return r;
    }

    private static boolean isContainer(BlockState s) {
        Block b = s.getBlock();
        return b instanceof ChestBlock || b instanceof BarrelBlock; // v1：單箱 27 格（雙箱當兩個單箱）
    }

    /** 開船上箱子的 GUI：從 BE NBT 載入物品 → ChestMenu，setChanged 時寫回 contraption NBT。 */
    private void openContainer(ServerPlayer player, BlockPos local, StructureBlockInfo info) {
        final int size = 27;
        CompoundTag nbt = info.nbt() != null ? info.nbt() : new CompoundTag();
        NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, items, level().registryAccess());
        SimpleContainer container = new SimpleContainer(size) {
            @Override
            public void setChanged() {
                super.setChanged();
                writeContainerBack(local, this);
            }
        };
        for (int i = 0; i < size; i++) container.setItem(i, items.get(i));
        Component title = info.state().getBlock().getName();
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> ChestMenu.threeRows(id, inv, container), title));
    }

    /** 把容器內容寫回 contraption 的 BE NBT（server 端；拆解才保留物品）。 */
    void writeContainerBack(BlockPos local, Container container) {
        if (contraption == null) return;
        var info = contraption.getBlocks().get(local);
        if (info == null) return;
        CompoundTag nbt = info.nbt() != null ? info.nbt().copy() : new CompoundTag();
        int size = container.getContainerSize();
        NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < size; i++) items.set(i, container.getItem(i));
        ContainerHelper.saveAllItems(nbt, items, level().registryAccess());
        contraption.setBlockNbt(local, nbt);
        renderBEs = null; // BE 快取作廢（保險）
        writeToShadow(local, info.state(), nbt); // 影子的箱子也同步（不然鏡射會蓋回去）
    }

    private static boolean isToggleable(BlockState s) {
        Block b = s.getBlock();
        return ((b instanceof DoorBlock || b instanceof TrapDoorBlock || b instanceof FenceGateBlock)
                && s.hasProperty(BlockStateProperties.OPEN))
                || ((b instanceof LeverBlock || b instanceof ButtonBlock)
                && s.hasProperty(BlockStateProperties.POWERED));
    }

    /** 切換互動方塊狀態（server）。門連另一半一起切。回傳是否有切。package-visible 給 GameTest。 */
    boolean tryToggleBlock(BlockPos local, BlockState s) {
        Block b = s.getBlock();
        if (b instanceof DoorBlock && s.hasProperty(BlockStateProperties.OPEN)) {
            boolean open = !s.getValue(BlockStateProperties.OPEN);
            setAndSync(local, s.setValue(BlockStateProperties.OPEN, open));
            // 門的另一半（上/下）也要切，否則只開一半
            BlockPos other = s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                    ? local.above() : local.below();
            var oi = contraption.getBlocks().get(other);
            if (oi != null && oi.state().getBlock() instanceof DoorBlock
                    && oi.state().hasProperty(BlockStateProperties.OPEN)) {
                setAndSync(other, oi.state().setValue(BlockStateProperties.OPEN, open));
            }
            playInteractSound(local, open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE);
            return true;
        }
        if ((b instanceof TrapDoorBlock || b instanceof FenceGateBlock) && s.hasProperty(BlockStateProperties.OPEN)) {
            boolean open = !s.getValue(BlockStateProperties.OPEN);
            setAndSync(local, s.setValue(BlockStateProperties.OPEN, open));
            playInteractSound(local, b instanceof FenceGateBlock
                    ? (open ? SoundEvents.FENCE_GATE_OPEN : SoundEvents.FENCE_GATE_CLOSE)
                    : (open ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE));
            return true;
        }
        if ((b instanceof LeverBlock || b instanceof ButtonBlock) && s.hasProperty(BlockStateProperties.POWERED)) {
            setAndSync(local, s.cycle(BlockStateProperties.POWERED)); // 船上無紅石，純視覺
            playInteractSound(local, SoundEvents.LEVER_CLICK);
            return true;
        }
        return false;
    }

    private void setAndSync(BlockPos local, BlockState ns) {
        updateContraptionBlock(local, ns);
        ShipBlockUpdatePacket.sendToClients(this, local, ns);
        writeToShadow(local, ns, null); // 影子也切（門開關等）
    }

    private void playInteractSound(BlockPos local, SoundEvent sound) {
        Vec3 w = rotatedWorldPoint(local.getX() + 0.5, local.getY() + 0.5, local.getZ() + 0.5);
        level().playSound(null, w.x, w.y, w.z, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    // ── 音效橋接(影子→船) ───────────────────────────────────────────────────
    /** 這個影子座標屬於本船嗎(在本船的影子區域 + 對應 local 有方塊)。 */
    public boolean ownsShadowPos(BlockPos sp) {
        if (shadowAnchor == null || contraption == null) return false;
        return contraption.getBlocks().containsKey(sp.subtract(shadowAnchor));
    }
    /** 影子座標 → 船上世界座標(含船姿勢)。 */
    public Vec3 shadowToWorld(double sx, double sy, double sz) {
        return rotatedWorldPoint(sx - shadowAnchor.getX(), sy - shadowAnchor.getY(), sz - shadowAnchor.getZ());
    }
    public BlockPos shadowAnchor() { return shadowAnchor; }

    // ── 船箱子開蓋的權威訊號(治本:不靠 client 猜畫面) ───────────────────────────
    // 每格箱子有幾個玩家開著。0→1 廣播開蓋 + 播開箱聲;1→0 廣播關蓋 + 播關箱聲。多人共開時蓋保持開。
    private final Map<BlockPos, Integer> chestViewers = new HashMap<>();

    /** server：玩家開了這格船箱子的 GUI。 */
    public void onChestOpened(BlockPos local) {
        if (level().isClientSide) return;
        int c = chestViewers.merge(local.immutable(), 1, Integer::sum);
        if (c == 1) {
            ShipChestLidPacket.sendToClients(this, local, true);
            playChestSound(local, SoundEvents.CHEST_OPEN);
        }
    }

    /** server：玩家關了這格船箱子的 GUI(或斷線/換容器)。 */
    public void onChestClosed(BlockPos local) {
        if (level().isClientSide) return;
        Integer c = chestViewers.get(local);
        if (c == null) return;
        if (c <= 1) {
            chestViewers.remove(local);
            ShipChestLidPacket.sendToClients(this, local, false);
            playChestSound(local, SoundEvents.CHEST_CLOSE);
        } else {
            chestViewers.put(local.immutable(), c - 1);
        }
    }

    private void playChestSound(BlockPos local, SoundEvent sound) {
        Vec3 w = rotatedWorldPoint(local.getX() + 0.5, local.getY() + 0.5, local.getZ() + 0.5);
        level().playSound(null, w.x, w.y, w.z, sound, SoundSource.BLOCKS,
                0.5f, level().random.nextFloat() * 0.1f + 0.9f);
    }

    /** client：挖船上方塊噴碎裂粒子（位置依旋轉算）。 */
    private void spawnBreakParticles(BlockPos local, BlockState state) {
        Vec3 c = rotatedWorldPoint(local.getX() + 0.5, local.getY() + 0.5, local.getZ() + 0.5);
        BlockParticleOption opt = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < 16; i++) {
            level().addParticle(opt,
                    c.x + (random.nextDouble() - 0.5) * 0.7,
                    c.y + (random.nextDouble() - 0.5) * 0.7,
                    c.z + (random.nextDouble() - 0.5) * 0.7,
                    (random.nextDouble() - 0.5) * 0.2, random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.2);
        }
    }

    /** 從玩家視線 raycast 找指到的 local 方塊（用 outline 形狀，逐方塊取最近命中）。 */
    @Nullable
    public record Pick(BlockPos local, Direction face, Vec3 hitLocal) {}

    /** Client 算準心瞄準的 pick(local+面+hit),透過 ShipInteractPacket 傳給 server,讓互動跟準心一致。 */
    @Nullable
    public Pick clientPick(Player player) {
        return pickLocal(player);
    }

    /** raycast 視線進船的 local 方塊，回傳最近命中的方塊 + 命中面 + 命中點(local，放方塊朝向要用)。 */
    @Nullable
    private Pick pickLocal(Player player) {
        if (contraption == null) return null;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(6.0));
        Vec3 ls = worldToLocalPoint(eye.x, eye.y, eye.z);
        Vec3 le = worldToLocalPoint(end.x, end.y, end.z);
        Pick best = null;
        double bestDist = Double.MAX_VALUE;
        for (var e : contraption.getBlocks().entrySet()) {
            BlockPos lp = e.getKey();
            VoxelShape shape = e.getValue().state().getShape(EmptyBlockGetter.INSTANCE, lp);
            if (shape.isEmpty()) shape = Shapes.block(); // 空 outline 也補滿格 → 渲染得出來就指得到
            BlockHitResult hit = shape.clip(ls, le, lp);
            if (hit != null) {
                double d = hit.getLocation().distanceToSqr(ls);
                if (d < bestDist) { bestDist = d; best = new Pick(lp, hit.getDirection(), hit.getLocation()); }
            }
        }
        return best;
    }

    @Nullable
    private BlockPos pickLocalBlock(Player player) {
        Pick p = pickLocal(player);
        return p == null ? null : p.local();
    }

    /** client 選取外框用：玩家瞄準的 local 方塊（沒指到回 null）。 */
    @Nullable
    public BlockPos getAimedLocalBlock(Player player) {
        return pickLocalBlock(player);
    }

    /** 命中面方向（放方塊用）；沒命中回 UP 當保底。 */
    private Direction pickFace(Player player) {
        Pick p = pickLocal(player);
        return p == null ? Direction.UP : p.face();
    }

    /**
     * 下船位置：出現在自己座位旁（往船右側一格），不再停在船中心(≈核心)。
     * vanilla 不替玩家呼叫 getDismountLocationForPassenger，所以在 removePassenger 自己擺位。
     */
    @Override
    protected void removePassenger(Entity passenger) {
        List<BlockPos> seats = getSeats();
        int idx = seatIndexOf(passenger); // 移除前先取指派的座位 index
        Vec3 spot = null;
        if (idx >= 0 && idx < seats.size()) {
            BlockPos seat = seats.get(idx);
            Vec3 seatWorld = rotatedWorldPoint(seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5);
            // 在座位周圍試幾個方向，挑第一個不卡在船方塊裡的（避免下船穿進牆/船模）
            double[][] offsets = {{1.2, 0, 0}, {-1.2, 0, 0}, {0, 0, 1.2}, {0, 0, -1.2}, {0, 1.2, 0}};
            for (double[] o : offsets) {
                Vec3 side = rotateVec(o[0], o[1], o[2], getYRot());
                Vec3 cand = new Vec3(seatWorld.x + side.x, seatWorld.y + o[1], seatWorld.z + side.z);
                if (!isInsideShip(cand.x, cand.y, cand.z)) { spot = cand; break; }
            }
            if (spot == null) spot = new Vec3(seatWorld.x, seatWorld.y + 1.5, seatWorld.z); // 都被擋就放上方
        }
        super.removePassenger(passenger);
        if (!level().isClientSide) unassignSeat(passenger); // 清掉座位指派
        if (spot != null && !level().isClientSide) {
            passenger.setPos(spot.x, spot.y, spot.z);
            if (passenger instanceof ServerPlayer sp) {
                sp.connection.teleport(spot.x, spot.y, spot.z, sp.getYRot(), sp.getXRot());
            }
        }
    }

    // 預設 Entity.isPickable() 回 false → 點不到實體、interact 不觸發。飛船要可被右鍵
    @Override
    public boolean isPickable() {
        return !isRemoved();
    }


    /**
     * 收船：把 contraption 方塊寫回世界並 discard。船會轉，所以 yaw snap 到最近的 90°，
     * 方塊位置與 blockstate 一起套用該旋轉（不能用任意角度放回方塊）。被擋則不動，回傳 false。
     */
    public boolean disassemble() {
        if (level().isClientSide || contraption == null) return false;
        Rotation rotation = snapRotation(getYRot());
        // 核心(local 0,0,0)的世界位置 = rotatedWorldCorner(0,0,0)，snap 到整數格當寫回錨點
        Vec3 coreWorld = rotatedWorldCorner(0, 0, 0);
        BlockPos target = new BlockPos(
                Mth.floor(coreWorld.x + 0.5), Mth.floor(coreWorld.y + 0.5), Mth.floor(coreWorld.z + 0.5));
        if (!contraption.addToWorld(level(), target, rotation)) return false;
        clearShadow(); // 收船：影子那份也清掉 + 釋放 slot
        intentionalDisassembly = true; // 正常拆解：remove() 的保險不要再寫一次
        discard();
        return true;
    }

    /** 把連續 yaw snap 到最近的 90° 對應的 Rotation。 */
    private static Rotation snapRotation(float yaw) {
        int steps = Math.floorMod(Math.round(yaw / 90f), 4);
        return switch (steps) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        // 駕駛 = 坐在最小駕駛位(index<MAX_DRIVERS)的玩家。依實際坐的座位，不再看上船順序。
        int primary = primaryDriverSeat();
        if (primary < 0) return null;
        for (Entity p : getPassengers()) {
            if (p instanceof Player pl && seatIndexOf(p) == primary) return pl;
        }
        return null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < seatCount();
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        List<BlockPos> seats = getSeats();
        if (seats.isEmpty()) return;
        int idx = seatIndexOf(passenger); // 依指派的座位
        if (idx < 0 || idx >= seats.size()) idx = 0;
        BlockPos seat = seats.get(idx);
        // 駕駛和乘客都「完整跟船轉」(pitch/roll 都跟)：坐在座位上、船俯仰/側翻時人跟著動，不會浮在空中。
        // 座位水平中心(+0.5)+座高(SEAT_SIT_HEIGHT)併進 local 再一起旋轉，傾斜時不偏離座位。
        Vec3 c = rotatedWorldPoint(seat.getX() + 0.5, seat.getY() + SEAT_SIT_HEIGHT, seat.getZ() + 0.5);
        callback.accept(passenger, c.x, c.y, c.z);
        // 讓坐姿身體面向椅子的坐姿方向（=該座椅 sit-dir + 船 yaw），否則身體用 vanilla 預設方向看起來反。
        // 只設身體(yBodyRot)不鎖視角：駕駛仍可自由看/操控、乘客頭也能轉。
        if (passenger instanceof LivingEntity living) {
            float seatWorldYaw = sitYawLocal(seat) + getYRot();
            living.setYBodyRot(seatWorldYaw);
            living.yBodyRotO = seatWorldYaw;
            // 非駕駛乘客視角也跟著船轉（駕駛自己控制視角、船跟著駕駛轉，不強制）
            if (passenger != getControllingPassenger()) {
                float dYaw = getYRot() - yRotO;
                if (dYaw != 0) {
                    living.setYRot(living.getYRot() + dYaw);
                    living.setYHeadRot(living.getYHeadRot() + dYaw);
                }
            }
        }
    }

    /**
     * 座位清單（local 座標）：座椅方塊依「離核心距離」排序，前 MAX_DRIVERS 張=駕駛位，其餘=乘客。
     * 不限總人數。完全沒座椅 → 空清單 → 不能上船/駕駛（要放座椅，或未來的無人駕駛模塊）。
     */
    public List<BlockPos> getSeats() {
        List<BlockPos> seats = new ArrayList<>();
        if (contraption != null) {
            contraption.getBlocks().entrySet().stream()
                    .filter(e -> e.getValue().state().getBlock() instanceof ShipSeatBlock)
                    .map(Map.Entry::getKey)
                    .sorted(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(BlockPos.ZERO))
                            .thenComparingInt(BlockPos::getX)
                            .thenComparingInt(BlockPos::getY)
                            .thenComparingInt(BlockPos::getZ))
                    .forEach(seats::add);
        }
        return seats;
    }

    private int seatCount() {
        return getSeats().size();
    }

    // 實體本體只有 1x1，但船可能很大：用 contraption 範圍當渲染剔除框，
    // 否則核心移出畫面時整艘船會被 frustum culling 剔掉
    @Override
    public AABB getBoundingBoxForCulling() {
        if (contraption == null) return super.getBoundingBoxForCulling();
        // bounds() 相對核心；核心世界位置 = 實體位置 - centerOffset
        Vec3 co = centerOffset();
        return contraption.bounds()
                .move(getX() - co.x, getY() - co.y, getZ() - co.z)
                .inflate(1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SEATS, new CompoundTag()); // 座位指派；contraption 走 complex spawn
        builder.define(DATA_ROLL, 0.0f);
        builder.define(DATA_BOW, 0.0f);
        builder.define(DATA_THROTTLE, 1.0f); // 預設滿油門
        builder.define(DATA_FUEL, 0);
        builder.define(DATA_WARP_FUEL, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Ship")) {
            ShipContraption c = new ShipContraption();
            c.readNbt(level(), tag.getCompound("Ship"));
            this.contraption = c;
            recomputeFuelSystem();
            refreshDimensions();
        }
        if (tag.contains("ShadowAnchor")) {
            int[] a = tag.getIntArray("ShadowAnchor");
            if (a.length == 3) shadowAnchor = new BlockPos(a[0], a[1], a[2]);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (contraption != null) {
            tag.put("Ship", contraption.writeNbt(level().registryAccess()));
        }
        if (shadowAnchor != null) {
            tag.putIntArray("ShadowAnchor", new int[]{shadowAnchor.getX(), shadowAnchor.getY(), shadowAnchor.getZ()});
        }
    }

    @Nullable private BlockPos shadowAnchor; // 影子維度裡這台船方塊的錨點（VM1，server-only）
    private transient boolean shadowForceLoaded; // VM4：本次載入是否已 force-load 影子（重啟/卸載後 reset）
    public void setShadowAnchor(BlockPos a) { this.shadowAnchor = a; }
    @Nullable public BlockPos getShadowAnchor() { return shadowAnchor; }

    /** VM4：船載入後確保影子區域 force-load（機器才會 tick）。組裝時已 force-load，這裡涵蓋重啟/重載。 */
    private void ensureShadowForceLoaded() {
        if (shadowForceLoaded || shadowAnchor == null || contraption == null) return;
        ServerLevel shadow = getShadow();
        if (shadow == null) return;
        ShipShadowManager.setForceLoad(shadow, shadowAnchor, contraption.bounds(), true);
        shadowForceLoaded = true;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        boolean has = contraption != null;
        buf.writeBoolean(has);
        if (has) contraption.writeToBuf(buf); // 直接 buffer 編碼(快+小)，非每方塊 NBT
        // VM3b：把影子錨點同步給 client，讓 entityMenu 能用影子座標反算 local、從船的 render BE 建機器選單
        boolean hasAnchor = shadowAnchor != null;
        buf.writeBoolean(hasAnchor);
        if (hasAnchor) buf.writeBlockPos(shadowAnchor);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            ShipContraption c = new ShipContraption();
            c.readFromBuf(buf);
            this.contraption = c;
            recomputeFuelSystem();
            refreshDimensions();
        }
        if (buf.readBoolean()) shadowAnchor = buf.readBlockPos();
        // 初始化 xOld/yRotO 等 = 現值，否則第一幀 client 的甲板碰撞用 xOld=0 在錯框算 → 剛 spawn 的船穿過去
        this.setOldPosAndRot();
    }

    /**
     * VM3b（client）：影子維度的 BE pos 反查船上的 render BE。entityMenu 在 client 找不到世界 BE 時用，
     * 讓機器選單能用對的型別 BE 建構（內容由 server 影子 BE 廣播同步）。pos 在某船影子區域內就回該 local 的 render BE。
     */
    @Nullable
    public static BlockEntity findShadowRenderBE(Player player, BlockPos shadowPos) {
        // 玩家剛跟船互動才開選單，船一定在身邊；只搜附近，不掃全世界
        for (ShipEntity ship : player.level().getEntitiesOfClass(ShipEntity.class,
                player.getBoundingBox().inflate(48.0))) {
            BlockPos anchor = ship.shadowAnchor;
            if (anchor == null || ship.contraption == null) continue;
            BlockPos local = shadowPos.subtract(anchor);
            if (!ship.contraption.getBlocks().containsKey(local)) continue;
            return ship.getRenderBlockEntities().get(local);
        }
        return null;
    }
}
