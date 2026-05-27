# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Player Changes / 玩家更新內容

- Added Leggings Multi-Jump Upgrade (Mk0-Mk3). Install in the leggings upgrade slot to unlock multi-segment jumping. Each extra jump consumes mana (less per jump at higher Mk). Higher Mk adds more jumps, greater height, and lower mana cost per jump. Fall damage is cancelled while the upgrade is installed. A 1.5-second cooldown applies after using the extra jumps, starting when you land. The leggings tooltip shows how many jump segments are available.
- 新增護腿多段跳升級（Mk0-Mk3）。安裝至護腿升級槽後可解鎖多段跳躍，每次額外跳躍消耗魔力（高 Mk 每跳消耗更低）。高等 Mk 增加跳躍次數、高度並降低每跳魔力消耗。安裝升級後墜落傷害歸零。使用額外跳躍後落地才開始計算 1.5 秒冷卻時間。護腿工具提示會顯示目前可用的跳躍段數。

- Added a container sort button. A small sort button now appears in supported container GUIs (chests, barrels, etc.). Left-click to sort the container, right-click to cycle sort mode (By Type, By Name, By Count). A second button sorts the player inventory. Can be disabled in the mod config under "inventory.sortButtonEnabled". The player inventory and creative/survival inventory screens are excluded.
- 新增容器整理按鈕。在支援的容器介面（箱子、木桶等）中會顯示一個小整理按鈕。左鍵整理容器，右鍵切換排序模式（依類型、依名稱、依數量）。另有一個按鈕整理玩家物品欄。可在模組設定的 inventory.sortButtonEnabled 關閉。玩家物品欄介面和創造模式介面不包含此功能。

- Added crafting recipes for Leggings Multi-Jump Upgrade (Mk0-Mk3) and Helmet Night Vision Upgrade. Multi-jump Mk0 uses Feather and Mana Ingot; higher Mk adds Rabbit Foot, Slime Ball, and Phantom Membrane. Night Vision uses Golden Carrot and Glow Ink Sac. All crafted at Mana Crafting table. Requires runData.
- 新增護腿多段跳升級（Mk0-Mk3）與頭盔夜視升級的合成配方。多段跳 Mk0 使用羽毛與魔力錠；高等 Mk 依序加入兔腳、黏液球、幻翼膜。夜視升級使用金胡蘿蔔與發光墨囊。均在魔力合成台製作，需跑 runData。

- Added Night Vision Upgrade for Mana Alloy Helmet. Install it in an upgrade slot to unlock night vision. Toggle on/off with the "Toggle Helmet Effect" keybind (default N). The helmet tooltip shows the current state (On/Off). Requires runData to generate item model.
- 新增魔力合金頭盔的夜視升級。安裝至升級槽後可解鎖夜視效果，按「切換頭盔效果」快捷鍵（預設 N）開關。頭盔工具提示會顯示目前狀態（已開啟/已關閉）。

- Added four armor toggle keybinds: Toggle Helmet Effect (N), Toggle Chestplate Effect (unbound), Toggle Leggings Effect (unbound), Toggle Boots Effect (unbound). Rebind in Controls settings.
- 新增四個裝備效果切換快捷鍵：切換頭盔效果（N）、切換胸甲效果（未綁定）、切換護腿效果（未綁定）、切換鞋子效果（未綁定），可在控制設定中自訂。

- Merged all per-armor capacity upgrades into a single universal item. There is now one Mana Capacity Upgrade per Mk level (Mk0-Mk3) that works for all mana armor pieces (helmet, chestplate, leggings, boots). Capacity bonus is now uniform: +1000 / +2000 / +3500 / +5500 per Mk. Requires runData.
- 將各部位獨立的容量升級合併為通用物品。現在每個 Mk 等級（Mk0-Mk3）只有一個「魔力容量升級」，適用於所有魔力盔甲（頭盔、胸甲、護腿、靴子）。容量加成統一為 +1000 / +2000 / +3500 / +5500。需跑 runData。

- Helmet Night Vision upgrade now drains mana while active (5 mana/second). When mana runs out, night vision turns off automatically. Toggling off instantly removes the effect. The upgrade no longer removes potion-based night vision when deactivated.
- 頭盔夜視升級現在開啟時會持續消耗魔力（5 魔力/秒）。魔力耗盡後夜視自動關閉。手動關閉時效果立即移除。關閉升級不再移除藥水提供的夜視效果。

### Developer Notes / 開發者備註

- Added `HelmetUpgradeBehavior.NIGHT_VISION` (purple, single item, no Mk scaling). Toggle state stored as `ModDataComponents.NIGHT_VISION_ACTIVE` (Boolean, persistent, networkSynchronized) on the helmet ItemStack.
- `ToggleNightVisionPacket` (C2S): verifies upgrade installed before toggling DataComponent.
- `HelmetNightVisionHandler`: server-side PlayerTickEvent.Post, refreshes Night Vision MobEffect only when duration < 60 ticks. Drains 1 mana per 4 ticks (5/s). Uses `NIGHT_VISION_WE_APPLIED` DataComponent to track whether we applied the effect, preventing removal of potion-based night vision on deactivation. Auto-sets `NIGHT_VISION_ACTIVE` false when mana hits 0.
- `ArmorCapacityUpgradeItem`: universal capacity upgrade class replacing 16 per-armor variants. `BONUS_PER_MK = [1000, 2000, 3500, 5500]`. Each armor piece's `isValidUpgradeItem`/`recalculateMaxMana`/`getUpgradeBehaviorKey` updated to accept it. `UnifiedArmorUpgradeScreen` boots branch also updated.
- `ModKeyMappings`: added TOGGLE_HELMET/CHESTPLATE/LEGGINGS/BOOTS for future extensibility.
- Run `runData` to generate `helmet_upgrade_night_vision` and `leggings_upgrade_multi_jump_mk0-mk3` model JSONs.
- `LeggingsUpgradeBehavior.MULTI_JUMP`: jumpVelocityPerMk [0.42, 0.52, 0.62, 0.72], manaCostPerMk [14, 7, 5, 4] (per 7000-unit bar), color 0xFF00EEFF.
- `LeggingsDoubleJumpHandler`: cooldown 30 ticks (post-landing), fall damage zeroed via LivingFallEvent when MULTI_JUMP installed.
- `ContainerSortGuiEvent` + `SortButton` + `InventorySorter`: sort injected via ScreenEvent.Init.Post into non-vanilla, non-mod-specific AbstractContainerScreen instances. Server-side sort via `SortContainerPacket` (C2S). Config: `ModClientConfig.sortButtonEnabled`.

---

- Fixed: Mana Grass Block no longer converts to vanilla dirt when a block is placed above it. It now correctly converts to Mana Soil instead.
- 修正：魔力草方塊在上方放置其他方塊後，不再錯誤地變成原版泥土，現在會正確地轉換為魔力泥土。

- Fixed: Items placed on Aspect Pedestals are now dropped when the pedestal block is broken instead of being lost.
- 修正：放置在元素底座上的物品，在底座被挖掉時現在會正確掉落，不再消失。

- Added: Mana Plate Press JEI recipe view now shows the pressing time in ticks.
- 新增：魔力壓板機 JEI 配方介面現在顯示壓制所需時間（ticks）。

- Updated Chinese (Traditional) display names for three music discs: "量化之境 (A/B)", "兩個視角的同一人 (A/B)", "我應該沒來遲吧! (A/B)". Audio files for these six tracks have also been updated.
- 更新三張音樂唱片的繁體中文名稱：「量化之境（A/B）」、「兩個視角的同一人（A/B）」、「我應該沒來遲吧!（A/B）」。這六個音軌的音訊檔案也已更新。

- Mana Alloy armor set (Helmet, Chestplate, Leggings) and Mana Sprint Boots are now crafted exclusively at the Altar (T1). The Mana Infuser no longer has duplicate armor recipes. Boots now require a two-step altar ritual: craft unfinished boots first, then activate them with crystal fragments, mana wire, and feathers.
- 魔力合金套裝（頭盔、胸甲、護腿）和魔力衝刺靴現在統一在 T1 祭壇合成，注入機不再有重複的裝備配方。靴子需要兩步祭壇儀式：先合成未完成靴，再以晶體碎片、魔力導線、羽毛激活。

- Added JEI support for Mana Plate Press: shows input count, mana cost bar, and animated progress arrow. Clicking the progress arrow in the machine GUI opens the JEI recipe list.
- 魔力壓板機現在支援 JEI：顯示輸入數量、魔力消耗條與動態進度箭頭。在機器介面點擊進度箭頭可開啟 JEI 配方列表。

- Fixed: Mana Infuser slot rejected items that needed more than one piece (e.g. armor plates). The slot now correctly allows any ingredient that matches a recipe regardless of count.
- 修正：魔力注入機的輸入槽拒絕放入需要多個的物品（例如製作裝備所需的強化板）。現在只要物品類型符合配方就可以放入，不再檢查數量。

- Fixed: Mana Infuser JEI recipe display showed all armor recipes as requiring only 1 plate. Input count is now shown correctly (e.g. x5 for helmet, x8 for chestplate). Mana bar no longer overflows due to a hardcoded max of 200.
- 修正：魔力注入機 JEI 配方顯示所有裝備配方都只需 1 塊板子。現在正確顯示輸入數量（例如頭盔 x5、胸甲 x8）。魔力條不再因硬寫最大值 200 而導致溢出。

- Added Mana Plate Press machine: accepts Mana Alloy Ingot and presses it into Mana Reinforced Plate (used to craft Mana Alloy armor sets). Connect arcane conduits for mana.
- 新增魔力壓板機：接受魔力合金錠並壓製成魔力強化板（用於合成魔力合金套裝）。連接奧術導管供應魔力。

- Added four new intermediate materials for the Mana Alloy armor crafting chain: Mana Iron (iron + mana ingot), Mana Crystal Alloy Dust (grinder), Mana Alloy Ingot (infuser), Mana Reinforced Plate (plate press).
- 新增四種魔力合金套裝的中間材料：魔力鐵（鐵錠 + 魔力錠合成）、魔力水晶合金粉（粉碎機）、魔力合金錠（注入機）、魔力強化板（壓板機）。

- The Mana Plate Press block now has a pressing animation: the press plate moves down as the recipe progresses and snaps back smoothly when done.
- 魔力壓板機方塊現在有壓板動畫：壓板隨配方進度下壓，完成後平滑彈回。

- Unified Armor Upgrade Screen navigation reworked: the nav strip now shows item icons only (no text). The current piece is displayed as a larger centered icon in the left panel. Clicking left/right triggers a 180ms slide-in animation. Upgrade slot backgrounds are darkened for better readability.
- 通用裝備升級介面導航列改版：導航條改為純圖示（無文字）。當前裝備以較大置中圖示顯示於左側面板，切換時觸發 180ms 滑入動畫。升級格底色加深以提高可讀性。

- Nara now gives a brief hint the first time you equip Mana Sprint Boots, pointing you to the Mana Equipment chapter in the guide.
- 首次裝備魔力衝刺靴時，娜拉會給一句簡短提示，指引你查看指引介面的「魔力裝備」章節。

- Mana Alloy Leggings now support double jump. Press jump again while in the air to consume 300 mana and jump a second time. Resets on landing.
- 魔力合金護腿現在支援二段跳。在空中再次按跳躍鍵消耗 300 魔力進行二段跳，落地後重置。

- Added Mana Alloy Helmet, Mana Alloy Chestplate, and Mana Alloy Leggings. All three store mana and support upgrade slots (Capacity and Armor upgrades, Mk0-Mk3 each). Open the upgrade interface with the same keybind as boots/wand (default U). Craft at the Altar (T1): center is the corresponding iron armor piece, surrounded by Mana Reinforced Plates (5 for Helmet, 8 for Chestplate, 7 for Leggings).
- 新增魔力合金頭盔、魔力合金胸甲、魔力合金護腿。三件均可儲存魔力並支援升級槽（容量升級和防甲升級各 Mk0-Mk3）。與靴子/法杖相同的按鍵（預設 U）可開啟升級介面。在 T1 祭壇合成：中心放對應鐵裝備，圍繞魔力強化板（頭盔 5 塊、胸甲 8 塊、護腿 7 塊）。

- Wand Core and Wand Upgrade tooltips now show a "Compatible:" line listing the wand rods they can be installed into, with the item names highlighted in yellow. Mk0-Mk1 upgrades list both rods; Mk2-Mk3 upgrades list the Arcane Pulse Resonator only.
- 法杖核心和法杖升級工具提示現在顯示「適用於：」欄位，列出可安裝的法杖柄，物品名稱以黃色標示。Mk0-Mk1 升級列出兩支杖柄；Mk2-Mk3 只列出術式脈衝諧振器。

- Boots Upgrade tooltip "Compatible:" item name is now highlighted in yellow.
- 靴子升級工具提示的「適用於：」物品名稱現在以黃色標示。

- Fixed: clicking the synthesis output in the Research Table had no effect when JEI was not installed. The knowledge sync packet was crashing silently due to a missing JEI class guard, leaving the client cache empty.
- 修正：未安裝 JEI 時，點擊研究台的合成輸出格沒有任何反應。知識同步封包因缺少 JEI 類別保護而無聲崩潰，導致客戶端快取永遠是空的。

- Fixed: clicking the synthesis output in the Research Table had no effect on some JVM distributions (including the CurseForge launcher's bundled JVM). The aspect synthesis packet handler crashed with "L32X64MixRandom not available" because RandomGenerator.getDefault() relies on a Java SPI provider absent in stripped JDKs.
- 修正：部分 JVM 發行版（包含 CurseForge 啟動器內建的 JVM）中，點擊研究台合成輸出格沒有任何反應。本源合成封包處理器因 RandomGenerator.getDefault() 依賴精簡版 JDK 中不存在的 Java SPI provider（L32X64MixRandom）而崩潰。

- Fixed: altar ritual could complete with the wrong output if pedestal items were swapped during the ritual.
- 修正：祭壇儀式進行中若底座物品被替換，可能以錯誤的配方完成儀式。

- Fixed: armor upgrade screen could fail to find the correct armor slot when the inventory was resynced.
- 修正：裝備升級介面在背包重新同步後可能找不到正確的裝備格。

- Added Mana Equipment research node (T1). Unlocks Chapter 5 of the Nara Guide. Requires Vitality, Mechanism, and Mana aspects.
- 新增魔力裝備研究節點（T1）。解鎖娜拉指引第 5 章，需要活力、機制、魔力本源。

- Added Mana Equipment chapter (Chapter 5) to the Nara Guide: five pages covering the full armor set overview, boots, leggings, chestplate, and helmet.
- 娜拉指引新增魔力裝備章節（第 5 章）：共五頁，涵蓋完整套裝概覽、靴子、護腿、胸甲、頭盔。

- Fixed: placing an aspect in the research grid would briefly show the piece in the wrong position if the server rejected the move (table in use, missing quill, etc.). The grid cell now correctly reverts to its previous state.
- 修正：在研究格放置本源時，若伺服器拒絕操作（研究台被占用、缺少羽毛筆等），格子短暫顯示錯誤位置後才回退。現在格子會立即還原為正確狀態。

- Fixed: research tree in the Nara Watch was displaying nodes in registration order with overlapping diagonal lines. Nodes are now arranged using a barycenter heuristic and connections use elbow-style routing.
- 修正：娜拉手錶研究樹依照登錄順序排列節點，連線重疊難以閱讀。現在改用重心啟發式排版，連線改為折線路由。

- Fixed: Nara's angry portrait would persist for all subsequent dialogues after the punishment event. It now automatically calms after 3 dialogues.
- 修正：娜拉憤怒表情在懲罰事件後會持續貼在所有後續對話上。現在改為在 3 次對話後自動恢復正常表情。

### Developer Notes / 開發者備註

- Added `ManaPlatePressBlock/BlockEntity/Menu/Screen/Recipe` (new machine type). Simplified BE: no upgrade inventory, no research gate. Crafting chain recipe provider added. Block model is custom Blockbench (`mana_plate_press_texture.png`). GUI texture: `mana_plate_press_gui.png`.
- Added `ManaPlatePressRenderer` (BER): parses Blockbench model at runtime, animates "壓板核心" group with `progress/maxProgress` ratio for downward press and lerp-smoothed return (`lerpFactor` 0.18 up / 0.35 down). Per-instance offset stored in `Map<BlockPos, Float>`.
- `ManaPlatePressBlockEntity` now fully implements `IConfigurableBlock`: `setIOMap`, `getIOMap`, `setIOConfig`, `getIOConfig`. Loot table (`dropSelf`) added to `ModBlockLootTableProvider`.
- Renamed item `mana_crystal_alloy_powder` to `mana_crystal_alloy_dust` (item ID, field name, lang key, recipe name updated across `ModItems`, `ManaGrinderRecipeProvider`, `ManaInfuserRecipeProvider`, both lang files).
- Added four new items: `mana_iron`, `mana_crystal_alloy_dust`, `mana_alloy_ingot`, `mana_reinforced_plate`. Textures drawn by user; datagen picks up item models on next `runData`.
- `UnifiedArmorUpgradeScreen` nav strip overhaul: side cells show item icon only (no text, no background fill), center cell fully removed. Left panel now renders one 2x-scaled item icon with `enableScissor`-clipped 180ms ease-out slide animation (`System.nanoTime` timing, per-direction). `computeUpgradeSlotsY` simplified to fixed offset. Slot `renderSlot` adds `0x88000000` dark underlay before texture blit.
- Added `ManaArmorItem` (abstract base), `ManaAlloyHelmetItem`, `ManaAlloyChestplateItem`, `ManaAlloyLeggingsItem` with mana storage + dynamic armor attributes + upgrade slot system.
- Added `HelmetUpgradeItem/ChestplateUpgradeItem/LeggingsUpgradeItem` with corresponding `*UpgradeBehavior` enums (CAPACITY, ARMOR). 24 upgrade items registered total (8 per piece).
- Added `ArmorUpgradeSwapPacket` (C2S, registered in `ModNetworking`). Generic packet handles install/remove for all three armor slots via `EquipmentSlot` ordinal.
- Added `ManaArmorUpgradeScreen` (reuses `wand_upgrade_gui.png`). 3-slot layout: horizontal row; 4-slot: 2x2 grid. Opens on upgrade GUI keybind when HEAD/CHEST/LEGS armor is worn.
- `ClientTickHandler`: upgraded GUI key now also checks HEAD/CHEST/LEGS slots for `ManaArmorItem` after checking FEET for boots.
- `ModCreativeModTabs`: `HelmetUpgradeItem`, `ChestplateUpgradeItem`, `LeggingsUpgradeItem` grouped after other items (same as wand/boots upgrades).
- `ModItemModelProvider`: 24 armor upgrade item names added to `UPGRADE_NAMES` (use `wand_upgrade` parent model). 3 armor item textures will auto-generate when texture files are added.
- Added `NaraTutorialFlow.BOOTS_EQUIP` trigger. Fires once via `LivingEquipmentChangeEvent` (FEET slot, `ManaSprintBootsItem`) in `NaraServerEvents`. Persisted in `ResearchSavedData` via `markTutorialSeen`. 1-line dialogue with confirm button, no sound.
- Added shared lang key `tooltip.koniava.upgrade.compatible` = `"Compatible: %s"` / `"適用於：%s"`. Removed `tooltip.koniava.boots_upgrade.compatible` (was hardcoded, replaced by the parameterized key).
- `WandCoreItem.appendHoverText`: appends compatible line with both rod names (yellow). `WandUpgradeItem.appendHoverText`: appends compatible line; if `mk <= 1` shows both rods, else shows resonator only. `BootsUpgradeItem.appendHoverText`: same pattern with `item.koniava.mana_sprint_boots`.
- Added `item.koniava.wand_rod_advanced` to `en_us.json` ("Arcane Pulse Resonator"). Was previously only in `zh_tw.json`.
- `ResearchClientPayloadHandler`: all three handlers now guard `AspectSynthesisJEIPlugin.refreshAspectIngredients()` with `ModList.get().isLoaded("jei")` to prevent `NoClassDefFoundError: mezz/jei/api/IModPlugin` when JEI is absent.
- `AspectSynthesisPacket.damageQuill()`: replaced `RandomGenerator.getDefault().nextInt(5)` with `player.getRandom().nextInt(5)`. `RandomGenerator.getDefault()` resolves to `L32X64MixRandom` via Java SPI, absent in some stripped JVMs; `player.getRandom()` returns Minecraft's `RandomSource` which is always available.
- `AspectAltarBlockEntity.completeRitual()`: now reads `cachedRecipe` directly instead of calling `findMatchingRecipe()` again.
- `ManaGrassBlock.randomTick()`: removed `super.randomTick()` call to prevent vanilla `GrassBlock` from converting the block to `Blocks.DIRT` when a full block is placed above. Re-implemented the die/spread logic: uses `GrassBlock.canBeGrass()` for light check; converts to `ModBlocks.MANA_SOIL` instead of vanilla dirt; spreads to adjacent mana soil unchanged.
- `AspectPedestalBlock.onRemove()`: added override to drop held item stack via `Containers.dropItemStack` when block is removed (server-side only, skipped when replacing with same block type).
- `PlatePressRecipeCategory`: added centered time-text row at `TIME_Y = 65` using `jei.koniava.plate_press.time` lang key. Added matching keys to `en_us.json` ("Time: %s ticks") and `zh_tw.json` ("時間: %s ticks").
- Updated OGG audio files for six music disc tracks: `quantified_mana_a/b`, `memory_two_sides_a/b`, `knowledge_shortcut_a/b`.
- Updated `zh_tw.json` jukebox song display names for three discs (A and B variants each): quantified_mana, memory_two_sides, knowledge_shortcut.
- `UnifiedArmorUpgradeScreen.findInventorySlot()`: changed comparison from reference equality to `ItemStack.isSameItemSameComponents()`.
- `ClientTickHandler`: added `onClientRespawn` handler; added `DamageNumberRenderer.clear()`, `TurretHitEffectManager.clear()`, `TurretHitEffectRenderer.release()`, `FloatingTurretPlayerRenderer.reset()` on logout.
- `DamageNumberRenderer`, `TurretHitEffectManager`: added `clear()`. `FloatingTurretPlayerRenderer`: added `reset()`.
- `KoniavacraftMod`: added `AltarExplosionRenderer.reload()` to the resource reload listener.
- Added `MANA_EQUIPMENT` research node to `ResearchRegistry` (T1, after `MANA_TOOLS`). Requires Vitality + Mechanism + Mana aspects, `holeRatio` 0.30. Gates Chapter 5 in `NaraGuideScreen`.
- Added Mana Equipment chapter to `NaraGuideScreen` (5 pages: set overview, boots, leggings, chestplate, helmet). EN + ZH lang keys added.
- `NaraWatchScreen`: replaced hardcoded `TIER_LABELS`/`STATUS_LABELS` String arrays with translation key arrays; `renderSideTab` now accepts `Component` instead of `String`.
- `NaraWatchScreen` research tree layout: replaced fixed registration-order placement with barycenter heuristic — siblings sorted by parent average X, then centered above that X. Connections replaced single diagonal lines with elbow connectors (vertical → horizontal midpoint → vertical).
- Added `ResearchCellRevertPacket` (S2C). All rejection branches in `ResearchAspectPlacePacket` call `revertClient()` before returning; `ResearchScreen.revertCell()` applies the authoritative cell state from the server.
- `NaraDialogueManager`: added `madCooldown` counter. `setPortraitMad()` sets it to 3; each `setPortraitShown()` decrements and only clears `portraitMad` when it reaches zero.

## [0.0.1.8-1] - 2026-05-25

### Player Changes / 玩家更新內容

- Fixed Mana Charger dropping its held item twice when broken (once from the block's own drop code and once from the base machine handler).
- 修復魔力充能台被破壞時物品重複掉落兩次的問題。

- Nara tutorial for Mana Grinder, Mana Infuser, and Mana Crafting Table now highlights the relevant GUI area on the final tutorial line, and clears the highlight when the player confirms.
- 魔力研磨機、魔力注魔台、魔力合成台的娜拉教學最後一行現在會高亮對應的 GUI 區域，玩家確認後自動清除高亮。

- Fixed Nara creeper punishment not triggering for players who had not yet bound to Nara.
- 修復未綁定娜拉的玩家炸死苦力怕時不會觸發懲罰對話的問題。

- Updated audio for Mana Grinder and Mana Infuser tutorial lines.
- 更新魔力研磨機與魔力注魔台教學語音。

- Added a keybind (default R, rebindable) to skip altar upgrade animations mid-playback. Camera and pitch reset immediately on skip. T6 Nara dialogue still triggers if skipped before it fires.
- 新增可自訂按鍵（預設 R）可在祭壇升級動畫播放中途跳過。跳過時鏡頭仰角立即歸位。T6 娜拉台詞若尚未觸發，跳過時仍會補發。

- Nara Watch scan messages now show the target's name (block, entity, item, or fluid) instead of a generic prompt.
- 娜拉手錶掃描訊息現在會顯示目標名稱（方塊、生物、物品或流體），不再只顯示通用提示。

- Research tier advancement is now automatic: completing all research in the current tier unlocks the next tier and shows a notification message. Previously the tier never advanced, blocking Tier 2 research (Mana Weapons).
- 研究層級現在自動升級：完成目前層級的所有研究後自動解鎖下一層，並顯示通知訊息。先前層級永遠不會推進，導致第 2 層研究（魔力武器）無法解鎖。

- The Altar Ring Architecture research description now clarifies that it is a technical reference and does not unlock any equipment.
- 祭壇環形升級結構研究說明現在明確標示：本條目為操作指引，不解鎖任何設備。

- Added Mana Sprint Boots: a new magical armor piece with a dash ability (default key V). Holds 6000 mana and supports up to 5 upgrade slots.
- 新增魔力衝刺靴：具備衝刺技能（預設按鍵 V）的魔力護甲，最多 6000 魔力儲量，支援 5 個升級插件槽。
- Added 16 boots upgrade items in 4 categories (Armor, Dash Distance, Mana Efficiency, Capacity), each with Mk0-Mk3 tiers.
- 新增 16 個靴子升級插件，分為防禦強化、衝刺距離、魔力效率、魔力容量四類，各有 Mk0-Mk3 四個等級。
- Crafting Mana Sprint Boots (Unfinished) requires a T1 altar: catalyst Mana Crystal, pedestals leather boots, 3x Mana Ingot, 2x Mana Crystal Fragment, 2x Feather.
- 魔力衝刺靴（未完成）需要 T1 祭壇合成，催化劑為魔力水晶，底座材料為皮革靴子、魔力錠×3、魔力水晶碎片×2、羽毛×2。
- Activate the boots by placing them in the Mana Infuser (costs 5000 mana).
- 將未完成的靴子放入魔力注魔台（消耗 5000 魔力）即可啟動為完成品。
- Open the boots upgrade UI with the Upgrade GUI key (default U) while the boots are equipped.
- 穿著靴子時按升級介面鍵（預設 U）可開啟靴子升級介面，介面含玩家 3D 穿著預覽。
- Fixed boots not charging past base mana capacity even with capacity upgrades installed.
- 修復靴子安裝容量升級後充能台仍只充到基礎容量的問題。
- Boots upgrade items now show a tooltip: upgrade tier, type, effect, and "Compatible: Mana Sprint Boots".
- 靴子升級插件工具提示現在顯示等級、類型、效果與「適用於：魔力衝刺靴」。
- Mana Sprint Boots tooltip now shows "Press [key] to open upgrade interface", reflecting the player's actual keybind setting.
- 魔力衝刺靴工具提示現在顯示「按下 [按鍵] 開啟升級介面」，自動對應玩家實際設定的按鍵。
- Rebalanced Mana Sprint Boots: base mana 6000, dash cost 10 (600 uses base). Capacity Mk0-3 adds +1000/+2000/+3000/+4000 mana (700/800/900/1000 uses). Dash distance base 3 blocks; Dash Distance upgrades Mk0-3 add +1/+2/+3/+4 blocks (total 4/5/6/7). A fixed 1-second cooldown limits dash rate; Cooldown Reducer upgrade items removed.
- 重新平衡魔力衝刺靴：基礎魔力 6000、衝刺消耗 10（600 次）；容量升級 Mk0-3 分別增加 +1000/+2000/+3000/+4000 魔力（對應 700/800/900/1000 次）；衝刺距離基礎 3 格，距離升級 Mk0-3 增加 +1/+2/+3/+4 格（共 4/5/6/7 格）。保留固定 1 秒冷卻限制衝刺頻率；移除冷卻縮減器升級物品（Mk0-3）。

- Each boots upgrade type can now only be installed once per boots. Installing a duplicate type is rejected by the server; the upgrade GUI automatically filters out already-installed types from the candidate list.
- 靴子每種升級類型現在只能安裝一次。伺服端拒絕重複類型；升級 GUI 自動從候選清單過濾已安裝的同類型物品。

- Dash is now disabled in water and lava. Pressing the dash key while submerged shows an actionbar message instead of silently failing.
- 衝刺在水中和岩漿中停用。在液體中按下衝刺鍵會在動作欄顯示提示訊息，而非無聲失敗。

- Fixed upgrade slot tooltip in the Wand and Boots upgrade GUI being covered by the right panel when hovering over left-panel slots. Tooltip now always renders on top of all panels.
- 修復法杖和靴子升級 GUI 左側槽位懸停時工具提示被右側面板遮蓋的問題，現在工具提示永遠在所有面板上層繪製。

- Wand upgrade GUI now correctly rejects Boots upgrade items. Boots upgrades can no longer be installed into a wand.
- 法杖升級 GUI 現在正確拒絕靴子升級物品，靴子升級插件不再能被裝入法杖。

- Upgrade items (Wand Cores, Wand Upgrades, Boots Upgrades) are now grouped together at the end of the creative tab, instead of being scattered among other items.
- 升級物品（法杖核心、法杖升級、靴子升級）現在統一排列在創意欄位末尾，不再散布在其他物品之間。

- Added a visual icon for the Sprint Cooldown status effect (blue feather, 18x18).
- 新增衝刺冷卻狀態效果的視覺圖示（藍色羽毛，18x18）。

- Fixed: certain consequences of ignoring Nara's first-login dialogue were not triggering correctly. They now work as intended.
- 修復：忽略娜拉第一次登入對話時，部分應有的後果未能正確觸發，現已修正。
- Added Arcane Pulse Resonator (advanced wand rod) altar infusion recipe: requires T3 altar, 40000 mana, 320 ticks. Catalyst: High-Density Mana Core. Pedestals: basic wand rod, 2x Precision Mana Circuit, 2x Mana Crystal, 2x Mana Ingot, Amethyst Shard.
- 新增術式脈衝諧振器（進階法杖柄）祭壇灌注配方，需要 T3 祭壇、40000 魔力、320 tick。催化劑：高密度魔力核心；底座：術式脈衝調制器、精密魔力迴路×2、魔力水晶×2、魔力錠×2、紫水晶碎片。
- Fixed Mana Charger recipe requiring altar-only materials (Mana Crystal, Mana Wafer, Basic Mana Circuit); replaced with pre-altar materials (Mana Ingot, Refined Mana Dust, Mana Wire, Mana Crystal Fragment, Mana Substrate) so it can be crafted before building the altar.
- 修復魔力充能台配方要求祭壇限定材料（魔力水晶、魔力晶圓、基礎魔力電路板）的問題，改用前祭壇可取得的材料（魔力錠、精煉魔力粉、魔力導線、魔力水晶碎片、魔力基板），現在可以在建造祭壇之前合成。
- Nara now plays a tutorial dialogue when the player first crafts a Mana Deployer, Mana Charger, Mana Grinder, Mana Infuser, Mana Crafting Table, or Solar Mana Collector. The tutorial fires when the player actually places the machine, and the machine GUI opens automatically so the dialogue appears in context.
- 娜拉現在會在玩家首次合成魔力部署器、魔力充能台、魔力研磨機、魔力注魔台、魔力合成台或太陽能魔力收集器後，等待玩家實際放下機器才觸發教學對話，放下的同時自動開啟機器介面，教學對話在 GUI 上直接播放。
- Added Nara tutorial for Solar Mana Collector explaining sky access, conduit placement, and upgrade slots.
- 新增太陽能魔力收集器娜拉教學，說明天空視野需求、導管接法與升級槽用途。
- The Nara dialogue skip button is now a rebindable keybind (default: R) instead of Escape. It can be changed in Options > Controls under the mod category.
- 娜拉對話跳過按鍵現在改為可自訂按鍵（預設：R），不再使用 Escape，可在「選項 > 控制項」中更改。
- Fixed Mana Deployer consuming mana even when the interaction was explicitly rejected (e.g. right-clicking a locked block or an item that refuses activation).
- 修復魔力部署器在互動明確被拒絕時（例如右鍵已鎖定的方塊，或物品拒絕啟用）仍然消耗魔力的問題。
- Fixed Mana Charger IO direction config being lost on world reload; all faces previously set to non-default now correctly persist.
- 修復魔力充能台的 IO 方向設定在世界重載後遺失的問題，非預設面的設定現在能正確保留。
- Fixed Mana Pickaxe not accepting enchantments at the enchanting table; added to minecraft:enchantable/mining, mining_loot, durability, and vanishing item tags.
- 修復魔力稿子無法在附魔台附魔的問題，現已加入 minecraft:enchantable/mining、mining_loot、durability、vanishing 物品標籤。
- Fixed Mana Charger duplicating the item inside it when broken: the charged item now drops correctly once (inside the picked-up charger block), not twice.
- 修復魔力充能台破壞時內部物品複製的問題：被充能的物品現在只會掉落一次（保留在撿起的充能台方塊內），不再重複掉落。

### Developer Notes / 開發者備註

- Added `SKIP_ALTAR_ANIM` keybinding (default R) to `ModKeyMappings`. `AltarUpgradeAnimManager.skipAll()` fires end sound, triggers T6 dialogue if pending, then clears `ACTIVE`. `ClientTickHandler.onClientTickPost` checks `consumeClick()` and also calls `AltarCameraController.reset()` + `player.setXRot(0)`.
- Added `AltarUpgradeAnimManager.hasAnyActive()` guard so the skip key only consumes input while an animation is running.
- `ScanTarget` records (`ItemTarget`, `EntityTarget`, `BlockTarget`, `FluidTarget`) now each override `scanHeader()` using `Component.translatable("item.koniava.nara_watch.scan.first_time_named", <name>)`. `PlayerTarget` already had its own header and is unchanged.
- Added tier advancement logic to `ResearchSavedData.completeResearch()` via `tryAdvanceTier(PlayerKnowledge)`: checks `ResearchRegistry.allForTier(currentTier)` and calls `knowledge.advanceTier()` if all are completed. `CompletedResearchItem.use()` compares tier before/after and sends `research.koniava.tier_advance` chat message on advancement.

- Added `ManaSprintBootsItem` (extends ArmorItem, ModArmorMaterials.MANA_ALLOY). Dynamic armor values via `getDefaultAttributeModifiers(ItemStack)` override in IItemExtension. Dash logic in `performDash()`.
- Added `EquipmentUpgradeData` DataComponent with custom `equals()` using `ItemStack.isSameItemSameComponents` to prevent spurious equip animations.
- Added `BootsUpgradeBehavior` enum (ARMOR, DASH_DISTANCE, MANA_EFFICIENCY, CAPACITY), `BootsUpgradeItem`, `DashPacket` (C2S), `BootsUpgradeSwapPacket` (C2S).
- Added `BootsUpgradeScreen`: 3-panel layout reusing `wand_upgrade_gui.png`; center panel renders player model via `InventoryScreen.renderEntityInInventoryFollowsMouse`.
- Added `DASH` keybinding (default V) to `ModKeyMappings`. `ClientTickHandler` opens `BootsUpgradeScreen` via existing `OPEN_UPGRADE_GUI` key when boots are equipped.
- Added `ModMobEffects.SPRINT_COOLDOWN` (HUD-display mob effect icon: `textures/mob_effect/sprint_cooldown.png`, 18x18). Dash applies a 20-tick (1s) instance; `ClientTickHandler` also checks `hasEffect(SPRINT_COOLDOWN)` client-side before sending `DashPacket` to suppress wasted packets.
- Added `ModArmorMaterials` with `MANA_ALLOY` armor material. Registered in `KoniavacraftMod`.
- Added altar recipe `mana_sprint_boots_unfinished` (T1, minTier=1) and infuser recipe `mana_sprint_boots_activate` to respective datagen providers.
- One-per-type enforcement: `BootsUpgradeSwapPacket.installToBoots()` iterates all occupied slots and returns early if any existing upgrade shares `getBehavior()` with the incoming item. `BootsUpgradeScreen.getCompatibleItems()` builds a `HashSet<BootsUpgradeBehavior>` of already-installed types (excluding the currently selected slot) and skips matching candidates.
- Deferred tooltip pattern in `BootsUpgradeScreen` and `WandUpgradeScreen`: `renderSlot()` stores tooltip into `pendingTooltip`/`pendingTooltipX`/`pendingTooltipY` instead of drawing immediately; `render()` flushes it after all panels to guarantee draw-order.
- `WandCoreSwapPacket.installToWand()` and `WandUpgradeScreen.getCompatibleItems()` changed from `instanceof IModUpgrade` to `instanceof WandUpgradeItem` to prevent boots upgrades from being accepted.
- `IWandUpgrade` marked `@Deprecated` and now extends `IModUpgrade` (empty body). `WandUpgradeItem` implements `IModUpgrade` directly.
- `ModCreativeModTabs.koniava_ITEMS_TAB`: items implementing `WandCoreItem`, `WandUpgradeItem`, or `BootsUpgradeItem` are deferred to a separate `upgradeItems` list and appended after all other non-dev, non-block items.
- `ManaChargerBlock`: after the block entity drops its held item on break, `setStackInSlot(ITEM_SLOT, ItemStack.EMPTY)` clears the slot immediately so the base machine handler does not drop it a second time.
- `NaraTutorialFlow` mana grinder/infuser/crafting final step: `withOnStart()` calls `NaraDialogueManager.setGuiHighlight(x, y, size)`; confirm callback calls `clearGuiHighlight()`.

- Fixed `NaraCreeperPunishPacket.handle()` incorrectly guarding on `NaraHelper.isBound(player)`: since `NaraBindRequestPacket` sets bound state before sending `NaraStartDialoguePacket`, the guard always returned early during first-login, preventing punishment scheduling and `nara_ignored` advancement. Removed the guard; duplicate punishment is already prevented by `naraPunishmentActive`.
- Added `AltarRecipeProvider.registerWandParts()` with wand_rod_advanced altar recipe (T3, 40000 mana).
- Machine tutorials (grinder, infuser, crafting, deployer, charger, solar collector) now use `Set<UUID>` pending-placement sets instead of timed GUI-close detection. `checkMachinePlaced()` in `NaraServerEvents` handles placement detection, menu open, and tutorial dispatch in one call.
- Added `ghostPositionKeys` tracking set and `removeGhostBlock()` helper in `NaraServerEvents`. Machine `onRemove` implementations check `NaraServerEvents.isGhostBlock(pos)` to suppress NBT item drops during test-command ghost block cleanup.
- Added `NARA_SKIP` keybinding to `ModKeyMappings` (default GLFW_KEY_R). `NaraDialogueOverlay` now uses `ModKeyMappings.NARA_SKIP.matches()` instead of checking for Escape directly.
- Added `SOLAR_COLLECTOR_CRAFT` tutorial to `NaraTutorialFlow` and `NaraCommand`. Voice lines (zh_tw + en) generated locally with Qwen3-TTS clone mode.
- Added `MANA_DEPLOYER_CRAFT` and `MANA_CHARGER_CRAFT` tutorial IDs to `NaraTutorialFlow`. Test all machine tutorials with `/koniava nara tutorial <id>`.
- Fixed `doRightClick` in `ManaDeployerBlockEntity` treating `InteractionResult.FAIL` as a successful activation; now uses `consumesAction()` to correctly distinguish PASS, FAIL, and SUCCESS.
- Fixed `ManaChargerBlockEntity.saveAdditional`/`loadAdditional` not serializing `directionConfig`; added IO config NBT round-trip matching the deployer pattern.

## [0.0.1.8] - 2026-05-23

### Player Changes / 玩家更新內容

- Arcane Pulse Modulator (wand rod) now renders installed core and upgrade plugins visually on the model: the core appears near the wrench head, and up to four upgrade USB-shaped plugins appear on the four side prongs. First-person view shows two slots; third-person shows all four.
- 術式脈衝調制器現在會在模型上視覺化顯示已安裝的核心與升級插件：核心顯示在板手頭部，最多四個 USB 形狀的升級插件顯示在四個側齒上。第一人稱顯示兩個槽位，第三人稱顯示全部四個。
- Upgrade GUI slots now show the installed item's name as a tooltip on hover, instead of always showing the slot label.
- 升級 GUI 槽位懸停時現在顯示已安裝物品的名稱，而非永遠顯示槽位標籤。
- Upgrade GUI can now be closed with the inventory key (default E).
- 升級 GUI 現在可以用背包鍵（預設 E）關閉。
- Added wand_upgrade model: USB-shaped upgrade plugin with outer housing tinted by upgrade type color; metal connector interior retains original texture.
- 新增升級插件 USB 建模：外殼根據升級類型染色，金屬接口內部保持原始貼圖。

- Damage dealt to enemies now shows as floating numbers above the target, visible only to the attacker. Normal hits show in white, critical hits in gold, and Floating Turret magic damage in purple. Rapid hits on the same target are merged into one number.
- 對敵人造成的傷害現在會在目標頭頂顯示浮動數字，只有攻擊者自己看得到。普通傷害白色、暴擊金色、浮游砲魔法傷害紫色，短時間內連續命中同一目標的傷害會合併顯示。
- Dual-wielding Floating Turrets now correctly applies damage from both shots regardless of invincibility frames; the second bolt no longer gets absorbed.
- 雙持浮游砲現在兩顆砲彈都能正確命中目標，不再因為無敵幀吃掉第二顆傷害。
- Floating Turret charging animation now smoothly returns to idle after firing, including when the shot is auto-released at max charge.
- 浮游砲蓄力動畫在發射後（包含滿蓄自動射出）現在能平滑回到閒置位置，不再瞬間跳回。
- Floating Turret projectiles now pass through water and lava instead of stopping on contact.
- 浮游砲砲彈現在可以穿過水和岩漿繼續飛行，不會在液體處消失。
- Added Floating Turret crafting: three new intermediate items (Mana Barrel, Precision Mana Circuit, High-Density Mana Core) and an Aspect Altar T3 recipe. Unlocked via the Mana Weapons research node.
- 新增浮游砲合成路線：三個中間物品（魔力砲管、精密魔力迴路、高密度魔力核）以及本源矩陣 T3 祭壇配方，透過魔力武器研究節點解鎖。
- Added Mana Charger research node, unlocking the Mana Charger block recipe.
- 新增魔力充能台研究節點，解鎖魔力充能台配方。
- Aspect Altar rituals now require a Ritual Wand to start. Right-clicking the altar with an empty hand shows the current mana level and ritual status instead.
- 本源矩陣祭壇儀式現在需要手持儀式魔杖才能啟動。空手右鍵改為顯示當前魔力量和儀式狀態。
- Altar rituals now consume mana continuously throughout the process instead of all at once at completion. If mana supply is interrupted, electric sparks appear on the altar core as a warning; the ritual collapses after 10 seconds with no supply, triggering an explosion.
- 祭壇儀式現在持續消耗魔力而非完成時一次扣除。魔力中斷時核心方塊出現電弧警告，10 秒內未恢復則儀式失控爆炸。
- Ritual failure explosion: red shockwave visual effect expands outward; players within 64 blocks lose 50% max HP and receive Slowness II; items on pedestals may scatter (50%) or vanish permanently (15%).
- 儀式失控爆炸：紅色衝擊波向外擴散；64 格內玩家損失 50% 最大 HP 並受到緩速 II；底座物品有機率散落（50%）或永久消失（15%）。
- Nara Guide (watch UI) Altar chapter now includes a dedicated page explaining the ritual wand, continuous mana requirement, warning mechanic, and explosion consequences.
- 娜拉指引介面的祭壇章節新增儀式機制說明頁，說明儀式魔杖用法、持續魔力需求、警告機制與爆炸後果。
- Jade tooltip now shows current and maximum mana for all mana machines and the Arcane Conduit when hovering over them.
- Jade 懸停提示現在對所有魔力機器和奧術導管顯示當前及最大魔力量。
- Added R key binding to skip the altar upgrade animation.
- 新增 R 鍵快捷鍵可跳過祭壇升級動畫。
- Nara now plays a tutorial dialogue when the player first crafts a wand rod, explaining its purpose and the need for a core plugin. Opening the upgrade GUI later triggers context-sensitive tips depending on whether a core is installed.
- 娜拉現在會在玩家首次合成魔杖杖柄時播放教學對話，說明其用途與安裝核心插件的必要性。之後開啟升級 GUI 時，會依是否已安裝核心顯示對應提示。
- Nara dialogue now plays voice acting for all 48 dialogue lines in both Traditional Chinese and English, covering every tutorial trigger including first scan, watch open, mana generator, wand rod, first research, altar formation, mana grinder, mana infuser, mana crafting table, and aspect synthesis. The correct audio is selected automatically based on the player's in-game language setting.
- 娜拉對話現在對全部 48 條台詞播放配音，涵蓋所有教學觸發點，包括初次掃描、手錶開啟、魔力發電機、魔杖杖柄、首次研究、祭壇成形、魔力粉碎機、魔力灌注機、魔力合成台與本源合成介面。繁體中文與英文各一套，根據玩家遊戲內語言自動選擇。
- Nara Guide screen (watch UI) can now be closed with the inventory key (default E).
- 娜拉指引介面（手錶 UI）現在可以用背包鍵（預設 E）關閉。

- Wand mana is now displayed as a blue bar (same position and size as the durability bar) instead of the custom arc ring. Visible in hotbar, inventory, and all container screens.
- 魔杖魔力現在以藍色條狀顯示（與耐久條相同位置和大小），取代自訂弧形圓環，在快捷欄、背包和所有容器介面均可見。
- Wand upgrade system now uses Mk tiers: each of the 4 upgrade types (Capacity, Efficiency, Range, Cooldown) has Mk0 through Mk3 variants with progressively stronger effects.
- 魔杖升級系統新增 Mk 等級：4 種升級類型（容量、效率、範圍、冷卻）各有 Mk0 到 Mk3 四個等級，效果隨等級遞增。
- Added Arcane Pulse Resonator (術式脈衝諧振器): an advanced wand rod with 6 upgrade slots and support for up to Mk3 upgrades. Crafted via altar infusion.
- 新增術式脈衝諧振器：高階魔杖，擁有 6 個升級槽，支援最高 Mk3 升級，透過祭壇灌注製作。
- Basic wand rod (術式脈衝調制器) supports 4 upgrade slots and up to Mk1 upgrades. Installing a higher-Mk upgrade shows a rejection message.
- 基礎術式脈衝調制器支援 4 個升級槽，最高可裝 Mk1 升級。嘗試安裝更高等級的升級會顯示拒絕訊息。
- Upgrade GUI now shows the correct number of slots based on the held wand tier: 4 slots for the basic rod, 6 slots for the resonator.
- 升級 GUI 現在根據手持調制器等級顯示正確槽位數量：基礎版 4 個槽，諧振器 6 個槽。
- Wand upgrade 3D preview can now rotate freely in all directions (X axis no longer clamped). Drag to spin, scroll to zoom.
- 魔杖升級 3D 預覽現在可以往任意方向自由旋轉（X 軸不再限制角度）。拖曳旋轉，滾輪縮放。
- Wand upgrade Mk1-Mk3 recipes now require the previous tier item in the center slot, preventing tier skipping.
- 魔杖升級 Mk1-Mk3 配方現在需要將前一等級物品放在中心格，無法跨等級製作。
- Extra equipment slots now show a tooltip describing what item type can be installed in each slot.
- 額外裝備欄位現在顯示工具提示，說明各槽位可安裝的物品類型。

### Developer Notes / 開發者備註

- `WandUpgradeBehavior`: replaced single `bonusPerSlot` with `int[] bonusPerMk` (4 values per type). `getBonusForMk(int mk)` clamps and returns the appropriate value. `bonusPerSlot` retained as Mk0 alias. Stats: CAPACITY 2000/3500/5000/7000, EFFICIENCY 15/20/27/35%, RANGE 2/3/4/6, COOLDOWN 3/5/7/10 ticks.
- `WandUpgradeItem`: added `int mk` field. Tooltip shows Mk level for Mk1+.
- `WandCoreData`: added `sumUpgradeBonus(WandUpgradeBehavior type)` which sums `getBonusForMk(wu.getMk())` across all matching slots. Replaces `countUpgrade * bonusPerSlot` in all call sites (recalculateMaxMana, FORMATION core, WandRangeEventHandler).
- `WandRodItem`: added `maxUpgradeMk` and `maxUpgradeSlots` constructor parameters. `wand_rod`: maxUpgradeMk=1, maxUpgradeSlots=4. `wand_rod_advanced`: maxUpgradeMk=3, maxUpgradeSlots=6.
- `WandCoreSwapPacket.installToWand()`: rejects if `wandSlot >= rod.getMaxUpgradeSlots()` or `upgrade.getMk() > rod.getMaxUpgradeMk()`.
- `WandUpgradeScreen`: slot count now read from `rod.getMaxUpgradeSlots()` at render and click time instead of `WandCoreData.UPGRADE_SLOTS`.
- `ModColorHandlers`: updated upgrade item color registration from 4 to 16 items (Mk0-Mk3 for each type).
- `WandManaRingRenderer` removed. `WandRodItem` now overrides `isBarVisible/getBarWidth/getBarColor` (color 0x4488FF) for built-in mana bar rendering.
- `DamageNumberEventHandler`: subscribes to `LivingDamageEvent.Post` on the game bus. On player-caused damage, sends a `DamageNumberPacket` (S2C) to the attacker with world position, final damage (`getNewDamage()`), damage type, and entity ID. Critical hit detection mirrors vanilla `Player.attack()` conditions.
- `DamageNumberPacket`: S2C record packet carrying `x, y, z, damage, dmgType, entityId`. Registered via `ModNetworking`.
- `DamageNumberRenderer`: client-only `@EventBusSubscriber`. Stores the projection and model-view matrices from `RenderLevelStageEvent.AFTER_LEVEL`. In `RenderGuiEvent.Post`, projects each entry's world position to screen via MVP multiply and draws text using `Font.drawInBatch` with ARGB alpha on `GuiGraphics.bufferSource()`. Entries for the same entity within 5 ticks are merged by accumulating damage and keeping the highest-priority color. Numbers fade out over 30 ticks.
- `FloatingTurretProjectile.onHitEntity()`: sets `target.invulnerableTime = 0` before `hurt()` so dual-wield hits both register.
- `FloatingTurretPlayerRenderer.renderHandTurret()`: charge smoothing now decays whenever `chargeRatio` drops (not only when `isCharging` is false), preventing the snap when a new charge starts at ratio 0 immediately after firing.
- `TurretHitEffectRenderer`: `MAX_EFFECTS` raised from 16 to 64 to prevent `BufferOverflowException` when dual-wield generates more simultaneous hit rings.
- `FloatingTurretProjectile.onHitBlock()`: checks `FluidState` before processing block hit; returns early for any non-empty fluid, allowing the bolt to continue through liquids.
- `AspectAltarBlockEntity`: added per-tick mana drain (`cachedRecipe`, `manaConsumedSoFar`, `warningTick`). `tryActivate()` no longer pre-checks mana; ritual is started only via `RitualWandItem`. `tickRitual()` extracts mana each tick; on insufficient mana increments `warningTick` and spawns `ELECTRIC_SPARK` particles every 10 ticks; at `WARNING_TICKS=200` calls `triggerExplosion()`.
- `triggerExplosion()`: sends `RitualExplosionPacket` to players within 64 blocks, deals 50% max HP magic damage and Slowness II to nearby `ServerPlayer` instances, and rolls per-pedestal item scatter/vanish.
- `AltarExplosionRenderer` and `AltarExplosionManager`: client-side GLSL depth-buffer ring renderer using `common_explosion.glsl` (red/orange palette) and `stage_altar_explosion.fsh`. Three waves expand to 64-block radius over 80 ticks. Triggered via `RitualExplosionPacket`.
- `ManaJadeProvider`: reads `ModCapabilities.MANA` at the hovered block position server-side; packs `manaStored` and `maxMana` into NBT. Registered for `AbstractManaMachineEntityBlock`, `AspectAltarBlockEntity`, and `ArcaneConduitBlockEntity`.
- `RitualWandItem`: `useOn()` calls `altar.tryActivate(playerUUID)` and displays the result component as an action bar message.
- `ModSounds`: `DeferredRegister<SoundEvent>` now has 96 programmatically registered Nara dialogue sound events (48 zh_tw + 48 en), keyed as `nara.{locale}.{group}.{line}`. Added 30 new keys covering all tutorial triggers: first_scan, first_watch_open, mana_gen_craft, mana_gen_placed, wand_rod (no_items/ready/got_core variants), first_research, first_altar_formed, mana_grinder, mana_infuser, mana_crafting, aspect_synthesis.
- `NaraSoundHelper`: client-only utility that resolves the player's Minecraft language code to `zh_tw` or `en` (fallback), plays the matching `SimpleSoundInstance`, and exposes `stopCurrent()` to halt playback mid-line.
- `NaraDialogueManager.advanceLine()` and `close()` now call `NaraSoundHelper.stopCurrent()` before transitioning, ensuring the previous voice clip stops when the player skips or dialogue ends.
- OGG files at 5x normalized volume are committed under `assets/koniava/sounds/nara/{locale}/` (96 total). Generated from Alibaba Cloud Qwen3-TTS VoiceDesign API (voice: qwen-tts-vd-nara-voice-20260523011917858-3c21) with per-line emotion instructions. Converted from WAV via ffmpeg `-af volume=5 -c:a libvorbis -q:a 4`.
- Added Nara tutorial dialogues for Mana Grinder, Mana Infuser, Mana Crafting Table (craft-triggered), and Aspect Synthesis Screen (client screen-open-triggered). Each has JEI-conditional lines. Mana Grinder and Mana Infuser share identical JEI click area coords (149,4,21,15).
- `NaraTutorialFlow`: added `MANA_GRINDER_CRAFT`, `MANA_INFUSER_CRAFT`, `MANA_CRAFTING_CRAFT`, `ASPECT_SYNTHESIS_OPEN` constants and corresponding dialogue methods. `claimAspectSynthesisShown()` replaces server-side tracking for the client-only screen.
- `NaraDialogueOverlay`: added `ScreenEvent.Opening` handler to detect first `AspectSynthesisScreen` open. Dialogue box uses compact mode (BOX_W=180, left-anchored) when `overlayOnScreen` is true, to avoid overlap with JEI's right-side item list.
- `NaraServerEvents`: added `tickWhenGuiClosed()` helper (DRY refactor of repeated close-GUI-wait pattern). Added craft detection and pending sets for the three new machines.
- `NaraCommand`: added `/koniava nara tutorial <id>` with tab-completion for all tutorial IDs, so tutorials can be re-triggered without resetting saved state. Removed redundant `/koniava nara machine` subcommand (superseded by the above).
- `first_watch_open` tutorial now has a third line mentioning the guide tab in the watch.
- `NaraServerEvents`: pending tutorial maps changed from `Set<UUID>` to `Map<UUID, Integer>` countdown (200-tick timeout). All GUI-wait blocks (`pendingWandRodCraft`, `pendingManaGenCraft`, `tickWhenGuiClosed`) now fire on timeout even if the player never closes their crafting GUI, eliminating the long reaction delay after crafting or placing a machine.
- Added `WAND_ROD_CRAFT` tutorial dialogue case to `NaraTutorialFlow`. Server now sends the `WAND_ROD_CRAFT` packet directly when the player closes the crafting GUI (or on timeout). `WAND_ROD_NO_ITEMS` and `WAND_ROD_READY` are triggered client-side in `WandUpgradeScreen.init()` using once-per-session flags.
- Added `wand_rod_craft_line1/2` to `ModSounds` DIALOGUE_KEYS and `sounds.json` (zh_tw and en). Added corresponding OGG entries to `generate_nara_voice.py`.

## [0.0.1.7-2]

### Player Changes / 玩家更新內容

- Killing a player with a Floating Turret projectile now broadcasts a random joke message to all online players. PvP kills have 5 variants, passive turret kills have 3 variants.
- 浮游砲彈擊殺玩家時，會向全伺服器廣播隨機搞笑訊息。PvP 擊殺有 5 種台詞，自走砲擊殺有 3 種。
- Fixed: charging sound (beacon ambient) now correctly stops on release instead of playing for an extra 2 seconds.
- 修正：蓄力音效（烽火台嗡嗡聲）現在放開後會立即停止，不再多播 2 秒。
- Extra equipment slot backgrounds are now correctly rendered in the Extra Equipment screen.
- 額外裝備欄位的格子背景現在能在額外裝備介面中正確顯示。
- Floating Turret now fires a traveling energy bolt on right-click. The bolt travels at 1.5 blocks/tick and disappears on hit or after 48 blocks.
- 浮游砲右鍵攻擊發射飛行能量彈，以每 tick 1.5 格的速度飛行，命中或超過 48 格後消失。
- Floating Turret energy bolt is rendered as a glowing blue orb (three-layer billboard with additive blending). Charged bolts grow larger and shift color toward white.
- 浮游砲能量彈以三層發光藍球渲染（加法混合 billboard），蓄力彈尺寸更大並向白色偏移。
- Dual-wielding two Floating Turrets enables a charged shot: hold right-click to charge (up to 2 seconds), release to fire. Damage scales from 16 to 24 based on charge ratio, and the bolt explodes in a small radius on block impact. Mana and durability are consumed from both hands.
- 雙持兩把浮游砲可蓄力攻擊：長按右鍵最多 2 秒蓄力，放開發射。傷害依蓄力比例從 16 到 24，命中方塊時觸發小範圍爆炸。魔力和耐久由兩手各自扣除。
- Floating Turret mana capacity increased from 500 to 15000.
- 浮游砲魔力上限從 500 提升至 15000。
- First-person floating turret now tracks the player's look direction including pitch: looking up raises the turret, looking down lowers it, matching vanilla item behavior.
- 第一人稱浮游砲現在跟蹤玩家完整視角（含仰角）：抬頭砲往上、低頭砲往下，與原版物品持握行為一致。
- First-person floating turret is now positioned closer (less forward offset) and further to the main hand side.
- 第一人稱浮游砲位置調整：減少前方偏移，增加主手方向的側偏距離。
- Floating Turret hand-held display now respects the player's dominant hand setting; left-handed players see the main turret on the left side.
- 浮游砲手持顯示現在支援玩家慣用手設定，左撇子玩家的主手浮游砲會顯示在左側。

### Developer Notes / 開發者備註

- `ExtraEquipmentScreen`: added blit loop in `renderBg` to draw slot backgrounds from UV (235, 1) at `EXTRA_SLOT_BASE_X/Y` using `ExtraEquipmentMenu.EQUIPMENT_SLOT_COUNT`. Position and UV values extracted to constants.
- `FloatingTurretPlayerRenderer`: new client-only `@EventBusSubscriber` class hooked to `RenderPlayerEvent.Post`. Renders hand-mode and equipment-slot turrets directly from the player's interpolated yaw/pitch, eliminating entity sync jitter. Hand-mode turrets (slot 2/3) no longer need server entities; slot-mode turrets (slot 0/1) retain entities for server logic but are visually rendered here.
- `FloatingTurretRenderer`: simplified to only render other players' equipment-slot turrets. Local player's turrets are fully handled by `FloatingTurretPlayerRenderer`.
- `FloatingTurretEventHandler`: removed hand-mode entity spawning (`syncHandEntity`). `MAX_TURRETS_PER_PLAYER` reduced to 2 (slot entities only).
- `FloatingTurretEntity.serverTick()`: removed slot >= 2 item-held check and hand-position calculation; entity type is now always equipment-slot (slot 0/1).
- `FloatingTurretItem.turretPos()`: computes world position mathematically using player yaw instead of looking up a server entity.
- `FloatingTurretEventHandler`: replaced per-player BBox spatial search (60-block inflate) with a static `Map<UUID, Map<Integer, FloatingTurretEntity>>` registry. `findTurretEntity` is now O(1); `spawnTurretEntity` uses registry count instead of `getEntitiesOfClass`. Player logout clears the registry entry.
- `FloatingTurretEntity`: added `remove(RemovalReason)` override to unregister from the registry on any removal path (discard, die, chunk unload).
- `FloatingTurretProjectile`: new entity extending `ThrowableProjectile`; no gravity, speed 1.5 b/t, max lifetime 32 ticks (~48 block range), deals 8 damage on entity hit, immune to all damage.
- `FloatingTurretProjectileRenderer`: three-layer billboard renderer using `RenderType.lightning()` (additive blending). OBJ geometry center offset `(2.8125, -0.6756, 0.0843)` applied after all rotations to keep model centered during spin.
- `FloatingTurretRenderer`: first-person branch now uses full pitch-aware look vector for position (`lookX/Y/Z = forwardXZ * cos(pitch), -sin(pitch)`); added `Axis.ZP.rotationDegrees(-pitch)` after yaw rotation so the barrel tracks the player's exact look direction. Handedness via `HumanoidArm` check.
- `ModEntities`: registered `floating_turret_projectile` (sized 0.3, trackingRange 10, updateInterval 2).
- `ModItems`: added `MANA_STORED=0` and `MAX_MANA=15000` as default data components on `FLOATING_TURRET` so every new instance is recognized by the Mana Charger without needing `getDefaultInstance()`.
- `FloatingTurretItem`: dual-wield detection via `isDualWielding(player)`; `use()` always returns CONSUME (no arm swing); `releaseUsing()` routes to normal or charged fire; `finishUsingItem()` fires at full charge ratio 1.0.
- `FloatingTurretProjectile`: added `CHARGE_RATIO_DATA` synced float; `shootCharged()` factory sets ratio; `onHitEntity()` interpolates damage between CHARGED_DAMAGE_MIN and MAX by ratio; `onHitBlock()` calls `level().explode()` with radius 1.0–2.5 when charged.
- `FloatingTurretProjectileRenderer`: orb scale and color (blue → blue-white) driven by `entity.getChargeRatio()`.

- Abandoned Altar structures no longer replace surrounding terrain with air blocks when generating.
- 廢棄祭壇結構生成時不再將周圍地形覆蓋成空氣方塊。
- Fixed Magic Ore and Deepslate Magic Ore missing the ore tag, which prevented the Mana Pickaxe chain mining from activating on them.
- 修正魔法礦石和深板岩魔法礦石缺少 ore tag 的問題，導致魔力鎬的連鎖挖掘無法對其觸發。
- Magic Ore now requires at least a stone pickaxe to drop items, consistent with standard ore behavior.
- 魔法礦石現在至少需要石鎬才能取得掉落物，與標準礦石行為一致。
- Research Table recipe now accepts any wood planks instead of requiring Dark Oak Planks specifically.
- 研究台配方現在接受任何種類的木板，不再限定深色橡木板。

- Added Mana Charger: accepts items with mana storage via conduit or generator, charges them at 20 mana per tick up to the item's maximum mana capacity.
- 新增魔力充能台：透過導管或發電機接收魔力，以每 tick 20 點的速度為物品充能至其最大魔力上限。
- Advanced Tech Wand can now store up to 8000 mana. Current mana is shown in the item tooltip.
- 高級科技魔杖現在可儲存最多 8000 點魔力，當前魔力顯示於物品 tooltip。
- Mana debug tool now supports +10000 and +100000 mana modes.
- 魔力除錯工具新增 +10000 和 +100000 魔力模式。
- Altar upgrade animations and tier advancements now only trigger for players within 64 blocks of the altar.
- 祭壇升級動畫與升級成就現在只對距祭壇 64 格以內的玩家觸發。
- Fixed JEI aspect synthesis panel crashing on load.
- 修正 JEI 本源合成面板在載入時崩潰的問題。
- Fixed Nara's intro dialogue being cut off when the player is in creative or spectator mode, because the Warden could not damage them and the apology sequence never triggered.
- 修正玩家在創造或旁觀模式下，娜拉懲罰怪物無法傷害玩家導致道歉對話永遠不觸發的問題。
- Added skip button to Nara's dialogue. Press Esc at any time during the intro to dismiss the dialogue.
- 娜拉對話新增跳過功能，對話進行中隨時按 Esc 可跳過整段對話。

### Developer Notes / 開發者備註

- `AbandonedAltarPiece`: added `BlockIgnoreProcessor(AIR, CAVE_AIR, VOID_AIR)` to `StructurePlaceSettings` so template air is skipped during placement.
- `ModBlockTagProvider`: added `Tags.Blocks.ORES` entry for `magic_ore` and `deepslate_magic_ore` so `state.is(Tags.Blocks.ORES)` in `ManaPickaxeItem.mineBlock` correctly resolves both ores. Also added `BlockTags.NEEDS_STONE_TOOL` for `magic_ore`.
- `ModRecipeProvider`: changed Research Table recipe `'C'` definition from `Blocks.DARK_OAK_PLANKS` to `ItemTags.PLANKS`.
- `AspectAltarBlockEntity`: player loop for upgrade events now skips players beyond 64 blocks (`distSqr > 64*64 → continue`).
- `AspectSynthesisJEIPlugin`: moved `removeIngredientsAtRuntime` for token stacks from `registerRecipes()` to `onRuntimeAvailable()`. Calling runtime ingredient operations during the registration phase caused JEI to report the plugin as crashed.
- `AspectSynthesisRecipeCategory.setRecipe()`: removed `createFocusLink` call. JEI requires all linked slots to have an equal ingredient count; input slots had 2 (first + second aspect) while output had 1 (result), causing `IllegalArgumentException` and the "This recipe crashed" overlay. Invisible ingredients are kept for search linking.

---

## [0.0.1.7-1] - 2026-05-19

### Player Changes / 玩家更新內容

- Abandoned Altar structures now generate in Mana Plains biomes. Four variants exist: heavily damaged structures appear frequently on smaller terrain patches, while more intact but still ruined versions appear less often on larger Mana Plains. Variant 04 contains a chest with mana materials.
- 廢棄祭壇結構現在會在魔力草原生物群系中生成。共有四種變體：高度損毀的版本在較小的魔力草原地形頻繁出現，而較完整但仍損毀的版本則在較大的地形低機率出現。變體 04 含有一個裝有魔力材料的箱子。
- Added three ruin decoration blocks: Cracked Mana Bricks, Mossy Mana Bricks, and Ruined Mana Pedestal. These appear in abandoned altar structures.
- 新增三種廢墟裝飾方塊：裂紋魔力磚、苔蘚魔力磚和損壞的魔力底座，出現在廢棄祭壇結構中。
- Mana Grass Block now correctly drops Mana Soil when mined without Silk Touch.
- 魔力草方塊在不使用精準採集時現在能正確掉落魔力泥土。
- Fixed Mana Deployer and Resonance Ring not being mineable with a pickaxe.
- 修正魔力部署器和共鳴環無法用鎬子挖掘的問題。
- Added Nara Guide, accessible via the Guide tab in the Nara Watch screen. Covers four chapters: Aspect System, Research System, Mana Facilities, and Altar System.
- 新增娜拉指引，可透過娜拉手錶介面的指引頁簽開啟，涵蓋四個章節：本源系統、研究系統、魔力設施、祭壇系統。
- Added advancement for crafting the Research Table.
- 新增合成研究台的進度成就。
- Added Consensus Glasses: binds to the first player who uses it, snapshotting their research and aspect data. Other players can use the item to copy that knowledge to themselves.
- 新增共識眼鏡：第一位使用的玩家將其綁定並快照研究與本源資料，其他玩家使用後可將該知識複製至自身。
- Added Source Tome: unlocks all research, sets research tier to 12, and grants 500 points of every aspect. No crafting recipe; intended as a late-game or creative tool.
- 新增源典：解鎖全部研究、將研究階段設為 12，並賦予每種本源 500 點，無合成配方，定位為後期或創意模式工具。

### Developer Notes / 開發者備註

- Structure templates must be placed in `data/<namespace>/structure/` (singular) and chest loot tables in `data/<namespace>/loot_table/` (singular) per 1.21 data pack path changes.
- Chest loot injection uses a `StructureProcessor` (`ChestLootProcessor`) added to `makeSettings()` to write `LootTable`/`LootTableSeed` directly into block entity NBT at template placement time, bypassing world gen pipeline persistence issues with `postProcess` scanning.
- `AbandonedAltarStructure.findGenerationPoint`: Y position now computed via `getFirstOccupiedHeight(..., WORLD_SURFACE_WG) + 1` instead of `getMiddleBlockPosition(0)`.

---

## [0.0.1.7] - 2026-05-19

### Player Changes / 玩家更新內容

- Fixed a critical bug where the Solar Mana Collector was never generating mana regardless of placement.
- 修正太陽魔力收集器在任何情況下都無法發電的致命錯誤。
- Fixed Mana Grinder byproducts silently disappearing when output slots were full.
- 修正研磨機輸出槽滿時副產物靜默消失的問題，現在副產物在無空間時不會強行插入。
- Fixed Mana Grinder with max efficiency upgrades being able to craft items for free (integer division rounding to zero).
- 修正研磨機效率升級滿級時因整數除法歸零而免費製作物品的漏洞。
- Fixed a bug where a fully upgraded Mana Infuser with max efficiency upgrades could craft items for free instead of consuming mana.
- 修正 Mana Infuser 在效率升級滿級時可免費製作物品的漏洞，現在至少消耗 1 點魔力。
- Fixed a bug where fuel items without an explicit generation interval in the data pack would generate mana every single tick instead of at the correct rate.
- 修正燃料資料包未設定生成間隔時每 tick 都產出魔力的漏洞，現在最短間隔為 1 tick。
- Altar upgrade tier is now saved when the altar core is broken and silently restored when rebuilt at the same position, provided the resonance rings are still in place.
- 祭壇核心被拆除時現在會記錄升級層數，在相同位置重建核心且共鳴環仍在位時靜默還原，無需重新播放升級動畫。
- Fixed several cases where corrupt or missing NBT data could crash the game when loading a world or opening a machine's interface.
- 修正多處損壞或缺失 NBT 資料可能導致載入世界或開啟機器介面時 crash 的問題。

- T4 and T5 altar upgrades now feature a full second-stage animation: ground shockwaves with spatial twist, screen fade to black with inward collapse wave, an expanding orbital sphere, four corner pillars plus a center pillar shooting skyward, four large suns rising from each pillar (each splitting into 4 small suns for 16 total), the 16 small suns orbiting and descending into a ring formation, then rapidly contracting as a massive skyward pillar engulfs the structure. A chime and the "The Grand Finale Begins!" challenge advancement appear at the end.
- T4 和 T5 祭壇升級現在有完整的第二階段動畫：地面衝擊波伴隨空間扭曲、黑幕淡入淡出附帶往內聚集波、膨脹球體 shader、四根角落柱子與中心核心各射出光柱、每根柱子冉冉升起一顆大太陽（各分裂出 4 顆小太陽共 16 顆）、16 顆小太陽旋轉降落並排成環狀陣形後急速收縮，同時一根巨大沖天柱包覆整個環狀結構，最終以叮聲和「好戲開場！」挑戰成就作結。

- Placing resonance rings on a T1-T3 altar now triggers an upgrade animation: three ground shockwave rings expand from the core, followed by a brief light pillar, ending with a chime sound.
- 對 T1-T3 祭壇放置共鳴環現在會觸發升級動畫：三段地面衝擊波環從核心向外擴散，接著短暫光柱，以叮聲結束。

- Advanced Arcane Conduit and Elite Arcane Conduit can no longer be crafted at a crafting table. They now require the Aspect Altar with Tier 1 and Tier 3 resonance rings respectively.
- 進階奧術導管和精英奧術導管不再能在合成台製作，現在分別需要 T1 和 T3 升級環的祭壇才能完成儀式。
- Altar upgrade rings now have a real mechanical effect: recipes can require a minimum ring tier to craft.
- 祭壇升級環現在有實際作用：配方可設定最低升級環層數需求，未達標時無法啟動儀式。
- JEI altar recipe panel now shows the required ring tier when a recipe needs one.
- JEI 祭壇配方面板在配方有 tier 需求時會顯示「需要第 X 層升級環」。
- Consensus Glasses redesigned: first use binds the item to the holder, snapshotting their research and aspect data. Subsequent use by other players copies the stored knowledge to them. Use by the bound player shows an "already bound" message.
- 共識眼鏡重新設計：第一次右鍵使用時，物品會綁定到該玩家並快照其研究進度與本源資料。其他玩家右鍵使用時，會將儲存的資料複製到自己；已綁定玩家再次使用時，顯示提示訊息。
- Source Tome now also grants all aspects (minimum 500 per aspect) in addition to unlocking all research.
- 源典現在在解鎖所有研究的同時，也會解鎖所有本源知識（每種本源至少 500 點）。

- Fixed guide descriptions incorrectly stating the altar consumes aspect points. The altar consumes mana and pedestal materials only.
- 修正指引文字錯誤描述祭壇消耗本源點數的說法，祭壇實際只消耗魔力和底座材料。
- Fixed Mana Pickaxe tooltip not specifying that chain mining only triggers on ore blocks.
- 修正魔力鎬 tooltip 未說明連鎖挖掘只對礦石方塊有效。
- Fixed research overview guide incorrectly stating a single click receives a research note. Two clicks are required.
- 修正研究概覽指引說「一次點擊即可取得研究筆記」的錯誤說法，實際需要點擊兩次。
- Fixed Source Tome item tooltip and activated message still referencing the old `source_codex` lang keys, causing raw key strings to display in-game.
- 修正源典（Source Tome）物品 tooltip 和激活訊息仍使用舊的 `source_codex` 語言鍵，導致遊戲內顯示原始鍵名的問題。

### Developer Notes / 開發者備註

- `ManaInfuserBlockEntity`: `canGenerate()` and `completeInfusion()` now use `Math.max(1, manaCost / getEfficiencyMultiplier())` to prevent integer division yielding 0 and granting free crafts.
- `ManaGenFuelRateLoader`: `DEFAULT_INTERVAL` changed from 0 to 1; `getIntervalTick()` returns `Math.max(1, intervalTick)` to prevent per-tick over-production; `warnedAlready` is now cleared on each data-pack reload.
- `AltarTierSavedData`: added `peekTier()` / `clearTier()` pair; `onRemove()` in `AspectAltarBlock` saves tier on core removal; `refreshUpgradeTier()` silently restores the tier without triggering an animation packet, then returns early so the normal upgrade path handles any extra rings on the next interval.
- Fixed multiple `ResourceLocation.parse()` calls in NBT load paths that could throw `ResourceLocationException` on corrupt saves; all changed to `tryParse()` with null-skip. Affected: `WorldAspectSavedData`, `ResearchAspectPlacePacket`, `ResearchTableBlockEntity`, `ResearchScreen`, `ResearchAspectPlacePacket`.
- `AbstractManaMachineEntityBlock.loadAdditional`: wrapped all `tag.getInt()` calls with `tag.contains()` guards so fields absent from older saves are not silently reset to zero.
- `ModNeoNalaEnergyStorage.deserializeNBT`: wrapped `new BigDecimal(tag.getString(...))` in `tag.contains` + try/catch to prevent `NumberFormatException` crash on corrupt saves.
- All server-side static Maps/Sets now cleared in `NaraServerEvents.onServerStopping`: `NaraServerEvents` (6 collections), `PlayerItemProtectionEvents.SOULBOUND_STORAGE`, `ArcaneConduitBlockEntity` caches, `ArcaneConduitBlock.playerBuildingHistory`, `ResearchGate.tickCache`.
- All client-side static state now cleared in `ClientTickHandler.onClientLogOut`: `NaraTutorialFlow`, `NaraFirstLoginFlow.ignoreCount`, `AltarUpgradeAnimManager`, `AltarCameraController`, `ManaStrikeShaderRenderer`, `OrbitalTestShaderRenderer`, `ClientResearchCache`.
- Added `initFailed` flag to `ManaStrikeShaderRenderer` (and altar renderers previously) so a failed shader load does not retry every frame; also moved VAO/VBO creation inside the try/catch block for all 5 altar renderers and ManaStrikeShaderRenderer.
- `NaraSyncPacket`: removed bogus `registerToServer()` method that re-registered the packet as `playToClient` with an empty handler, which caused duplicate-type registration on dedicated servers.
- `ResearchGate.hasCompleted()`: added per-server-tick result cache (`tickCache`) so 20+ machines calling `canOperate` in the same tick share a single `SavedData` lookup.
- `ManaCraftingTableBlockEntity.setChanged()` override that called `sendBlockUpdated` on every tick has been removed.
- `AspectAltarBlockEntity`: removed `syncToClient()` from the `completionAnimTick` countdown and throttled `tickRitual()` sync to every 10 ticks.

- `AltarFadeRenderer`: new `@EventBusSubscriber` class (client-only). Hooks `RenderLevelStageEvent.AFTER_LEVEL` to render a full-screen black overlay using `altar_fade.vsh/fsh` and `AltarUpgradeAnimManager.getScreenFadeAlpha()`. Handles T4+ screen blackout (60-270t).
- `AltarUpgradeAnimManager`: added `getScreenFadeAlpha()` returning 0-1 for T4+ tiers; added orbital shader trigger at tick 150 (ORBITAL_SPHERE_SATS mode); added `OrbitalTestShaderRenderer` and `Vec3` imports.
- `AspectAltarRenderer.renderT4T5Upgrade()`: five staggered ground ring waves (0-130t, radius 18) and a final tall pillar (500-580t, height 30). Dispatched from `renderUpgradeAnimation()` for tier 4-5.
- `AspectAltarBlockEntity.refreshUpgradeTier()`: grants `altar_upgrade_t5` advancement to all online players in the level when `newTier >= 5`.
- Added `data/koniava/advancement/altar_upgrade_t5.json` (challenge frame, impossible trigger, server-side awarded).

- `AltarRecipe`: added `minTier` field (int, default 0). JSON field `min_tier` is optional for backward compatibility.
- `AspectAltarBlockEntity.findMatchingRecipe()`: switched from `getRecipeFor()` to `getAllRecipesFor()` stream with tier filter (`h.value().getMinTier() <= upgradeTier`).
- `AltarRecipeProvider.save()`: added overload accepting `minTier` parameter.
- `AltarRecipeCategory`: HEIGHT increased to 82; draws tier requirement line when `minTier > 0`.

- T6 altar upgrade animation camera controller reworked: pitch-only control (no yaw lock), smooth tilt to sky during climax, tracks magic circle descent to horizon, then stamps `setXRot(0)` at release so camera stays at horizon instead of snapping back to pre-animation view.
- T4/T5/T6 screen fade base alpha raised from 0.25 to 0.9 so the world stays dark throughout the orb animation, not just during the initial flash peak.
- T6 prefix (T4-T5 replay) no longer fades back to bright before the climax begins; the base darkness carries through seamlessly and the climax ramps from 0.9 to 0.94.
- `AltarFadeRenderer`: replaced VBO quad with `gl_VertexID` fullscreen triangle in `altar_fade.vsh`, removing vertex attribute location ambiguity that prevented the black overlay from rendering.
- Magic circle: rotation, descent, and radius shrink now all run simultaneously from 870t; rotation extends to 1020t for 3 full turns total.
- Magic circle shader: removed upward-ray-only restriction and geometry depth occlusion so the disc is visible from above and when overlapping the altar structure.

**Security / Packet validation (2026-05-19 sweep)**
- All server-bound packets that modify world state now validate that the sending player is within 8 blocks of the target block: `SetDeployerIntervalPacket`, `ToggleDeployerEnabledPacket`, `PriorityUpdatePacket`, `ResetPrioritiesPacket`, `ResearchCompletePacket`, `ResearchAspectPlacePacket`, `AspectSynthesisPacket`.
- `ResearchCompletePacket`: added `isAvailableTo(knowledge)` prerequisite check so players cannot complete research they have not unlocked.
- `ResearchAspectPlacePacket`: validates `packet.researchId()` against `table.getCurrentResearchId()` (the actual note in the slot) to prevent forged IDs from corrupting another player's puzzle.
- `AspectSynthesisPacket`: when `aspect1 == aspect2`, now requires count >= 2 instead of 1 before consuming.
- `NaraCreeperPunishPacket`: skips if player is already bound to Nara, preventing Warden spawn spam.
- `NaraBindRequestPacket`: skips entire bind flow if player is already bound, preventing firework spam.

**Research system**
- `CompletedResearchItem.use()`: scroll now consumed in both the first-time and already-completed branches (was not consumed in the else branch).
- `ResearchGate.hasCompleted()`: tick cache key now includes `dimension.location()` prefix to prevent cross-dimension result pollution in multi-world setups.
- `ResearchTableMenu.stillValid()`: added null guard on `blockEntity.getLevel()`.
- `ResearchScreen.init()`: wrapped `Integer.parseInt()` on `savedPlacements` keys in try/catch to prevent crash on corrupt NBT.

**Mana systems**
- `ManaStorage.setMana()`: clamps to `[0, capacity]` (was only upper-bounded; negative values were possible).
- `ManaStorage.getFillRatio()`: returns 0f when capacity is 0 (was NaN from divide-by-zero).
- `ManaStorage.deserializeNBT()`: clamps loaded mana to `[0, capacity]` so corrupt saves cannot produce invalid state.
- `ManaGeneratorNbtManager`: removed duplicate mana/energy serialization; base class `AbstractManaMachineEntityBlock` already handles `"ManaStorage"`/`"EnergyStorage"` keys. Backward-compat fallback retained for saves that only have legacy `"Mana"`/`"Energy"` keys.
- `ManaGeneratorMenu` fuel slot: `mayPlace()` now calls `fuelLogic.isValidFuel(stack)` instead of returning `true` unconditionally.
- `ManaGrinderBlockEntity.finishGrinding()`: byproduct insertion now simulates first; if no output slot can fit the item, the byproduct is skipped instead of being silently discarded mid-insertion.
- `ManaGrinderBlockEntity`: efficiency multiplier division uses `Math.max(1, ...)` to prevent free crafting.
- `ManaGrinderMenu.stillValid()`: added proper 8-block distance check (was always `true`).
- `SolarManaCollectorBlockEntity.isOpenToSky()`: fixed `canSeeSkyFromBelowWater()` -> `canSeeSky()` (critical: previous implementation always returned false outside of underwater swimming context).
- `ManaInfuserBlockEntity.completeInfusion()`: added `setChanged()` after `currentOutput.grow()` to ensure the incremented count is saved to NBT.

**Conduit / VirtualNetwork**
- `VirtualNetwork.addConduit()`: added early-return if conduit is already in `connectedConduits`, preventing capacity inflation when a conduit is joined to the same network more than once.
- `ArcaneConduitBlockEntity.restoreVirtualNetworkData()`: switched from master-only restore to take-max strategy. Any conduit whose `savedMana` exceeds the current network mana now updates the pool, fixing truncation when chunks load in arbitrary order and the first conduit's partial capacity caps the restore value.

**Altar**
- `AspectAltarBlockEntity.scanForPedestals()`: the `removeIf` callback now also clears `centerPedestal` when the removed pedestal is the center one, preventing stale `BlockEntity` references.

**Nara system**
- `PlayerLoginEvent`: `NaraSyncPacket` and `KnowledgeSyncPacket` now sent before the `showIntroAnimation` config check so reconnecting players always receive correct bind and research state.
- `NaraServerEvents`: added `onPlayerLogout` handler that removes per-player state (`pendingPunishmentDialogue`, `naraPunishmentActive`, `awaitingRespawn`, `pendingResearchTableTutorial`, `tutorialLoginDelay`) to prevent stale state on reconnect.
- `NaraDialogueManager.close()`: now resets `choiceTimerTicks` and `selectedChoiceIndex` so a forced-closed dialogue does not leave stale timer/selection state for the next dialogue.

**JEI**
- `AspectSynthesisRecipeCategory.setRecipe()`: removed duplicate output ingredient registration (output aspect and token were each added twice).

**Misc**
- `CommandRegistrationHandler`: added missing `modid = KoniavacraftMod.MOD_ID` to `@EventBusSubscriber`.

---

## [0.0.1.6-6] - 2026-05-18

### Player Changes / 玩家更新內容

- Fixed crash on the main menu introduced in 0.0.1.6-5.
- 修正 0.0.1.6-5 引入的主選單 crash。
- First Nara binding now announces to all players in chat, letting others know a new player has arrived.
- 首次綁定娜拉現在會廣播到所有玩家的聊天室，讓老玩家知道有新人加入。

### Developer Notes / 開發者備註

- `NaraDialogueOverlay.onClientTick()`: added `mc.level == null || mc.player == null` guard at method entry. `mc.level` is null on the main menu, causing NPE when accessing `mc.level.random`.

---

## [0.0.1.6-5] - 2026-05-18

### Player Changes / 玩家更新內容

- Completing an altar ritual now triggers a visual performance: a burst glow at the altar center and a blue light pillar shooting into the sky (T3+). A completion chime plays on finish.
- 完成祭壇儀式後現在會播放一場視覺演出：核心爆發輝光、藍色光柱衝天（T3 以上），完成時播放叮聲。
- The performance duration scales with the recipe's processing time (320 to 700 ticks).
- 演出持續時間依配方處理時間縮放（320 至 700 tick）。
- Added "First Altar Ritual!" challenge advancement granted on first ritual completion.
- 新增「第一次完成祭壇合成!」挑戰成就，首次完成祭壇儀式時授予。
- Fixed: Completing an altar ritual and re-entering the world would incorrectly replay the completion animation.
- 修正：完成祭壇儀式後重進世界，完成動畫會不正確地重新播放。
- First-time login now triggers a visual novel-style dialogue with Nara (the mod's guide character). She introduces herself and explains the Holographic Watch.
- 首次登入會觸發娜拉（模組引導角色）的視覺小說風格對話，介紹自己並說明全息手錶的用途。
- Ignoring Nara's dialogue repeatedly will summon a Warden that instantly attacks. After dying and respawning, Nara will demand an apology before letting you go.
- 反覆無視娜拉的對話會召喚監守者瞬間攻擊。死亡重生後娜拉會要求道歉才肯放人。
- Completing the first watch binding now triggers welcome fireworks.
- 完成首次手錶綁定後會觸發歡迎煙火。
- Added three hidden advancements related to Nara's introduction: completing the binding, ignoring her, and making her angry.
- 新增三個與娜拉介紹相關的隱藏成就：完成綁定、無視娜拉、讓娜拉生氣。
- Added `/koniava nara replay` command to replay the Nara introduction dialogue.
- 新增 `/koniava nara replay` 指令，可重新播放娜拉介紹對話。
- Nara now appears after you craft the Research Table and close the crafting GUI, instead of when you first open the table.
- 娜拉現在會在你合成出研究台並關閉合成介面後才出現說話，而非第一次開啟研究台時。
- Nara now appears with a tutorial dialogue the first time you open the Research Table, explaining how to use notes and quills.
- 娜拉會在你第一次開啟研究台時跳出來說明操作方式（只出現一次）。
- Completing a research entry now unlocks its associated crafting recipes in the recipe book.
- 完成研究時，研究條目指定的配方會自動解鎖並顯示於配方書中。
- Added `/koniava research unlock_all <targets>` command to instantly unlock all research entries for target players.
- 新增指令 `/koniava research unlock_all <目標>`，可一次解鎖目標玩家的所有研究。
- Scanner (Nara Watch) can now scan other players. Each player has a unique stable aspect fingerprint of 6 to 8 aspects drawn from all compound aspects.
- High-level compound aspects (e.g. Cognition, Wisdom, Commerce, Sensus) now have a chance to appear when scanning items and entities that logically connect to them.
- 娜拉手錶現可掃描其他玩家。每位玩家擁有獨特穩定的本源組合，從全部複合本源中隨機抽取 6 至 8 個。
- 高階複合本源（如知識、知慧、感知、交易等）現在會根據掃描目標的本源鏈出現，不再永遠掃不出來。
- Fixed: Breaking a pillar of a formed altar no longer keeps the structure in the formed state. The altar now correctly reverts when any structural block is removed.
- 修正：打掉已成形祭壇的矩陣柱子，結構現在會正確解散並恢復成魔力磚狀態。
- Mana Deployer GUI now has an ON/OFF toggle button; the machine stops working and freezes its animation when disabled.
- 魔力部署器 GUI 新增開關按鈕，關閉時機器停止運作且動畫也會停止。
- Redstone pulse (rising edge) toggles the Mana Deployer on/off, like the Copper Bulb mechanic.
- 紅石脈衝（上升沿）可切換魔力部署器開關，與銅燈機制相同。
- Mana Deployer speed is now a free-input field (10 to 12000 ticks) instead of preset cycles.
- 魔力部署器速度改為自由輸入欄位（10 至 12000 tick），不再是固定預設值切換。
- Right-clicking the Mana Deployer with an item in hand no longer auto-inserts the item; use the GUI slot instead.
- 持物右鍵魔力部署器不再自動插入物品，請透過 GUI 欄位操作。
- Mana Deployer now has a crafting recipe: 4 Mana Ingots + 2 Iron Ingots + 2 Basic Arcane Conduits + 1 Dispenser.
- 魔力部署器現在有合成配方：4 魔力錠 + 2 鐵錠 + 2 基礎奧術導管 + 1 投放器。
- Fixed Mana Deployer 3D model rendering: the model now displays correctly without mirroring or rotation errors.
- 修正魔力部署器 3D 模型渲染：模型不再出現鏡像或旋轉錯誤。

### Developer Notes / 開發者備註

- `AspectAltarBlockEntity`: added `completionAnimTick` + `completionDuration` (320 to 700 ticks); `completeRitual()` sets duration and tick, grants `first_altar_ritual` advancement to activator; `tick()` decrements and syncs while > 0. Fields NOT in `saveAdditional` (prevents replay on reload); added to `getUpdateTag()` only for server-to-client sync; `loadAdditional()` reads them (gets 0 from disk, correct value from sync packet).
- `AspectAltarRenderer`: `renderCompletionShow()` with tier gates (T1+ billboard burst, T3+ blue pillar); `renderBluePillar()` 4-layer glow cylinder; `renderPillarCylinder()` with bottom/top alpha fade.
- `tryActivate()` now accepts `UUID playerUUID`; `activatorUUID` stored for advancement grant in `completeRitual()`.
- `first_altar_ritual` advancement (`data/koniava/advancement/first_altar_ritual.json`): `minecraft:impossible` trigger, granted programmatically via `PlayerAdvancements.award()`.
- Completion sound changed to `EXPERIENCE_ORB_PICKUP` (pitch 1.2).
- Nara dialogue system: `NaraDialogueManager` (state machine: INACTIVE/TYPING/WAITING/CHOOSING), `NaraDialogueOverlay` (HUD with portrait reveal animation, typewriter text, scrollable choices, timeout progress bar), `NaraFirstLoginFlow` (dialogue script with escalating timeouts 200→40 ticks over 10 ignores).
- Network packets: `NaraStartDialoguePacket` + inner `Punishment` record, `NaraCloseDialoguePacket` (server→client on death), `NaraCreeperPunishPacket`, `NaraAngryPacket` — all registered in `ModNetworking`.
- `NaraServerEvents`: `pendingPunishmentDialogue` countdown map, `awaitingRespawn` set (death during punishment), `naraPunishmentActive` set, `pendingWardenDespawn` map (Warden discarded after 80 ticks).
- Punishment mob changed from Creeper to Warden: spawns at player position, `increaseAngerAt(player, 150)`, immediately calls `player.hurt(mobAttack, dmg)` for instant damage, despawns after 80 ticks.
- All HUD text uses lang keys: `nara.hud.hint.*`, `nara.hud.name`, `nara.hud.name.unknown`; `displayName` is now `Component` (not raw `String`).
- Advancements in `data/koniava/advancement/`: `nara_welcome` (root), `nara_ignored` (hidden child), `nara_angry` (hidden child).
- Moved research table tutorial trigger from `ResearchTableBlock.useWithoutItem()` to `NaraServerEvents`.
- `NaraServerEvents`: added `onItemCrafted()` — detects research_table craft, adds UUID to `pendingResearchTableTutorial` set if tutorial not yet seen. `onServerTick()` checks pending players; fires when `player.containerMenu == player.inventoryMenu`.
- `PlayerKnowledge`: added `seenTutorials Set<String>` with `hasSeenTutorial/markTutorialSeen` + save/load under "SeenTutorials" NBT tag.
- `NaraTutorialPacket` (new): server→client packet carrying `tutorialId` string, routes to `NaraTutorialFlow.start()`.
- `NaraTutorialFlow` (new): client-side static class mapping `tutorialId` to dialogue lines. `RESEARCH_TABLE = "research_table"`.
- `CompletedResearchItem.use()`: on `isNew`, calls `sp.awardRecipes()` with `RecipeHolder` list resolved from `ResearchTemplate.getUnlockedRecipes()`.
- `ResearchCommand.unlockResearch()/unlockAllResearch()`: same recipe grant logic added for cheat commands.
- `ResearchCommand`: added `unlock_all` subcommand — iterates `ResearchRegistry.all()`, calls `completeResearch()` on each; lang keys added to `en_us.json` and `zh_tw.json`.
- `ResearchRegistry.MANA_BASICS`: fixed `unlocks()` pointing to removed `mana_scanner`; now points to `nara_watch`.
- `EntityAspectResolver.resolve()`: detect `Player` before cache lookup and delegate to `resolvePlayer()`. Player aspects bypass entity-type cache entirely (UUID-based scan id).
- `EntityAspectResolver.resolvePlayer()`: draws from all non-primary aspects. Capacity 6–8 determined by UUID bit hash for per-player stability. `scanId = koniava:player_{uuidHex}` so same player always produces same aspects per world seed.
- `BaseMaterialRegistry`: added `expandCandidatePool()` — three-pass `getLogicalPool` expansion reaching Tier-C/D aspects such as `SENSUS`, `WISDOM`, `LANGUAGE`, `COMMERCE` that were previously unreachable.
- `NaraWatchItem`: removed `instanceof Player` skip in `getEntityTarget()`; added `PlayerTarget ScanTarget` record with UUID-based `ResourceLocation` id; `scanHeader()` override shows player name in discovery message.
- `AspectAltarBlockEntity.tick()`: call `checkStructure()` first in the `CHECK_INTERVAL` branch when formed, before `scanForPedestals()`. Breaking a pillar now triggers disband within 40 ticks.
- `ManaDeployerBlockEntity`: removed `SPEED_PRESETS/cycleSpeed()`; added `enabled`, `wasRedstonePowered` fields; `setIntervalTick()` clamps [10, 12000]; `toggleEnabled()`; `onNeighborChanged()` for redstone edge detection; `ContainerData` now has 5 slots (index 4 = enabled).
- `ManaDeployerBlock`: added `neighborChanged()` to delegate redstone signals to `BlockEntity`.
- `ManaDeployerScreen`: replaced speed-cycle click with vanilla `EditBox`; added ON/OFF button widget; override `containerTick()` to sync box when unfocused.
- Replaced `CycleDeployerSpeedPacket` with `SetDeployerIntervalPacket` + `ToggleDeployerEnabledPacket`.
- `ManaDeployerRenderer`: corrected Geckolib coordinate convention (negate X for pivot/pos, ZYX rotation order, X-mirror for vertices).
- `ModBlockStateProvider`: replaced static blockstate/model JSONs with datagen; extracted `createHorizontalFacingVariants(Block, ModelFile)` helper.
- `ModRenderLayers`: fixed `RegisterClientExtensionsEvent` import (`client.extensions.common`, not `client.event`).
- `ModRecipeProvider`: added shaped recipe for `mana_deployer` (MIM/CDC/MIM).
- Redesigned `basic_mana_circuit` texture: dark board with dithered surface, teal circuit lines, purple nodes, and a 2x2 crystal core (top-left bright, bottom-right shadow for gem depth).
- `ModCreativeModTabs`: added `koniava_dev_tab`; introduced `DEV_ITEM_PATHS` Set (`mana_debug_tool`, `dev_render_test_1`) to exclude dev items from `koniava_items_tab` and show them only in the new tab.
- `DevRenderTestItem`: added `appendHoverText()` with two tooltip lines (usage hint + orbital-railgun prototype note); both lang files updated.
- New WIP shader files: `ClientEffectEvents`, `ManaStrikeShaderRenderer`, `OrbitalTestShaderRenderer` (orbital-railgun prototype reference, not yet production-ready).

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
- JEI structure guide now shows a ghost overlay of the altar structure directly in the world. Blocked positions show red outlines; correctly placed blocks fade out; the overlay auto-closes when the altar finishes forming.
- JEI 結構指引現在可在世界中直接顯示幽靈結構投影。被方塊佔據的位置顯示紅色輪廓；已正確放置的方塊自動淡出；祭壇成形後投影自動關閉。
- Key bindings category renamed to "Koniavacraft-娜拉工藝" in Controls settings.
- 控制設定中的按鍵分類更名為「Koniavacraft-娜拉工藝」。

### Developer Notes / 開發者備註

- Registered 5 new items: mana_substrate, mana_wire, mana_adhesive, mana_wafer, basic_mana_circuit with textures and auto item model generation.
- Added 3 ManaGrinder recipes (mana_substrate, mana_wire×4, mana_adhesive×3), 1 AltarRecipe (mana_wafer×2), 1 ManaCrafting shaped recipe (basic_mana_circuit) for the T1 circuit chain.
- ManaGrinderMenu / ManaInfuserMenu: added getBlockEntityPos(); ManaCraftingScreen: null guard on getBlockEntity().
- AspectPedestalBlockEntity.getItemHandler(): returns an IItemHandler wrapping the single heldItem slot (accepts 1 item, ejects on extract). Registered as Capabilities.ItemHandler.BLOCK in ModCapabilities.
- AltarMultiblockCategory: full rewrite — replaced 7-tab system with SliderWidget (0–6 snap positions), cumulative render map (base + rings 0..tier-1), InventoryButtonWidget (one-shot inventory scan → cachedCounts[], countColor()), addTooltipCallback on each JEI slot for need/have/missing lines, auto-scale on tier change.
- Surface rule fix: shallow mana soil uses UNDER_FLOOR (stoneDepthCheck default 0–5 blocks), deep mana soil uses not(UNDER_FLOOR) within stoneDepthCheck(0, true, 4, FLOOR). BiomeTerrainRegistration deepSoilThreshold changed from 20 → 4.
- GhostProjectionHandler: PLACING/PLACED mode distinction for conflict detection; REACH_CLOSE=6.0 (Shift); G key (ModKeyMappings.GHOST_LOCK) to toggle lock/unlock; auto-deactivate via isFormed() check in onLevelTick; removed all mouse/key interception. GhostProjectionState: added unlock(), deactivate() now resets origin to ZERO.
- GhostProjectionStateTest: replaced Blocks.* with raw Map + Object placeholders (MC bootstrap unavailable in pure JUnit); added deactivateResetsOrigin() test.

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
- Altar now outputs crafted results to adjacent containers (hoppers, chests, etc.) before dropping on the ground.
- 祭壇儀式完成後，結果物品現在會優先輸出至相鄰容器（漏斗、箱子等），無法輸出時才掉落在地。
- Resonance Ring no longer appears in the creative mode block tab (it is an internal structure component placed via the Structure Build Wand).
- 共鳴環不再出現在創意模式方塊欄（屬於內部結構零件，請使用結構建造法杖放置）。

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
- `AspectAltarBlockEntity`: ritual completion now checks adjacent block entities for `Capabilities.ItemHandler.BLOCK` and inserts result before falling back to dropping above altar.
- `ModCreativeModTabs`: `resonance_ring` added to path exclusion list in `koniava_blocks_tab`.

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

- `BaseMaterialRegistry`: ~80 new `atom()` entries for all 48 new aspects; `getLogicalPool()` extended with inference chains across all tiers.
- `BlockAspectResolver`: added tag/keyword matching for frost, soul, death, undead, machine, void, poison categories.
- `EntityAspectResolver`: full rewrite with 60+ specific entity mappings via switch statement; specific mapping takes priority over tag-based fallback; boss capacity scales to 6 (dragon/wither), 5 (elder_guardian/warden), 4 (evoker/witch/allay).
- `AspectSynthesisScreen` and `ResearchScreen`: `isOverPaletteArea()` now uses fixed `PALETTE_ROWS` height instead of current-page row count — fixes scroll deadzone on last page.
- `ModItemModelProvider`: added `item/generated` model entry for `completed_research` using `koniava:item/completed_research` texture.

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