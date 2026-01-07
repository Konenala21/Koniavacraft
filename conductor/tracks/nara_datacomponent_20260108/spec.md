# Track Spec: Nara System 核心引導與 DataComponent 完善

## 1. 目標
*   **Nara System 完善:** 實作完整的引導對話流程、銘印 (Imprint) 機制，並確保 `NaraIntroScreen` 能正確引導新玩家。
*   **DataComponent 系統優化:** 解決 `個人開發者小記錄/DataComponent系統待完善事項.md` 中提到的問題，確保機器與物品的狀態儲存符合 1.21.1 的新標準。

## 2. 核心功能
### 2.1 Nara System
*   **引導流程 (Intro Sequence):** 實作 `NaraIntroSchedulerEvent` 的邏輯，管理玩家首次進入遊戲時的對話節奏。
*   **對話介面 (Nara Screen):** 優化 `NaraIntroScreen` 與 `NaraInitScreen` 的視覺表現，確保其符合「重魔法 + 現代」的風格。
*   **銘印機制 (Imprint Mechanism):** 實作 `NaraImprintHelper`，讓玩家能透過特定動作或對話與世界/機器建立連結。

### 2.2 DataComponent 完善
*   **機器數據組件:** 將原本儲存在 NBT 中的複雜狀態轉換為自定義的 `DataComponent`。
*   **同步優化:** 確保 `DataComponent` 能與 `Auto-Sync` 系統協同工作。

## 3. 技術限制
*   **效能:** 引導對話不應造成客戶端 FPS 大幅下降。
*   **相容性:** 數據組件的更改必須考慮舊有存檔的遷移或基本的安全性檢核。
