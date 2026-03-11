# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Player Changes / 玩家更新內容

- EN: Mana Plains now generates as large, continent-scale regions with smooth, natural-looking borders — no longer appears as small scattered patches invading vanilla terrain.
- ZH: 魔力草原現在以大陸規模的平滑區域生成，邊界自然有機，不再以小塊 patch 的方式侵蝕原版地形。

### Developer Notes / 開發者備註

- EN: Added `PlacementMode` enum (`ZOOM_LAYER` / `SMOOTH_NOISE`) to `SimpleBiomeRegion`. Future biomes can choose their spatial placement strategy independently.
- ZH: 在 `SimpleBiomeRegion` 加入 `PlacementMode` enum（`ZOOM_LAYER` / `SMOOTH_NOISE`），未來每個 biome 可獨立選擇空間生成策略。
- EN: Added `RegionNoiseSampler` — replaces Zoom-Layer patch logic for `SMOOTH_NOISE` regions. Uses single-octave `NormalNoise` at low frequency (`firstOctave=-7`, `REGION_SCALE=0.04`) to produce ~3200-block smooth regions. No external dependencies.
- ZH: 新增 `RegionNoiseSampler`，對 `SMOOTH_NOISE` region 取代 Zoom Layer 的格狀邏輯。使用單 octave `NormalNoise` 低頻採樣（`firstOctave=-7`，`REGION_SCALE=0.04`），產生約 3200 格寬的平滑區域，無外部依賴。
- EN: `BiomeRegionManager` now runs both systems in parallel — smooth-noise checked first, zoom-layer as fallback. `initForWorld` splits regions by `PlacementMode` and builds each system only if needed.
- ZH: `BiomeRegionManager` 現在雙系統並行運作——先查 smooth-noise，再查 zoom-layer 作後備。`initForWorld` 依 `PlacementMode` 分組，只建構需要的系統。
- EN: `BiomeTerrainRegistration`: Mana Plains region registered with `PlacementMode.SMOOTH_NOISE`. All existing Zoom-Layer files retained for future use.
- ZH: `BiomeTerrainRegistration`：魔力草原 region 改用 `PlacementMode.SMOOTH_NOISE` 註冊。所有現有 Zoom Layer 檔案完整保留供未來使用。

### 玩家版變更 / Player-facing Changelog

#### 中文

- 修正發布流程缺少 datagen 資源的問題：GitHub Release 已修正，現在補齊 Modrinth / CurseForge 上傳前的 `runData`，避免平台版本仍產出缺少 biome、blockstate 與 item model JSON 的壞包。
- 調整正式版機器與世界生成診斷 log：改為英文並降低部分初始化訊息等級，減少 Windows 啟動器出現中文亂碼。
- 更新 README 底部的開發分支說明，將過時的 `dev/test7` 改為目前使用中的 `dev/1.21.1`。
- **太陽魔力收集器** 基礎產量提升為 20、運作間隔縮短為 60 ticks，效率與速度升級加成同步提高。
- **魔力草原** 生成量與區塊尺寸下調，分布更稀疏，避免過度覆蓋原版地形。
- **魔力發電機** 停止燃燒後仍會持續推送殘餘能量與魔力，外部機器接收更穩定。
- **基礎科技魔杖** 快速滾動切換模式時，畫面顯示不再延遲跳動。
- 導管配方改為階梯式升級路線，並隱藏舊版 `arcane_conduit` 以避免混淆。
- 修正 `mana_plains` datagen 與生物群系標籤輸出，降低 `runData` 失敗風險。

#### English

- Fixed the remaining publishing pipeline issue around missing datagen-generated resources: GitHub Release was already corrected, and now Modrinth and CurseForge uploads also run `runData` first so platform builds stop shipping jars missing biome, blockstate, and item model JSON files.
- Cleaned up production machine and worldgen diagnostics by switching visible runtime logs to English and lowering noisy initialization messages, reducing mojibake in Windows launchers.
- Updated the README branch note at the bottom by replacing the outdated `dev/test7` reference with the current `dev/1.21.1` branch.
- **Solar Mana Collector** now produces 20 mana by default, runs every 60 ticks, and gains stronger speed and efficiency upgrade scaling.
- **Mana Plains** now generate less frequently with smaller patches, reducing biome overtake on vanilla terrain.
- **Mana Generator** keeps pushing buffered energy and mana after fuel stops burning, improving compatibility with external receivers.
- **Basic Tech Wand** no longer shows delayed mode flicker when rapidly switching scroll modes.
- Arcane conduit recipes now follow a tiered upgrade path, and the legacy `arcane_conduit` is hidden to reduce confusion.
- Fixed `mana_plains` datagen and biome-tag output so `runData` is less likely to fail.

### 開發者版變更 / Developer-facing Changelog

#### 數值平衡

- **太陽魔力收集器** 基礎產量 5→20、間隔 200→60 ticks，效率升級加成 10%→25%，速度升級加成 10%→25%（最小間隔 40→20）
- **魔力草原** 覆蓋率 ~17%→~9%（vanillaWeight 10→20）、patch 大小 ~1024→~512 格（zoomCount 4→3）

#### Bug 修復

- 修正 `publish-modrinth`、`publish-curseforge` 仍未先執行 `runData` 的問題；先前只有 GitHub Release 構件正確，平台上傳版本仍可能缺少 `src/generated/resources` 產物。
- 將正式版 `INFO/WARN/ERROR` 的中文 logger 逐步改為英文，並將 renderer / 快取 / worldgen 初始化診斷盡量降到 `DEBUG`，降低 Windows / 啟動器 console 亂碼機率。
- 更新 README 的開發分支註記，避免文件仍指向已過時的 `dev/test7`。
- **魔力發電機** 輸出邏輯移至燃燒判斷外，確保 buffer 殘餘能量/魔力在停止燃燒後仍持續推送（修復無法輸出至 AE2 等外部接受器的問題）
- **基礎科技魔杖** 快速滾動模式切換時顯示跳動：`onMouseScroll` 現在立即同步 client 端 item 狀態，不再等 server 回應

#### 效能優化

- `BiomeRegionManager.hasCustomRegions()` 改為 volatile boolean 快取，避免每次 biome query 重複建立 List 物件（大幅降低飛行時的 GC 壓力）
- `SimpleBiomeRegion` 新增 `hasEntries()` 方法（直接檢查 CopyOnWriteArrayList.isEmpty()，不分配新物件）

#### 配方與世界生成

- 將舊 `arcane_conduit` 合成配方轉移到 `basic_arcane_conduit`，保留舊方塊註冊僅作存檔相容用途
- 新增 `advanced_arcane_conduit`、`elite_arcane_conduit`、`mana_grinder` 的 datagen 配方，導管改為階梯式升級路線
- 從創造模式方塊分頁隱藏舊版 `arcane_conduit`，避免與新導管階級並存造成混淆
- 修正 `runData` 的 `koniava:mana_plains` registry 錯誤：改為由 datagen 直接輸出 biome JSON，並讓 `add_mana_bloom` 使用 biome tag 而非直接綁定自訂 biome key
- `runData` 期間停用 `OverworldBiomeBuilderMixin` 的自訂 biome 注入，避免 datagen registry 建立時出現 `Unreferenced key: koniava:mana_plains`

## [0.0.1.5-beta] - 2026-03-06

### 玩家版變更 / Player-facing Changelog

#### 中文

- 重做自訂生物群系的區域生成邏輯，讓 **魔力草原** 與原版地形混合更自然，邊界更平滑。
- 新增與調整多種地表材質，讓魔力地貌辨識度更高。
- **魔力研磨機** 全面取代舊 `OreGrinder`，同步更新 GUI、JEI 顯示與配方體驗。
- **魔力灌注機**、**魔力發電機**、**魔力合成台** 與 **太陽魔力收集器** 完成一輪平衡與同步修正。
- **魔力導管** 傳輸穩定性與速率提升，並加入更清楚的階級化進程。
- 新增 **魔力研磨機** 方塊實體動畫，並調整多個機器 GUI 與 JEI 版面。
- 擴展部分機器渲染範圍，改善高模型方塊的顯示完整性。
- 新增 **娜拉系統** 的逐字對話與印記資料結構基礎。
- 補齊 JUnit 與 GameTest 基礎設施，強化存檔與機器行為驗證。

#### English

- Reworked custom biome region generation so **Mana Plains** blend into vanilla terrain more naturally with smoother borders.
- Added and adjusted multiple surface materials to make mana-themed terrain easier to recognize in the world.
- **Mana Grinder** fully replaces the old `OreGrinder`, including updated GUI, JEI integration, and recipe flow.
- Rebalanced and fixed synchronization across the **Mana Infuser**, **Mana Generator**, **Mana Crafting Table**, and **Solar Mana Collector**.
- **Mana Conduits** now transfer more reliably, run faster, and provide a clearer tiered progression path.
- Added a dedicated **Mana Grinder** block-entity animation and refreshed several machine GUI and JEI layouts.
- Expanded render bounds for tall machines so their visuals no longer clip as easily.
- Added the foundation for the **Nara System**, including typewriter dialogue and imprint data support.
- Expanded JUnit and GameTest coverage to better validate machine behavior and persistence.

### 開發者版變更 / Developer-facing Changelog

#### 生物群系 & 世界生成 (Biome & World Generation)

**Zoom-Layer 地域索引系統（取代 UniquenessNoise）**
- 新增 `biome/region/noise` 子包：`PixelTransformer`、`AreaContext`（LCG 亂數）、`Area`（StampedLock thread-safe cache）、`AreaFactory`、`AreaTransformer0/1`、`ZoomLayer`（FUZZY/NORMAL）、`InitialRegionLayer`（加權隨機）、`RegionNoiseUtil`（組合 zoom 鏈）
- 刪除 `UniquenessNoise.java`（float threshold 方案），改為整數 regionIndex 路由
- `MultiNoiseBiomeSourceMixin` 改為以 regionIndex 選取各 region 獨立的 `Climate.ParameterList`，自訂 biome 不再與原版 biome 在 6D climate 空間競爭
- `BiomeRegionManager` 新增：`uniquenessIndex` 自動分配、`initForWorld(seed)` 注入世界 seed、`getRegionIndex(x,z)`、`setVanillaWeight`、`setZoomCount`
- `SimpleBiomeRegion` 新增 `uniquenessIndex` 欄位；`BiomeTerrainRegistration` 新增 `LevelEvent.Load` listener 以注入世界 seed
- patch 大小由 `zoomCount` 控制（預設 4 ≈ 1024 格）；FUZZY zoom 確保邊界為有機曲線

**生物群系地形系統重構（前版）**
- 新增 `VanillaClimateBands` enum（對齊原版 temperature / humidity / continentalness / erosion / depth / weirdness 數值邊界）
- 新增 `ParameterPointListBuilder`（笛卡爾積 climate point 建構器）
- 新增 `BiomeRegionManager` + `SimpleBiomeRegion` region 架構，取代直接 inject 邏輯
- 新增 `BiomeClimateConfigLoader`：datapack JSON 動態覆蓋 / 新增生物群系 climate 設定（`data/*/worldgen/biome_climates/*.json`）
- 新增 `BiomeParameterOverlayBuilder`：priority / weight / namespace 衝突解決，確保 parameter point 不重疊
- 新增 staged surface rule registry（namespace + stage + priority）
- 新增 `VanillaBiomeParameterReader.addBiomeSimilar()`：鏡像原版生物群系的 climate slots
- 快取合併後的 ecosystem surface rules，降低 worldgen overhead
- 新增 mana_grass_block / mana_soil / deep_mana_soil 地表材質

---

### 魔力機器系統 (Mana Machine System)

**魔力研磨機 (ManaGrinder)**
- 全面重命名：`OreGrinder` → `ManaGrinder`（類別、包、資源、翻譯）
- 修正 diamond recipe 產出（改為 Mana Dust，非 Glass）
- 修正 IO config 反序列化，兼容舊版/無效 NBT，防止導管傳輸中斷
- IO 變更立即觸發 capability 失效與鄰近導管網路重掃
- 工作狀態改為追蹤實際進度（無魔力 = idle，BlockState `working` 與 GUI 自動對正）
- 每個配方使用獨立加工時間與魔力消耗（craft 完成時一次性扣除）
- 新增 persistence GameTest（驗證 mana/IO save-load 正確性）

**GUI / JEI**
- 更新研磨機 GUI：slot 位置、進度條樣式對齊灌注機
- 新增 JEI 整合：mana cost bar、加工時間顯示、chance tooltip、catalyst 顯示
- 隱藏 mana bar 背景避免遮擋 GUI 框線

**魔力發電機 (ManaGenerator)**
- 修正預設 IO map（從全關改為全側面輸出）
- 讀取舊版存檔缺少 `IOMap` 時自動補上預設值
- 統一 generator capacity config

**魔力灌注機 (ManaInfuser)**
- 增加容量上限以超過最高配方耗魔量
- 降低灌注機 book recipe 魔力消耗，固定賦予 Unbreaking I 附魔

**魔力合成台 (ManaCraftingTable)**
- 降低 `mana_infuser_machine` 配方耗魔量：9000 → 3500
- 新增 persistence GameTest

**太陽能魔力收集器 (SolarManaCollector)**
- 使用正確的 max mana 上限與更慢的基礎輸出/間隔，支援 Mek-style 可疊加升級
- 修正白天狀態同步（即使 mana 未改變也同步，確保時間指令後 GUI 更新）
- 新增 tooltip 診斷：天光、雨、雷、遮蔽、日夜、超界面等 debug flag（按 Shift 顯示）
- Overworld 偵測改用 dimension id + `canSeeSkyFromBelowWater`
- 新增 debug 按鍵綁定

**通用機器邏輯**
- 新增 side-aware item handler wrapping（漏斗/管道遵守 IO 方向與 per-slot 策略）
- 機器記錄放置玩家為 owner；output 依 owner RPG intelligence 縮放
- 升級物品欄變更時標記 dirty 確保升級效果即時同步

---

### 魔力導管 (Mana Conduit)

- 修正 idle throttling 改用全網路可見魔力量（非僅本地 buffer），解決虛擬網路傳輸飢餓
- 重做環形路徑保護：僅對短窗口內導管迴圈生效、自動重置過期路徑記錄、抑制迴圈阻斷 debug log
- 提升導管傳輸速率上限，對齊 basic tier legacy 速率
- 新增導管等級階層

---

### UI & 渲染 (UI & Rendering)

**研磨機動畫**
- 新增 `ManaGrinder` block entity renderer：idle 水晶浮動動畫 + 內縮反向旋轉破碎輪
- 調整風扇葉片動畫為扇形樣式
- LOD 距離/縮放可透過 client config（`koniava-client.toml`）調整
- 渲染工具移至 `common.utils.render` 包

**創意模式標籤**
- 機器方塊依類型自動分組排列（不需手動維護列表）

**雜項**
- 擴展發電機與太陽能收集器渲染包圍盒以涵蓋 1×2×1 視覺高度
- 魔力研磨機 GUI 尺寸與 slot 位置更新

---

### 娜拉系統 (Nara System)

- 新增 `TypewriterTextWidget`：dialog 風格逐字顯示（含打字音效）
- 新增 `NaraImprintHelper` + `INaraImprint` 邏輯，使用 `NARA_IMPRINT` DataComponent
- 實作娜拉引導排程器（訊息佇列 + 時序）
- 整合 DataComponent sync（`MachineSyncManager` + 登入同步）
- 定義並註冊機器與娜拉系統的 custom DataComponents

---

### 測試基礎設施 (Testing)

- 新增 JUnit 5 測試基礎建設
- GameTest 類別移至 `src/test/java`，`runGameTestServer` 納入 test source set
- 新增 persistence GameTest：ManaGrinder、ManaCraftingTable、SolarManaCollector
- 移除因 RPG 屬性解耦後過時的 owner multiplier GameTest
- NeoForge 從 21.1.217 升級至 21.1.219

---

### 清理 & 重構 (Cleanup & Refactor)

- 移除整個粒子特效堆疊（particle core、render/shader 工具、封包/事件、debug items、assets/shaders）
- 移除未使用的舊版框架資料夾：`init`、`display`、`event`、`network`、`commands`、`barrages`、`annotations`、`animation`、`platform`
- 移除反射掃描工具（`com.github.nalamodikk.reflect`）
- 移除實驗性魔法效果渲染堆疊（`experimental/effects`、`experimental/render/effects`、`MagicEffectHelper`）
- 移除未使用的渲染鷹架（`com.github.nalamodikk.render`）及死亡 mixin（`LevelRendererMixin`、`ParticleEngineAccessor`）
- 移除未使用的粒子 JSON：arcane_spark、energy_burst、explosion_magic 等 7 個
- 移除 RPG 指令類別，UI / test 指令加上 `koniava` 前綴

## [0.0.1.5 Preview] - 2026-01-04
### Changed
- Removed RPG command registration and class.
- Prefixed test and UI commands with `koniava`.
- Fixed mana generator GUI mode sync index to reflect actual mode state.
