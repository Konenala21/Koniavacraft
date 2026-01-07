# Track Plan: Nara System 核心引導與 DataComponent 完善

## Phase 1: Nara System 核心機制與介面實作 [checkpoint: 9db8ea0]
*目標：建立完整的引導流程與介面交互*

- [x] **Task 1: 實作 Nara 引導排程器 (Intro Scheduler)** [33b4008]
    - [ ] 撰寫測試驗證 `NaraIntroSchedulerEvent` 的觸發條件
    - [ ] 實作引導訊息的隊列管理與定時顯示邏輯
- [x] **Task 2: 優化 Nara 介面渲染與交互** [c989c9e]
    - [ ] 撰寫測試驗證 `NaraIntroScreen` 的開啟與組件載入
    - [ ] 實作基於 `ModularScreen` 的對話文字逐字顯示特效
- [x] **Task 3: 實作銘印 (Imprint) 基礎邏輯** [d3bdebd]
    - [ ] 撰寫測試驗證 `NaraHelper.bindPlayer` 的數據寫入
    - [ ] 實作玩家與 Nara 系統的初次綁定流程
- [ ] Task: Conductor - User Manual Verification 'Nara System 核心機制與介面實作' (Protocol in workflow.md)

## Phase 2: DataComponent 系統遷移與完善
*目標：將核心數據結構遷移至 1.21.1 DataComponent 標準*

- [x] **Task 1: 定義自定義 DataComponents** [8d7d9f9]
    - [ ] 在 `ModDataComponents` 中註冊機器狀態、RPG 基礎數據等組件
    - [ ] 撰寫測試驗證組件的序列化與反序列化 (Codec)
- [ ] **Task 2: 遷移機器 NBT 至 DataComponents**
    - [ ] 修改 `AbstractManaMachineEntityBlock` 以優先讀取組件數據
    - [ ] 實作 NBT 數據到 DataComponent 的自動遷移邏輯（如有必要）
- [ ] **Task 3: 整合 DataComponent 與 Auto-Sync**
    - [ ] 擴充 `MachineSyncManager` 支援 `@Sync` 標記組件欄位
    - [ ] 撰寫測試驗證組件變動時的客戶端即時同步
- [ ] Task: Conductor - User Manual Verification 'DataComponent 系統遷移與完善' (Protocol in workflow.md)

## Phase 3: 整合測試與拋光
*目標：確保全系統穩定運作並符合風格指南*

- [ ] **Task 1: 進行引導流程全路徑測試**
    - [ ] 模擬新玩家登入，完成完整 Nara 引導
- [ ] **Task 2: 視覺與音效拋光**
    - [ ] 加入魔法粒子特效與對話音效
- [ ] Task: Conductor - User Manual Verification '整合測試與拋光' (Protocol in workflow.md)
