# 實作計畫 - CooParticlesAPI (高階粒子渲染系統)

## 第一階段：基礎數學與工具鏈 (Foundation & Math Utilities) [checkpoint: a9149ba]
- [x] 任務：建立數學工具類別
    - [x] 建立 `QuaternionUtil` 用於旋轉計算（封裝 JOML 的 Quaternionf）。
    - [x] 建立 `ParticleLerpInterpolator` 用於平滑的位置與旋轉插值（支援 partial tick）。
    - [x] **測試：** 撰寫單元測試驗證四元數旋轉與 SLERP 插值的準確性。 [3e5a16c]
- [x] 任務：建立物理工具
    - [x] 建立 `PhysicsUtil` 用於輕量級的 AABB 碰撞檢測。
    - [x] **測試：** 撰寫單元測試驗證碰撞偵測邏輯。 [3e5a16c]
- [x] 任務：Conductor - 使用者手動驗證 '基礎數學與工具鏈' (遵循 workflow.md 協議)

## 第二階段：控制通訊協議 (Control Protocol)
- [x] 任務：定義指令架構
    - [x] 建立 `IParticleCommand` 函式式介面 (對應 `ControlableParticle` 的消費者)。
    - [x] 實作基礎指令：`SetSpeedCommand` (設定速度)、`RotateToCommand` (旋轉至)、`ColorTransitionCommand` (顏色漸變)。 [babcd4f]
- [~] 任務：實作粒子管理器 (Particle Manager)
    - [ ] 建立 `ParticleManager` 單例，使用 `ConcurrentHashMap<UUID, List<IParticleCommand>>` 儲存指令。
    - [ ] 實作 `cleanup()` 方法以定期清理失效的 UUID。
- [ ] 任務：實作開發者控制器 (Particle Controller)
    - [ ] 建立 `ParticleController` 類別，提供 API 供其他系統呼叫。
    - [ ] 將控制器連結至管理器（將指令推送到對應粒子的隊列中）。
    - [ ] **測試：** 單元測試指令的推送到讀取流程（模擬粒子物件進行驗證）。
- [ ] 任務：Conductor - 使用者手動驗證 '控制通訊協議' (遵循 workflow.md 協議)

## 第三階段：渲染管線注入 (Rendering Pipeline Injection)
- [ ] 任務：定義自定義渲染類型 (Render Types)
    - [ ] 建立 `CooParticleRenderTypes` 類別。
    - [ ] 實作 `ADDITION_BLEND` (加法混合) 渲染類型。
    - [ ] 實作 `TRANSLUCENT_NO_DEPTH` (無視深度的半透明) 渲染類型（視需求）。
- [ ] 任務：Mixin 底層注入
    - [ ] 建立 `ParticleEngineMixin`。
    - [ ] 透過 Mixin 將自定義渲染類型注入到原版的 `RENDER_ORDER` 對照表中。
    - [ ] **驗證：** 確保遊戲啟動不崩潰且渲染類型已正確註冊。
- [ ] 任務：Conductor - 使用者手動驗證 '渲染管線注入' (遵循 workflow.md 協議)

## 第四階段：粒子行為與 3D 渲染實作 (Entity Behavior & 3D Rendering)
- [ ] 任務：實作基礎可控粒子 (ControlableParticle)
    - [ ] 建立抽象類別 `ControlableParticle` 繼承自 `TextureSheetParticle`。
    - [ ] 實作 `tick()` 方法：從管理器讀取指令並執行，更新物理位移。
- [ ] 任務：實作 3D 幾何渲染邏輯
    - [ ] 覆寫 `render()` 方法。
    - [ ] 實作基於四元數旋轉的頂點構建 (支援 `faceToCamera = false` 自由旋轉)。
    - [ ] 應用 `ParticleLerpInterpolator` 確保視覺上的平滑度。
- [ ] 任務：建立具體粒子範例
    - [ ] 實作 `MagicCircleParticle` (魔法陣，驗證 3D 空間旋轉)。
    - [ ] 實作 `GuidedFlowParticle` (引導流，驗證路徑追蹤)。
- [ ] 任務：Conductor - 使用者手動驗證 '粒子行為與 3D 渲染實作' (遵循 workflow.md 協議)

## 第五階段：效能優化與整合驗證 (Optimization & Integration)
- [ ] 任務：實作動態負載平衡
    - [ ] 建立 `PerformanceMonitor` 監控遊戲 FPS。
    - [ ] 在 Mixin 中實作驅逐邏輯 (`cooParticlesAPI$onEvict`)，根據 FPS 動態調整上限。
    - [ ] **測試：** 模擬低 FPS 環境，驗證系統是否會主動回收舊粒子。
- [ ] 任務：最終整合測試
    - [ ] 更新現有的 `DebugParticleItem`，加入生成魔法陣與流動粒子的測試邏輯。
    - [ ] 驗證視覺效果是否正確（混合模式、旋轉角度）。
- [ ] 任務：Conductor - 使用者手動驗證 '效能優化與整合驗證' (遵循 workflow.md 協議)
