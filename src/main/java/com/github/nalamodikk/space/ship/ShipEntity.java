package com.github.nalamodikk.space.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

    @Nullable private ShipContraption contraption;

    // 駕駛輸入（由 ShipControlPacket 每 tick 設；server 端用）
    private float inForward, inStrafe;
    private int inVertical;
    private float riderYaw;
    private Vec3 shipVel = Vec3.ZERO;

    public ShipEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;     // 自己用 contraption 邏輯處理，不走 vanilla 方塊碰撞
        this.setNoGravity(true);   // 飛船不受重力（太空/停在原地）
    }

    /** 由 ShipControlPacket 設定駕駛輸入（forward/strafe ∈ [-1,1]，vertical ∈ {-1,0,1}）。 */
    public void setControlInput(float forward, float strafe, int vertical, float yaw) {
        this.inForward = forward;
        this.inStrafe = strafe;
        this.inVertical = vertical;
        this.riderYaw = yaw;
    }

    // client 渲染用：從 contraption NBT 還原的臨時 BlockEntity（箱子等 BER 方塊），建一次快取
    @Nullable private Map<BlockPos, BlockEntity> renderBEs;
    // client 渲染用：靜態方塊烤好的 VBO 快取（型別 Object 避免 server 載入 client 類別）
    @Nullable private Object meshCache;
    private boolean meshFailed;

    public void setContraption(ShipContraption contraption) {
        this.contraption = contraption;
        this.renderBEs = null;   // contraption 換了，BE 快取作廢
        this.meshCache = null;   // 烤好的 VBO 也作廢（contraption 只在 spawn 設一次，實務上不會走到）
        refreshDimensions();     // 依新 contraption 撐大 hitbox
    }

    // ── 靜態方塊 VBO 快取（client）──────────────────────────────────────────
    @Nullable public Object getMeshCache() { return meshCache; }
    public void setMeshCache(@Nullable Object cache) { this.meshCache = cache; }
    public boolean isMeshFailed() { return meshFailed; }
    public void markMeshFailed() { this.meshFailed = true; }

    @Override
    public void remove(Entity.RemovalReason reason) {
        // 釋放烤好的 VBO（GL 資源）。client 端、render thread 上做
        if (level().isClientSide && meshCache instanceof AutoCloseable ac) {
            try { ac.close(); } catch (Exception ignored) {}
            meshCache = null;
        }
        super.remove(reason);
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
                    if (info.nbt() == null || !(info.state().getBlock() instanceof EntityBlock)) continue;
                    BlockEntity be = BlockEntity.loadStatic(e.getKey(), info.state(), info.nbt(),
                            level().registryAccess());
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

        // 載具移動由「控制者的 client」算（vanilla 模型：駕駛 client 是位置權威，
        // 會自動送 ServerboundMoveVehiclePacket 同步給 server）。在 server 端算會被
        // client 送來的位置覆蓋，等於白算。其餘端（server/旁觀 client）只跟同步位置。
        if (isControlledByLocalInstance()) {
            if (!(getControllingPassenger() instanceof Player)) {
                inForward = 0; inStrafe = 0; inVertical = 0;
            }
            double rad = Math.toRadians(riderYaw);
            Vec3 forwardDir = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
            Vec3 rightDir = new Vec3(Math.cos(rad), 0, Math.sin(rad));
            Vec3 target = forwardDir.scale(inForward).add(rightDir.scale(inStrafe)).add(0, inVertical, 0);
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
        setDeltaMovement(0, 0, 0); // 自己用 setPos 移動，不靠 vanilla 速度
    }

    /**
     * 船撞地形：逐軸檢查移動後有沒有船方塊會卡進世界固體方塊，會的話該軸歸零（沿牆滑）。
     * 船方塊不在世界裡（在 entity），所以 noCollision 只會撞到真實地形，船自己不互撞。
     */
    private Vec3 resolveTerrain(Vec3 move) {
        if (contraption == null) return move;
        double mx = move.x != 0 && blockedBy(move.x, 0, 0) ? 0 : move.x;
        double my = move.y != 0 && blockedBy(0, move.y, 0) ? 0 : move.y;
        double mz = move.z != 0 && blockedBy(0, 0, move.z) ? 0 : move.z;
        return new Vec3(mx, my, mz);
    }

    private boolean blockedBy(double dx, double dy, double dz) {
        for (BlockPos local : contraption.getBlocks().keySet()) {
            double x0 = getX() + local.getX() + dx, y0 = getY() + local.getY() + dy, z0 = getZ() + local.getZ() + dz;
            AABB box = new AABB(x0, y0, z0, x0 + 1, y0 + 1, z0 + 1).deflate(0.06);
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
        }
        return false;
    }

    /** 發射台鷹架（底座/組裝架/組裝台/核心/座椅）不算地形，不擋船移動。 */
    private static boolean isShipScaffolding(BlockState s) {
        return s.getBlock() instanceof ShipAssemblyBaseBlock
                || s.getBlock() instanceof ShipAssemblyGantryBlock
                || s.getBlock() instanceof ShipAssemblyPadBlock
                || s.getBlock() instanceof ShipCoreBlock
                || s.getBlock() instanceof ShipSeatBlock;
    }

    // hitbox：把實體尺寸撐到涵蓋整艘船（getBoundingBox 是 final 不能覆寫，改由 dimensions 決定），
    // 這樣對船身任何地方右鍵都點得到。EntityDimensions 是以 x/z 為中心、腳底往上的對稱盒，
    // 核心在角落故取四向最大延伸當半寬（會略大於船，picking 可接受）；核心下方的方塊邊角情況不涵蓋。
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (contraption == null) return super.getDimensions(pose);
        AABB b = contraption.bounds();
        double halfW = Math.max(Math.max(Math.abs(b.minX), Math.abs(b.maxX)),
                Math.max(Math.abs(b.minZ), Math.abs(b.maxZ)));
        float w = (float) Math.max(halfW * 2, 1.0);
        float h = (float) Math.max(b.maxY, 1.0);
        return EntityDimensions.scalable(w, h);
    }

    // ── 騎乘 ────────────────────────────────────────────────────────────────

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.sidedSuccess(true);
        // 右鍵 = 上船（坐進下一個空位，第一個上船=駕駛）。拆解改走組裝台 GUI 的拆解按鈕
        if (getPassengers().size() < getSeats().size()) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(false);
    }

    // 預設 Entity.isPickable() 回 false → 點不到實體、interact 不觸發。飛船要可被右鍵
    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    /** 收船：把 contraption 方塊寫回世界（snap 到整數格）並 discard 自己。被擋則不動，回傳 false。 */
    public boolean disassemble() {
        if (level().isClientSide || contraption == null) return false;
        BlockPos target = new BlockPos(
                Mth.floor(getX() + 0.5), Mth.floor(getY() + 0.5), Mth.floor(getZ() + 0.5));
        if (!contraption.addToWorld(level(), target)) return false;
        discard();
        return true;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        // 駕駛 = 坐核心(座位 index 0)那人，也就是第一個上船的 passenger
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < seatCount();
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        List<BlockPos> seats = getSeats();
        int idx = getPassengers().indexOf(passenger);
        if (idx < 0 || idx >= seats.size()) idx = 0;
        BlockPos seat = seats.get(idx);
        callback.accept(passenger, getX() + seat.getX() + 0.5, getY() + seat.getY() + 1.0,
                getZ() + seat.getZ() + 0.5);
    }

    /** 座位清單（local 座標，確定性順序）：核心(0,0,0) 在 index 0=駕駛，其餘為座椅方塊依座標排序。 */
    public List<BlockPos> getSeats() {
        List<BlockPos> seats = new ArrayList<>();
        seats.add(BlockPos.ZERO); // 核心 = 駕駛位
        if (contraption != null) {
            contraption.getBlocks().entrySet().stream()
                    .filter(e -> e.getValue().state().getBlock() instanceof ShipSeatBlock)
                    .map(Map.Entry::getKey)
                    .sorted(Comparator.<BlockPos>comparingInt(BlockPos::getX)
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
        return contraption.bounds().move(getX(), getY(), getZ()).inflate(1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // M2a 無需同步欄位；contraption 走 complex spawn
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Ship")) {
            ShipContraption c = new ShipContraption();
            c.readNbt(level(), tag.getCompound("Ship"));
            this.contraption = c;
            refreshDimensions();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (contraption != null) {
            tag.put("Ship", contraption.writeNbt(level().registryAccess()));
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        boolean has = contraption != null;
        buf.writeBoolean(has);
        if (has) buf.writeNbt(contraption.writeNbt(level().registryAccess()));
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            CompoundTag tag = buf.readNbt();
            if (tag != null) {
                ShipContraption c = new ShipContraption();
                c.readNbt(level(), tag);
                this.contraption = c;
                refreshDimensions();
            }
        }
    }
}
