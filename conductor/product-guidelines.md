# Product Guidelines: Koniavacraft

## UI/UX Design Principles
*   **混合式美學 (Hybrid Aesthetics):** 介面設計介於 Minecraft 原版風格 (Vanilla) 與現代扁平化 (Modern Flat) 之間。
    *   保留原版的材質感與色調，確保玩家感到親切。
    *   引入現代 UI 配置邏輯（如 FlexLayout），提升複雜機器的操作便利性。
*   **資訊透明化:** 機器狀態、魔力存量與 RPG 屬性加成應直觀呈現，減少玩家查閱手冊的頻率。
*   **流暢交互:** 利用 `ModularScreen` 提供無延遲的介面更新，確保操作手感紮實。

## Engineering Principles (優先順序：C > B > A)
1.  **快速開發 (Rapid Development - 最高優先):** 
    *   優先使用成熟的 `coreapi` 與 `Auto-Sync` 框架。
    *   避免過度設計 (Over-engineering)，以達成功能目標為首要任務。
2.  **效能至上 (Performance Focused):** 
    *   在保證功能實現的前提下，嚴格控制 Tick 邏輯與渲染開銷。
    *   大規模傳輸應優先使用虛擬網路網路，避免對伺服器 TPS 造成負擔。
3.  **模組化與解耦 (Pragmatic Modularity):** 
    *   保持邏輯與表現層的清晰分離（Common/Client）。
    *   在不影響開發速度的情況下，維持系統的可擴充性。

## Localization & Accessibility
*   所有玩家可見的字串必須使用語言檔案 (`.json`)，嚴禁將文字寫死 (Hardcode) 在程式碼中。
*   預設提供繁體中文 (`zh_tw`)，並確保英文 (`en_us`) 的翻譯品質。
