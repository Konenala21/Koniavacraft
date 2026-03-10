# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Bug 修復 (Bug Fixes)

- **魔力發電機** 輸出邏輯移至燃燒判斷外，確保 buffer 殘餘能量/魔力在停止燃燒後仍持續推送（修復無法輸出至 AE2 等外部接受器的問題）
- **基礎科技魔杖** 快速滾動模式切換時顯示跳動：`onMouseScroll` 現在立即同步 client 端 item 狀態，不再等 server 回應

### 效能優化 (Performance)

- `BiomeRegionManager.hasCustomRegions()` 改為 volatile boolean 快取，避免每次 biome query 重複建立 List 物件（大幅降低飛行時的 GC 壓力）
- `SimpleBiomeRegion` 新增 `hasEntries()` 方法（直接檢查 CopyOnWriteArrayList.isEmpty()，不分配新物件）

### 配方 (Recipes)

- 將舊 `arcane_conduit` 合成配方轉移到 `basic_arcane_conduit`，保留舊方塊註冊僅作存檔相容用途
- 新增 `advanced_arcane_conduit`、`elite_arcane_conduit`、`mana_grinder` 的 datagen 配方，導管改為階梯式升級路線

## [0.0.1.5-beta] - 2026-03-06

### 生物群系 & 世界生成 (Biome & World Generation)

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
### Changed
- Added a region-style biome injection architecture (`BiomeRegionManager`) and made the Overworld biome mixin consume the manager instead of direct injector logic.
- Added datapack-driven biome climate overrides/additions under `data/*/worldgen/biome_climates/*.json` via a reload listener.
- Added conflict-safe biome parameter overlay resolution to prevent unstable parameter point collisions.
- Added staged surface rule registration (`namespace + stage + priority`) and switched worldgen rule composition to the new registry.
- Added side-aware machine item handler wrapping so hoppers/pipes obey IO direction and per-slot insert/extract policies.
- Enabled mana grinder item capability exposure and routed mana infuser item capability through the new side-aware wrapper.
- Hardened mana grinder IO config deserialization to recover from legacy/invalid NBT states that could block conduit mana transfer.
- Made mana grinder IO changes invalidate capabilities immediately and switched grinder "working" state to tick-actual progress (no mana now shows idle).
- Added grinder working-state reconciliation so BlockState `working` and GUI sync state self-correct when they diverge.
- Mana grinder IO updates now notify adjacent conduits to force immediate network rescan after IO face changes.
- Changed mana generator default IO map from fully disabled to all-side outputs, and preserved defaults when legacy saves lack `IOMap`.
- Reduced mana crafting table machine recipe cost for `mana_infuser_machine` from 9000 to 3500.
- Fixed conduit idle throttling to use total network-visible mana (not local buffer only), preventing virtual-network transfer starvation to machines like mana grinder.
- Reworked conduit circular-path guard to apply only to short-window conduit loops, auto-reset stale path history, and throttle loop-block debug logs.
- Updated the custom blocks creative tab to auto-group machine blocks first by block type (no manual list maintenance).
- Removed outdated owner multiplier GameTests after machine output decoupled from RPG attributes.
- Moved GameTest classes to `src/test/java` and configured `runGameTestServer` to include `test` source set classes.
- Added persistence GameTests for mana grinder and mana crafting table to verify mana/IO save-load behavior without opening the client.
- Bumped NeoForge from 21.1.217 to 21.1.219.
- Fully renamed `OreGrinder` to `ManaGrinder` across the entire codebase (classes, packages, variables, and assets).
- Corrected `ManaGrinder` diamond recipe to output `Mana Dust` instead of `Glass`.
- Implemented `NaraImprintHelper` and `INaraImprint` logic using the `NARA_IMPRINT` DataComponent.
- Updated JEI integration and GameTests to reflect the `ManaGrinder` rename.
- Removed the whole particle-effects stack (particle core, particle render/shader tools, particle packets/events, debug particle items, and particle assets/shaders).
- Removed unused legacy framework folders: `init`, `display`, `event`, `network`, `commands`, `barrages`, `annotations`, `animation`, and `platform`.
- Removed unused reflection scan utilities under `com.github.nalamodikk.reflect`.
- Removed the whole experimental magic-effect rendering stack (`experimental/effects`, `experimental/render/effects`, effect examples, and `MagicEffectHelper`) for a clean rewrite baseline.
- Removed unused render scaffolding under `com.github.nalamodikk.render` and dead mixins (`render.LevelRendererMixin`, `particle.ParticleEngineAccessor`).
- Removed unused particle JSON configs: `arcane_spark`, `energy_burst`, `explosion_magic`, `healing_magic`, `mana_flow`, `nara_system`, `teleport_magic`.
- Expanded render bounding boxes for mana generator and solar mana collector renderers to cover 1x2x1 visuals.
- Made block-entity animation LOD distances/scales configurable via client config (`koniava-client.toml`).
- Added `mana_grinder` block-entity renderer with idle crystal floating animation and inward counter-rotating crusher wheels.
- Synced solar collector daytime state even when mana is unchanged, so GUI updates after time commands.
- Added solar collector tooltip diagnostics for skylight, rain, thunder, and sky obstruction.
- Solar collector now treats Overworld as having skylight and uses `canSeeSkyFromBelowWater` for sky checks.
- Added solar collector tooltip debug flags for daytime, skylight, sky visibility, and weather.
- Added solar collector tooltip debug flag for overworld detection.
- Adjusted overworld detection and sky checks to use dimension id and skylight level.
- Avoided client-side syncFrom calls that overwrote solar collector generating state.
- Solar collector tooltip debug lines now show only while holding Shift.
- Added a Shift hint line for solar collector tooltip debug details.
- Solar collector tooltip now detects Shift via GLFW and shows the hint even while generating.
- Added a key mapping for solar collector debug details and used the key's translation in the tooltip hint.
- Standardized debug detail key mapping and registered it on the client mod event bus.
- Cached combined ecosystem surface rules to reduce regeneration overhead during worldgen.
- Mana machines now record the placing player as owner in the base block.
- Mana generator, solar collector, mana grinder, and mana infuser now scale output or speed with the owner's RPG intelligence.
- Prevented duplicate payload registration by skipping server-side networking registration on client.
- Registered SyncRPGDataPacket on the server to avoid missing payload types.
- Hid the mana crafting table mana bar background to avoid overlapping the GUI frame.
- Hid the mana infuser mana bar background to avoid overlapping the GUI frame.
- Restricted server-side payload registration to dedicated servers to avoid duplicate registration on integrated client.
- Updated mana grinder GUI size and slot layout to align with the mana infuser layout.
- Added a recipe hint hotspot for the mana grinder and linked it to JEI.
- Updated mana grinder progress drawing to match the mana infuser style and removed the progress background.
- Hid the mana grinder mana bar background to avoid overlapping the GUI frame.
- Fixed JEI grinder recipe rendering by using the client font and translation keys.
- Aligned grinder JEI title with the block translation key and set the JEI background crop to 171x77.
- Updated grinder JEI slot positions to match the new GUI layout and avoided tooltip overlap when JEI is loaded.
- Shifted grinder JEI slot positions by one pixel to match the in-game layout.
- Added a dedicated mana grinder recipe datagen provider and moved grinder recipes there.
- Renamed the ore grinder block to mana grinder in translations.
- Shifted mana grinder GUI and JEI recipe slots two pixels to the right.
- Rendered the grinder JEI mana cost as a bar instead of text.
- Replaced deprecated JEI background override with recipe extras background drawable.
- Mana grinder now uses each recipe's processing time and consumes the recipe mana cost once per craft.
- Rebalanced grinder recipe mana costs based on current mana generation rates and added processing time to the multi-input recipe.
- Rendered grinder JEI time text via recipe extras to avoid hidden text.
- Centered the grinder JEI time label and moved chance text into the output slots with tooltips.
- Ensured grinder JEI mana bar always renders for non-zero costs and moved time into recipe extras.
- Increased mana infuser capacity to exceed its crafting mana cost.
- Assigned a fixed Unbreaking I enchantment to the mana infuser book recipe and reduced its mana cost.
- Solar mana collector now uses correct max mana with slower base output/interval and stackable Mek-style upgrades.
- Added low-end mana generation for coal fuel.
- Increased arcane conduit transfer rates and aligned legacy transfer rate to the basic tier.
- Registered the ore grinder as a JEI recipe catalyst so the usage view shows the machine.
- Upgrade inventory now marks changes when setting items to keep upgrade effects in sync.
- Simplified Solar Mana Collector GameTests to avoid shared weather/time state and expanded flag-logic checks (including night).
- Added a Taiwan-style mana-industrial progression draft and three-tier recipe plan to docs.
- Stabilized the controllable particle system with proper client tick registration and command queue pre-creation.
- Switched controllable particles to additive blending and routed particle logs through the mod logger.
- Fixed client particle tick event bus registration for NeoForge (Bus.GAME).
- Ensured custom particle render types bind the particle shader and mark translucency.

## [0.0.1.5 Preview] - 2026-01-04
### Changed
- Removed RPG command registration and class.
- Prefixed test and UI commands with `koniava`.
- Fixed mana generator GUI mode sync index to reflect actual mode state.
