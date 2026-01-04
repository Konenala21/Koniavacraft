# 實用測試策略 - 混合自動化與手動測試

## 🎯 核心原則

**認清現實**：AI 代理無法操控遊戲 GUI，因此我們需要：
1. **最大化自動化測試**（純邏輯，不依賴遊戲環境）
2. **最小化手動測試**（僅限必須在遊戲內驗證的部分）
3. **建立快速驗證流程**（減少重複啟動遊戲的次數）

---

## 🏗️ 三層測試架構

### **第一層：自動化單元測試（✅ 完全自動化）**

**可以在沒有 Minecraft 環境下測試的純 Java 邏輯**：

#### 1️⃣ **數據同步系統**
```java
MachineSyncManagerTest.java
├─ testIntTracking()          // 測試 int 值追蹤
├─ testBooleanTracking()      // 測試 boolean 值追蹤
├─ testDataSlotCount()        // 測試 ContainerData 槽位數量
└─ testSyncOrderConsistency() // 測試註冊順序一致性
```

#### 2️⃣ **狀態管理邏輯**
```java
ManaGeneratorStateManagerTest.java
├─ testModeToggle()           // 測試模式切換邏輯
├─ testBurnTimeCalculation()  // 測試燃燒時間計算
└─ testModeValidation()       // 測試模式驗證
```

#### 3️⃣ **NBT 序列化**
```java
BlockEntityNBTTest.java
├─ testSaveAndLoad()          // 測試保存與讀取
├─ testIOConfigPersistence()  // 測試 IO 配置持久化
└─ testRecipeStatePersistence() // 測試配方狀態保存
```

#### 4️⃣ **數值計算**
```java
ManaCalculationTest.java
├─ testManaToEnergyConversion()
├─ testInfusionCostCalculation()
└─ testProgressPercentage()
```

---

### **第二層：模擬測試（⚡ 部分自動化）**

**使用 Mock 對象模擬 Minecraft 環境**：

#### 1️⃣ **Menu 同步測試**
```java
ManaInfuserMenuTest.java
├─ testClientSideDataAccess()  // Mock ContainerData 測試客戶端讀取
├─ testSyncHelperBinding()     // 測試 SyncHelper 綁定
└─ testProgressSync()           // 測試進度同步
```

#### 2️⃣ **Widget 事件處理**
```java
AbstractWidgetTest.java
├─ testMouseClickCoordinates() // 測試座標轉換邏輯
├─ testParentChildRelation()   // 測試父子關係
└─ testVisibilityHandling()    // 測試可見性處理
```

---

### **第三層：手動遊戲測試（👤 必須手動）**

**無法避免，但可以優化**：

#### ✋ **必須手動測試的項目**
1. **GUI 渲染正確性**
   - 元件位置是否正確
   - 材質顯示是否正常
   - 工具提示是否出現

2. **視覺反饋**
   - 按鈕按下動畫
   - 進度條動畫
   - 粒子效果

3. **多人模式同步**
   - 客戶端與伺服器數據一致性
   - 多玩家同時操作同一機器

#### 📋 **快速驗證清單**（每次修改後使用）

```
□ 啟動遊戲（單人創造模式）
□ 放置機器方塊
□ 右鍵打開 GUI
  ├─ □ 標題顯示正確
  ├─ □ 魔力條顯示正確
  ├─ □ 進度條位置正確
  └─ □ 按鈕可點擊
□ 測試功能
  ├─ □ 模式切換按鈕生效
  ├─ □ 物品輸入輸出正常
  └─ □ 配方處理正確
□ 關閉遊戲
```

**預期時間**：3-5 分鐘（相比之前 10+ 分鐘大幅減少）

---

## 📝 實際可行的實現計劃

### **階段 1：簡單開始（1 小時）**
✅ **不需要任何測試框架即可開始**

1. **創建測試目錄**
   ```bash
   mkdir -p src/test/java/com/github/nalamodikk/common/sync
   ```

2. **手寫第一個簡單測試**
   - 創建 `MachineSyncManagerTest.java`
   - 使用最基本的 `assert` 語句（不依賴 JUnit）
   - 用 `main` 方法運行測試

3. **建立測試日誌**
   - 在項目根目錄創建 `TEST_LOG.md`
   - 記錄每次手動測試結果
   - 比對自動測試與手動測試的差異

### **階段 2：逐步優化（根據需求）**

**只在真正需要時才添加**：

#### 選項 A：最小化依賴（推薦）
```gradle
// build.gradle - 只添加最基本的測試支持
dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
}
```

#### 選項 B：完整測試套件（可選）
```gradle
// 如果需要 Mock 功能才添加
dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
    testImplementation 'org.mockito:mockito-core:5.7.0'
}
```

### **階段 3：建立測試習慣（持續）**

1. **每次修改邏輯時**
   - 先寫一個簡單測試（5 分鐘）
   - 確認邏輯正確後才啟動遊戲

2. **快速驗證流程**
   ```bash
   # 1. 運行自動測試（快速）
   ./gradlew test --tests "MachineSyncManagerTest"

   # 2. 如果測試通過，編譯遊戲（較慢）
   ./gradlew build

   # 3. 啟動遊戲進行最終驗證（最慢）
   ./gradlew runClient
   ```

---

## 🚀 實用命令

### **快速測試單個類**
```bash
./gradlew test --tests "MachineSyncManagerTest"
```

### **測試失敗時顯示詳細信息**
```bash
./gradlew test --info
```

### **跳過測試直接構建（緊急時使用）**
```bash
./gradlew build -x test
```

### **清理並重新測試**
```bash
./gradlew clean test
```

---

## 🎯 實際測試覆蓋率目標

| 模塊 | 優先級 | 目標覆蓋率 | 原因 |
|------|--------|-----------|------|
| MachineSyncManager | 🔴 最高 | 90%+ | 核心系統，Bug 影響所有機器 |
| SyncHelper 類別 | 🟡 中等 | 70%+ | 簡單邏輯，主要測試索引正確性 |
| Widget 座標轉換 | 🟡 中等 | 60%+ | 已修復過，建立回歸測試 |
| BlockEntity Tick | 🟢 較低 | 40%+ | 複雜邏輯，大部分需遊戲測試 |
| GUI 渲染 | ⚪ 不測試 | 0% | 必須手動測試 |

---

## 🔧 當前狀態

### ✅ **已完成**
- [x] 設計測試策略
- [x] 明確自動化與手動測試邊界
- [x] 建立快速驗證清單

### ⏳ **下一步（按需求執行）**
- [ ] 創建第一個測試類 `MachineSyncManagerTest`
- [ ] 添加 JUnit 依賴到 `build.gradle`
- [ ] 測試現有的座標轉換邏輯
- [ ] 建立測試日誌 `TEST_LOG.md`

### 💡 **建議工作流**
1. 先完成當前功能開發
2. 對核心邏輯添加簡單測試
3. 用遊戲驗證 GUI
4. 提交代碼前運行一次測試

**不要過度工程化** - 測試是為了節省時間，不是增加負擔！
