# Specification: CooParticlesAPI (高階粒子渲染系統)

## 1. 概述 (Overview)
實作一個具備高度可控性、3D 幾何渲染與效能優化的粒子系統 API。該系統旨在超越 Minecraft 原生的廣告牌 (Billboard) 限制，支援代理控制模式 (Proxy Control Pattern)，並能應用於魔法陣、導向魔力流及複雜的物理特效。

## 2. 功能需求 (Functional Requirements)
*   **代理控制模式 (Proxy Control Pattern):**
    *   實作 `ParticleController`，開發者透過 UUID 發送變換指令（旋轉、位移、顏色變更）。
    *   實作 `ControlableParticle`，繼承自 `TextureSheetParticle`，每邏輯幀 (Tick) 消耗指令隊列。
*   **3D 渲染管線 (Rendering Pipeline):**
    *   支援自由旋轉模式 (`faceToCamera = false`)，使用四元數 (Quaternionf) 計算空間變換。
    *   提供手動頂點生成 (Vertex Building)，計算 3D 幾何座標與插值。
    *   支援加法混合渲染類型 (`ADDITION_BLEND`)，模擬發光效果。
*   **動態負載平衡 (Dynamic Balancing):**
    *   監控 FPS 並動態調整粒子生存上限。
    *   實作粒子驅逐機制 (Eviction Strategy)，在資源受限時自動清理舊粒子。
*   **物理與插值:**
    *   實作輕量級物理碰撞檢測 (AABB/Raycast)。
    *   使用 `Partial Tick` 進行渲染位置與旋轉的平滑插值。

## 3. 非功能需求 (Non-Functional Requirements)
*   **效能優化:** 大量粒子（>2000 個）時仍須保持 60 FPS 以上。
*   **相容性:** 透過 Mixin 注入 `ParticleEngine`，確保與原版及其他模組的渲染流程相容。

## 4. 驗收標準 (Acceptance Criteria)
*   [ ] 能夠透過代碼生成一個不受攝影機角度限制的「水平魔法陣」。
*   [ ] 粒子能夠沿著指定的曲線路徑追蹤目標。
*   [ ] 當 FPS 下降至 30 以下時，系統應自動減少不重要的粒子生成或提前驅逐。
*   [ ] 關閉遊戲或離開世界時，所有 Controller 的引用應被正確清理（無記憶體洩漏）。

## 5. 後續階段實作 (Future Implementation)
*   服務端粒子的實體同步（目前僅限客戶端渲染與本地控制）。
*   自定義着色器 (Custom Shaders) 注入。
