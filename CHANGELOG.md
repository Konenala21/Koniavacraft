# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased] - 2026-05-11

### Player Changes / 玩家更新內容

- EN: Machine GUIs now feature a "LOCKED" overlay with a detailed tooltip if the required research is not yet completed.
- EN: Nara Watch HUD now supports identifying living entities and dropped items, displaying their names when targeted.
- EN: Research state now syncs instantly upon completing a research scroll, immediately unlocking machines without re-opening GUIs.
- ZH: 機器介面現在新增「已鎖定」遮罩；若未完成必要研究，將顯示提示並說明所需解鎖的研究條目。
- ZH: 娜拉手錶 HUD 現在支援識別生物與地面掉落物，對準目標時會即時顯示其名稱。
- ZH: 研究狀態現在會在點擊研究卷軸後即時同步，不需重新開啟介面即可立刻解鎖機器操作。

### Developer Notes / 開發者備註

- EN: Implemented `ResearchLockWidget` using the `ModularScreen` framework for universal GUI research gating.
- EN: Added `KnowledgeSyncPacket` to handle full player research state synchronization during login and completion events.
- EN: Enhanced `BlockSelectorUtils` with entity ray-tracing support (`getTargetEntity`).
- EN: Added `ResearchGameTests` suite for verifying multi-player research isolation and dynamic unlocking logic.
- EN: Refactored networking packet handlers (`ResearchClientPayloadHandler`) to prevent `RuntimeDistCleaner` crashes on dedicated servers.
- EN: Fixed `SolarCollectorSyncHelper` to correctly synchronize upgrade counts and environmental status to the client.
- ZH: 實作 `ResearchLockWidget` 並整合至 `ModularScreen` 框架，達成通用的介面研究鎖定機制。
- ZH: 新增 `KnowledgeSyncPacket` 用於處理玩家登入與完成研究時的完整狀態同步。
- ZH: 強化 `BlockSelectorUtils` 支援實體射線偵測（`getTargetEntity`）。
- ZH: 新增 `ResearchGameTests` 測試套件，驗證多人環境下的研究隔離與動態解鎖邏輯。
- ZH: 重構網路封包處理器（`ResearchClientPayloadHandler`），修復專用伺服器端載入客戶端類別導致的崩潰。
- ZH: 修正 `SolarCollectorSyncHelper` 以正確同步升級模組數量與環境狀態至客戶端。

## [Unreleased] - 2026-05-09

### Player Changes / 玩家更新內容

- EN: The Nara Watch scanner now supports dropped item entities as scan targets, not only blocks.
- EN: Scanning now plays rising tick sounds and emits target particles, making the 0.5-second hold clearer.
- EN: Research nodes now stay locked until the required compound aspects have been discovered by scanning.
- EN: Added more aspect scan mappings for vanilla and Koniava blocks/items.
- ZH: 娜拉手錶掃描器現在可掃描地上的掉落物，不再只支援方塊。
- ZH: 掃描過程加入漸升音效與目標粒子，0.5 秒長按回饋更清楚。
- ZH: 研究節點現在會檢查必要本源是否已透過掃描發現，未發現時維持鎖定。
- ZH: 補充更多原版與 Koniava 方塊/物品的本源掃描對應。

- EN: Completing a research puzzle now gives a Completed Research Scroll. Right-clicking the scroll in the air officially commits the research and unlocks its benefits. First-ever completion triggers a firework celebration.
- EN: Research aspects now show composition info on hover (compound aspects show "A + B" in the palette tooltip).
- EN: Fixed research puzzle — aspects now also connect if they share a common component (e.g. Mana ↔ Resonance via Wu), and same-aspect cells always connect.
- EN: Research node graph (Nara Watch) now supports scroll-to-zoom and drag-to-pan. Click an AVAILABLE node to receive the Research Note.
- EN: Clicking a node in the Nara Watch checks for duplicate notes in inventory and already-completed research before giving a note.
- EN: Solar Mana Collector now correctly stops when any block is placed directly above it.
- EN: Nara system bind now grants the Nara Holographic Watch if the player doesn't already have one.
- EN: Research Table GUI now uses the custom texture drawn by the artist — looks much better than the old placeholder.
- EN: Removing an Arcane Conduit from a network no longer silently destroys stored mana — excess mana is returned to the conduit's own buffer.
- EN: Upgrade GUI now correctly closes when you move too far from the machine (was always staying open before).
- ZH: 研究台介面現在使用自訂貼圖，不再是佔位色塊。
- ZH: 從網路移除弧光導管時，多餘的魔力不再無聲消失，而是退回到被移除導管的 buffer 中。
- ZH: 升級介面現在會在玩家走遠後正確關閉（之前永遠保持開啟）。

### Developer Notes / 開發者備註

- EN: `WatchSyncPacket` now carries discovered aspect IDs and updates `ClientResearchCache` before opening `NaraWatchScreen`.
- EN: `NaraWatchScreen.stateOf()` now checks required aspects through `ClientResearchCache` before marking a node available.
- EN: `NaraWatchItem` now ray-checks nearby `ItemEntity` targets, scans item aspects, and sends target particles from the server.
- EN: `AspectScanner` now has item mappings plus tag fallback for common item categories.
- EN: Added `.gitnexusignore` and ignored `.gitnexus` local index data.
- ZH: `WatchSyncPacket` 現在會攜帶已發現本源 ID，並在開啟 `NaraWatchScreen` 前更新 `ClientResearchCache`。
- ZH: `NaraWatchScreen.stateOf()` 現在會先檢查必要本源是否已發現，再判定節點可研究。
- ZH: `NaraWatchItem` 新增掉落物射線檢查、物品本源掃描，以及伺服器端目標粒子效果。
- ZH: `AspectScanner` 新增物品對應表與常見物品 tag fallback。
- ZH: 新增 `.gitnexusignore`，並忽略 `.gitnexus` 本地索引資料。

- EN: `CompletedResearchItem` added — transforms note on puzzle completion; `use()` applies research to `PlayerKnowledge` + triggers first-research firework (particles + sounds).
- EN: `ResearchCompletePacket` no longer saves to `ResearchSavedData`; responsibility moved to `CompletedResearchItem.use()`.
- EN: `Aspect.canConnectTo()` extended with same-aspect rule and shared-component rule.
- EN: `ResearchRegistry` aspect lists corrected so all required aspects can connect: MANA_FLOW uses WU+WATER+MANA, MANA_GENERATION uses FIRE+ENERGY+MANA, MANA_CRYSTALLISATION uses CRYSTAL+RESONANCE+MANA.
- EN: `SolarManaCollectorBlockEntity.isOpenToSky()` fixed — was checking `above(2)`, now checks `above(1)` so a block directly on top correctly stops generation.
- EN: `NaraWatchScreen` — added left tier-filter tabs and right status-filter tabs protruding into empty screen space outside the 400×252 panel; replaced planned aspect-filter tabs with a search `EditBox` below the panel; `isNodeVisible()` dynamically stacks tier/status/search filters; typing in the search box no longer closes the screen via E key.
- EN: `NaraWatchScreen` added scroll-zoom (pivot at cursor, 0.4x–3.0x) and drag-to-pan.
- EN: `StartResearchPacket` now checks for duplicate research note in player inventory before giving another.
- EN: `NaraBindRequestPacket` gives `NaraWatchItem` on successful bind if player doesn't already have one.
- EN: `ResearchTableScreen` now blits `textures/gui/research_table_gui.png` (176×181) as background; removed all programmatic fill/outline drawing. `imageHeight` updated from 166 to 181.
- EN: `ResearchTableMenu` slot positions updated to align with texture pixel data: NOTE_SLOT (47,33), QUILL_SLOT (116,33); player inventory shifted to x=9+col*18, y=99+row*18; hotbar at y=157.
- EN: Nara Watch UI redesigned — purple area is the interactive node graph (drag/zoom/click); blue area shows research description and lore on selection. Two-click confirmation: first click selects a node and shows its description, second click on the same node receives the note.
- EN: Nara Watch research tree now has side tabs — left tabs filter by tier (All / T1–T4), right tabs filter by status (All / Unlockable / Completed), and a search box below the panel for name searching. All three filters stack.
- EN: Added Nara Holographic Watch item — right-click to open TC4-style research node graph. Click an available node to receive the corresponding Research Note.
- EN: Research puzzle grid is now persisted in `ResearchTableBlockEntity` via NBT — closing and reopening the puzzle screen resumes the same grid state.
- EN: `ResearchAspectPlacePacket` extended to carry `researchId`, `q`, `r`, and optional `aspectId`; server now saves each cell change to the block entity and only damages the quill on placement (not removal).
- ZH: `ResearchTableScreen` 改為 blit `research_table_gui.png`（176×181）作為背景，刪除所有程式碼繪製的色塊與外框。`imageHeight` 從 166 更新為 181。
- ZH: `ResearchTableMenu` 插槽座標依貼圖像素調整：NOTE_SLOT (47,33)、QUILL_SLOT (116,33)；玩家背包移至 x=9+col*18、y=99+row*18；快捷欄改為 y=157。
- ZH: 娜拉手錶研究樹新增側邊標籤 — 左側按等級篩選（全/T1-T4），右側按狀態篩選（全部/可解鎖/已完成），面板下方新增搜尋框可依名稱篩選，三種條件可同時疊加。
- ZH: 新增娜拉全息手錶物品 — 右鍵開啟 TC4 風格研究節點圖，點選可用節點即可獲取對應研究筆記。
- ZH: `NaraWatchScreen` — 新增向外突出至空白螢幕空間的左側等級篩選標籤與右側狀態篩選標籤；將原先規劃的元素篩選標籤改為面板下方搜尋框 `EditBox`；`isNodeVisible()` 動態疊加等級/狀態/搜尋三種篩選；搜尋框有焦點時按 E 不再關閉介面。
- ZH: 研究謎題格子狀態現在透過 NBT 存進 `ResearchTableBlockEntity`，關閉再開啟謎題畫面會還原上次進度。
- ZH: `ResearchAspectPlacePacket` 擴充攜帶 `researchId`、`q`、`r` 及可選的 `aspectId`；伺服器現在將每個格子變更存入 BE，且只有放置時才損耗羽毛筆（移除不損耗）。

- EN: `VirtualNetwork.removeConduit()` now returns excess mana to the leaving conduit's buffer before shrinking capacity, instead of truncating silently.
- EN: `VirtualNetwork.logNetworkInfo()` demoted from `LOGGER.info` to `LOGGER.debug` to stop log spam.
- EN: `NetworkManager.performPassiveCleanup()` bug fixed — was incorrectly evicting all non-conduit endpoints (machines) from cache; now only removes entries where the neighbour *was* a conduit but no longer is.
- EN: `UpgradeMenu.stillValid()` now checks player proximity via `ContainerLevelAccess` instead of always returning `true`.
- EN: `TransferManager` — removed deprecated `LEGACY_TRANSFER_RATE` constant and dead fallback branch in `getTransferRateLimit()`; removed empty `if (currentTick % 10 == 0)` block; removed commented-out debug logger.
- EN: `OutputHandler` — remainder mana/energy after proportional distribution now goes to the hungriest target instead of always `targets[0]`; collapsed duplicate 15-line TODO blocks to 2 lines each.
- EN: `BiomeTerrainRegistration` — removed 4 dead stub methods (`registerVolcanicLands`, `registerCrystalDesert`, `registerFrozenWasteland`, `registerComplexCustomTerrain`) that were never called.
- ZH: `VirtualNetwork.removeConduit()` 現在在縮容前先把多餘魔力退還給離開的導管，不再靜默截斷。
- ZH: `VirtualNetwork.logNetworkInfo()` 從 `info` 降至 `debug`，避免正式環境 log 爆量。
- ZH: `NetworkManager.performPassiveCleanup()` 修正 bug：之前會錯誤地把所有非導管端點（機器連線）從快取中移除；現在只移除「原本是導管但已不再是導管」的方向。
- ZH: `UpgradeMenu.stillValid()` 改為透過 `ContainerLevelAccess` 檢查玩家距離，不再永遠回傳 `true`。
- ZH: `TransferManager` 刪除已棄用的 `LEGACY_TRANSFER_RATE` 常數及其 fallback 分支；刪除空的 `if (tickCounter % 10 == 0)` 區塊；刪除被 comment out 的 debug logger。
- ZH: `OutputHandler` 魔力/能量分配的餘數現在流向需求最大的目標，不再固定給 `targets[0]`；將兩段重複的 15 行 TODO 各縮短為 2 行。
- ZH: `BiomeTerrainRegistration` 刪除 4 個從未被呼叫的 dead stub method。

## [0.0.1.5-beta-hotfix06] - 2026-04-14

### 玩家版變更 / Player-facing Changelog

#### 中文

- 升級模組改為可堆疊安裝，並補上詳細 tooltip 說明，現在可直接查看效果數值、可安裝機器與堆疊規則。
- 提高 **魔力發電機** 的預設能量對外輸出上限至 `1000/t`，讓高容量接收端不再被原本偏低的推送速率卡住。
- **弧光導管** 的共享儲量上限現在會依照連接導管的等級自動加總（BASIC=256、ADVANCED=1024、ELITE=4096），拆除或升級導管時容量同步調整，不再是固定的 10000。

#### English

- Upgrade modules are now stack-installable, with expanded tooltips that explain effect values, supported machines, and stack behavior directly in-game.
- Increased the **Mana Generator** default external energy output cap to `1000/t`, so high-capacity receivers are no longer bottlenecked by the previous low push rate.
- **Arcane Conduit** shared network capacity now scales with connected conduit tiers (BASIC=256, ADVANCED=1024, ELITE=4096). Capacity updates live when conduits are added, removed, or upgraded.

### 開發者版變更 / Developer-facing Changelog

#### 中文

- 升級物品改為可堆疊，`UpgradeInventory#getUpgradeCount` 現在改為計算堆疊總數；`UpgradeSlot` 也會依機器型別限制可安裝的升級種類。
- `OutputHandler` 將魔力與能量輸出上限拆開處理；魔力維持 `40/t`，能量預設對外輸出上限提升為 `1000/t`。
- `VirtualNetwork`：容量從硬寫 10000 改為 `conduitCapacities` Map 追蹤每個導管的 tier 貢獻；`addConduit()` 增加容量，`removeConduit()` 縮容並截斷魔力，`updateConduitCapacity()` 處理 tier 升級 delta。`ArcaneConduitBlockEntity.setTier()` 現在會通知 VirtualNetwork 更新。
- `StatsManager`：移除全部 `System.currentTimeMillis()` 呼叫，改用內部 `tickCounter` 與 game tick。`recordTransfer()` 新增 `gameTick` 參數；`IDLE_THRESHOLD` 與 `performMaintenance()` 閾值統一以 tick 為單位（600 ticks = 30s、6000 ticks = 5min）。
- `NetworkManager`：`lastScanTime`/`lastLogTime` 改用 game tick；`MIN_SCAN_INTERVAL` 由 100ms → 2 ticks，`LOG_INTERVAL` 由 30000ms → 600 ticks；移除 `scanNetworkTopology()` 裡的死變數 `now`。
- `ArcaneConduitBlockEntity.isTransferringMana()`：改用 `level.getGameTime()` 對比 game tick，不再呼叫 `System.currentTimeMillis()`。
- VirtualNetwork create/join/leave 的 INFO log 降為 DEBUG，避免大型基地 chunk 載入時塞爆 log。
- `ManaGeneratorBlockEntity`：刪除無作用的空 `setChanged()` override。
- `gradle.properties`：補回 `org.gradle.java.home=Java 21`（誤刪導致 Gradle Daemon 切回 Java 25 造成 test task 失敗）。
- 新增 `VirtualNetworkGameTests`：6 個 GameTest 覆蓋容量邏輯（單導管基準、共享網路、移除縮容、魔力截斷、混合 tier、tier 升級）。
- **Bug Fix** `ManaGeneratorTicker`：修正發電機無法啟動的 bug。原本 `!success → pauseBurn()` 在發電機完全閒置（無燃料、burnTime=0）時也會被呼叫，導致 `failedFuelCooldown=20` 阻擋 `tryConsumeFuel()`，形成無法跳出的 20-tick 無限迴圈。現在 `pauseBurn()` 只在 `isBurning()` 為 true 時（儲量滿導致生成失敗）才呼叫。
- 新增 `ManaGeneratorGameTests`：4 個 GameTest 覆蓋 MANA 模式產魔、ENERGY 模式產能、儲量滿暫停、NBT 持久化。

#### English

- Upgrade modules are now stack-installable; `UpgradeInventory#getUpgradeCount` now counts stack totals; `UpgradeSlot` restricts installable upgrade types per machine.
- `OutputHandler` splits mana and energy output caps; mana stays at `40/t`, energy default external cap raised to `1000/t`.
- `VirtualNetwork`: capacity changed from hardcoded 10000 to a per-conduit `conduitCapacities` Map tracking each conduit's tier contribution. `addConduit()` grows capacity, `removeConduit()` shrinks it and truncates mana, `updateConduitCapacity()` handles tier upgrade deltas. `ArcaneConduitBlockEntity.setTier()` now notifies the network.
- `StatsManager`: removed all `System.currentTimeMillis()` calls; switched to internal `tickCounter` and game ticks throughout. `recordTransfer()` takes a `gameTick` param; `IDLE_THRESHOLD` and `performMaintenance()` threshold unified to ticks (600 = 30s, 6000 = 5min).
- `NetworkManager`: `lastScanTime`/`lastLogTime` switched to game ticks; `MIN_SCAN_INTERVAL` 100ms → 2 ticks, `LOG_INTERVAL` 30000ms → 600 ticks; removed dead `now` variable in `scanNetworkTopology()`.
- `ArcaneConduitBlockEntity.isTransferringMana()`: uses `level.getGameTime()` diff instead of `System.currentTimeMillis()`.
- VirtualNetwork create/join/leave logs demoted from INFO to DEBUG to prevent log spam on chunk load.
- `ManaGeneratorBlockEntity`: removed no-op `setChanged()` override.
- `gradle.properties`: restored `org.gradle.java.home=Java 21` (accidental removal caused Gradle Daemon to fall back to Java 25, breaking test task).
- Added `VirtualNetworkGameTests`: 6 GameTests covering capacity scaling (single conduit baseline, shared network, removal shrink, mana truncation, mixed tiers, tier upgrade).
- **Bug Fix** `ManaGeneratorTicker`: fixed generator never starting. `pauseBurn()` was called on every tick with no output (including idle ticks with no fuel), setting `failedFuelCooldown=20` and blocking `tryConsumeFuel()` indefinitely. Now `pauseBurn()` is only called when `isBurning()` is true (generation failed due to full storage).
- Added `ManaGeneratorGameTests`: 4 GameTests covering MANA mode generation, ENERGY mode generation, pause-when-full behavior, and NBT persistence.

## [0.0.1.5-beta-hotfix05] - 2026-03-12

### 玩家版變更 / Player-facing Changelog

#### 中文

- 修正 **魔力發電機** 在輸出緩衝區已滿時燈光快速閃爍並拖慢畫面的問題；現在必須連續穩定 10 tick 才會切換亮燈狀態。
- 修正 **魔力發電機** 無法將能量直接輸出到緊貼的 **AE2 ME Energy Acceptor**；大容量接收端的需求值計算不再發生整數溢位。
- 修正 **魔力草原** 會覆蓋河流與海岸附近原版生態域的問題，現在河流槽位與海洋/海岸地表材質會維持原版邏輯。
- 調整 **魔力草原** 區域尺寸，從過大的大陸級範圍縮回較適合探索的規模，較容易在正常冒險中遇到邊界。
- 修正 **魔力粉碎機** 被挖掘時不顯示裂紋動畫的問題。

#### English

- Fixed **Mana Generator** light flicker and frame drops when its energy or mana output buffer was full. The lamp now changes only after the state stays stable for 10 ticks.
- Fixed **Mana Generator** failing to output energy directly into an adjacent **AE2 ME Energy Acceptor**. Demand calculation for high-capacity receivers no longer overflows `int`.
- Fixed **Mana Plains** overriding nearby river slots and ocean/coast surface terrain. Rivers and vanilla coastal terrain now keep their intended behavior.
- Rebalanced **Mana Plains** region size down from oversized continent-scale coverage to a more exploration-friendly range so biome borders are easier to encounter in normal play.
- Fixed **Mana Grinder** not showing block crack animation while being mined.

### 開發者版變更 / Developer-facing Changelog

#### Bug 修復

- `ManaGeneratorBlockEntity` 新增亮燈狀態 debounce，僅在工作狀態連續穩定 10 tick 後更新 `LIT` blockstate，避免輸出受阻時反覆閃爍與額外 block update。
- 修正發電機對外能量輸出需求值使用 `int` 計算造成溢位，現在可正確對接 AE2 `ME Energy Acceptor` 這類高容量接收端。
- `OverworldBiomeBuilderMixin` 與 biome climate 設定加入河流/低 weirdness 保護，避免 `mana_plains` 覆蓋原版河流槽位。
- 補上海洋與海岸相鄰區域的表層材質保護，避免 `mana_grass` / `mana_soil` 滲入 vanilla ocean biome。
- `ManaGrinderBlock` 的 `getRenderShape` 改為 `ENTITYBLOCK_ANIMATED`，恢復挖掘裂紋渲染。

#### 世界生成調整

- `RegionNoiseSampler.REGION_SCALE` 由 `0.04` 提升到 `0.15`，將平滑噪聲模式下的 `Mana Plains` 區域尺寸從約 3200 格縮到約 850 格。
- `PlacementMode.SMOOTH_NOISE` 仍保留作為自訂 biome region 的平滑分布方案，但目前參數已調整為較適合生存探索的尺度。

### 玩家版變更 / Player-facing Changelog

#### 中文

- 新增專案發版 workflow skill，固定 GitHub Release / Modrinth 的版本、tag、`runData` 與構件驗證流程，方便後續交給 Claude 與 Codex 重複使用。
- 整理部分設定與世界生成原始碼的行尾格式，避免開發環境切換時產生無意義差異。
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

- Added a reusable project release-workflow skill for Claude and Codex so version bumps, tags, `runData`, and artifact validation for GitHub Release / Modrinth follow the same proven process.
- Normalized line endings in a few config and worldgen source files to avoid noisy diffs when switching development environments.
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

- 新增專案專用發版 skill，收斂 `CHANGELOG.md`、`mod_version`、新 tag、CI `runData` 與公開 jar 驗證的固定步驟。
- 整理 `gradle.properties`、`PlacementMode`、`RegionNoiseSampler` 的行尾格式，減少跨環境作業時的純格式雜訊。
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
