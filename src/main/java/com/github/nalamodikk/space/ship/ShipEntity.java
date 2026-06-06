package com.github.nalamodikk.space.ship;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
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

    @Nullable private ShipContraption contraption;

    public ShipEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;     // 自己用 contraption 邏輯處理，不走 vanilla 方塊碰撞
        this.setNoGravity(true);   // 飛船不受重力（太空/停在原地）
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
        setDeltaMovement(0, 0, 0); // M3 才加六方向移動
        if (!level().isClientSide && contraption == null) {
            discard(); // 沒有 contraption 的飛船無意義
        }
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
