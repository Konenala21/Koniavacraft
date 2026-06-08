package com.github.nalamodikk.space.ship;

import com.github.nalamodikk.common.network.packet.client.ship.ShipBlockUpdatePacket;
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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.ArrayVoxelShape;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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

    // client 端平滑跟隨 server 廣播位置用（lerpSteps 在 Entity 是 private 無 getter，自己存一份）
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYRot, lerpXRot;

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
        this.hullBlocksCache = null;
        this.dynamicMirrorCache = null;
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
        // client：挖掉方塊(state=air)時噴破壞粒子（移除前用舊 state）
        if (level().isClientSide && state.isAir()) {
            var old = contraption.getBlocks().get(local);
            if (old != null && !old.state().isAir()) spawnBreakParticles(local, old.state());
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
        localCollisionShapeCache = null;
        hullBlocksCache = null;
        dynamicMirrorCache = null;
        renderBEs = null;
        if (meshCache instanceof ShipMeshHandle h) {
            h.markDirty(); // 保留舊 VBO，debounce 後重烤一次(大船每次編輯都砍重建會卡)
        } else {
            meshCache = null; // 還沒建/烤失敗 → 讓渲染器重建
            meshFailed = false;
        }
        refreshDimensions(); // bounds 可能變了，更新 hitbox
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        // 保險：被 /kill 或非預期 discard（reason.shouldDestroy()=KILLED/DISCARDED）而不是正常拆解時，
        // 把方塊寫回世界（船散架在原地），不要讓整艘船無聲蒸發。區塊卸載/維度切換不觸發（會存檔/搬移）。
        if (!level().isClientSide && contraption != null && !intentionalDisassembly && reason.shouldDestroy()) {
            recoverContraptionToWorld();
        }
        // VM4：船卸載/換維度（非銷毀）時，解除影子 force-load 讓機器暫停省效能；方塊保留，重載再開
        if (!level().isClientSide && !reason.shouldDestroy() && shadowAnchor != null && contraption != null) {
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
            if (contraption != null) {
                for (var e : contraption.getBlocks().entrySet()) {
                    var info = e.getValue();
                    if (!(info.state().getBlock() instanceof EntityBlock eb)) continue;
                    BlockEntity be;
                    if (info.nbt() != null) {
                        // 有 NBT（組裝時抓的）：還原內容
                        be = BlockEntity.loadStatic(e.getKey(), info.state(), info.nbt(), level().registryAccess());
                    } else {
                        // 停船編輯新放的 BE 方塊沒 NBT：建一個空 BE 才渲染得出來（例如箱子）
                        be = eb.newBlockEntity(e.getKey(), info.state());
                    }
                    if (be != null) {
                        be.setLevel(level());
                        renderBEs.put(e.getKey(), be);
                    }
                }
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
        shadow.setBlock(sp, state, Block.UPDATE_ALL);
        if (nbt != null) {
            BlockEntity be = shadow.getBlockEntity(sp);
            if (be != null) be.loadWithComponents(nbt, shadow.registryAccess());
        }
    }

    // 鏡射只需掃「會變」的方塊：機器(EntityBlock，運轉/亮/進度) + 隨機刻(作物生長)。靜態方塊永不變。
    // 編輯(門/拉桿/放挖)已在 writeToShadow 兩端同寫，不靠鏡射，所以不必納入。快取，加/減方塊才失效。
    @Nullable private java.util.List<BlockPos> dynamicMirrorCache;
    private java.util.List<BlockPos> dynamicMirrorBlocks() {
        if (dynamicMirrorCache == null) {
            java.util.List<BlockPos> dyn = new java.util.ArrayList<>();
            if (contraption != null) {
                for (var e : contraption.getBlocks().entrySet()) {
                    BlockState st = e.getValue().state();
                    if (st.getBlock() instanceof EntityBlock || st.isRandomlyTicking()) dyn.add(e.getKey());
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
            // 安全：影子讀到空氣但視覺有方塊 = 影子那格沒載/放置失敗，絕不可把視覺船的方塊鏡射成空氣抹掉
            if (ss.isAir() && !info.state().isAir()) continue;
            if (!ss.equals(info.state())) {
                contraption.setBlockState(local, ss);          // blockstate 變(作物/熔爐亮/機器 active)
                ShipBlockUpdatePacket.sendToClients(this, local, ss);
                localCollisionShapeCache = null;               // 形狀可能變(作物長/門開)
            }
            if (ss.getBlock() instanceof EntityBlock) {        // BE NBT 鏡射(機器內容/進度)：拆解才保留正確狀態
                BlockEntity sbe = shadow.getBlockEntity(sp);
                if (sbe != null) {
                    CompoundTag tag = sbe.saveWithFullMetadata(shadow.registryAccess());
                    tag.remove("x"); tag.remove("y"); tag.remove("z");
                    contraption.setBlockNbt(local, tag);
                }
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
        target = target.scale(MAX_SPEED);

        shipVel = shipVel.add(target.subtract(shipVel).scale(ACCEL));
        if (shipVel.lengthSqr() < 1e-6) shipVel = Vec3.ZERO;
        Vec3 allowed = resolveTerrain(shipVel); // 撞地形的軸歸零（會沿牆滑）
        if (allowed.lengthSqr() > 0) {
            setPos(getX() + allowed.x, getY() + allowed.y, getZ() + allowed.z);
        }
        shipVel = allowed; // 撞到的軸不保留動量
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
        // 用 vanilla collideBoundingBox 對船形狀碰撞(跟 Create 同招，robust)。world border 在 local 原點附近不觸發。
        Vec3 lm = new Vec3(lmv.x, lmv.y, lmv.z);
        Vec3 collided = Entity.collideBoundingBox(e, lm, lb, level(), java.util.List.of(shape));
        Vec3 newLocal = L0.add(collided.x, collided.y, collided.z);
        Vec3 newWorld = localToWorldAt(newLocal.x, newLocal.y, newLocal.z, getX(), getY(), getZ(), q1);
        Vec3 result = newWorld.subtract(P);
        // 旋轉 round-trip 在沒實際被擋的軸留下 ~1e-10 epsilon；vanilla Entity.move 用精確 != 判碰撞，
        // 會把這 epsilon 當成撞牆 → 每 tick 歸零玩家速度 → 走/跳「黏黏的」。差距極小的軸 snap 回 desired。
        double rx = Math.abs(result.x - worldMotion.x) < 1.0e-4 ? worldMotion.x : result.x;
        double ry = Math.abs(result.y - worldMotion.y) < 1.0e-4 ? worldMotion.y : result.y;
        double rz = Math.abs(result.z - worldMotion.z) < 1.0e-4 ? worldMotion.z : result.z;
        return new Vec3(rx, ry, rz);
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

    /** 實體是否「站在」這艘船上（腳底正下方有船的方塊）。用來決定要不要 carry。 */
    public boolean isSupporting(Entity e) {
        VoxelShape shape = localCollisionShape();
        if (shape.isEmpty()) return false;
        AABB b = e.getBoundingBox();
        Vec3 cen = b.getCenter();
        Vec3 lc = worldToLocalPoint(cen.x, b.minY - 0.02, cen.z); // 腳底略下方
        AABB probe = AABB.ofSize(lc, Math.max(b.getXsize(), 0.1), 0.12, Math.max(b.getZsize(), 0.1));
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
        double mx = move.x != 0 && blockedBy(move.x, 0, 0) ? 0 : move.x;
        double my = move.y != 0 && blockedBy(0, move.y, 0) ? 0 : move.y;
        double mz = move.z != 0 && blockedBy(0, 0, move.z) ? 0 : move.z;
        return new Vec3(mx, my, mz);
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
        if (contraption == null) return InteractionResult.PASS;
        BlockPos local = pickLocalBlock(player);
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
                if (!opened && isContainer(info.state())) openContainer(sp, local, info); // 退回鏡像容器
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        // 停船編輯：手持方塊 + 指到非互動方塊的面 → 放方塊到相鄰空位（互動方塊優先，所以放在這後面）
        if (isParked() && player.getItemInHand(hand).getItem() instanceof BlockItem bi) {
            Pick pick = pickLocal(player);
            if (pick != null) {
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
        }
        // TODO Phase 4：機器 → 虛擬世界 tick
        return InteractionResult.PASS;
    }

    /** 船是否停著（沒人在駕駛）→ 才允許編輯。 */
    public boolean isParked() {
        return getControllingPassenger() == null;
    }

    /** 左鍵(攻擊)停著的船 = 挖掉指到的方塊（核心不可挖）。不真的扣血。 */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && contraption != null
                && source.getEntity() instanceof Player player && isParked()) {
            breakAimedBlock(player);
        }
        return false;
    }

    private void breakAimedBlock(Player player) {
        BlockPos local = pickLocalBlock(player);
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
    private record Pick(BlockPos local, Direction face, Vec3 hitLocal) {}

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
        // 座位水平中心(+0.5)要在旋轉前併入 local，否則船一轉乘客被推離座位（第三人稱看「坐在空氣上」）。
        // 高度放座椅座板高(SEAT_SIT_HEIGHT)，原本 +1.0 是整格頂、會浮在椅子上方。
        Vec3 c = rotatedWorldPoint(seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5);
        callback.accept(passenger, c.x, c.y + SEAT_SIT_HEIGHT, c.z);
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
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Ship")) {
            ShipContraption c = new ShipContraption();
            c.readNbt(level(), tag.getCompound("Ship"));
            this.contraption = c;
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
