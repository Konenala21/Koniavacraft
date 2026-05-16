# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Player Changes / 玩家更新內容

- Ghost structure projection now highlights blocked positions with red outlines and prevents locking when any block is in the way — move to a clear area first.
- 幽靈結構投影遇到被方塊佔據的位置時會顯示紅色輪廓，並阻止在有衝突時鎖定位置，需移動到空地才能固定。
- Ghost projection controls redesigned: hold Shift to pull the ghost closer (within 6 blocks); Shift+Left Click to lock position; Shift+Right Click to close. Clicking without Shift now passes through normally so you can interact with the world freely.
- 幽靈投影操作重新設計：按住 Shift 拉近投影（6 格內）；Shift+左鍵固定位置；Shift+右鍵關閉。不按 Shift 的點擊直接穿透，可正常與世界互動。
- Pressing Escape no longer closes the ghost projection; close it with Shift+RMB or by clicking the PRJ button again in JEI.
- 按下 Esc 不再關閉幽靈投影；改用 Shift+右鍵或在 JEI 再按一次 PRJ 按鈕關閉。

### Developer Notes / 開發者備註

- GhostProjectionHandler: added CF_R/G/B conflict color constants; outline loop now checks canBeReplaced() per block and uses red for conflicts; onMouseButton guards lockPosition() with a conflict scan. Added REACH_CLOSE=6.0; onLevelTick uses short reach when player.isShiftKeyDown(). Removed onKey ESC handler entirely.

## [0.0.1.6-4] - 2026-05-16

### Player Changes / 玩家更新內容

- Added T1 Basic Mana Circuit Board manufacturing chain (5 steps, 5 new intermediate items): Mana Substrate, Mana Wire, Mana Adhesive, Mana Wafer, and Basic Mana Circuit Board. Requires Mana Grinder, Aspect Altar, and Mana Crafting Table.
- 新增 T1 基礎魔力電路板製造鏈（5 步驟、5 個中間材料）：魔力基板、魔力導線、魔力黏膠、魔力晶片、基礎魔力電路板，需要磨粉機、本源聚陣與魔力工作台。
- Upgrade button is now shown in the Mana Grinder, Mana Crafting Table, and Mana Infuser GUIs.
- 魔力磨粉機、魔力工作台、魔力注入機的 GUI 現在顯示升級按鈕。
- Aspect Pedestal now supports automation: hoppers, Create, AE2, and other mods with item transport can insert and extract items automatically.
- 本源底座現在支援自動化：漏斗、Create、AE2 等模組可直接存取底座物品。
- JEI altar multiblock guide completely redesigned: a slider now shows the full altar structure with all upgrade rings stacked cumulatively (slide to T3 = base + T1 + T2 + T3 rings visible at once). Material count updates cumulatively as you slide. Press the "Inv" button to scan your inventory once — counts turn green/orange/red, and hovering over each material shows how many you have and how many are still missing.
- JEI 祭壇多方塊指南全面重新設計：滑條模式可累積顯示整體祭壇結構（拉到 T3 = 同時顯示基礎 + T1 + T2 + T3 環）。材料數量依層數累積計算。按「Inv」按鈕即時掃描背包，數量顏色轉為綠/橙/紅，懸停材料可查看需要/擁有/還缺多少。
- Material items in the JEI altar guide are now clickable — left-click to see uses, right-click to see recipes.
- JEI 祭壇指南的材料物品現在可點擊，左鍵查用途，右鍵查配方。
- Deep Mana Soil now generates correctly in Mana Plains biomes. Previously it never appeared due to a surface rule configuration error.
- 深層魔力土壤現在可在魔力草原生物群系中正確生成，先前因地表規則設定錯誤導致完全不生成。

### Developer Notes / 開發者備註

- Registered 5 new items: mana_substrate, mana_wire, mana_adhesive, mana_wafer, basic_mana_circuit with textures and auto item model generation.
- Added 3 ManaGrinder recipes (mana_substrate, mana_wire×4, mana_adhesive×3), 1 AltarRecipe (mana_wafer×2), 1 ManaCrafting shaped recipe (basic_mana_circuit) for the T1 circuit chain.
- ManaGrinderMenu / ManaInfuserMenu: added getBlockEntityPos(); ManaCraftingScreen: null guard on getBlockEntity().
- AspectPedestalBlockEntity.getItemHandler(): returns an IItemHandler wrapping the single heldItem slot (accepts 1 item, ejects on extract). Registered as Capabilities.ItemHandler.BLOCK in ModCapabilities.
- AltarMultiblockCategory: full rewrite — replaced 7-tab system with SliderWidget (0–6 snap positions), cumulative render map (base + rings 0..tier-1), InventoryButtonWidget (one-shot inventory scan → cachedCounts[], countColor()), addTooltipCallback on each JEI slot for need/have/missing lines, auto-scale on tier change.
- Surface rule fix: shallow mana soil uses UNDER_FLOOR (stoneDepthCheck default 0–5 blocks), deep mana soil uses not(UNDER_FLOOR) within stoneDepthCheck(0, true, 4, FLOOR). BiomeTerrainRegistration deepSoilThreshold changed from 20 → 4.

## [0.0.1.6-3] - 2026-05-15

### Player Changes / 玩家更新內容

- License changed from MIT to LGPL-3.0-only.
- 授權條款由 MIT 更改為 LGPL-3.0-only。
- Aspect Convergence Array structure redesigned: corner pillars now placed at 6-block diagonal distance (±3, ±3) instead of ±1. Structure now requires 5 Aspect Pedestals: one directly below the altar core (catalyst slot), and one each to the north, south, east, and west.
- 本源聚陣結構重新設計：角落柱子移至對角 ±3 距離。現在需要 5 個本源底座：核心正下方（催化物槽）+ 東西南北各一。
- Structure Build Wand now also places Aspect Pedestals at the required positions. It can pull pedestals directly from your inventory — no need to place them one by one manually.
- 結構建造法杖現在也可以在必要位置自動放置本源底座，直接從背包取用，不需要逐一手動放置。
- Seal rune symbols float near each corner pillar whenever the altar is formed, visible from all four sides of the pillar.
- 本源聚陣成形後，四個角落柱子旁會顯示浮空封印符文，每個柱子的四個側面都可看到。
- Energy beams connect the corner pillars to the altar core during an active ritual.
- 儀式進行中，角落柱子與核心矩陣之間會出現能量光柱。
- Solar core glow effect now renders above the altar when Dyson Ring tier reaches T3 or higher. The glow fades in as you step back and disappears when you get close.
- 戴森環升級達到 T3 以上時，祭壇核心上方會出現太陽核心輝光效果，靠近時淡出，退後時漸顯。
- JEI multiblock structure guide now shows an isometric 3D preview of the full altar layout instead of flat per-layer diagrams.
- JEI 多方塊結構指南改為等角 3D 預覽，直接呈現整體祭壇擺放樣貌，不再是分層平面圖。
- Mana Infuser now processes Refined Mana Dust into Mana Crystal Fragments (3 dust → 1 fragment, 4200 mana).
- 魔力注入機現在可將精煉魔力粉轉化為魔力水晶碎片（3粉→1碎片，消耗 4200 魔力）。
- Added crafting recipes for all four Mana Generator upgrade modules: Accelerated Processing, Expanded Fuel Chamber, Catalytic Converter, and Diagnostic Display.
- 新增四種魔力發電機升級模組的合成配方：加速處理、擴充燃料室、催化轉換器、診斷顯示。
- Mana Grinder, Mana Infuser, and Mana Crafting Table now support upgrade modules. Use the Basic Tech Wand on any of these machines to open the upgrade GUI.
- 魔力研磨機、魔力注入機、魔力合成台現在支援升級模組，使用基礎科技魔杖對機器按右鍵即可開啟升級介面。
- Mana Grinder and Mana Infuser support Speed upgrades (faster processing) and Efficiency upgrades (lower mana cost). Mana Crafting Table supports Efficiency upgrades only.
- 魔力研磨機與魔力注入機支援速度升級（加快處理）與效率升級（降低魔力消耗）；魔力合成台僅支援效率升級。
- Mana Grinder recipes overhauled: Raw Mana Dust now grinds into Mana Dust ×2 (more efficient than smelting); Corrupted Mana Dust ×2 purifies into Mana Dust ×1; added cobblestone→gravel, gravel→sand, bone→bone meal ×4, blaze rod→blaze powder ×4.
- 魔力研磨機配方大幅調整：原魔塵現在可研磨出魔力粉 ×2（比熔爐更有效率）；汙穢魔力粉 ×2 可淨化成魔力粉 ×1；新增圓石→碎石、碎石→沙子、骨頭→骨粉 ×4、烈焰棒→烈焰粉 ×4。
- Added new T1 research: Mana-Forged Tools — unlocks the concept of mana crystal tools and the Basic Tech Wand. Requires Mana Crystallisation.
- 新增 T1 研究節點「魔力鍛造工具」，解鎖魔力水晶工具與基礎科技魔杖的使用概念，前置條件為魔力晶化。
- Mana Pickaxe now chain mines up to 32 connected ore blocks. Hold Shift to mine single blocks.
- 魔力鎬現在可連鎖挖掘最多 32 個相鄰礦石方塊。按住 Shift 可單格挖掘。
- Ink Quill no longer breaks when durability runs out — it locks instead (glows, durability bar fully depleted). Locked quills cannot be used and must be refilled.
- 墨水羽毛筆耗盡時不再消失，改為鎖定狀態（閃光、耐久條全紅）。鎖定狀態無法使用，需補充墨水。
- Added Ink Quill refill recipe: combine any Ink Quill (any durability) with an Ink Sac in the crafting grid.
- 新增墨水羽毛筆補充配方：任意耐久的墨水羽毛筆 + 墨囊 = 全新羽毛筆。
- Added Aspect Convergence Array (multiblock altar) ritual crafting system. Place catalyst on altar, items on 4 diagonal pedestals, right-click to start ritual. Result drops above the altar when complete.
- 新增本源聚陣儀式合成系統。將催化物品放在祭壇上，材料放在四個底座，空手右鍵觸發儀式，完成後結果掉落於祭壇上方。
- Added Jade support for Aspect Convergence Array: shows structure status, catalyst, and pedestal fill count in the overlay.
- 新增本源聚陣的 Jade 支援：準星對著祭壇會顯示結構狀態、催化物品及底座填充數。
- Research Table now uses BlockEntityRenderer (BER) instead of a standard block model, fixing glass transparency and z-fighting issues.
- 研究台改用 BlockEntityRenderer 渲染，修正玻璃透視與 Z-fighting 問題。
- Nara Holographic Watch now has a crafting recipe (4 gold ingots + 4 copper ingots + 1 mana dust). This is the entry point to the entire research system.
- 娜拉全息手錶現在有合成配方（4金錠 + 4銅錠 + 1魔力粉），這是整個研究系統的起點。
- Added multiblock framework (IMultiblockController / IMultiblockPart / MultiblockPattern) as the foundation for all future multi-block machines.
- 新增多方塊框架（IMultiblockController / IMultiblockPart / MultiblockPattern），作為所有未來多方塊機器的基礎。
- Aspect Altar core now animates as a floating Rubik's cube when formed: 9 face slices cycle one at a time, each rotating 90° and pausing before the next, while the whole structure floats and tilts slowly.
- 本源聚陣核心成形後呈現懸浮魔術方塊動畫：9個切面依序輪流旋轉90°並暫停，同時整體緩慢浮動傾斜自轉。
- Altar Pillar block now uses a custom OBJ model with a dedicated texture.
- 矩陣柱現在使用自訂 OBJ 模型與專屬貼圖。
- Aspect Pedestal now uses a custom model with a dedicated texture; displayed items are better positioned and sized.
- 本源底座現在使用自訂模型與專屬貼圖；物品顯示位置與大小已優化。
- Aspect Convergence Array Rubik's cube animation is now ritual-only: during a ritual all 9 face slices rotate; otherwise the core just slowly spins in place.
- 本源聚陣魔術方塊動畫改為僅儀式進行時才切面輪動，平時只做緩慢自轉。
- Aspect Convergence Array structure formation now requires using the Advanced Tech Wand — it no longer forms automatically. A short ascending chime plays on successful formation.
- 本源聚陣結構成形現在需要使用進階科技法杖觸發，不再自動成形，觸發成功時播放上揚音符盒音效。
- Added Advanced Tech Wand and Structure Build Wand items.
- 新增進階科技法杖與結構建造法杖物品。
- Aspect Convergence Array now supports Dyson Ring upgrades (T1–T6, up to T12 planned): surround the altar with Resonance Rings to gain upgrade tiers. Each tier adds a glowing ring that rotates around the altar core.
- 本源聚陣現在支援戴森環升級（T1–T6，最高 T12 規劃中）：在祭壇周圍放置共鳴環即可獲得升級階級，每個階級都會增加一個繞核心旋轉的發光環。
- Pedestal scan radius increased from 4 to 6 blocks.
- 底座偵測半徑從 4 增加到 6 格。

### Developer Notes / 開發者備註

- `AspectAltarBlockEntity`: `PILLAR_BOTTOM`/`PILLAR_TOP` moved to ±3 diagonal; `PEDESTAL_OFFSETS` (5 positions) added as public list; `PATTERN` now requires 5 pedestals + 8 pillar positions; `ringPhaseStart` added for ring animation sync.
- `AspectAltarRenderer`: `renderSealSystem()` — seal runes use `entityCutoutNoCull` (4 directional textures, float 0.25 blocks); energy beams use `MIRenderTypes.sealChain()` (LIGHTNING_TRANSPARENCY tube geometry) only when `isActive()`; two-pass rendering (seals first, beams second) to prevent `MultiBufferSource.BufferSource` switching crash ("Not building").
- `MIRenderTypes`: added `sealChain()` (`POSITION_COLOR`, `LIGHTNING_TRANSPARENCY`, `COLOR_WRITE`); added `solarGlow()` (`POSITION_COLOR`, additive no-depth-write) for solar core billboard.
- `AspectAltarRenderer.renderSolarCore()`: 6-layer concentric billboard glow (pre-computed `CIRCLE_COS`/`CIRCLE_SIN`), horizontal distance fade 5–12 blocks, pulse animation, activates at tier ≥ 3.
- `AltarMultiblockCategory` (JEI): replaced flat 3-layer 3×3 grid with isometric 3D projection (14 block positions, painter's-algorithm depth sort).
- `StructureBuildWandItem`: split into `missingPillar` + `missingPedestal` lists; pedestal auto-placement from inventory added; `findInInventory()` generalised helper replaces `findManaBlockInInventory()`.
- `AspectAltarRenderer`: complete rewrite — loads JSON elements as a flat list, partitions 27 cubies dynamically per phase using XYZ bounds; 9 phases smoothstep to 90° then hold; earth-tilt + Y-spin overlay preserved.
- `BlockbenchModelRenderUtils.renderCube`: fixed normal transformation via `poseStack.last().normal()`; added `renderElementList(List<ModelElement>)`.
- New textures: `aspect_altar_texture.png`, `aspect_pedestal_texture.png`, `altar_pillar_texture.png`, `resonance_ring_texture.png`, `advanced_tech_wand.png`, `structure_build_wand.png`, `entity/altar/pillar_seal1–4.png`.
- `ManaInfuserRecipeProvider`: added `refined_mana_dust_to_crystal_fragment` (3×refined → 1×fragment, 4200 mana, 80t).
- `ModRecipeProvider`: added shaped recipes for all four Mana Generator upgrade modules.
- `ManaGrinderBlockEntity`, `ManaInfuserBlockEntity`, `ManaCraftingTableBlockEntity`: implemented `IUpgradeableMachine`; 4-slot `UpgradeInventory`; SPEED/EFFICIENCY scaling; NBT save/load.
- `IWandActivatable` interface added; `AdvancedTechWandItem` and `StructureBuildWandItem` new items.
- `ModCapabilities`: `MANA` capability registered for `ASPECT_ALTAR_BE` as input-only.
- Updated LICENSE and `gradle.properties` `mod_license` to LGPL-3.0-only.

## [0.0.1.6-2] - 2026-05-13

### Player Changes / 玩家更新內容

- All 48 new aspects are now discoverable by scanning blocks, items, and entities — no longer synthesis only.
- 全部 48 種新本源現在可透過掃描方塊、物品、實體發現，不再只能靠合成取得。
- Fixed aspect palette scroll not working on the third page in the Research Puzzle and Aspect Synthesis screens.
- 修正研究謎題與本源合成介面的本源板第三頁無法滾動的問題。
- Over 60 entity types now return meaningful aspects when scanned (e.g. Zombie → Undead+Death, Bee → Vitality+Harvest, Villager → Humanity+Commerce, Ender Dragon → Void+Eldritch).
- 超過 60 種實體掃描後回傳對應本源（例：殭屍→亡靈+死滅、蜜蜂→生機+豐收、村民→人性+交易、末影龍→虛空+異界）。
- Completed Research scroll now displays its custom item texture.
- 完成研究卷軸現在能正確顯示自訂貼圖。

### Developer Notes / 開發者備註

- EN: `BaseMaterialRegistry`: ~80 new `atom()` entries for all 48 new aspects; `getLogicalPool()` extended with inference chains across all tiers.
- EN: `BlockAspectResolver`: added tag/keyword matching for frost, soul, death, undead, machine, void, poison categories.
- EN: `EntityAspectResolver`: full rewrite with 60+ specific entity mappings via switch statement; specific mapping takes priority over tag-based fallback; boss capacity scales to 6 (dragon/wither), 5 (elder_guardian/warden), 4 (evoker/witch/allay).
- EN: `AspectSynthesisScreen` and `ResearchScreen`: `isOverPaletteArea()` now uses fixed `PALETTE_ROWS` height instead of current-page row count — fixes scroll deadzone on last page.
- EN: `ModItemModelProvider`: added `item/generated` model entry for `completed_research` using `koniava:item/completed_research` texture.
- ZH: `BaseMaterialRegistry`：新增 ~80 個 `atom()` 條目涵蓋全部 48 種新本源；`getLogicalPool()` 補齊各層本源的推導鏈。
- ZH: `BlockAspectResolver`：新增冰寒、靈魂、死滅、亡靈、機器、虛空、毒素等 tag/keyword 匹配。
- ZH: `EntityAspectResolver`：以 switch 語句全面重寫，60+ 種精確實體映射；精確映射優先於標籤分類回退；Boss 容量縮放：龍/凋零=6、遠古守衛/監守者=5、喚魔者/女巫/愉靈=4。
- ZH: `AspectSynthesisScreen` 與 `ResearchScreen`：`isOverPaletteArea()` 改用固定的 `PALETTE_ROWS` 高度，修正最後一頁滾輪失效的問題。
- ZH: `ModItemModelProvider`：為 `completed_research` 新增 `item/generated` 模型，對應 `koniava:item/completed_research` 貼圖。

## [0.0.1.6-1] - 2026-05-13

### Player Changes / 玩家更新內容

- The aspect system has been greatly expanded — 80 aspects total across 7 categories: Natural Phenomena, Knowledge & Mind, Magic, Life & Biology, Economy, Machines & Industry, and Society & Civilization.
- 本源系統大幅擴充，共 80 種本源，分為七大類別：自然現象、知識心靈、魔法、生命生物、經濟資源、機器工業、社會文明。
- Fixed research state going out of sync after resetting research.
- 修正重置研究後客戶端狀態不同步的問題。
- Fixed research puzzle progress and aspect counts not syncing correctly after reconnecting.
- 修正重新連線後研究謎題進度與本源計數無法正確同步的問題。
- Fixed JEI aspect synthesis display not refreshing after unlocking research.
- 修正 JEI 本源合成顯示在解鎖研究後未重新整理的問題。

### Developer Notes / 開發者備註

- `ModAspects` expanded from 32 to 80 aspects in dependency tiers (A→E). `void` field name is `VOID_ASPECT` due to Java keyword conflict.
- Scanner mappings for 48 new aspects added in v0.0.1.6-2.
- Fixed `ResearchCommand` reset logic and `ClientResearchCache` staleness.
- Fixed `ResearchScreen` puzzle count desync via `KnowledgeSyncPacket`.
- Fixed `AspectSynthesisJEIPlugin` not refreshing after research state change.

## [0.0.1.6] - 2026-05-11

### Player Changes / 玩家更新內容

- Machine GUIs now show a "LOCKED" overlay with a tooltip explaining which research is required.
- 機器介面新增「已鎖定」遮罩，並顯示提示說明所需研究。
- Nara Watch HUD now identifies living entities and dropped items by name when targeted.
- 娜拉手錶 HUD 現在可對準生物與地面掉落物顯示其名稱。
- Completing a research scroll now instantly syncs research state and unlocks machines without reopening GUIs.
- 完成研究卷軸後立即同步研究狀態，不需重新開啟介面即可解鎖機器。

### Developer Notes / 開發者備註

- Implemented `ResearchLockWidget` for universal GUI research gating.
- Added `KnowledgeSyncPacket` for login and completion-time research sync.
- Refactored `ResearchClientPayloadHandler` to fix dedicated server crashes.
- Fixed `SolarCollectorSyncHelper` upgrade count sync.

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

- Added `reset_non_primary_aspects` to clear all non-primary aspects and restore primary aspect amounts to their initial value.
