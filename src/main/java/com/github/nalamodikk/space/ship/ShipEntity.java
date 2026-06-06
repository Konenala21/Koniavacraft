package com.github.nalamodikk.space.ship;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.Nullable;

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

    public void setContraption(ShipContraption contraption) {
        this.contraption = contraption;
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

        // server 端算移動；沒有駕駛者就讓輸入歸零、速度自然衰減（慣性收尾）
        if (!level().isClientSide) {
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
            if (shipVel.lengthSqr() > 0) {
                setPos(getX() + shipVel.x, getY() + shipVel.y, getZ() + shipVel.z);
            }
        }
        setDeltaMovement(0, 0, 0); // 自己用 setPos 移動，不靠 vanilla 速度
    }

    // ── 騎乘 ────────────────────────────────────────────────────────────────

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide && getControllingPassenger() == null) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty(); // 先一人駕駛
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        // 坐在核心方塊上方（entity 落在核心角落）
        callback.accept(passenger, getX() + 0.5, getY() + 1.0, getZ() + 0.5);
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
            }
        }
    }
}
