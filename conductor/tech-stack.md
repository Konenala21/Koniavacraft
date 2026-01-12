# Tech Stack: Koniavacraft

## Core Engine
*   **Game Engine:** NeoForge 1.21.1
*   **Language:** Java 21 (Mojang 1.20.5+ standard)
*   **Build Tool:** Gradle (Plugin: `net.neoforged.moddev` v2.0.78)

## Primary Compatibility
*   **JEI (Just Enough Items):** 用於配方查詢與 GUI 整合。

## In-house Frameworks & Systems
*   **Auto-Sync System:** 自研反射與註解驅動的數據同步框架，簡化 BlockEntity 與 ContainerMenu 間的通訊。
*   **ModularScreen System:** 基於 Widget 與 FlexLayout 的模組化 UI 框架。
*   **CooParticlesAPI (Refactored):** 全 Java 移植的高階粒子引擎。
    *   **Core:** `PointsBuilder` (幾何構建), `StyleSystem` (樣式控制), `MathPresets` (數學圖形庫).
    *   **Render:** `ShaderManager` (自定義渲染管線), `DynamicVertexBuffer` (批次渲染).
    *   **Logic:** `BarrageSystem` (彈幕碰撞), `PathMotion` (路徑動畫), `NetworkSync` (伺服器同步).
*   **VirtualNetwork:** 高效能魔力/能量傳輸系統，採用虛擬網路運算以減少伺服器負擔。
*   **Nara System:** 敘事引導與世界觀互動核心系統。

## Infrastructure
*   **Data Generation:** 利用 NeoForge DataGen 系統自動生成資源與數據。
*   **Performance Tuning:** 針對開發環境進行 JVM 參數優化，確保順暢開發體驗。
*   **Testing:** 採用 JUnit 5 進行單元測試，配合 NeoForge GameTest 框架進行整合測試。