package com.github.nalamodikk.space.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.Rotation;
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
    private static final float YAW_LERP = 0.15f;   // 船頭轉向駕駛視角的平滑度
    public static final int MAX_DRIVERS = 2;        // 駕駛位數量（離核心最近的 N 張椅子）。其餘椅子=乘客

    @Nullable private ShipContraption contraption;

    // 駕駛輸入（由 ShipInputPacket 每 tick 設；server 端用）。每個駕駛位一份，tick 時合併。
    private final float[] inForward = new float[MAX_DRIVERS];
    private final float[] inStrafe = new float[MAX_DRIVERS];
    private final int[] inVertical = new int[MAX_DRIVERS];
    private final float[] inYaw = new float[MAX_DRIVERS];
    private Vec3 shipVel = Vec3.ZERO;

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
    public void setControlInput(int seatIndex, float forward, float strafe, int vertical, float yaw) {
        if (seatIndex < 0 || seatIndex >= MAX_DRIVERS) return;
        this.inForward[seatIndex] = forward;
        this.inStrafe[seatIndex] = strafe;
        this.inVertical[seatIndex] = vertical;
        this.inYaw[seatIndex] = yaw;
    }

    /** 玩家在第幾個座位（=上船順序），對應 getSeats 的 index。非乘客回 -1。 */
    public int seatIndexOf(Entity passenger) {
        return getPassengers().indexOf(passenger);
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
        // 記住上一 tick 的位置與角度，渲染對位置/yaw 的 partialTick 插值才不會跟碰撞分家（轉向歪）。
        this.setOldPosAndRot();

        if (level().isClientSide) {
            tickLerp(); // server 權威：client 只平滑跟隨 server 廣播的位置，不自己算移動
        } else {
            tickServerMovement(); // server 是唯一真相，依駕駛輸入算移動
        }
        setDeltaMovement(0, 0, 0); // 自己用 setPos 移動，不靠 vanilla 速度
    }

    /**
     * 船頭在 local 座標的 yaw = 駕駛椅（座位0）的朝向（你坐上去面對的方向）。
     * 座椅模型坐姿 = FACING 的反向，故 sit-dir = FACING.getOpposite()。沒座椅（核心駕駛）時退回北(yaw180)。
     */
    private float bowLocalYaw() {
        List<BlockPos> seats = getSeats();
        if (contraption != null && !seats.isEmpty()) {
            var info = contraption.getBlocks().get(seats.get(0));
            if (info != null && info.state().getBlock() instanceof ShipSeatBlock
                    && info.state().hasProperty(ShipSeatBlock.FACING)) {
                Direction sitDir = info.state().getValue(ShipSeatBlock.FACING).getOpposite();
                return sitDir.toYRot();
            }
        }
        return 180f; // 無座椅後備：船頭=北（理論上沒座椅不會有駕駛，這只是防呆）
    }

    /** server 端：合併各駕駛位輸入算移動並 setPos/setYRot。位置經 entity tracking 自動廣播給所有 client。 */
    private void tickServerMovement() {
        int occupiedDrivers = Math.min(MAX_DRIVERS, getPassengers().size());
        // 空著的駕駛位輸入清零（駕駛離座後殘留輸入不該繼續推船）
        for (int i = occupiedDrivers; i < MAX_DRIVERS; i++) {
            inForward[i] = 0; inStrafe[i] = 0; inVertical[i] = 0;
        }
        // 油門合併：兩個駕駛位的前後/左右/升降相加後夾到 [-1,1]
        float f = 0, s = 0; int v = 0;
        for (int i = 0; i < occupiedDrivers; i++) { f += inForward[i]; s += inStrafe[i]; v += inVertical[i]; }
        f = Mth.clamp(f, -1f, 1f); s = Mth.clamp(s, -1f, 1f); v = Mth.clamp(v, -1, 1);

        // 船頭 = 駕駛椅朝向（你坐上去面對的方向），不再寫死 local -Z，所以可朝任意方位建造。
        float bowLocal = bowLocalYaw(); // 駕駛椅朝向在 local 座標的 yaw
        if (occupiedDrivers > 0) {
            // 主駕駛(座位0)視角定船頭：要讓 bow(local yaw=bowLocal)轉到指向視角，故 shipYaw=look-bowLocal。
            // 不改組裝（靜止 yaw 仍 0=照建造樣子，因為 bow 也在 local 量）。
            setYRot(Mth.rotLerp(YAW_LERP, getYRot(), inYaw[0] - bowLocal));
        }
        // 移動相對「可見船頭」：bow 的世界 yaw = bowLocal + shipYaw。前進朝船頭、右手為其右側。
        double bowRad = Math.toRadians(bowLocal + getYRot());
        Vec3 forwardDir = new Vec3(-Math.sin(bowRad), 0, Math.cos(bowRad));
        Vec3 rightDir = new Vec3(-Math.cos(bowRad), 0, -Math.sin(bowRad));
        Vec3 target = forwardDir.scale(f).add(rightDir.scale(s)).add(0, v, 0);
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

    /** 任意 local 點（可含 +0.5 之類的偏移）轉世界座標：偏移會一起繞船中心旋轉，旋轉後才不錯位。 */
    public Vec3 rotatedWorldPoint(double lx, double ly, double lz) {
        Vec3 c = centerOffset();
        double ox = lx - c.x, oy = ly - c.y, oz = lz - c.z;
        double rad = Math.toRadians(getYRot());
        double cos = Math.cos(rad), sin = Math.sin(rad);
        double rx = ox * cos - oz * sin;
        double rz = ox * sin + oz * cos;
        return new Vec3(getX() + rx, getY() + oy, getZ() + rz);
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

    private boolean blockedBy(double dx, double dy, double dz) {
        for (BlockPos local : contraption.getBlocks().keySet()) {
            // 用旋轉後的世界角落（船會轉），碰撞用軸對齊 1x1 近似（夠用，不做 OBB）
            Vec3 c = rotatedWorldCorner(local.getX(), local.getY(), local.getZ());
            double x0 = c.x + dx, y0 = c.y + dy, z0 = c.z + dz;
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

    /**
     * 只有組裝架(gantry)不算地形：船要能從框架往上升起飛，所以穿過 gantry。
     * 底座/組裝台維持實心（會擋船=玩家預期）。核心/座椅組裝後已從世界移除，不在世界裡，不必列。
     */
    private static boolean isShipScaffolding(BlockState s) {
        return s.getBlock() instanceof ShipAssemblyGantryBlock;
    }

    // hitbox：實體在船中心，所以對稱盒貼合船的實際大小。寬取 footprint 較大邊（方形盒），高=船高。
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (contraption == null) return super.getDimensions(pose);
        AABB b = contraption.bounds();
        float w = (float) Math.max(Math.max(b.maxX - b.minX, b.maxZ - b.minZ), 1.0);
        float h = (float) Math.max(b.maxY - b.minY, 1.0);
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
        // 座位水平中心(+0.5)要在旋轉前併入 local，否則 +0.5 偏移不跟船轉，船一轉玩家就被推離座位
        // （第三人稱看「坐在空氣上」）。Y 不受 yaw 影響，放上方 +1.0 即可。
        Vec3 c = rotatedWorldPoint(seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5);
        callback.accept(passenger, c.x, c.y + 1.0, c.z);
        // 非駕駛乘客的視角跟著船轉（駕駛自己控制視角、船跟著駕駛轉，不強制）
        if (passenger != getControllingPassenger()) {
            float dYaw = getYRot() - yRotO;
            if (dYaw != 0) {
                passenger.setYRot(passenger.getYRot() + dYaw);
                if (passenger instanceof LivingEntity living) {
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
