package com.github.nalamodikk.particle;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.particle.control.ControlParticleManager;
import com.github.nalamodikk.particle.control.ParticleControler;
import com.github.nalamodikk.particle.utils.RelativeLocation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ?舀?嗥?摮憿? */
public class ControlableParticle extends TextureSheetParticle implements ICooParticle {

    private final UUID particleId;
    private boolean faceToCamera = true;
    private final Quaternionf currentRotation = new Quaternionf();
    private final Quaternionf prevRotation = new Quaternionf();

    private Vec3 prevPosition;
    private Vec3 currentPosition;

    private final List<Consumer<ControlableParticle>> preTickActions = new ArrayList<>();
    private final List<Consumer<ControlableParticle>> postTickActions = new ArrayList<>();

    public ControlableParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, UUID uuid) {
        super(level, x, y, z, vx, vy, vz);  // 傳遞速度給父類
        this.particleId = uuid;
        this.prevPosition = new Vec3(x, y, z);
        this.currentPosition = new Vec3(x, y, z);
        this.lifetime = 100;

        ParticleControler controler = ControlParticleManager.getControl(uuid);
        if (controler != null) {
            controler.loadParticle(this);
            controler.particleInit();
        }
    }

    @Override
    public void tick() {
        for (Consumer<ControlableParticle> action : preTickActions) {
            action.accept(this);
        }

        this.prevPosition = this.currentPosition;
        this.prevRotation.set(this.currentRotation);

        ParticleManager.getInstance().executeCommands(particleId, this);

        // 參考框架的做法：默認不調用 super.tick()，完全由控制器控制
        // 只有需要原版物理行為時才手動啟用
        // super.tick();  // 註解掉，粒子不再有自動物理行為

        // 手動更新 prevPos 用於渲染插值
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.currentPosition = new Vec3(this.x, this.y, this.z);

        // 手動增加 age，當達到 lifetime 時移除粒子
        if (this.age++ >= this.lifetime) {
            this.remove();
        }

        for (Consumer<ControlableParticle> action : postTickActions) {
            action.accept(this);
        }
    }

    @Override
    public void remove() {
        ControlParticleManager.removeControl(particleId);
        ParticleManager.getInstance().unregisterParticle(particleId);
        super.remove();
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
        if (faceToCamera) {
            // 使用 TextureSheetParticle 的內建渲染方法（已測試和優化）
            super.render(vertexConsumer, camera, partialTick);
        } else {
            // 對於自由旋轉，使用 renderRotatedQuad
            Vec3 cameraPos = camera.getPosition();
            float offsetX = (float)(Mth.lerp(partialTick, this.xo, this.x) - cameraPos.x);
            float offsetY = (float)(Mth.lerp(partialTick, this.yo, this.y) - cameraPos.y);
            float offsetZ = (float)(Mth.lerp(partialTick, this.zo, this.z) - cameraPos.z);

            Quaternionf renderRotation = new Quaternionf();
            prevRotation.slerp(currentRotation, partialTick, renderRotation);

            // 使用 TextureSheetParticle 提供的 renderRotatedQuad 方法
            this.renderRotatedQuad(vertexConsumer, renderRotation, offsetX, offsetY, offsetZ, partialTick);
        }
    }


    @Override
    public ParticleRenderType getRenderType() {
        // 臨時使用原版 RenderType 進行測試，確認粒子可見後再切換回 ADDITIVE_BLEND
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        // TODO: 測試通過後改回 CooParticleRenderTypes.ADDITIVE_BLEND
    }

    @Override public void setPosition(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    @Override public void setVelocity(double vx, double vy, double vz) { this.xd = vx; this.yd = vy; this.zd = vz; }
    @Override public void setColor(float r, float g, float b) { this.rCol = r; this.gCol = g; this.bCol = b; }
    @Override public void setAlpha(float alpha) { this.alpha = alpha; }

    // Getter 方法（用於 Helper 類）
    public float getAlpha() { return this.alpha; }
    public int getRed() { return (int) (this.rCol * 255); }
    public int getGreen() { return (int) (this.gCol * 255); }
    public int getBlue() { return (int) (this.bCol * 255); }
    public Vec3 getPosition() { return new Vec3(this.x, this.y, this.z); }
    public Vec3 getVelocity() { return new Vec3(this.xd, this.yd, this.zd); }

    // 設置速度（Vec3 版本）
    public void setVelocity(Vec3 velocity) {
        this.xd = velocity.x;
        this.yd = velocity.y;
        this.zd = velocity.z;
    }

    // 旋轉控制方法（目前使用 Quaternion，暫時返回 0）
    public float getRoll() { return 0.0f; } // TODO: 從 Quaternion 提取
    public float getPitch() { return 0.0f; } // TODO: 從 Quaternion 提取
    public float getYaw() { return 0.0f; } // TODO: 從 Quaternion 提取
    public void setRoll(float roll) { /* TODO: 設置到 Quaternion */ }
    public void setPitch(float pitch) { /* TODO: 設置到 Quaternion */ }
    public void setYaw(float yaw) { /* TODO: 設置到 Quaternion */ }
    @Override public void setScale(float scale) { this.quadSize = scale; }
    public float getScale() { return this.quadSize; }
    @Override public void setRotation(Quaternionf rotation) { this.currentRotation.set(rotation); }
    @Override public void setFaceToCamera(boolean faceToCamera) { this.faceToCamera = faceToCamera; }

    // 生命週期相關 getter
    public int getAge() { return this.age; }
    public int getLifetime() { return this.lifetime; }

    public void addPreTickAction(Consumer<ControlableParticle> action) { this.preTickActions.add(action); }
    public void addPostTickAction(Consumer<ControlableParticle> action) { this.postTickActions.add(action); }

    @Override public UUID getParticleId() { return particleId; }
    public void teleportTo(Vec3 pos) { this.x = pos.x; this.y = pos.y; this.z = pos.z; }
    public void teleportTo(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    
    public boolean isRemoved() { return this.removed; }
}
