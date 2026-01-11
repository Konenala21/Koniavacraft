package com.github.nalamodikk.particle;

import com.github.nalamodikk.KoniavacraftMod;
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

/**
 * 可控制的粒子實體
 *
 * 實現了代理控制模式，可以在生成後動態改變行為
 */
public class ControlableParticle extends TextureSheetParticle {

    // 粒子唯一標識
    private final UUID particleId;

    // 渲染控制
    private boolean faceToCamera = true;
    private Quaternionf rotation = new Quaternionf();

    // 位置插值（用於平滑渲染）
    private Vec3 prevPosition;
    private Vec3 currentPosition;

    // 旋轉插值
    private Quaternionf prevRotation = new Quaternionf();
    private Quaternionf currentRotation = new Quaternionf();

    // 顏色控制
    private float red = 1.0f;
    private float green = 1.0f;
    private float blue = 1.0f;

    public ControlableParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);

        this.particleId = UUID.randomUUID();
        this.prevPosition = new Vec3(x, y, z);
        this.currentPosition = new Vec3(x, y, z);

        // 預設生命週期
        this.lifetime = 100;

        // 註冊到管理器
        ParticleManager.getInstance().registerParticle(particleId, this);

        KoniavacraftMod.LOGGER.debug("✅ 創建粒子: {} 位置: {}, {}, {}", particleId, x, y, z);
    }

    @Override
    public void tick() {
        // 儲存上一幀狀態
        this.prevPosition = this.currentPosition;
        this.prevRotation.set(this.currentRotation);

        // 執行控制器的指令
        ParticleManager.getInstance().executeCommands(particleId, this);

        // 更新當前位置
        this.currentPosition = new Vec3(this.x, this.y, this.z);

        // 調用原版更新邏輯
        super.tick();
    }

    @Override
    public void remove() {
        // 從管理器移除
        ParticleManager.getInstance().unregisterParticle(particleId);
        super.remove();
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
        // 計算插值位置
        Vec3 renderPos = new Vec3(
            Mth.lerp(partialTick, prevPosition.x, currentPosition.x),
            Mth.lerp(partialTick, prevPosition.y, currentPosition.y),
            Mth.lerp(partialTick, prevPosition.z, currentPosition.z)
        );

        // 計算插值旋轉
        Quaternionf renderRotation = new Quaternionf();
        prevRotation.slerp(currentRotation, partialTick, renderRotation);

        // 計算相機偏移
        Vec3 cameraPos = camera.getPosition();
        float offsetX = (float)(renderPos.x - cameraPos.x);
        float offsetY = (float)(renderPos.y - cameraPos.y);
        float offsetZ = (float)(renderPos.z - cameraPos.z);

        // 構建頂點
        if (faceToCamera) {
            renderBillboard(vertexConsumer, camera, offsetX, offsetY, offsetZ, partialTick);
        } else {
            renderRotated(vertexConsumer, offsetX, offsetY, offsetZ, renderRotation, partialTick);
        }
    }

    /**
     * 渲染面向相機的粒子（Billboard）
     */
    private void renderBillboard(VertexConsumer consumer, Camera camera,
                                 float x, float y, float z, float partialTick) {
        // 獲取相機旋轉
        Quaternionf cameraRotation = camera.rotation();

        // 計算四個角的位置
        Vector3f[] corners = new Vector3f[] {
            new Vector3f(-1, -1, 0),
            new Vector3f(-1,  1, 0),
            new Vector3f( 1,  1, 0),
            new Vector3f( 1, -1, 0)
        };

        float scale = this.getQuadSize(partialTick);

        for (Vector3f corner : corners) {
            // 應用縮放
            corner.mul(scale);
            // 應用相機旋轉
            cameraRotation.transform(corner);
            // 移動到世界位置
            corner.add(x, y, z);
        }

        // 發射頂點
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        int light = this.getLightColor(partialTick);

        consumer.addVertex(corners[0].x(), corners[0].y(), corners[0].z())
            .setUv(u1, v1)
            .setColor(red, green, blue, alpha)
            .setLight(light);

        consumer.addVertex(corners[1].x(), corners[1].y(), corners[1].z())
            .setUv(u1, v0)
            .setColor(red, green, blue, alpha)
            .setLight(light);

        consumer.addVertex(corners[2].x(), corners[2].y(), corners[2].z())
            .setUv(u0, v0)
            .setColor(red, green, blue, alpha)
            .setLight(light);

        consumer.addVertex(corners[3].x(), corners[3].y(), corners[3].z())
            .setUv(u0, v1)
            .setColor(red, green, blue, alpha)
            .setLight(light);
    }

    /**
     * 渲染自由旋轉的粒子
     */
    private void renderRotated(VertexConsumer consumer, float x, float y, float z,
                               Quaternionf rotation, float partialTick) {
        // 計算四個角的位置
        Vector3f[] corners = new Vector3f[] {
            new Vector3f(-1, -1, 0),
            new Vector3f(-1,  1, 0),
            new Vector3f( 1,  1, 0),
            new Vector3f( 1, -1, 0)
        };

        float scale = this.getQuadSize(partialTick);

        for (Vector3f corner : corners) {
            // 應用縮放
            corner.mul(scale);
            // 應用自定義旋轉
            rotation.transform(corner);
            // 移動到世界位置
            corner.add(x, y, z);
        }

        // 發射頂點（同上）
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        int light = this.getLightColor(partialTick);

        consumer.addVertex(corners[0].x(), corners[0].y(), corners[0].z())
            .setUv(u1, v1)
            .setColor(red, green, blue, alpha)
            .setLight(light);

        consumer.addVertex(corners[1].x(), corners[1].y(), corners[1].z())
            .setUv(u1, v0)
            .setColor(red, green, blue, alpha)
            .setLight(light);

        consumer.addVertex(corners[2].x(), corners[2].y(), corners[2].z())
            .setUv(u0, v0)
            .setColor(red, green, blue, alpha)
            .setLight(light);

        consumer.addVertex(corners[3].x(), corners[3].y(), corners[3].z())
            .setUv(u0, v1)
            .setColor(red, green, blue, alpha)
            .setLight(light);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return CooParticleRenderTypes.ADDITIVE_BLEND;
    }

    // ========== 控制方法（由 Controller 通過指令調用） ==========

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setVelocity(double vx, double vy, double vz) {
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
    }

    public void setColor(float r, float g, float b) {
        this.red = r;
        this.green = g;
        this.blue = b;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void setScale(float scale) {
        this.quadSize = scale;
    }

    public void setRotation(Quaternionf rotation) {
        this.currentRotation.set(rotation);
    }

    public void setFaceToCamera(boolean faceToCamera) {
        this.faceToCamera = faceToCamera;
    }

    public UUID getParticleId() {
        return particleId;
    }
}
