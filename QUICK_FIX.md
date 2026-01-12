# 粒子看不見 - 快速修復方案

## 根本原因分析

從參考框架對比，你的實現缺少了關鍵的渲染步驟。參考框架使用了 `renderRotatedQuad()`（繼承自 TextureSheetParticle），而你的實現完全自定義了渲染邏輯。

## 修復方案 1：使用 TextureSheetParticle 的內建渲染（推薦）

### 修改 `ControlableParticle.java` 的 render 方法

**當前的問題代碼：**
```java
@Override
public void render(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
    // ... 你的自定義渲染邏輯
    if (faceToCamera) {
        renderBillboard(...);  // 自定義方法
    } else {
        renderRotated(...);     // 自定義方法
    }
}
```

**修復後：**
```java
@Override
public void render(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
    if (faceToCamera) {
        // 使用 TextureSheetParticle 的內建方法（已經過測試和優化）
        super.render(vertexConsumer, camera, partialTick);
    } else {
        // 對於自由旋轉，使用 renderRotatedQuad
        Vec3 cameraPos = camera.getPosition();
        float offsetX = (float)(Mth.lerp(partialTick, this.xo, this.x) - cameraPos.x);
        float offsetY = (float)(Mth.lerp(partialTick, this.yo, this.y) - cameraPos.y);
        float offsetZ = (float)(Mth.lerp(partialTick, this.zo, this.z) - cameraPos.z);

        Quaternionf renderRotation = new Quaternionf();
        prevRotation.slerp(currentRotation, partialTick, renderRotation);

        this.renderRotatedQuad(vertexConsumer, renderRotation, offsetX, offsetY, offsetZ, partialTick);
    }
}
```

**關鍵：`renderRotatedQuad()` 是 TextureSheetParticle 提供的方法，會自動處理：**
- UV 座標
- 頂點顏色
- 光照
- 旋轉變換

## 修復方案 2：確保 Sprite 被正確設置

### 在 `MagicCircleParticle.java` 中

**檢查構造函數：**
```java
public MagicCircleParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, UUID uuid) {
    super(level, x, y, z, uuid);
    this.sprites = sprites;

    // ✅ 關鍵：必須調用這個方法設置 sprite
    this.pickSprite(sprites);  // 或 this.setSpriteFromAge(sprites);

    this.lifetime = 200;
    this.quadSize = 2.0f;  // 增大以便看見
    this.alpha = 1.0f;      // 完全不透明

    this.setFaceToCamera(false);
    this.setRotation(new Quaternionf().rotateX((float) Math.toRadians(90)));
}
```

## 修復方案 3：使用正確的 RenderType

### 臨時測試：使用原版 RenderType

在 `ControlableParticle.java`:
```java
@Override
public ParticleRenderType getRenderType() {
    // 臨時使用原版渲染類型進行測試
    return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    // 或者: return ParticleRenderType.PARTICLE_SHEET_LIT;
}
```

**等確認粒子可見後，再切換回自定義的 ADDITIVE_BLEND**

## 修復方案 4：檢查 CooParticleRenderTypes

你修改過的版本應該包含 `setShader()` 調用。確保：

```java
public static final ParticleRenderType ADDITIVE_BLEND = new ParticleRenderType() {
    @Override
    public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
        // ✅ 關鍵：必須設置 shader
        RenderSystem.setShader(GameRenderer::getParticleShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE
        );
        RenderSystem.depthMask(false);

        return tesselator.begin(
            VertexFormat.Mode.QUADS,
            DefaultVertexFormat.PARTICLE
        );
    }

    @Override
    public String toString() {
        return "KONIAVA_ADDITIVE_BLEND";
    }
};
```

## 立即測試步驟

1. 應用修復方案 1 + 2 + 3
2. 編譯：`./gradlew compileJava`
3. 啟動遊戲
4. 使用測試物品
5. 應該能看見粒子了

如果還是看不見，請檢查：
- 遊戲日誌中的錯誤訊息
- F3 除錯畫面粒子數量是否增加
- 粒子的位置是否正確（不在相機後面或太遠）

## 對比參考框架的關鍵差異

參考框架的 `ControlableParticle.kt` 在 `faceToCamera` 模式下調用：
```kotlin
this.renderRotatedQuad(vertexConsumer, q, lerpPos.x, lerpPos.y, lerpPos.z, tickDelta)
```

而不是完全自定義頂點構建。這是最安全且已測試的方法。
