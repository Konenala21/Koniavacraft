# 粒子調試檢查清單

根據當前代碼狀態，請執行以下測試：

## 1. 編譯測試
```bash
./gradlew compileJava
```
✅ 已完成 - 編譯成功

## 2. 遊戲內測試步驟

### A. 使用原版粒子對比
1. 進入遊戲
2. 使用指令：`/particle minecraft:flame ~ ~1 ~`
3. 確認原版粒子是否可見

### B. 測試自定義粒子
1. 獲取測試物品：`/give @s koniava:debug_particle_item`
2. 普通右鍵方塊 - 應該生成 GuidedFlowParticle
3. 蹲下右鍵方塊 - 應該生成 MagicCircleParticle

## 3. 檢查 F3 除錯資訊
- 按 F3 查看：
  - P: xxxx （粒子數量）
  - 如果粒子數量增加，說明粒子被創建了
  - 如果看不見，可能是渲染問題

## 4. 可能的問題

### 問題 A: 粒子被創建但看不見
**可能原因：**
1. 粒子 alpha 太低（< 0.1）
2. 粒子大小太小（< 0.05）
3. 粒子在相機後面
4. RenderType 設置錯誤
5. 沒有正確設置 sprite

**解決方案：**
- 增大粒子大小到 1.0f
- 設置 alpha 為 1.0f
- 使用原版的 ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT

### 問題 B: 粒子沒有被創建
**可能原因：**
1. ParticleProvider 沒有註冊
2. 粒子 JSON 文件缺失
3. UUID 控制器問題

**檢查方法：**
- 查看日誌中是否有 "✅ 粒子系統註冊完成"
- 檢查 F3 除錯畫面的粒子計數

## 5. 推薦修改

### 臨時測試修改（確認渲染是否工作）

在 `ControlableParticle.java` 中：
```java
@Override
public ParticleRenderType getRenderType() {
    // 臨時使用原版渲染類型測試
    return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
}
```

在 `MagicCircleParticle.java` 構造函數中：
```java
this.quadSize = 2.0f; // 增大到 2.0 更容易看見
this.alpha = 1.0f;    // 完全不透明
```

### 添加調試日誌

在 `MagicCircleParticle.Provider.createParticle()` 中添加：
```java
@Override
public Particle createParticle(...) {
    KoniavacraftMod.LOGGER.info("🔧 Creating MagicCircleParticle at {}, {}, {}", x, y, z);
    MagicCircleParticle particle = new MagicCircleParticle(level, x, y, z, sprites, type.getUuid());
    KoniavacraftMod.LOGGER.info("   Size: {}, Alpha: {}", particle.quadSize, particle.alpha);
    return particle;
}
```

## 6. 快速修復建議

如果粒子仍然看不見，最簡單的修復方法是參考框架中的實現，特別是：

1. **使用 TextureSheetParticle.renderRotatedQuad()** 而不是自定義渲染
2. **確保 sprite 被正確設置**（調用 `pickSprite(sprites)` 或 `setSpriteFromAge(sprites)`）
3. **使用原版的 RenderType** 先確認基本渲染工作

## 當前狀態總結

從 git status 看，你已經修改了很多文件，包括整個粒子系統架構。建議：

1. **先恢復到簡單版本測試**
2. **確認基本渲染工作後再添加複雜功能**
3. **使用原版 RenderType 和 TextureSheetParticle 的方法**
