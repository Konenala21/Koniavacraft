# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Player Changes / 玩家更新內容

The Moon is now a landable low-gravity world. It is solid all the way down: moon dust on top, moon stone and deepstone below, and a glowing core layer at the bottom. Movement has a floaty low-gravity feel and falls deal greatly reduced damage.
月球現在是可降落的低重力世界。它從上到下都是實心的：表層月壤、底下月岩與深層月岩，最底部是一層發光核心。移動有低重力的飄飄感，摔落傷害也大幅降低。

Saturn, Uranus, and Neptune now have visible rings. Saturn has detailed rings using Solar System Scope textures with the Cassini Division clearly visible. Uranus has 9 narrow blue-gray rings with dominant epsilon ring tilted at 97.8° (looks like a standing wheel). Neptune has 5 dark reddish rings with two bright arc clumps on the Adams ring (Liberté and Égalité arcs). All planets now orbit at realistic proportions: Earth completes one orbit per in-game year (365 days), with other planets scaled accordingly. Titan orbits Saturn and the Moon orbits Earth, and both are properly occluded when passing behind their parent planet instead of visually popping. Rain and weather effects no longer appear in the space dimension. Planet textures now display correctly in all lighting conditions including the very dark night side. The space dimension no longer crashes AMD GPUs (RX 6600 / driver 26.x) on F3+T resource reload.
土星、天王星和海王星現在有可見的行星環。土星使用 Solar System Scope 真實貼圖，卡西尼環縫清晰可見。天王星有 9 條細藍灰環加上明顯的 ε 環，傾斜達 97.8°（外觀像直立的輪子）。海王星有 5 條暗紅環，亞當斯環上有兩個弧狀亮塊（Liberté 和 Égalité 弧）。所有行星現在有現實比例的軌道：地球每遊戲年（365 天）公轉一圈。土衛六繞土星、月球繞地球，兩者在父行星後方時正確消失。太空維度不再下雨。行星貼圖在所有光照條件下（包括極暗的夜側）都能正確顯示。AMD GPU（RX 6600 / driver 26.x）F3+T 重載不再崩潰。

Added a space dimension with a procedural starfield sky, physically-based planet rendering, and a full solar system (Mercury, Venus, Moon, Mars, Jupiter, Saturn, Titan, Uranus, Neptune, Pluto) plus a stub Alpha Centauri binary system. Planets are rendered as real 3D spheres whose apparent size scales with your distance. The Sun is visible with a corona glow. Players spawn near Earth's orbital position when entering the dimension.
新增太空維度，包含程序生成的星空天空、物理正確的行星渲染，以及完整太陽系（水星、金星、月球、火星、木星、土星、土衛六、天王星、海王星、冥王星）和比鄰星系雛形（雙星系統）。行星為真實 3D 球體，視角大小隨距離動態縮放。太陽有日冕光暈。進入維度時玩家會出現在地球軌道附近。

### Developer Notes / 開發者備註

Spaceship hitbox and ship-vs-terrain collision (collision stage 1 of 2). Hitbox: ShipEntity overrides getDimensions to a box covering the contraption (getBoundingBox is final, so the picking box is driven by dimensions), and calls refreshDimensions whenever the contraption is set (setContraption / readSpawnData / load), so right-clicking anywhere on the ship now boards it. The dimensions box is symmetric around the core corner and feet-up, so it is slightly larger than the ship and does not cover blocks below the core, which is fine for picking. Terrain collision: ShipEntity.tick now resolves the movement per axis against the world before setPos, zeroing any axis where a ship block would enter a solid block (level.noCollision per block), so the ship stops at walls and the ground and slides along them instead of phasing through; blocked-axis momentum is dropped. Ship blocks are not in the world (they live in the entity) so the ship never collides with itself. A GameTest drives a ship into a wall and asserts it does not pass through. Stage 2 (entities standing/walking on the ship deck) is the hard Create-collider piece and is still to come.
飛船 hitbox 與船撞地形（碰撞兩階段的第一階段）。Hitbox：ShipEntity 覆寫 getDimensions 成涵蓋 contraption 的盒（getBoundingBox 是 final，所以 picking 盒由 dimensions 決定），並在設定 contraption 時（setContraption / readSpawnData / 載入）呼叫 refreshDimensions，所以現在對船身任何地方右鍵都能上船。dimensions 盒以核心角落為中心、腳底往上，故略大於船且不涵蓋核心下方的方塊，picking 可接受。地形碰撞：ShipEntity.tick 現在在 setPos 前逐軸對世界解算移動，任何會讓船方塊卡進固體方塊的軸歸零（每方塊 level.noCollision），船因此撞到牆/地面會停下並沿牆滑，不再穿過去；撞到的軸不保留動量。船方塊不在世界裡（在 entity），所以船不會撞到自己。GameTest 把船開向一道牆並驗它沒穿過去。第二階段（實體站在/走在船甲板上）是 Create collider 的硬骨頭，還沒做。

Spaceship boarding fix, VBO bake, and block textures. Boarding: ShipEntity now overrides isPickable (default Entity.isPickable is false, so the ship could not be right-clicked at all and interact never fired), and the board check fills any free seat (getPassengers < getSeats) instead of only when there is no driver, so passengers can actually board (right-click the core; the interaction hitbox is still the 1x1 core for now). Added the entity.koniava.ship lang key (en/zh). VBO bake-once: ShipMeshCache bakes all static MODEL blocks into one VertexBuffer per chunk render layer via the fake world, then each frame just draws them with the entity model-view matrix and the layer chunk shader (RenderType.setupRenderState + drawWithShader), so the static geometry is no longer re-tesselated every frame. The cache is stored on the entity (typed Object to avoid loading the client class on the server), freed in ShipEntity.remove, and wrapped in try/catch that falls back to the per-frame tesselate path on any error. Block textures: added 16x16 industrial textures for ship_core (glowing core), ship_assembly_pad (control panel), ship_assembly_base (floor plate), ship_assembly_gantry (hazard beam), ship_seat (cushioned seat) via a /scripts python generator, plus blockWithItem (cubeAll) datagen entries; run runData to generate the models.
飛船登船修復、VBO 烤、方塊貼圖。登船：ShipEntity 覆寫 isPickable（Entity 預設回 false，飛船根本點不到、interact 不觸發），且登船判定改成「有空位就坐」（getPassengers < getSeats）而非「沒駕駛才上」，乘客才真的上得去（右鍵核心；互動 hitbox 目前仍是核心那 1x1）。補 entity.koniava.ship lang（中英）。VBO 烤一次：ShipMeshCache 透過假世界把所有靜態 MODEL 方塊烤進每個 chunk render layer 一個 VertexBuffer，之後每幀只用實體 model-view 矩陣 + layer chunk shader（RenderType.setupRenderState + drawWithShader）畫，靜態幾何不再每幀重 tesselate。快取存在實體上（型別 Object 避免 server 載 client 類別），ShipEntity.remove 時釋放，並用 try/catch 包住，出錯就退回每幀 tesselate。方塊貼圖：用 /scripts python 產生器加了 5 張 16x16 工業貼圖（ship_core 發光核心、ship_assembly_pad 控制面板、ship_assembly_base 地板、ship_assembly_gantry 警示樑、ship_seat 軟墊座椅），並加 blockWithItem(cubeAll) datagen；需跑 runData 生成模型。

Spaceship BlockEntity rendering: chests, signs and other blocks that need a BlockEntityRenderer now show up on a ship. ShipEntity.getRenderBlockEntities lazily rebuilds, once per entity (cached, cleared in setContraption), a Map of transient BlockEntity instances from the contraption blocks that are EntityBlocks with NBT (BlockEntity.loadStatic + setLevel to the real level so the BER can query time/light). ShipEntityRenderer then draws each via the BlockEntityRenderDispatcher, translated by localPos, alongside the static tesselateBlock pass. Ship chests render closed (no openers on a detached BE) which is fine. The transient BEs are cached so they are not rebuilt every frame; the per-frame cost is only the BER draw (which must run per frame for animation) plus the static-block tesselation (whose VBO bake-once caching is the next optimization).
飛船 BlockEntity 渲染：箱子、告示牌等需要 BlockEntityRenderer 的方塊現在會在飛船上顯示。ShipEntity.getRenderBlockEntities 每個實體只建一次（快取，setContraption 時清除）：把 contraption 裡是 EntityBlock 且有 NBT 的方塊用 BlockEntity.loadStatic 還原成臨時 BlockEntity 並 setLevel 到真實 level（讓 BER 能查時間/光照）。ShipEntityRenderer 再用 BlockEntityRenderDispatcher 逐個畫、translate(localPos)，與靜態 tesselateBlock pass 並行。飛船箱子渲染為關閉狀態（脫離的 BE 沒有開啟者）可接受。臨時 BE 已快取不每幀重建；每幀成本只剩 BER draw（動畫必須每幀）加靜態方塊 tesselation（其 VBO 烤一次快取是下一個優化）。

Spaceship seats and multiplayer riding: a new ShipSeatBlock is a normal ship block (captured by assembly, travels with the ship). After assembly the ship has a deterministic seat list = the core at local (0,0,0) as the driver/helm at index 0, then every ShipSeatBlock position sorted by (x,y,z). Right-clicking the ship seats the next rider (board order = passenger index = seat index, consistent on client and server without extra sync); canAddPassenger caps at the seat count. getControllingPassenger returns the index-0 rider, and only that driver controls: both the client ShipControlClientHandler (only sends if getControllingPassenger == local player) and the server ShipControlPacket handler (only applies if the sender is the controlling passenger) gate input, so passengers just ride along. positionRider places each passenger at their seat localPos. A GameTest assembles a core+1-seat ship, boards two mock players (third rejected), checks the first is the driver, and that the two map to different seat indices/positions. Also hardened the ship gametests against cross-test contamination from untracked entities: entity lookups now use a tight inflate(4) and filter ShipEntity instances by expected contraption size, since ships spawned via addFreshEntity in other tests are not cleaned by the framework and a wide search could pick up a neighbour test ship.
飛船座椅與多人搭乘：新的 ShipSeatBlock 是一般飛船方塊（會被組裝、跟船走）。組裝後飛船有確定性的座位清單＝核心在 local (0,0,0) 當駕駛/舵（index 0），其後是所有 ShipSeatBlock 位置依 (x,y,z) 排序。右鍵飛船把下一位乘客坐上（上船順序＝passenger index＝座位 index，client/server 一致免額外同步）；canAddPassenger 以座位數為上限。getControllingPassenger 回 index 0 那位，只有駕駛能控制：client 的 ShipControlClientHandler（只在 getControllingPassenger == 本地玩家時送）與 server 的 ShipControlPacket handler（只在送出者是駕駛時套用）雙邊 gate 輸入，乘客只是跟著飛。positionRider 把每位乘客放到各自座位 localPos。GameTest 組一艘 core+1 座椅的船，兩個 mock player 上船（第三被擋），驗第一個是駕駛、兩人對應不同座位 index/座標。另外強化飛船 gametest 對未追蹤實體的跨測試污染：實體查找改用緊湊 inflate(4) 並依預期 contraption 大小過濾 ShipEntity，因為別的測試用 addFreshEntity 生的船不被框架清理，搜尋範圍太寬會撈到鄰測試的船。

Spaceship M2.5 (better contraption rendering): the ship renderer now draws each block with ModelBlockRenderer.tesselateBlock against a fake world (ShipRenderWorld, a BlockAndTintGetter backed by the contraption block map) instead of renderSingleBlock. This gives neighbour face-culling (internal faces between ship blocks are skipped, far fewer triangles), ambient occlusion, and correct lighting that includes each block own light emission, which fixes glowing blocks (glowstone etc.) rendering dark after the ship became an entity. ShipRenderWorld returns sky light 15 and block light = max emission of the cell and its neighbours, and delegates getShade/getLightEngine to the real level. Still rebuilt per frame (no VBO bake yet); the true bake-once optimization for very large ships is deferred and intentionally avoids custom VBO/shader paths for AMD safety. Non-MODEL blocks (chests etc. needing a BER) are still skipped. Client-visual only.
飛船 M2.5（contraption 渲染改善）：渲染器改用 ModelBlockRenderer.tesselateBlock 搭配假世界（ShipRenderWorld，一個以 contraption 方塊表為底的 BlockAndTintGetter）逐方塊畫，取代 renderSingleBlock。多了鄰面剔除（飛船方塊間的內部面不畫，三角形大減）、AO、以及把每個方塊自身發光值算進光照，修復發光方塊（發光石等）變成實體後渲染變暗的問題。ShipRenderWorld 天光固定 15、方塊光取該格與鄰格的最大發光值，getShade/getLightEngine 委派真實 Level。仍每幀重建（未做 VBO 烤一次）；超大船的真正烤一次優化延後，且刻意不走自訂 VBO/shader 以策 AMD 安全。非 MODEL 方塊（箱子等需 BER）仍不畫。純 client 視覺。

Spaceship M4 (disassembly, work in progress): sneak + right-click the ship parks it back into world blocks (plain right-click still boards). ShipEntity.disassemble snaps the entity position to the grid and calls ShipContraption.addToWorld(target), which first checks every target cell is air or replaceable (aborts the whole write if any is blocked, with a dock-blocked message) then writes each block back and restores BlockEntity contents via loadWithComponents (re-adding x/y/z to the saved NBT), then discards the entity. Two GameTests cover it: disassemble returns the blocks and removes the entity, and a chest round-trip (assemble then disassemble) keeps its 5 diamonds, proving the UPDATE_SUPPRESS_DROPS + loadWithComponents path preserves container contents. Test isolation fix: the ship gametests now clear their build volume in setupBaseAndPad, because the disassembly tests write blocks via world.setBlock (real gameplay) which the gametest framework does not track/clean, so leftover blocks were contaminating later tests scan counts.
飛船 M4（收船，開發中）：潛行 + 右鍵飛船把它停回世界方塊（一般右鍵仍是上船）。ShipEntity.disassemble 把實體位置 snap 回整數格，呼叫 ShipContraption.addToWorld(target)：先檢查每個目標格都是空氣/可替換（有一格被擋就整批不寫並提示 dock-blocked），再把每塊寫回並用 loadWithComponents 還原 BlockEntity 內容（NBT 補回 x/y/z），最後 discard 實體。兩個 GameTest：收船把方塊還原且實體消失，以及箱子 round-trip（組裝再收船）保住 5 顆鑽石，證明 UPDATE_SUPPRESS_DROPS + loadWithComponents 能保留容器內容。測試隔離修正：飛船 gametest 現在在 setupBaseAndPad 先清空建造區，因為收船測試用 world.setBlock（真實遊戲行為）寫方塊，gametest 框架不追蹤清理，殘留方塊會污染後續測試的 scan 計數。

Spaceship M3 (6-direction flight, work in progress): right-clicking the ship boards it (interact -> startRiding); ShipEntity overrides getControllingPassenger/positionRider to seat the player on the core. While riding, a client tick handler (ShipControlClientHandler, Dist.CLIENT) reads the movement keys each tick and sends ShipControlPacket (C2S) with forward/strafe/vertical + the player yaw; the server ShipEntity.tick turns that into a target velocity (forward/strafe relative to look yaw, jump=up, sprint=down) and lerps the ship velocity toward it (ACCEL=0.1, MAX_SPEED=0.4) for inertia, moving via setPos with noPhysics (no collision yet, flies through terrain; Earth collision is M5). Shift stays vanilla dismount, so sprint is used for descend. With no controlling passenger the input is zeroed and velocity decays. An 8th GameTest boards a mock player, sets forward input, and asserts the ship flies +Z and the rider moves with it. Client-driven feel is in-game only.
飛船 M3（六方向飛行，開發中）：右鍵飛船上船（interact -> startRiding）；ShipEntity 覆寫 getControllingPassenger/positionRider 把玩家放在核心上。駕駛時 client tick handler（ShipControlClientHandler，Dist.CLIENT）每 tick 讀移動鍵送 ShipControlPacket（C2S）帶 forward/strafe/vertical + 玩家 yaw；server 端 ShipEntity.tick 轉成目標速度（前後左右相對視角 yaw、跳躍=上、疾跑=下）並 lerp 船速逼近（ACCEL=0.1、MAX_SPEED=0.4）做慣性，用 setPos + noPhysics 移動（暫無碰撞，會穿地形；地球碰撞是 M5）。Shift 保留 vanilla 下船，所以下降用疾跑。沒有駕駛者時輸入歸零、速度衰減。第 8 個 GameTest 讓 mock player 上船、設前進輸入，驗船往 +Z 飛且玩家跟著走。client 端手感只能實機。

Spaceship M2b (naive contraption rendering, work in progress): ShipEntityRenderer (client) draws the ship by iterating the contraption blocks and calling BlockRenderDispatcher.renderSingleBlock per block, translated by localPos. This avoids needing a VirtualRenderWorld for now: renderSingleBlock does no neighbour face-culling or AO and uses uniform lighting, which is the simplest path to actually see the ship (the baked-buffer + fake-world optimization is deferred to M2.5). The ShipEntity is now spawned at the core block corner (not +0.5) so translate(localPos) aligns with the original block positions. ShipEntity.getBoundingBoxForCulling is overridden to the contraption bounds so large ships are not frustum-culled when the 1x1 core leaves the screen. Non-MODEL blocks (chests etc. needing a BER) are skipped for now but their data stays in the contraption. Renderer registered for koniava:ship in ClientModBusSetup. Client-visual only, not gametestable.
飛船 M2b（naive contraption 渲染，開發中）：ShipEntityRenderer（client）逐 contraption 方塊呼叫 BlockRenderDispatcher.renderSingleBlock 畫，translate(localPos)。這樣暫時不需要 VirtualRenderWorld：renderSingleBlock 不做鄰面剔除/AO、用統一光照，是最快能真的看到船的路（烤 buffer + 假世界優化留 M2.5）。ShipEntity 現在生在核心方塊角落（非 +0.5），translate(localPos) 即與原方塊位置對齊。覆寫 ShipEntity.getBoundingBoxForCulling 用 contraption 範圍，避免大船在 1x1 核心移出畫面時被 frustum culling 剔掉。非 MODEL 方塊（箱子等需 BER）暫不畫，資料仍留 contraption。渲染器在 ClientModBusSetup 註冊 koniava:ship。純 client 視覺，不能 gametest。

Spaceship M2a (entity spawn, work in progress, ship still invisible): the assembly pad GUI now has an Assemble button (ShipAssemblePacket, C2S) that turns the scanned ship into a ShipEntity. ShipAssemblyPadBlockEntity.assembleShip recomputes the box (shared computeBox helper extracted from scan), finds the single core, runs ShipContraption.assemble, then removeFromWorld (sets the captured blocks to air with UPDATE_SUPPRESS_DROPS so containers keep their contents in the contraption NBT) and spawns a ShipEntity at the core position holding the contraption. ShipEntity (space/ship, registered koniava:ship, MISC) holds the ShipContraption, syncs it to clients on spawn via IEntityWithComplexSpawn (writeSpawnData/readSpawnData with the contraption NBT) and saves/loads it; tick is noGravity/noPhysics and stationary for now (M3 adds 6-direction flight). New status LAUNCHED. A 7th GameTest verifies assemble removes the world blocks and spawns a ShipEntity with the right contraption size (uses a sequence + 2-tick idle because addFreshEntity is processed next tick). Next: M2b render the contraption (naive per-block via a VirtualRenderWorld equivalent, then baked), M3 flight + ride, M4 disassembly.
飛船 M2a（生成實體，開發中，船還是隱形）：組裝台 GUI 加「組裝」按鈕（ShipAssemblePacket，C2S）把掃到的船變成 ShipEntity。ShipAssemblyPadBlockEntity.assembleShip 重算盒（從 scan 抽出共用 computeBox），找唯一核心，跑 ShipContraption.assemble，然後 removeFromWorld（用 UPDATE_SUPPRESS_DROPS 把方塊換空氣，箱子內容物留在 contraption NBT 裡）並在核心位置生成持有 contraption 的 ShipEntity。ShipEntity（space/ship，註冊 koniava:ship，MISC）持有 ShipContraption，spawn 時用 IEntityWithComplexSpawn（writeSpawnData/readSpawnData 帶 contraption NBT）同步到 client 並存讀；tick 目前 noGravity/noPhysics 不動（M3 才加六方向飛行）。新增狀態 LAUNCHED。第 7 個 GameTest 驗組裝後世界方塊消失 + 生成 ShipEntity 且 contraption 大小正確（用 sequence + idle 2 tick，因為 addFreshEntity 下一 tick 才處理）。後續：M2b 渲染 contraption（先 naive 逐方塊用 VirtualRenderWorld 等價物，後烤 buffer），M3 飛行+駕駛，M4 重組。

Spaceship contraption foundation (M1, work in progress, no player-facing feature yet): chose to build a custom Contraption inspired by Create (MIT) rather than depend on Create/Valkyrien Skies, since our case only needs a translating rigid body in the void (no rotation, no world collision in space). ShipContraption (space/ship) assembles a ship via a bounded flood-fill from a ShipCoreBlock anchor: BFS over 6-neighbours, capturing every connected movable block (skips air, LiquidBlock, unbreakable destroySpeed<0) up to MAX_BLOCKS=256, each stored as a StructureBlockInfo at localPos = pos - anchor plus its BlockEntity NBT (saveWithFullMetadata minus x/y/z). Assembly is bounded by a build box so it never grabs the surrounding terrain, and the box is read from a player-built launch-pad structure (visible, not a hidden cube): a ShipAssemblyPad controller (BlockEntity, GUI) flood-fills the connected ShipAssemblyBase floor blocks at its own Y to get the footprint bounding box (base may be irregular), and the tallest ShipAssemblyGantry post within that footprint sets the height (no gantry falls back to DEFAULT_HEIGHT=8). Box = footprint x [baseY+1, baseY+height], capped at MAX_FOOTPRINT=32 per side and MAX_HEIGHT=32; MAX_BASE_BLOCKS=2048. Box size and block count are deliberately decoupled: the box is generous, but ShipContraption.MAX_BLOCKS=512 is the real perf cap for now (naive render), to be raised by tier + after the baked-buffer renderer. Right-clicking the pad opens a GUI (ShipAssemblyPadMenu/Screen, no item slots, ContainerData syncs block count + core count + status + box WxHxD) that scans and reports build area, core count, and status (idle/ok/no_core/multi_core/failed/no_base/too_big); a Scan button re-runs via ShipScanPacket (C2S). ShipCoreBlock right-click now just hints to use a pad. GUI reuses wand_upgrade_gui.png as a placeholder texture; blocks have no model/lang-icon datagen yet. Reference notes for the Create class mapping are in AI-context/.../系統設計/飛船Contraption-Create對照筆記.md. Next: M2 spawn ShipEntity + remove world blocks + render (naive then baked, needs a VirtualRenderWorld equivalent since Create's renderer depends on Flywheel/catnip), M3 6-direction flight + ride, M4 disassembly write-back.
飛船 contraption 地基（M1，開發中，尚無玩家可見功能）：決定參考 Create（MIT）自刻 Contraption，不依賴 Create/Valkyrien Skies，因為我們只需要虛空中的平移剛體（無旋轉、太空無世界碰撞）。ShipContraption（space/ship）從 ShipCoreBlock 錨點做**有界** flood-fill 組裝：BFS 掃 6 鄰居，抓所有相連可移動方塊（跳過空氣、LiquidBlock、不可破壞 destroySpeed<0），上限 MAX_BLOCKS=256，每塊存成 localPos = pos - anchor 的 StructureBlockInfo 加其 BlockEntity NBT（saveWithFullMetadata 去掉 x/y/z）。組裝受**建造盒**限制，不會抓到周圍地形，而且盒是從玩家蓋的**發射台結構**讀出來（看得到，不是隱形方盒）：ShipAssemblyPad 控制台（BlockEntity、GUI）在自己那層 flood-fill 相連的 ShipAssemblyBase 地板，取外接矩形當 footprint（底座可不規則），footprint 內最高的 ShipAssemblyGantry 柱決定高度（沒組裝架用 DEFAULT_HEIGHT=8）。盒 = footprint x [baseY+1, baseY+height]，上限 MAX_FOOTPRINT=32 單邊、MAX_HEIGHT=32、MAX_BASE_BLOCKS=2048。盒尺寸與方塊數**刻意分離**：盒給寬鬆，但 ShipContraption.MAX_BLOCKS=512 才是現在的效能上限（naive 渲染），之後依 tier + 烤 buffer 渲染再開大。右鍵組裝台開 GUI（ShipAssemblyPadMenu/Screen，無物品槽，ContainerData 同步方塊數+核心數+狀態+盒 WxHxD），掃描回報建造範圍、核心數、狀態（idle/ok/no_core/multi_core/failed/no_base/too_big）；掃描按鈕經 ShipScanPacket（C2S）重掃。ShipCoreBlock 右鍵改成提示去用組裝台。GUI 暫借 wand_upgrade_gui.png 當佔位貼圖；方塊還沒做模型/lang 圖示 datagen。Create 類別對照筆記在 AI-context/.../系統設計/飛船Contraption-Create對照筆記.md。後續：M2 生成 ShipEntity + 移除世界方塊 + 渲染（先 naive 後烤 buffer，需要 VirtualRenderWorld 等價物，因 Create 渲染器依賴 Flywheel/catnip），M3 六方向飛行 + 駕駛，M4 重組寫回。

Moon terrain and gravity: MoonChunkGenerator is now solid (blockAt: regolith top 4, moonStone, deepStone, glowing core below CORE_TOP=-40); the hollow cavity is gone. CRUST_BOTTOM is repurposed to 0 as the underground threshold for sky gating only. MoonCoreTraversalHandler is now just low gravity (server adds back upward velocity each air tick, skipped while flying) plus reduced fall damage via LivingFallEvent. The earlier hollow-Moon antipodal core traversal (teleport to (D-x,-z)) and its inverted-gravity prototype (camera roll + turn/movement remap) were both removed: the two identical-looking sides made the teleport read as a confusing relocation, and the upside-down controls felt bad regardless of scoping.
月球地形與重力：MoonChunkGenerator 改為實心（blockAt：表層 4 格月壤、月岩、深層月岩、CORE_TOP=-40 以下發光核心），中空已移除。CRUST_BOTTOM 改為 0，只當作畫星空的地底門檻。MoonCoreTraversalHandler 現在只剩低重力（server 每個空中 tick 補回上升速度，飛行時跳過）加 LivingFallEvent 摔落減傷。先前的空心月球對蹠核心貫穿（傳送到 (D-x,-z)）與其反重力原型（相機 roll + turn/移動輸入重映）都已移除：兩面長得一樣讓傳送像莫名其妙的位移，顛倒操作也不論怎麼限縮都很差。

Space rendering architecture: StarSystemRegistry supports arbitrary star systems with multi-star support (single/binary/triple). PlanetDef uses physicalRadius with dynamic angular size from player distance. Ring system: ring.fsh renders Saturn/Uranus/Neptune rings via ray-disc intersection with tilt support and single-exit-point GLSL (AMD driver compatibility). Satellite occlusion: uOccluderDir/uOccluderCos uniforms prevent moons from visually popping when crossing behind parent planet. AMD driver 26.x crash fix: ring textures padded to ≥64px height (extreme aspect ratios like 512×4 cause EXCEPTION_ACCESS_VIOLATION on second glTexImage2D after F3+T). Texture detection uses brightness threshold (dot(rgb,1)>0.001) rather than alpha to handle all texture types reliably. Scale: 1 AU = 1500 blocks; solar system radius ~60 000 blocks; Alpha Centauri at 150 000 blocks.
太空渲染架構：StarSystemRegistry 支援多星系、多恆星（單/雙/三星）。PlanetDef 使用 physicalRadius，視角大小從玩家距離動態計算。環系統：ring.fsh 透過射線與圓盤相交渲染環，支援軸傾斜，使用單一出口點 GLSL（AMD 驅動相容）。衛星遮擋：uOccluderDir/uOccluderCos uniform 防止衛星穿過父行星時瞬間跳躍。AMD driver 26.x 崩潰修復：環貼圖 padding 至最低 64px 高（極端比例如 512×4 在 F3+T 後第二次 glTexImage2D 會觸發 EXCEPTION_ACCESS_VIOLATION）。貼圖檢測改用亮度閾值（dot(rgb,1)>0.001）確保各類貼圖可靠顯示。比例尺：1 AU = 1500 格，太陽系半徑約 60,000 格，比鄰星在 150,000 格外。

Fixed the Speed and Efficiency machine upgrade tooltips, which said they only fit the Solar Mana Collector. They actually also work in the Mana Grinder and Mana Infuser (Speed and Efficiency), and the Mana Crafting Table (Efficiency). The "compatible machines" line and the effect description now list every machine each upgrade fits.
修正速度與效率機器升級的提示，原本只寫「適用太陽魔力收集器」。它們其實也能裝在魔力粉碎機、魔力注入機（速度與效率），以及魔力合成台（效率）。「可安裝於」那行與效果說明現在會列出每個升級實際適用的所有機器。

Fixed missing English text: wand upgrade items, armor effect toggle keys, night vision, and several screen and error messages that previously showed a raw translation key in English are now translated. A few messages (placement blocked, conduit error, solar collector upgrade stats) were also missing in both languages and are now shown.
修正缺失的英文文字：魔杖升級物品、護甲效果切換鍵、夜視，以及數個之前在英文下會顯示原始翻譯鍵的介面與錯誤訊息，現在都有翻譯了。另有幾條訊息（無法放置、導管錯誤、太陽能收集器升級數據）原本中英文都缺，現在會正常顯示。

### Developer Notes / 開發者備註

Lang parity pass: en_us.json was missing 28 keys that zh_tw had (now added, terminology matched to existing entries); 5 genuinely-missing message/screen keys (cannot_place_here, conduit.error_occurred, solar.no_upgrades, solar.upgrade_stats, upgrade.fallback_error) added to both files. Verified that the per-armor-piece `*_upgrade_capacity` keys the lang-check flagged are dead-code references (the CAPACITY behavior enum values are never registered as items; capacity uses the shared ArmorCapacityUpgradeItem), so no lang was added for them. Updated the lang-check skill with a dead-code reachability check + an en/zh parity check + dynamic-prefix filtering.
Lang 對齊：en_us.json 缺了 28 個 zh_tw 有的 key（已補，用詞對齊現有條目）；5 個中英都真缺的訊息/介面 key（cannot_place_here、conduit.error_occurred、solar.no_upgrades、solar.upgrade_stats、upgrade.fallback_error）兩邊都補上。確認 lang-check 報的各裝備 `*_upgrade_capacity` key 是死碼引用（CAPACITY behavior enum 值從未註冊成物品，容量走共用的 ArmorCapacityUpgradeItem），所以不為它們加 lang。更新 lang-check skill：加入死碼可達性檢查 + en/zh 對齊檢查 + 動態前綴過濾。

## [0.0.2.0] - 2026-06-03

### Player Changes / 玩家更新內容

Fixed Nara watch model position on the wrist (shifted down and centered).
修正娜拉手錶在手腕上的位置（向下移並置中）。

Six new skill carriers (ways to deliver a skill), bringing the total to 14. Nova (兌) bursts around you, hitting and lightly knocking back everything nearby, great for a melee/self-reaction build. Shockwave (坤) is a wide expanding ring with lighter damage but a strong outward shove, for crowd control. Earthwave (艮) sends a wave forward along the ground in a cone and pops grounded foes up. Lob (坎) throws an arcing grenade that falls under gravity and triggers on impact, good for hitting over cover. Meteor (離) drops a strike from the sky onto where you are aiming. Orbital (靈魄) summons spirit orbs that circle you for a while and pulse your skill's effect onto nearby enemies (more multishot = more orbs). Put the matching aspect in the carrier slot in the encoder to use them.
新增六種技能載體（把技能送出去的方式），總數來到 14 種。環爆（兌）以你為中心爆發，打到並輕微擊退周圍所有敵人，適合近戰／自我反應流。衝擊波環（坤）是大範圍擴散環，傷害較低但有強力外推，適合控場。震波（艮）沿地面往前扇形推出一道波並把地面上的敵人掀起。拋物榴彈（坎）丟出受重力的拋物線手雷，落地觸發，適合越過掩體打。天降（離）對你瞄準的位置從天空砸下一擊。軌道靈球（靈魄）召喚繞著你轉一陣子的靈球，週期性把技能效果脈衝到附近敵人（多重發射＝更多顆）。在編碼台把對應本源放進載體槽就能用。

Skills no longer hurt their own caster. Reaction explosions (combustion, overload, kinetic blast, etc.) no longer damage you when you cast them at close range. The lingering poison/ash hazard clouds (Plague, Biohazard, Ash Storm, Tainted Water) stay on the ground and keep hitting enemies who stand in them, but they no longer affect you, the caster. One deliberate exception: the Flight carrier puts the effect on yourself, so Flight + an explosive effect is a self-blast (rocket-jump / kamikaze) and still hurts you, on purpose.
技能不再傷到施放者自己。反應的爆炸（爆燃、超載、動能爆等）近距離施放時不再炸到你。滯留的毒雲／灰燼雲（瘟疫、生化浩劫、灰燼風暴、污水）會留在地上持續傷害站進去的敵人，但不再影響身為施放者的你。一個刻意的例外：飛翔載體會把效果套在你自己身上，所以飛翔 + 爆炸效果是自爆（火箭跳／玉石俱焚），仍然會炸到你，這是故意保留的。

Explosion reactions now scale much more with skill power. The blast radius of every explosive reaction (energy, combustion, overload, thermonuclear, singularity, etc.) grows noticeably with how strong the skill is (more effect aspects, fuel, and power modifiers), up to a higher cap. A lightly built energy blast is small; a heavily invested one is huge. The Flight self-blast scales with it too, so a powerful self-cast hurts more.
爆炸類反應現在會隨技能威力放大很多。每種爆炸反應（能量、爆燃、超載、熱核、奇點等）的爆炸半徑會隨技能強度（效果本源數、燃料、增傷修飾）明顯變大，上限也拉高了。輕配的能量爆炸很小，重度投資的會很巨大。飛翔自爆也跟著放大，所以高威力自炸會更痛。

The Nara watch has a new 3D model and texture made by collaborator Bksesame.
娜拉全息手錶換上了合作者 Bksesame 製作的新 3D 模型與貼圖。

The training dummy now uses a proper cactus model with idle and hit animations. The dummy sways gently when idle, blinks, and reacts visually when struck.
訓練假人現在使用正式的仙人掌模型，附帶待機動畫（輕微搖晃 + 眨眼）和被打反應動畫。

Many reactions now reward setting up the target's state first, so order matters. Shatter and Cryocrystal hit much harder and consume the ice on a chilled or frozen target; Thermal Shock hits harder and extinguishes a burning target; Spite Bolt and Thermion deal a heavy bonus to a wet or chilled (conductive) target; Combustion ignites a poisoned target's toxin for a bonus and consumes the poison; Soulfire feeds on a withering target; Annihilation grows with every harmful effect already on the target; Shrapnel digs into bleeding targets; Overload and Kinetic Blast bite deeper into a cracked-armor (vulnerable) target; Death Siphon heals you more from a withering target; Radiant Surge and Holy Nova sear the undead; Purge detonates a poisoned target's toxin. Chain the right reactions in order and they pay off.
許多反應現在會獎勵你先鋪陳目標的狀態，順序有差。碎冰與寒晶對被冰緩或冰凍的目標傷害大增並消耗冰；熱裂對燃燒中的目標傷害更高並滅火；怨雷與熱離子對潮濕或冰緩（導電）的目標造成大量額外傷害；爆燃會引爆中毒目標的毒氣造成額外傷害並消耗中毒；煉獄火會吞噬凋零中的目標；湮滅會隨目標身上的負面效果數量增強；晶爆會刺入流血的目標；超載與動能爆對破甲（易傷）目標咬得更深；死亡虹吸從凋零中的目標吸更多血；聖能爆與聖核爆灼燒不死生物；淨毒會引爆中毒目標的毒素。把對的反應照順序串起來就有回報。

Reactions now look and play distinctly, not just stacked potion effects. Every reaction spawns its own particle signature on hit (fire bursts, snow, sparks, green clouds, light rays, an inward implosion swirl, and so on), so you can tell at a glance which reaction fired. A few also gained unique mechanics: Implosion and Magnetic Storm now vacuum nearby dropped items toward the center; Plague, Biohazard, Ash Storm and Tainted Water leave a lingering hazard cloud on the ground that keeps damaging or blinding; Deep Freeze briefly freezes the target solid (can't move at all).
反應現在看起來、玩起來都不一樣了，不只是疊藥水效果。每個反應命中時會噴自己的粒子簽名（火花四散、雪花、電花、綠色毒雲、光束、向心吸入的內爆漩渦等），一眼就能認出觸發了哪個反應。有幾個還拿到獨特機制：內爆與磁暴會把附近掉落物吸向中心；瘟疫、生化浩劫、灰燼風暴、污水會在地上留下持續傷害或致盲的危險雲；冰封會短暫把目標凍住（完全不能動）。

Aspect reactions are now a real chemistry engine, like a periodic table. Each effect aspect carries chemical traits (heat, cold, water, electric, toxic, acid, metal, light, dark, force, organic, arcane, life), and reactions fire from trait combinations rather than fixed aspect pairs, so far more combos do something. Best of all, reactions can chain: a reaction's product becomes a new ingredient that can react again. For example fire + ice makes thermal shock and steam; add lightning and the steam superheats into plasma. Acid + metal dissolves into a volatile gas; add heat and it detonates. Fire + plant makes wildfire and ash; add force and the ash becomes a blinding storm. There are now 87 reactions: nearly every pair of chemical traits does something, plus chains and three-trait ultimates. New ones include scald, meltdown, eruption, magnetic storm, plague, photosynthesis, sanctify, superconduct, deep freeze, rust, miasma, holy water, EMP, petrify and virulence. The JEI "Aspect Reactions" page lists them all. Mix aspects and see what cascades.
本源反應現在是一套真正的化學引擎，像元素週期表。每個效果本源帶化學性質（熱、寒、水、電、毒、酸、金屬、光、暗、力、生機、奧能、生命），反應靠性質組合觸發而不是固定的本源配對，所以更多組合都會有反應。最棒的是反應會連鎖：反應的產物會變成新原料再次反應。例如火 + 冰產生熱裂與蒸氣；再加雷，蒸氣就過熱成電漿。酸 + 金屬溶解出揮發氣；再加熱就引爆。火 + 植物產生野火與灰燼；再加力，灰燼就成致盲風暴。現在共有 87 個反應：幾乎每對化學性質都有反應，再加上連鎖與三性質終極反應。新增蒸爆、熔毀、噴發、磁暴、瘟疫、光合、聖療、超導、冰封、鏽蝕、沼氣、聖水、電磁脈衝、礦化、劇毒等。JEI「本源反應」分頁全部列出。把本源混在一起，看看會級聯出什麼。

The Aspect Researcher villager is back, done right. Craft an Aspect Trading Station (bookshelf + mana crystal + amethyst) and an unemployed villager nearby becomes an Aspect Researcher who trades a Mirror Core Shard for Basic Aspect Essence. It uses its own dedicated block (not your research table, and named so you won't confuse the two), so it no longer causes the open stutter.
本源研究員村民回來了，這次做對了。合成「本源交易站」（書架 + 魔力水晶 + 紫水晶），附近的失業村民就會轉職成本源研究員，收鏡核碎片換基礎本源精華。它用自己專屬的方塊（不是你的研究台，名字也分開避免混淆），所以不再造成開啟卡頓。

Hovering an aspect in the Skill Encoder now shows what it does in a skill (its role and effect), so you can read each aspect's behavior right where you build skills instead of guessing.
在技能編碼台滑到本源現在會顯示它在技能裡做什麼（角色與效果），組技能的當下就能看懂每個本源的行為，不用再用猜的。

Aspect reactions now have a JEI page. Open JEI and look up "Aspect Reactions" (or right-click a spell core) to see every chemistry combo: which traits react and what happens, so reactions are no longer hidden knowledge.
本源反應現在有 JEI 分頁了。打開 JEI 查「本源反應」（或右鍵法術核心）就能看到每個化學組合：哪些性質會反應、會發生什麼，反應不再是隱藏知識。

This makes aspects renewable, so collecting them no longer dead-ends once you have scanned everything: right-clicking the Basic Aspect Essence grants 5 of each of the six base aspects (water, fire, wood, metal, earth, wu). Since the Mirror World boss is repeatable, you can farm Mirror Core Shards, trade them at the Aspect Trading Station for essence, and top up aspects to keep authoring skills (and feed the synthesis bench for higher aspects).
這讓本源變成可再生，掃完世界後收集本源不再是死路：右鍵「基礎本源精華」可獲得六大基礎本源（水火木金土烏）各 5 點。鏡中世界 boss 可重複挑戰，所以你能 farm 鏡核碎片、在本源交易站換精華、補本源來持續組裝技能（再餵本源合成台合出更高階本源）。

New material, the Aspect Codec Board, is the heart of the skill system, and getting it walks the whole tech tree: assemble a Blank Aspect Codec Board at the mana crafting table (grinder substrate + circuit-chain precision circuit + altar wafer + wire), then activate it at the altar with a Mirror Core Shard from the Mirror World boss. The Skill Encoder is now an altar build too, and the Spell Core uses the activated board, so the entire skill path runs through the machines you already built. Skills cast from the spell core also hit much harder now (higher base damage and a bigger bonus per effect), so a deeply combined skill is worth the long road to unlock it. (Board textures pending.)
新材料「本源編碼基板」是技能系統的核心，而且取得它要走過整條技術樹：先在魔力工作台組出「空白本源編碼基板」（研磨機的魔力基板＋電路鏈的精密電路板＋祭壇的魔力晶圓＋導線），再到祭壇用鏡中世界 boss 掉的鏡核碎片激活。技能編碼台現在也改成祭壇製作，法術核心則用激活後的基板，所以整條技能路線都會走過你前面蓋的機器。法術核心組出的技能傷害也大幅提高（基礎傷害更高、每個效果加成更多），組合越深的技能越強，配得上解鎖它的漫長路程。（基板貼圖待補。）

Fixed a crash that stopped the mod from loading on dedicated servers (the new collaborator armor model was registered in a way that pulled client-only rendering code onto the server). Single-player was unaffected.
修正一個讓模組無法在專用伺服器載入的崩潰（新的合作者盔甲模型註冊方式把 client 專用的渲染程式碼帶到了伺服器端）。單人遊戲不受影響。

### Developer Notes / 開發者備註

- Fixes from /code-review (3 confirmed): (1) a Machine-carrier support skill (e.g. heal turret) fired a bolt that fell back to the native turret damage instead of the skill's 0 damage, hurting the ally it should heal — FloatingTurretProjectile now gates on a skillPayload flag, not `skillDamage > 0`. (2) The reaction-engine rewrite dropped the Energy+Arcana → Overload combo (both are pure ARCANE, the rule needed ARCANE+METAL); added an ARCANE+ARCANE rule so any two arcane sources overload again. (3) Encoding failure now shows a new `encode.not_enough` message (it consumes aspects, and an aspect used in two slots costs double) instead of the misleading "you don't own these".
- /code-review 修正(3 個確認):(1) 機械載體的支援技能(如治療砲)發射的彈會退回原生砲塔傷害而非技能的 0 傷,反而傷到要治療的隊友 — FloatingTurretProjectile 改用 skillPayload 旗標判斷而非 `skillDamage > 0`。(2) 反應引擎改寫時掉了「能量+奧術→超載」組合(兩者都是純 ARCANE,規則卻要 ARCANE+METAL);加一條 ARCANE+ARCANE 規則,任兩個奧能源都會超載。(3) 編碼失敗改顯示新的 `encode.not_enough`(編碼會消耗本源,同本源用兩槽算兩份)而非誤導的「你沒擁有」。

- Cleanup (/simplify pass, no behavior change): added a SkillTargeting helper (safeDir + forEachEnemy) and adopted it. safeDir replaces the `dir.lengthSqr()<1e-4 ? (0,0,1) : dir.normalize()` idiom at 8 sites; forEachEnemy replaces the duplicated `getEntitiesOfClass(LivingEntity, box, e != caster && alive)` AoE loop at 6 reaction ops (the other ~22 left for incremental adoption since many use `continue`, which doesn't map to a forEach lambda). ReactionCloud now injects the owner into the vanilla victims map once instead of reflecting + re-putting it every tick. Removed a stale duplicate Javadoc on SkillCompiler.applyReactions and corrected ReactionInfo's doc (reaction matching lives in ReactionEngine now).
- 清理(/simplify 一輪,行為不變):新增 SkillTargeting helper(safeDir + forEachEnemy)並採用。safeDir 取代 8 處 `dir.lengthSqr()<1e-4 ? (0,0,1) : dir.normalize()`;forEachEnemy 取代 6 個反應 op 重複的 AoE 迴圈(其餘約 22 處留待漸進採用,因為很多用 `continue`)。ReactionCloud 改成把 owner 注入 vanilla victims map 一次,而非每 tick 反射重塞。移除 applyReactions 重複舊 Javadoc、修正 ReactionInfo 過時說明。

- Fix: the Research Table screen stuttered every time it opened. Real cause: opening sends an AspectSyncPacket, and the client handler called AspectSynthesisJEIPlugin.refreshAspectIngredients() unconditionally every time, which rebuilds JEI's ingredient index (heavy). Now ClientResearchCache.updateAspects returns whether the discovered set actually changed, and the JEI refresh only runs when it did (rare), not on every open. The same gate was applied to the Nara watch sync and the knowledge sync (all three handlers shared the unconditional-refresh anti-pattern) via a shared refreshJeiIfAspectsChanged helper. (A blur-removal override was tried first but was a red herring and broke the GUI texture, so it was reverted.)
- 修復:研究台介面每次開都卡一下。真因:開啟會送 AspectSyncPacket,client handler 每次都無條件呼叫 AspectSynthesisJEIPlugin.refreshAspectIngredients() 重建 JEI ingredient 索引(很重)。現在 ClientResearchCache.updateAspects 會回報本源集合有沒有真的變,JEI 只在變了(很少)才刷,不再每次開都刷。娜拉手錶同步與知識同步也是同一個反模式,用共用的 refreshJeiIfAspectsChanged 一起 gate。(一開始試過拿掉 blur,但那是紅鯡魚還弄不見了 GUI 貼圖,已還原。)

- Fix: villagers wouldn't claim the Aspect Trading Station. The aspect_researcher POI type was registered and detected, but jobless villagers only claim POIs in the minecraft:acquirable_job_site POI-type tag (Villager.java uses VillagerProfession.ALL_ACQUIRABLE_JOBS = is(PoiTypeTags.ACQUIRABLE_JOB_SITE)). Added a ModPoiTypeTagProvider (datagen) that puts koniava:aspect_researcher into acquirable_job_site. Run runData to regenerate. (Found by reading the decompiled NeoForge/vanilla sources.)
- 修復:村民不認領本源交易站。aspect_researcher POI type 有註冊也偵測得到,但失業村民只會搶在 minecraft:acquirable_job_site POI-type tag 裡的 POI(Villager.java 用 VillagerProfession.ALL_ACQUIRABLE_JOBS = is(PoiTypeTags.ACQUIRABLE_JOB_SITE))。新增 ModPoiTypeTagProvider(datagen)把 koniava:aspect_researcher 加進 acquirable_job_site。需跑 runData 重新生成。(讀反編譯 NeoForge/vanilla 原始碼找到的。)

- Wired the Shockwave carrier to a shader visual (step 3 of the carrier-FX refactor). New CarrierFxType enum (SHOCKWAVE) + CarrierFxPacket (S2C, sent to players within 64 blocks) + client-side CarrierFxClient dispatcher. CarrierFx.shockwave now also sends the packet. The client plays it through a dedicated ShockwaveRenderer (its own expanding ground ring, vertex-coloured via vanilla position_color, with its own onRenderLevel + release). Originally this piggybacked on ManaStrikeShaderRenderer for speed, but that coupled the shockwave to an unrelated animation that also draws a vertical mana-strike beam (wrong for a ground shockwave), so it was split into its own renderer. gameplay still calls CarrierFx in one line; adding a shader carrier = one CarrierFxType entry + one CarrierFxClient case. Packet registered in ModNetworking. The handler is client-only so CarrierFxClient never loads on a dedicated server (same pattern as SkillCooldownPacket).
- 把衝擊波載體接上 shader 視覺(載體 FX 重構第 3 步)。新增 CarrierFxType enum(SHOCKWAVE)+ CarrierFxPacket(S2C,送給 64 格內玩家)+ client 端 CarrierFxClient 派發器。CarrierFx.shockwave 現在多送這個 packet。client 用專屬的 ShockwaveRenderer 播(自己一份地面擴散環,走 vanilla position_color 純頂點色,有自己的 onRenderLevel + release)。原本為了快直接接到 ManaStrikeShaderRenderer,但那會把衝擊波綁死在一個不相干、還會畫垂直魔力打擊光柱的動畫上(對地面衝擊波不對),所以拆成獨立 renderer。gameplay 仍只呼叫 CarrierFx 一行;加 shader 載體 = 一個 CarrierFxType + 一個 CarrierFxClient case。packet 註冊在 ModNetworking。handler 只在 client 跑,所以 CarrierFxClient 不會在 dedicated server 載入(跟 SkillCooldownPacket 同 pattern)。 Moved the inline particle code (slash sweep, pipeline beam, gravity field swirl, chain spark) out of SkillCompiler into a new server-side CarrierFx layer; SkillCompiler now calls CarrierFx.slash/beam/field/chainSpark. Pure structural move, behavior unchanged. Sets up adding shader-backed carriers (shockwave/orbital) via a CarrierFxPacket without touching hit/effect logic.
- 把載體視覺從 gameplay 解耦(載體 FX 重構第 1 步)。將內聯粒子(劍氣扇形、管道光束、重力漩渦、連鎖火花)從 SkillCompiler 抽到新的 server 端 CarrierFx 層;SkillCompiler 現在只呼叫 CarrierFx.slash/beam/field/chainSpark。純結構搬移,行為不變。為之後用 CarrierFxPacket 接 shader 載體(衝擊波/軌道)鋪路,不必動命中/效果邏輯。

- Fix (dedicated-server crash): the mod failed to load on a dedicated server with "Attempted to load class RenderLayerParent for invalid dist DEDICATED_SERVER". The collaborator armor AddLayers listener inlined `new ManaAlloyArmorLayer<>(pr, ...)` in the @Mod main class; verifying that class on the server loads the client RenderLayerParent (the Dist.CLIENT runtime guard does not stop class-load-time verification). Moved all client mod-bus registration into a new client-only ClientModBusSetup.init(modEventBus); the main class now holds zero client-typed bytecode and only the server-safe header of ClientModBusSetup is touched. (Found via runGameTestServer failing to boot; root cause traced in decompiled FMLModContainer + Explosion.)
- 修復(專用伺服器崩潰):模組在專用伺服器報「Attempted to load class RenderLayerParent for invalid dist DEDICATED_SERVER」無法載入。合作者盔甲的 AddLayers listener 在 @Mod 主類別內聯了 `new ManaAlloyArmorLayer<>(pr, ...)`,server 驗證主類別時會載入 client 的 RenderLayerParent(Dist.CLIENT 執行期守衛擋不了 class 載入期的驗證)。把所有 client mod-bus 註冊抽到新的 client-only ClientModBusSetup.init(modEventBus);主類別現在零 client 型別位元組,server 只碰 ClientModBusSetup 的 header(server-safe)。(由 runGameTestServer 無法啟動發現;在反編譯的 FMLModContainer + Explosion 追到根因。)

- Tests: added SkillEffectGameTests (in-world GameTests) + SkillEncodingCostTest (JUnit). Covers the heal-turret-no-ally-damage fix, the self-AoE caster shielding, the Flight self-blast self-damage, and the ReactionCloud owner-skip (asserted on the victims skip-map, since a headless GameTest region does not reliably tick a freshly added AreaEffectCloud). All 70 GameTests + the JUnit suite pass. Finding that came out of these: vanilla Explosion.explode() excludes its own source entity from damage (getEntities(except = source)), so a caster is ALWAYS shielded from their own skill explosion. That made the safeExplode invuln guard redundant (removed) AND meant the Flight self-blast (target == caster) dealt no self-damage despite the "self-blast hurts" intent. Fixed: safeExplode now deals explicit explosion self-damage (radius * SELF_BLAST_DAMAGE_PER_RADIUS, currently 2.0, tunable) when target == caster, so Flight + an explosive effect is a real rocket-jump/kamikaze cost.
- 測試:新增 SkillEffectGameTests(實機 GameTest)+ SkillEncodingCostTest(JUnit)。涵蓋治療砲不傷隊友、自我 AoE 不傷施放者、飛翔自炸自傷、ReactionCloud owner-skip(改斷言 victims 跳過集合,因為 headless GameTest 區域不會可靠 tick 剛加入的 AreaEffectCloud)。全 70 個 GameTest + JUnit 全過。由此挖出的發現:vanilla Explosion.explode() 會把自己的 source 實體排除在傷害外(getEntities(except = source)),所以施放者一定不會被自己的技能爆炸傷到。這讓 safeExplode 的 invuln 守衛變冗餘(已移除),也讓飛翔自炸(target == caster)其實沒有自傷,儘管「自爆會痛」的本意如此。已修:safeExplode 現在在 target == caster 時對施放者補一刀明確的爆炸傷害(radius × SELF_BLAST_DAMAGE_PER_RADIUS,目前 2.0,可調),所以飛翔 + 爆炸效果是真的有火箭跳/玉石俱焚的代價。

- Balance: every explosion op's radius now scales harder with power (the `power` arg the ops receive is the computed `damage`, not the 1.0 power multiplier, so it grows with effect count + fuel + power modifiers). Bumped each radius coefficient x1.5 and each cap +1: ENERGY 0.06/7 -> 0.09/8, COMBUSTION 0.07/8 -> 0.105/9, the two 0.08/9 ops -> 0.12/10, THERMONUCLEAR 0.09/10 -> 0.135/11, SINGULARITY 0.06/7 -> 0.09/8, the two 3.5+0.07/8 ops -> 3.5+0.105/9. Affects both enemy AoE and the Flight self-blast (radius * SELF_BLAST_DAMAGE_PER_RADIUS). Conservative tuning pass; values are easy to push further.
- 平衡:每個爆炸 op 的半徑現在更吃 power(ops 收到的 `power` 其實是算好的 `damage`,不是 1.0 倍率,所以會隨效果數 + 燃料 + 增傷修飾變大)。每個半徑係數 ×1.5、上限 +1:ENERGY 0.06/7 → 0.09/8、COMBUSTION 0.07/8 → 0.105/9、兩個 0.08/9 的 op → 0.12/10、THERMONUCLEAR 0.09/10 → 0.135/11、SINGULARITY 0.06/7 → 0.09/8、兩個 3.5+0.07/8 的 op → 3.5+0.105/9。同時影響對敵 AoE 與飛翔自炸(radius × SELF_BLAST_DAMAGE_PER_RADIUS)。保守調一輪,要再往上推很容易。

- Reaction system rewritten from a hand-coded if-chain into a trait-rule engine (research/skill/reaction/). ReactionTrait (~18 traits incl. products), ReactionRule (requires -> op + product traits + chain), ReactionEngine (Aspect->trait table + rule list + cascade loop). run() seeds a source pool from the effect aspects' traits and iterates up to MAX_DEPTH=4: each pass fires every matchable unused rule, appends its op + chain, and stages its product traits as new sources for the next pass (products feeding back = chain reactions). Self-trigger guard: a rule only fires if its required traits have a System of Distinct Representatives among the sources (distinct providers), so a single multi-trait aspect (e.g. magma = heat+force) can't trigger a heat+force rule alone, but a product counts as its own source so chains stay legal. SkillCompiler.applyReactions now just calls ReactionEngine.run and forwards ops + chain; the old if-chain and isFire/isDark helpers were removed. ReactionInfo.ALL is derived from ReactionEngine.RULES (JEI auto-follows the rules). 26 new SkillEffectOp reaction ops + 48 rules in the first pass; a second pass (batch 4) added 39 more to fill nearly the whole 13x13 trait matrix, for 65 new reaction ops and 87 total rules.
- 反應系統從手寫 if 串改寫成性質規則引擎（research/skill/reaction/）。ReactionTrait（~18 性質含產物）、ReactionRule（需求 -> op + 產物性質 + 連鎖）、ReactionEngine（本源->性質表 + 規則表 + 級聯迴圈）。run() 用效果本源的性質播種來源池,迭代到 MAX_DEPTH=4:每輪觸發所有可配對的未用規則、加上 op + 連鎖,並把產物性質作為新來源排進下一輪（產物回饋=連鎖反應）。防自觸發:規則只在需求性質能在來源間找到 SDR（相異代表/不同提供者）時才觸發,所以單一多性質本源（如熔岩=熱+力）不能獨自觸發熱+力規則,但產物算自己的來源所以連鎖合法。SkillCompiler.applyReactions 現在只呼叫 ReactionEngine.run 轉發 ops + 連鎖;舊 if 串與 isFire/isDark 移除。ReactionInfo.ALL 由 ReactionEngine.RULES 衍生（JEI 自動跟著規則）。第一批新增 26 個 SkillEffectOp 反應 op + 48 條規則;第二批(batch 4)再加 39 條補滿幾乎整個 13x13 性質矩陣,合計 65 個新反應 op + 共 87 條規則。

- Renamed the Aspect Researcher job block's display name to "Aspect Trading Station" (block id aspect_research_desk unchanged) so it no longer collides with the player's Research Table. Lang-only change, both locales.
- 把本源研究員職業方塊的顯示名改成「本源交易站」（方塊 id aspect_research_desk 不變），避免跟玩家的「研究台」撞名。只動 lang，兩個語系都改。

- WIP (revert research-table POI: fixes the open stutter + non-working profession): the custom Aspect Researcher profession bound its POI to the Research Table, but that block is a BaseEntityBlock (block entity + animated render), so making it a POI caused a hitch when opening the table and villagers wouldn't reliably claim it. Removed ModVillagers (POI + profession) entirely; the Mirror Core Shard -> Basic Aspect Essence trade now lives on the vanilla Librarian (level 3, knowledge theme, works out of the box). A proper custom researcher can be redone later on a dedicated simple (non-BE) job block.
- WIP（移除研究台 POI：修開啟卡頓 + 村民不任職）：自訂「本源研究員」職業把 POI 綁在研究台,但研究台是 BaseEntityBlock（有 BlockEntity + 動畫渲染），當 POI 會在開啟時卡頓、村民也不易認領。整個移除 ModVillagers（POI+職業）；鏡核碎片→基礎本源精華的交易改掛 vanilla 圖書管理員（等級 3,知識主題,開箱即用）。要正式的本源研究員之後用專屬的簡單（非 BE）方塊另做。

- WIP (boss reward chest: no leftover loot on open): the reward chest used a lazy loot table (setLootTable, rolled on open), which could leave the previous clear's items in the chest. spawnRewardChest now clears the chest, clears any pending loot table, and rolls the loot table immediately into the chest (LootTable.fill) so opening it always shows only this clear's reward, with zero carry-over. Same table id, so JEI is unchanged.
- WIP（boss 獎勵寶箱：開箱不再有殘留）：獎勵寶箱原本用懶填 loot table（setLootTable，開箱才 roll），可能把上一場的物品留在箱子裡。spawnRewardChest 現在清空寶箱、清掉待填的 loot table，並當場把 loot table roll 進去（LootTable.fill），所以開箱永遠只有這一場的獎勵、零殘留。表 id 不變，JEI 照舊。

- WIP (boss-resistant skill CC + modifiers work on beam/slash/field): two balance/coverage fixes. (1) Skill control effects (root, daze, chill) now go through TurretControlHelper.applySkillControl, which shares the floating turret's boss control-resistance (control_resistant tag halves the duration) and supports amplifiers, so skills can no longer perma-lock a boss. All 20 root/daze/chill applications were routed through it. (2) The multi-shot modifiers (refraction/resonance/automation) now repeat the beam, slash and field carriers (count times), and chain (arc/propagation/conduction) now jumps from the carrier's hits to nearby foes via a shared chainOps helper, so those modifiers do something on non-projectile carriers instead of being ignored.
- WIP（技能控制吃 boss 抗性 + 修飾對光束/劍氣/力場生效）：兩個平衡/覆蓋修正。(1) 技能控制效果（定身/迷盲/冰緩）改走 TurretControlHelper.applySkillControl，跟浮游砲共用 boss 控制抗性（control_resistant tag 時間打折）並支援等級，所以技能不能再對 boss 無限鎖。20 處 root/daze/chill 全部改走它。(2) 連發修飾（折射/共鳴/自動）現在會讓光束/劍氣/力場載體多打幾道（count 次），連鎖（電弧/繁衍/導電）用共用 chainOps helper 從命中目標跳到附近敵人，所以這些修飾在非投射載體上不再是沒效果。

- WIP (playtest round 2: reactions on dummy, mind effects on mobs, stronger pull): four fixes. (1) The training dummy now also shows which reactions fired on it (thermal shock, combustion, etc.), not just lingering effects: SkillEffectOp.applyTo logs reaction ops onto the dummy, which appends them to the stats packet and pushes an immediate update. (2) Eldritch and Cognition did nothing to mobs (nausea/darkness are player-only); they now also apply Daze so the mob loses its target. (3) Kan's pull now scales with distance (0.8 + dist*0.35, capped 3.0) so a far target is yanked across the gap instead of nudged. (4) Doc clarified that Sensus is a see-through-walls glow mark, not pierce.
- WIP（實測第二輪：假人顯示反應、心智效果對怪、拉近加強）：四個修正。(1) 訓練假人現在也顯示觸發了哪些反應（熱裂/爆燃…），不只持續效果：SkillEffectOp.applyTo 把反應 op 記到假人上，併進狀態封包並立刻推更新。(2) 異界與知識對怪沒效果（混亂/黑暗只影響玩家）→ 也加迷盲讓怪丟失目標。(3) 坎的拉近改成依距離放大（0.8 + 距離×0.35，上限 3.0），遠目標被拉過來而非微推。(4) 文件講清楚感知是隔牆發光標記，不是穿透。

- WIP (training dummy shows applied effects; custom researcher villager; boss chest fix): three playtest items. (1) The training dummy HUD now lists the status effects currently on the dummy (name + level), so you can see what your skill applied: armor break, bleed, vulnerable, dazed, chilled, etc. TrainingDummyStatsPacket carries an effect list (description id + amplifier) gathered from getActiveEffects each update. (2) The Mirror Core Shard -> Basic Aspect Essence trade moved from vanilla Cleric to a new custom Aspect Researcher profession whose job site is the Research Table (new PoiType over the table's blockstates + VillagerProfession). (3) The Mirror boss reward chest no longer spills leftover loot when re-cleared: it keeps the existing chest block and re-applies the loot table instead of swapping the block.
- WIP（訓練假人顯示效果；自訂研究員村民；boss 寶箱修復）：三個實測項目。(1) 訓練假人 HUD 現在列出假人目前身上的狀態效果（名稱+等級），看得到技能打了什麼：破甲、流血、易傷、迷盲、冰緩等。TrainingDummyStatsPacket 帶上效果清單（翻譯鍵+等級），每次更新從 getActiveEffects 收集。(2) 鏡核碎片→基礎本源精華的交易從 vanilla 神職移到新的自訂「本源研究員」職業，職業方塊是研究台（研究台 blockstate 註冊成 PoiType + VillagerProfession）。(3) 鏡中 boss 獎勵寶箱重複過關不再噴出殘留物品：保留原寶箱方塊、重設 loot table，而不是換方塊。

- WIP (renewable aspect source: essence item + cleric trade): scanning is one-time, but encoding skills now consumes aspects, so added a faucet. New AspectEssenceItem (use() grants a configured aspect set + amount, consumes one, syncs the watch via AspectSyncPacket); registered BASIC_ASPECT_ESSENCE granting the six base aspects x5. New ModVillagerTrades (@EventBusSubscriber on VillagerTradesEvent) adds a Cleric level-3 listing: Mirror Core Shard -> Basic Aspect Essence. Lang en/zh_tw; item auto-models once its texture exists.
- WIP（可再生本源來源：精華物品 + 神職交易）：掃描一次性，但組裝技能現在會消耗本源，所以加了水龍頭。新增 AspectEssenceItem（use() 給設定好的一組本源各 N 點、消耗一個、用 AspectSyncPacket 同步手錶）；註冊 BASIC_ASPECT_ESSENCE 給六大基礎本源各 5。新增 ModVillagerTrades（掛 VillagerTradesEvent）給神職村民等級 3 一個交易：鏡核碎片→基礎本源精華。lang en/zh_tw；貼圖存在後物品模型自動產。

- WIP (aspects are now spent to author skills): aspect counts finally have a sink. Encoding a skill at the bench now consumes the aspects it uses (1 per use, summed over carrier/effects/modifiers), instead of only checking ownership. SkillEncoding gained aspectCost/canAfford/consumeAspects; EncodeSkillPacket verifies, writes, consumes, marks dirty and syncs the watch counts via AspectSyncPacket. As a consequence, casting a baked skill no longer needs the aspects (already spent), so SkillCasting dropped the cast-time aspect gate and now costs mana only. Model: aspects = material to make a skill (one-time), mana = fuel to cast it. Tests green.
- WIP（本源現在會被花掉來做技能）：本源數量終於有消耗去處。在編碼台組裝技能現在會消耗用到的本源（每用一次扣 1，載體/效果/修飾加總），不再只是檢查擁有。SkillEncoding 新增 aspectCost/canAfford/consumeAspects；EncodeSkillPacket 驗證足夠→寫入→消耗→setDirty→用 AspectSyncPacket 同步手錶數量。連帶地，施放已烤好的技能不再需要本源（編碼時就花掉了），所以 SkillCasting 拿掉施放時的本源 gate，改成只吃魔力。模型：本源＝做技能的素材（一次性），魔力＝施放的燃料。測試綠。

- WIP (custom Chill slow that bites flying mobs): added a Chill effect (ChillEffect) that scales an entity's horizontal velocity each tick instead of only lowering the ground move-speed attribute, so it actually slows flying/custom-movement mobs (ghast, phantom, ender dragon, wither, ...) that ignore vanilla Slowness. Strength by amplifier (x0.6 down to x0.15), vertical velocity untouched. All skill slow ops (frost, arc, vapor, spiritus, desire, sensus, cryo-venom, elemental storm) now use Chill; the heavier near-immobile ops (law, overgrowth, blight) now use the true ROOT effect so their lockdown also holds flying mobs. Lang en/zh_tw. Tests green.
- WIP（自訂冰緩，連飛行怪都拖得慢）：新增冰緩效果（ChillEffect），每 tick 縮放實體水平速度，而不是只降地面移動速度屬性，所以對無視 vanilla 緩速的飛行/自訂移動怪（惡魂/夜魅/終界龍/凋零…）也真的有效。強度依層數（×0.6 到 ×0.15），不動垂直速度。所有技能緩速 op（冰寒/電弧/蒸騰/靈魂/慾望/感知/凍毒/元素風暴）改用冰緩；較重的近定身 op（法則/蔓生/腐化）改用真正的 ROOT 效果，連飛行怪都鎖得住。lang en/zh_tw。測試綠。

- WIP (skill root now truly immobilizes, incl. flying mobs): the skill ROOT op (growth effect + binding/gen modifiers) used vanilla Slowness amp5, which only lowers the ground movement-speed attribute and so does nothing to flying/custom-movement mobs (ender dragon, ghast, phantom, bee, wither, ...). It now uses the mod's custom ROOT effect (RootMobEffect zeroes horizontal velocity each tick, the same one the floating turret control bolt uses), so root actually holds everything. Plain slows (frost/arc/etc) still use vanilla slowness by design and remain ground-only.
- WIP（技能定身現在真的定得住，含飛行怪）：技能 ROOT（孕育效果 + 束縛/艮修飾）原本用 vanilla 緩速 amp5，那只降地面移動速度屬性，對飛行/自訂移動的怪（終界龍/惡魂/夜魅/蜜蜂/凋零…）沒用。現在改用模組自訂的 ROOT 效果（RootMobEffect 每 tick 把水平速度歸零，跟浮游砲控制彈同一個），所以定身對所有東西都有效。一般緩速（冰寒/電弧等）照設計仍用 vanilla 緩速，維持只對地面怪。

- WIP (custom status effects, real substance over vanilla): three mod-owned MobEffects replace bland vanilla uses. Bleed (BleedEffect) ticks armor-bypassing true damage each second and can actually kill (poison can't); bestia now applies it. Soulburn (SoulburnEffect) is a faster, heavier armor-bypassing DoT used by the soulfire reaction. Vulnerable is a mark: SkillStatusEffectHandler hooks LivingDamageEvent.Pre to amplify damage to a marked target (+20%, +10% per level), so sensus marks a target for a burst finisher (and feeds law's per-debuff scaling). Registered in ModMobEffects; lang en/zh_tw. Icons missing for now (like root, harmless, no HUD icon until drawn). Tests green.
- WIP（自訂狀態效果，比 vanilla 有料）：三個模組自有 MobEffect 取代平淡的 vanilla 用法。流血（BleedEffect）每秒造成無視護甲的真實傷害、而且能真的打死目標（中毒不行），獸性改用它。靈焰（SoulburnEffect）是更快更重的無視護甲 DoT，煉獄火反應用它。易傷是標記：SkillStatusEffectHandler 掛 LivingDamageEvent.Pre，對被標記目標放大傷害（+20%、每層 +10%），所以感知標記目標後可用大招收尾（也餵律法的依層數加傷）。註冊在 ModMobEffects；lang en/zh_tw。圖示暫缺（跟 root 一樣無害，畫了才有 HUD 圖示）。測試綠。

- WIP (three-aspect ultimate reactions): six top-tier reactions that need all three effect slots, stacking on top of the two-aspect reactions for a payoff combo. elemental storm (fire+frost+lightning, multi-element AoE burst), thermonuclear (fire+energy+lightning, the largest explosion), biohazard (venom+corrosion+fire, wide toxic inferno), holy nova (radiance+dark+energy, massive AoE burst), blight (growth+fire+venom, AoE root+fire+poison), maelstrom (storm+kan+dui, yanks foes up + damages). Same applyReactions table; 22 reactions total. Tests green.
- WIP（三本源終極反應）：六個要用滿三個效果槽的頂級反應，疊在兩兩反應之上當收尾組合。元素風暴（火+霜+雷，多元素範圍爆發）、熱核（火+能量+雷，最大爆炸）、生化浩劫（毒+腐蝕+火，大範圍毒火地獄）、聖核爆（光+暗+能量，超大範圍爆發）、腐化（生長+火+毒，範圍定身+火+毒）、引力亂流（風暴+坎+兌，把敵人捲起來+傷害）。同一張 applyReactions 表；總共 22 反應。測試綠。

- WIP (aspect reaction system, chemistry v1): SkillCompiler.applyReactions scans the skill's effect set for reacting pairs and appends an emergent reaction op (on top of each aspect's own op), so a combination does something neither aspect does alone. Ten starter reactions: thermal shock (fire+frost), combustion (fire+energy), toxic burn (fire+venom), shatter (frost+force), conduction (lightning+water, adds chaining), overload (energy+crystal/arcana), death siphon (dark+lifesteal), wildfire (growth+fire), strong acid (venom+corrosion), annihilation (radiance+dark). New reaction-only ops in SkillEffectOp (never mapped from a single aspect). Single-skill-internal for now (no cross-skill element state); the table is the single place to add more. Tests green.
- WIP（本源反應系統，化學 v1）：SkillCompiler.applyReactions 掃技能的效果集合找會反應的對，疊加一個湧現的反應 op（在各本源自己的 op 之上），所以組合會做出單一本源做不到的事。十個起手反應：熱裂（火+霜）、爆燃（火+能量）、毒燃（火+毒）、碎冰（霜+力量）、導電（雷+水，加連鎖）、超載（能量+水晶/奧法）、死亡虹吸（黑暗+吸血）、野火（生長+火）、強酸（毒+腐蝕）、湮滅（光輝+黑暗）。SkillEffectOp 加了只給反應用的 op（不由單一本源映射）。目前是單技能內反應（沒有跨技能元素狀態）；要加更多反應就改那張表一處。測試綠。

- WIP (every aspect now has a distinct combat variant): the last society/special aspects, previously non-combat, each got their own variant so all 80 aspects are usable in skills. Modifiers (self): civilization is a bulwark (Resistance II), humanity is resolve (big absorption + speed). Effects: commerce plunders (damage + a profit absorption shield for the caster), language is a word of weakness (weakness II + mining fatigue), primordial mutates (a different random debuff each cast), wealth is a Midas strike (the heaviest flat bonus hit). No two aspects share behavior now. Tests green.
- WIP（每個本源現在都有獨立戰鬥變種）：最後的社會/特殊本源（原本非戰鬥）也各自獨立成變種，所以全部 80 個本源都能用在技能裡。修飾（自我）：文明是堡壘（抗性 II）、人性是決意（大吸收+速度）。效果：商貿掠奪（傷害+給施法者護盾）、語言是詛咒之言（虛弱 II+挖掘疲勞）、原初突變（每次隨機一種 debuff）、財富點金一擊（最重單發加傷）。現在沒有任何兩個本源行為相同。測試綠。

- WIP (abstract aspects get imaginative effects): the 8 aspects that had no skill role at all are now usable, each with a creative effect. EXCAVATION shreds armor (armor-bypassing hit + mining fatigue); BESTIA is a savage maul + bleed; HARVEST is a scythe AoE that hits everything around the target; SENSUS exposes (long see-through-walls glow + slow); COGNITION scrambles the mind (nausea + weakness); DESIRE lures the target toward you and slows it; LAW passes judgment, rooting the target and hitting harder the more debuffs it already carries (a finisher you set up with other effects); AUTOMATION is a modifier that adds an extra shot (automated repeater). Roles + opFor + a couple of imports; tests green.
- WIP（抽象本源給有創意的效果）：原本完全沒技能角色的 8 個本源現在都能用，各有腦洞效果。挖掘＝破甲（無視護甲的傷害+挖掘疲勞）；獸性＝兇猛撕咬+流血；收割＝鐮刀範圍橫掃打到目標周圍所有東西；感知＝曝光（長時間穿牆發光+緩速）；認知＝心智干擾（暈眩+虛弱）；慾望＝把目標拉向你並緩速；法則＝制裁，定身目標、目標身上負面層數越多打得越重（拿來收尾、配合其他效果疊層）；自動化＝修飾，多射一發（自動連射）。角色+opFor+幾個 import；測試綠。

- WIP (energy = explosion, scales to a nuke): the ENERGY effect aspect is now a no-grief detonation (entity damage + knockback, no block breaking) whose radius scales with power. So energy turns any carrier into its explosive variant, and amplifiers turn a grenade into a nuke: machine + energy = grenade launcher, machine + energy + qian (power x2) = a bigger blast, machine + energy + refraction = a 3-shell cluster, momentum + energy = an exploding orb. One op, a whole row of variants. Tests green.
- WIP（能量＝爆炸，疊到核彈）：能量效果本源現在是無破壞爆炸（對實體傷害+擊退、不炸地形），半徑隨 power 放大。所以能量讓任何載體變成爆裂版，增幅本源把榴彈變核彈：機械+能量=榴彈砲、機械+能量+乾（power×2）=更大爆炸、機械+能量+折射=三連集束彈、動力+能量=會爆的法球。一個 op 開一整排變種。測試綠。

- WIP (MACHINE carrier fires floating-turret bolts): the MACHINE aspect (previously roleless) is now a CARRIER that fires the boss weapon's FloatingTurretProjectile, carrying the skill's damage and effect ops. So the carrier x effect matrix produces variants: machine + fire = a burning turret bolt, machine + frost = an ice bolt, machine + wither = a decay bolt, etc. FloatingTurretProjectile gained an optional skill payload (setSkillPayload: custom damage + ops applied on hit); empty by default so the normal turret is unchanged. MACHINE carrier damage x1.1. Tests green; needs runClient to feel it.
- WIP（機械載體發射浮游砲子彈）：MACHINE 本源（原本沒角色）現在是 CARRIER，發射 boss 武器的 FloatingTurretProjectile，帶技能傷害 + 效果 ops。所以「載體 × 效果」矩陣會產生變種：機械+火=燃燒砲彈、機械+霜=冰砲彈、機械+凋零=腐蝕砲彈…。FloatingTurretProjectile 加了選用技能酬載（setSkillPayload：自訂傷害 + 命中套 ops），預設空所以一般浮游砲行為不變。機械載體傷害 ×1.1。測試綠；要 runClient 體驗。

- WIP (every effect aspect is now unique): previously ~31 effect aspects collapsed into ~8 shared ops (all 6 fire aspects did the exact same thing, all 6 death aspects too, etc.), so picking different aspects in the same family was identical and felt repetitive. SkillEffectOp now has one op per effect aspect, each with distinct behavior built from vanilla status effects + small mechanics: e.g. magma erupts the target upward, li brands with glow, furnace is a long smelt-burn, vapor slows, steam blinds; kan drags the target toward you, dui launches it up, storm blasts it away; eldritch is darkness+nausea, spiritus is a wither+slow+glow soul drag, void levitates. opFor maps each aspect 1:1 (abstract/society aspects still return no op). The carrier pipeline (List<SkillEffectOp>) is unchanged; tests green.
- WIP（每個效果本源現在都獨一無二）：先前約 31 個效果本源被收斂成約 8 個共用 op（6 個火本源效果完全一樣、6 個死亡本源也一樣…），所以同類裡換不同本源毫無差別、很重複。SkillEffectOp 現在每個效果本源一個專屬 op，各有不同行為，用 vanilla 狀態效果 + 小機制做出來：例如熔岩把目標噴飛、離烙上發光標記、爐火是長時間熔燒、蒸騰減速、蒸汽致盲；坎把目標拉向你、兌把它打上天、風暴把它轟飛；異界是黑暗+混亂、靈魂是凋零+減速+發光的拉魂、虛空使其漂浮。opFor 每個本源 1:1 對應（抽象/社會類本源仍無 op）。載體管線（List<SkillEffectOp>）不變；測試綠。

- WIP (aspect codec board, foundation industry payoff): added the Aspect Codec Board item, the single skill-system material that routes through the existing tech tree (grinder -> substrate, circuit chain -> precision circuit, altar -> wafer, mirror boss -> shard, mana crafting table assembles the board). The skill encoder and spell core recipes now consume the board instead of the raw shard, so the shard appears in exactly one recipe (the board) as the single boss gate, and the earlier machines all gain purpose. Item auto-models via the ModItemModelProvider loop once the texture exists (skipped, with a warn, until then). Lang en/zh_tw added. Datagen: run runData.
- WIP（本源編碼基板，地基工業的用處）：新增本源編碼基板物品，作為技能系統唯一的材料，串起既有技術樹（研磨機→基板、電路鏈→精密電路板、祭壇→晶圓、鏡中 boss→碎片，魔力工作台組成基板）。技能編碼台與法術核心配方改吃這片基板而非生碎片，所以碎片只出現在一個配方（基板）當單一 boss 閘門，前面的機器全都有了用處。物品模型由 ModItemModelProvider 迴圈自動產（貼圖存在前會跳過並警告）。補 lang en/zh_tw。Datagen：跑 runData。 mana materials (dusts, ingots, crystals, alloy, adhesive, wire, substrate, wafer), circuits, the mana eye, and the magic ores were redrawn with fresh textures contributed by 蕎麥麵 (bksesame). The Aspect Altar and the Mana Bloom flower also got new 3D models and textures (the flower is now a sculpted bloom instead of a flat cross).
更新多個物品與方塊貼圖：魔力材料（各種粉、錠、水晶、合金、黏膠、導線、基板、晶片）、電路板、魔力之眼、魔法礦，由貢獻者 蕎麥麵 (bksesame) 繪製。本源祭壇與魔力花也換上新的 3D 模型與貼圖（魔力花從平面十字改成立體花朵）。

### Developer Notes / 開發者備註

- WIP (JEI Boss Drops page + extensible boss-loot system): players can now see what a boss drops in JEI. New shared BossLootRegistry / BossLootEntry / BossDrop model is the single source of truth: datagen builds each boss's chest loot table from it, the JEI "Boss Drops" page renders the same definition, and a future boss is one register() call (its loot table and JEI row both follow automatically). Refactored the mirror boss loot table to be built from the registry, and PlayerCloneDeathSequence now references the registry's table id. New BossLootCategory + BossLootJEIPlugin (client-only via @JeiPlugin; the shared model imports no JEI, so the server path stays clean). Right-clicking a drop opens the page; boss icons are catalysts. Lang keys added (en/zh_tw).
- WIP（JEI Boss 掉落分頁 + 可擴充 boss 掉落系統）：玩家現在能在 JEI 看 boss 掉什麼。新增共用 BossLootRegistry / BossLootEntry / BossDrop 模型當單一真實來源：datagen 用它建每隻 boss 的寶箱 loot table、JEI「Boss 掉落」分頁渲染同一份定義，之後加 boss 只要一個 register()（loot table 跟 JEI 列都自動跟上）。把鏡中 boss 的 loot table 改成從註冊表建，PlayerCloneDeathSequence 也改引用註冊表的表 id。新增 BossLootCategory + BossLootJEIPlugin（透過 @JeiPlugin 只在 client 載；共用模型不 import 任何 JEI，伺服器路徑乾淨）。右鍵掉落物開分頁；boss 圖示是催化劑。補 lang（en/zh_tw）。

- WIP (mirror boss reward = real loot table): the Mirror World boss reward chest was hand-filled with five hardcoded setItem calls. It now uses a proper chest loot table (chests/mirror_boss_reward: guaranteed Mirror Core Shard + a rolled pool of mana ingot/dust/corrupted dust/crystal) applied via setLootTable, so the chest fills from the table on open and re-rolls each clear (the open-the-chest reward moment is preserved). Single source of truth, JER/JEI-readable; sets up the upcoming "Boss Drops" JEI page. Datagen: run runData to emit the loot JSON.
- WIP（鏡中 boss 獎勵改成正規 loot table）：鏡中世界 boss 的獎勵寶箱原本用五行寫死的 setItem 填。現在改用正規 chest loot table（chests/mirror_boss_reward：保證鏡核碎片 + 隨機池魔力錠/粉/汙穢粉/水晶），透過 setLootTable 套用，所以寶箱在開啟時才從表填、每次過關重 roll（保留開箱領獎的體驗）。單一真實來源、JER/JEI 可讀；為接下來的「Boss 掉落」JEI 分頁鋪路。Datagen：跑 runData 生 loot JSON。

- WIP (skill system recipes, mirror-boss material gate): the two skill-system pieces are now obtainable, gated by material rather than research (matching the design: only the early tutorial researches hard-lock; later ones only guide). Both the Skill Encoder block and the Spell Core item now require the Mirror Core Shard, the Mirror World boss drop that was previously unused by any recipe, so the whole skill system naturally opens only after that boss (its "本源核心" spine placement). Encoder = precision circuit + high-density core + wafer + shard + substrate; Spell Core = amethyst + shard + crystal + blank core + wire + refined dust. Mana crafting table recipes (datagen): run runData to emit the JSON.
- WIP（技能系統配方，鏡中 boss 材料卡位）：技能系統的兩個關鍵物現在做得出來，用材料卡位而非研究鎖（照設計：只有前期教學研究硬鎖，後續研究只給指引不鎖）。技能編碼台方塊與法術核心物品現在都要鏡核碎片，那是鏡中世界 boss 的掉落物、先前沒被任何配方用到，所以整套技能系統天然只在打贏那隻 boss 之後才開（對應它「本源核心」脊椎的定位）。編碼台＝精密電路板+高密度核+晶圓+鏡核碎片+基板；法術核心＝紫水晶+鏡核碎片+魔力水晶+空白核心+導線+精煉粉。魔力工作台配方（datagen）：跑 runData 才生 JSON。

- WIP (datapack aspect assignment API, mod-compat foundation): modpacks and other mods can now declare item/block -> aspect mappings via JSON without touching code or depending on Koniavacraft. New AspectOverrideLoader (SimpleJsonResourceReloadListener on `data/<ns>/aspect_assignments/*.json`, registered through AddReloadListenerEvent) loads a flat `{ "itemId": ["aspectId", ...] }` table on datapack reload; BlockAspectResolver + RecipeInferenceEngine now consult it right after the cache, ahead of all inference, so assignments are the highest-priority and deterministic (seed only varies what is NOT assigned). Later packs override earlier ones; unknown aspect ids are skipped with a warning. Ships a koniava_defaults example (spell_core / nara_watch / mirror_core_shard). Modder format doc written. Compiles green; needs runClient to verify live.
- WIP（datapack 本源指派 API，模組相容地基）：模組包與其他模組現在能用 JSON 宣告物品/方塊→本源，不用碰程式碼也不用相依 Koniavacraft。新增 AspectOverrideLoader（掛在 `data/<ns>/aspect_assignments/*.json` 的 SimpleJsonResourceReloadListener，透過 AddReloadListenerEvent 註冊），datapack reload 時載入 `{ "物品id": ["本源id", ...] }` 表；BlockAspectResolver + RecipeInferenceEngine 在 cache 之後、所有推論之前查它，所以指派是最高優先且確定性的（種子只變沒指派的部分）。後載蓋前載；未知本源 id 跳過並警告。附 koniava_defaults 範例（spell_core / nara_watch / mirror_core_shard）。已寫給模組作者的格式文件。編譯綠；需 runClient 實機驗。

- WIP (abstract aspects step 1+2): added NON_COMBAT / SPECIAL / SYSTEM aspect roles and tagged the society aspects (civilization/commerce/language/humanity = non-combat, primordial = special, wealth = system) so they stay out of the skill palette; isUsable now means "has a carrier/effect/modifier role". Then gave six more aspects real combat behavior as modifiers: CORPUS (health-boost self-buff), FAITH (regen), ORDER (cleanses the caster's harmful effects on cast), INSTINCT (per-cast crit chance), WISDOM (cuts the skill's mana cost), GEAR (shortens the skill's cooldown). Cooldown is now part of SkillCost (complexity-based) so GEAR can modify it.
- WIP（抽象本源 step 1+2）：新增 NON_COMBAT / SPECIAL / SYSTEM 本源角色，把社會類本源標好（文明/商貿/語言/人性=非戰鬥、原初=特殊、財富=系統），讓它們不出現在技能調色盤；isUsable 改成「有載體/效果/修飾角色」。接著再給六個本源實際戰鬥行為（修飾類）：軀體（肉體強化自我 buff）、信仰（再生）、秩序（施法淨化自身負面）、本能（每次施放暴擊機率）、智慧（降技能魔力）、齒輪（縮短技能冷卻）。冷卻現在是 SkillCost 的一部分（依複雜度），所以齒輪能改它。

- WIP (semantic scanning A+B, complete): item->aspect now combines accuracy + per-world variation + completeness. SemanticAspectMatcher.candidates gives a semantic pool; AspectExpression.seededPick deals from it by genome seed (sensible but varied per world); and SemanticDealer runs a once-per-world pass over the whole item registry that guarantees every plausible aspect has a few "guaranteed carrier" items, so no aspect (and no skill needing it) can become unobtainable by seed luck. BlockAspectResolver + RecipeInferenceEngine resolve via SemanticDealer (guaranteed carriers first, then seeded pick). Abstract aspects with no item carrier still come from block/entity/player scans by design. Unit-tested.
- WIP（語意掃描 A+B，完成）：物品→本源現在同時做到準確 + 每世界變 + 完整。SemanticAspectMatcher.candidates 給語意池；AspectExpression.seededPick 用 genome seed 發牌（合理但每世界不同）；SemanticDealer 每世界跑一次全域 pass 掃整個物品登錄，保證每個合理本源都有幾個「保證來源」物品，所以不會有本源（與需要它的技能）因種子運氣而拿不到。BlockAspectResolver + RecipeInferenceEngine 改用 SemanticDealer 解析（保證來源優先、再種子挑）。沒有物品對應的抽象本源照設計仍由方塊/生物/玩家掃描提供。已單元測試。

- WIP (skill combat content): made most of the AspectRoles dictionary actually do something instead of just costing mana. New on-hit ops: LIGHTNING (zhen/arc), KNOCKBACK (storm/kan/dui), BLIND (radiance), WITHER (death/undead/taint/eldritch/spiritus/void), ROOT (growth, and the binding/gen modifiers), LIFESTEAL (vitae). New modifier behaviors: ANIMA homing, ARC/PROPAGATION chain-to-2, WARDING absorption-on-cast (plus the existing refraction/blade/fortify/qian). New carriers beyond projectiles: FLIGHT dashes the caster, PIPELINE fires an instant hitscan beam, GRAVITY drops a pulling singularity. SkillEffectOp.apply now gets the caster + impact direction. Still no-op (deferred): the heal-family effects (vitality/mending/lifeflow/nourish, which would heal enemies) and some amplify modifiers.
- WIP（技能戰鬥內容）：把 AspectRoles 字典裡大部分本源從「只吃魔力」變成真的有效果。新命中 op：雷擊(震/電弧)、擊退(風暴/坎/兌)、致盲(光輝)、凋零(死/亡靈/污染/異界/靈魂/虛空)、定身(生長,以及定身/艮修飾)、吸血(血氣)。新修飾行為：靈魄追蹤、電弧/蔓延連鎖 2 個、護盾施法時給吸收(加上原有折射/鋒刃/強化/乾)。投射以外的新載體：飛行衝刺、管道瞬發光束、重力黑洞拉扯。SkillEffectOp.apply 現在拿得到施法者 + 命中方向。仍未接(延後)：補血系效果(生命/修補/生命流/滋養,會補到敵人)與部分增幅修飾。

- WIP (skill switching, HUD, icons; A3 + cosmetics): added a Cycle Spell Skill keybind (default X) + SwitchSkillPacket that advances SELECTED_SKILL_INDEX on the held spell core (rewriting the nested core stack) with an action-bar readout. Added a HUD above the hotbar showing the selected skill's name, index (n/total), and mana cost when holding a spell-core wand. Skills now carry an optional custom icon (item id): the encoding bench has a ghost icon cell (click with an item to set, empty-handed to clear, not consumed), and where no icon is set one is auto-generated from the dominant aspect (first effect or carrier) as a tinted hex badge. Icons show in the bench slot row, the HUD, and the live editor preview. The encoding bench screen also: aligns to the real gui texture grids, shows owned aspect counts, has an aspect search box, keeps vanilla item tooltips, and loads a core's existing encoding back into the editor when placed.
- WIP（技能切換、HUD、圖標；A3 + 外觀）：新增切換法術技能按鍵（預設 X）+ SwitchSkillPacket，循環手持法術核心的 SELECTED_SKILL_INDEX（改寫巢狀核心 stack），action bar 顯示切到哪個。新增 hotbar 上方 HUD：手持法術核心法杖時顯示選中技能名、索引（n/總數）、魔力花費。技能可帶可選自訂圖標（物品 id）：編碼台有幽靈圖標格（手持物品點一下設定、空手清除、不消耗），沒設就依主導本源（第一個效果或載體）自動生成染色 hex 徽章。圖標顯示在編碼台核心格、HUD、即時預覽。編碼台另：對齊正式貼圖格線、顯示本源擁有數量、有搜尋框、保留 vanilla 物品 tooltip、放入核心會載回既有編碼。

- WIP (Skill Core Encoding Bench, A2b): a new block + screen to author skills onto a spell core. New SkillEncoderBlock (lightweight container block, no ticking) + SkillEncoderBlockEntity (single core slot) + SkillEncoderMenu + SkillEncoderScreen. The screen mirrors the Aspect Synthesis bench: discovered aspects render as tinted hex cells in a paginated palette (read from ClientResearchCache, filtered to usable roles). Manual role slots (1 carrier, 3 effects, 2 modifiers): select an aspect, click a slot to place it (the AspectRoles dictionary rejects illegal placements with a message), pick which of the core's 6 slots to write, name it, Encode. New EncodeSkillPacket (C2S) validates the recipe + aspect ownership server-side and writes the StoredSkill onto the core. Registered block/BE/menu/screen/packet; self-drop loot. The screen now uses the real gui/skill_encoder.png background with element coordinates measured to match its slot grids (palette over the upper grid, inventory over the lower grid, core slot on the detached top-right cell). The block model placeholder was removed (no borrowed texture): the block has no model until its own block texture + datagen are added. Compiles green; visual pass needs runClient.
- WIP（技能核心編碼台 A2b）：新方塊 + 畫面，把技能編寫進法術核心。新增 SkillEncoderBlock（輕量容器方塊、不 tick）+ SkillEncoderBlockEntity（單核心槽）+ SkillEncoderMenu + SkillEncoderScreen。畫面照本源合成台：已發現本源以染色 hex cell 排成分頁調色盤（讀 ClientResearchCache、過濾成可用角色）。手動角色槽（1 載體 / 3 效果 / 2 修飾）：選本源 → 點槽放入（AspectRoles 字典擋非法放置並提示）→ 選要寫進核心 6 格的哪格 → 命名 → 編碼。新增 EncodeSkillPacket（C2S）server 端驗配方 + 本源擁有後把 StoredSkill 寫進核心。已註冊方塊/BE/選單/畫面/封包；datagen blockstate（placeholder 貼圖）+ 自掉落 loot。GUI 是 placeholder（純色面板、借方塊貼圖），美術待補；方塊模型需跑 runData。編譯綠；視覺要 runClient 驗。

- WIP (skill core storage + cast-from-core, A2a): skills are now stored on the spell core item as player-authored recipes and resolved at cast time. New StoredSkill record (name + carrier + effect ids + modifier ids) with codec + stream codec + compile() (no fuel: the dual cost model makes aspects a gate, mana the consumable). Two new spell-core components: STORED_SKILLS (up to 6) and SELECTED_SKILL_INDEX. New SkillEncoding helper: isValidRecipe (role legality via the AspectRoles dictionary: carrier carries, effects are effects, modifiers modify, at least one effect), playerOwns (aspect gate), setSlot/getSelected (core read/write), resolveCastSkill (reads the wand's installed core's selected skill). CastSkillPacket now casts the core's selected skill, falling back to the demo registry id only when none is encoded. No GUI yet (that is A2b: the encoding bench). Unit-tested recipe compile + role validation; compiles green.
- WIP（技能核心存放 + 從核心施放，A2a）：技能改成以玩家自組「配方」存在法術核心物品上，施放當下才解析。新增 StoredSkill record（名稱 + 載體 + 效果 ids + 修飾 ids）含 codec + stream codec + compile()（無料：雙花費模型下本源是 gate、魔力才是消耗）。法術核心兩個新元件：STORED_SKILLS（上限 6）+ SELECTED_SKILL_INDEX。新增 SkillEncoding：isValidRecipe（用 AspectRoles 字典查角色合法性：載體能載、效果是效果、修飾能修飾、至少一個效果）、playerOwns（本源 gate）、setSlot/getSelected（核心讀寫）、resolveCastSkill（讀法杖安裝核心的選中技能）。CastSkillPacket 改成施放核心選中的技能，沒編碼時才 fallback 到範例 registry id。還沒 GUI（那是 A2b：編碼台）。已單元測試配方編譯 + 角色驗證；編譯綠。

- WIP (skill dual cost model): split a skill's price into an aspect GATE (you must own each aspect, never consumed) plus a MANA cost (deducted from the casting wand's MANA_STORED buffer). New SkillCost record; SkillEffect.cost() now returns it. SkillCompiler derives the gate (every used aspect -> own 1) and a mana price from the combination shape (base + carrier + per-effect + per-modifier + per-fuel). New SkillCasting.tryCast(player, wand, skill) is the shared server path: gate-check (no deduct) -> wand mana check/deduct -> execute. CastSkillPacket now finds the held wand and routes through it. This replaces the old "aspects are consumed on cast" behavior. Foundation for the Skill Core Encoding Bench (A1, no UI yet). Unit-tested (cost shape, mana scaling); compiles green.
- WIP（技能雙花費模型）：技能花費拆成本源 GATE（必須擁有、永不消耗）+ 魔力（從施放法杖的 MANA_STORED 扣）。新增 SkillCost record，SkillEffect.cost() 改回傳它。SkillCompiler 從組合推出 gate（每個用到的本源 → 擁有 1）+ 魔力價（base + 載體 + 每效果 + 每修飾 + 每料）。新增 SkillCasting.tryCast(player, wand, skill) 當共用 server 路徑：gate 檢查（不扣）→ 法杖魔力檢查/扣 → 執行。CastSkillPacket 改成找手上法杖再走它。取代舊的「施放扣本源」。技能核心編碼台的地基（A1，還沒 UI）。已單元測試（花費形狀、魔力縮放）；編譯綠。

- WIP (semantic scanning tuning): retuned the `arc` aspect anchor (dropped the polysemous "arc"/"lightning"/"bolt" words that collided with storm/energy, now electric-discharge specific: spark/voltage/electrode/discharge). Expanded the vocab table's DOMAIN_WORDS with more modded tokens (electrum/invar/signalum/enderium/certus/assembler/pulverizer/...) and Koniavacraft coined names (koniava/koneiava/nara/konia) so they stay in-vocab instead of oov-skip. Regenerated the 12MB table; matcher unit test still green.
- WIP（語意掃描調校）：重調 `arc` 本源錨點（拿掉多義的 "arc"/"lightning"/"bolt"，它們跟 storm/energy 撞，改成放電專屬：spark/voltage/electrode/discharge）。詞庫 DOMAIN_WORDS 補了更多模組 token（electrum/invar/signalum/enderium/certus/assembler/pulverizer 等）+ Koniavacraft 自創名（koniava/koneiava/nara/konia），讓它們留在 vocab 內不被 oov-skip。重生 12MB 表；matcher 單元測試仍綠。

- WIP (semantic aspect scanning): items/blocks that miss tag/keyword/recipe layers now get aspects from name semantics via a precomputed word-vector table. SemanticAspectMatcher loads /koniava_data/aspect_semantic/ (built offline by tools/aspect_embedding with a 226MB fastText model; only the ~12MB int8 table ships) into RAM once, then tokenizes the id, averages token vectors, and cosine-matches aspect anchors (top 1-2 above a confidence margin). Wired into BlockAspectResolver + RecipeInferenceEngine before the hash fallback. Handles OOV modded words via subword (osmium_ingot -> metal). Table absent (fresh checkout) = no-op. Unit-tested (no MC context needed).
- WIP（本源語意掃描）：tag/keyword/配方都沒中的物品/方塊，改用名字語意 + 預算詞向量表掃本源。SemanticAspectMatcher 把 /koniava_data/aspect_semantic/（tools/aspect_embedding 用 226MB fastText 離線生，出貨只帶 ~12MB int8 表）載進 RAM 一次，tokenize id → 向量平均 → cosine 比對本源錨點（信心門檻取 top 1-2）。接進 BlockAspectResolver + RecipeInferenceEngine 的 hash fallback 前。OOV 模組詞靠 subword（osmium_ingot→metal）。表缺則 no-op。已單元測試（不需 MC 環境）。

- WIP (skill resolve engine): skills are now compiled from aspect combinations, not hardcoded. SkillCompiler.compile(carrier, effects, modifiers, fuel) -> CompiledSkill (cost + action). SkillEffectOp (FIRE/FROST/POISON/WEAKEN) as on-hit payloads; SpellProjectileEntity carries ops + pierce. Demos fireball/frostbolt/fire_split are all compiled from aspect mixes (deleted hardcoded FireballEffect). Proves B: new combos = new skills, no new code.
- WIP（技能 resolve 引擎）：技能改成從本源組合編譯，非寫死。SkillCompiler.compile(載體, 效果, 修飾, 料) → CompiledSkill。SkillEffectOp（火/冰/毒/弱）當命中酬載；SpellProjectileEntity 帶 ops + 穿透。fireball/frostbolt/fire_split 全是組合編譯（刪了寫死的 FireballEffect）。證明 B：換組合=換技能、不寫 code。

- WIP (aspect skill system foundation): generic skill execution layer + spell core trigger + C dictionary. New `research/skill/` package: SkillEffect (cost()+execute()), SkillContext, SkillRegistry, SkillRole (FUEL/CARRIER/EFFECT/MODIFIER/TRIGGER), AspectRoles (Aspect -> Set<SkillRole>, multi-role, isUsable/isHighTier). FireballEffect demo + SpellProjectileEntity (NoopRenderer, particle-driven). CastSkillPacket (C2S, server-authoritative aspect check/consume). New SPELL WandCoreBehavior + spell_core item; WandRodItem.use() delegates to IWandCore.coreUse(). SELECTED_SKILL data component. Not player-complete: casting path is transitional pending the Skill Core Encoding Bench + combination resolver. Compiles green.
- WIP（本源技能系統地基）：通用技能執行層 + 法術核心觸發 + C 字典。新 `research/skill/` 套件：SkillEffect、SkillContext、SkillRegistry、SkillRole、AspectRoles（本源→角色，多角，isUsable/isHighTier）。FireballEffect 範例 + SpellProjectileEntity（NoopRenderer、粒子驅動）。CastSkillPacket（C2S，server 權威檢查/扣本源）。新增 SPELL 核心行為 + spell_core 物品；WandRodItem.use() 委派給 IWandCore.coreUse()。SELECTED_SKILL data component。尚未玩家可用：施放路徑是過渡，待技能核心編碼台 + 組合 resolver。編譯綠。

## [0.0.1.9-2] - 2026-05-31

### Player Changes / 玩家更新內容

Fixed: the Mana Plate Press could not receive mana (or items) from conduits or adjacent blocks, so it never worked. Its capabilities were missing and are now registered like the other machines.
修復：魔力壓板機無法從導管或相鄰方塊接收魔力（與物品），所以根本不會運作。它的 capability 之前漏註冊，現在比照其他機器補上。

Fixed: the Mana Plate Press press head no longer stays frozen while working. It now visibly stamps up and down during pressing and eases back when it stops.
修復：魔力壓板機運作時壓板頭不再卡住不動。加工中會明顯上下壓印，停止時平滑回到原位。

Added the Sandbag Cactus: a craftable cactus dummy you place to test damage. It can't be pushed and never dies. The moment you hit it you enter a combat session and a timer starts at the lower-center of the screen, showing elapsed time, total damage, DPS, and hit count (alongside the floating damage numbers). You control the pace: keep hitting for as long as you like; after 10 seconds without a hit the session ends and a result panel shows your total damage, time, and DPS. It ignores environmental damage (lava, fire, fall, etc.) so only your attacks count, and sneak + right-click picks it back up.
新增沙包仙人掌：可合成的仙人掌假人，放下來測傷害用。推不動、不會死。一打到就進入戰鬥狀態，螢幕中下開始計時，顯示用時、總傷害、DPS、命中數（搭配浮動傷害數字）。節奏你自己掌握：想打多久打多久；超過 10 秒沒再打就結束，跳出結算面板顯示總傷害、用時、DPS。無視環境傷害（岩漿、火、掉落等），只算你的攻擊；潛行 + 右鍵可收回。

Dual-wielded floating turrets can now fire control shots by charge-release timing. While charging, a bar appears at the lower-center of the screen: release in the left zone for a weak charged shot, the right zone for a full charged shot, and the middle zone for a control shot. The middle zone splits into one slot per installed control upgrade (slow / root / levitate), each its own color, so where you release picks which effect fires. No extra keys, no mode toggle: pure release-timing skill. Only shows when dual-wielding turrets.
雙持浮游砲現在能靠蓄力鬆手時機發射控制彈。蓄力時螢幕中下出現一條蓄力表：鬆手在左段是弱蓄力彈，右段是強蓄力彈，中段則發射控制彈。中段依你裝的控制插件數量分格（緩速 / 定身 / 漂浮各一格、各自顏色），鬆手在哪格就放哪個效果。不用多按鍵、不用切模式，純靠鬆手時機。只在雙持浮游砲時顯示。

Fixed: in the Mirror Dimension, Nara's phantom would clip into the ground while following you. She now tracks you horizontally and stays at your height.
修復：鏡中世界裡娜拉幻影跟隨你時會鑲進地底。改成水平追蹤並貼合你的高度。

Fixed: re-entering the Mirror boss fight would scatter the previous run's unclaimed reward chest items onto the floor. The arena reset no longer drops them.
修復：重新挑戰鏡中 boss 時，上一場沒拿走的獎勵箱物品會散落一地。場地重置不再噴出它們。

Fixed: the Mirror boss death no longer plays vanilla's red hurt overlay or the sideways fall-over. The custom death cinematic now shows clean.
修復：鏡中 boss 死亡不再出現原版的紅色受傷濾鏡與側倒動畫。自製死亡演出現在乾淨呈現。

Fixed: Nara's phantom no longer blocks your attacks during the boss fight. She is now non-targetable, so melee, projectiles, and turret auto-aim pass through her to the boss.
修復：戰鬥中娜拉幻影不再擋住你的攻擊。她現在不可被瞄準，近戰、投射物、浮游砲自動瞄準都會穿過她打到 boss。

The two sound settings (Nara voice volume and boss BGM volume) are now grouped together under a single "Sound" page in the mod config.
模組設定中兩個聲音選項（娜拉配音音量、boss BGM 音量）現在合併到同一個「音效」分頁。

Added a config toggle for floating damage numbers (render page). Turn it off if you prefer no damage numbers when dealing damage.
新增浮動傷害數字的開關（在渲染分頁）。不想看到傷害數字可以關掉。

The boss BGM and Nara voice volume sliders now also appear directly in the vanilla Sound options screen, right after the built-in volume sliders. Dragging the boss BGM slider takes effect in real time, even while the music is already playing.
Boss BGM 與娜拉配音的音量條現在也直接出現在原版「音效」設定畫面，接在原版音量條後面。拖動 boss BGM 音量條會即時生效，連音樂正在播放時也行。

Added a "Reload Sound" button to the top-right of the Sound options screen. If your audio cuts out (for example after Windows switches audio device), press it to restart the sound engine without quitting the game.
在「音效」設定畫面右上角新增「重載聲音」按鈕。如果聲音斷掉（例如 Windows 切換音訊裝置後），按一下就能重啟音效引擎，不用重開遊戲。

Filled in the in-game guide with the previously blank machine and weapon pages (solar mana collector, mana charger, mana deployer, mana plate press, upgrades, wand, sandbag cactus).
補齊遊戲內指引中原本空白的機器與武器頁面（太陽能魔力收集器、魔力充能器、魔力部署器、魔力壓板機、升級、魔杖、沙包仙人掌）。

Fixed outdated in-game guide text: the wand page no longer points to a nonexistent "resonator altar", and the altar instructions now use the modular wand (fitted with an Activation Core Plugin) instead of the removed Ritual Wand. Also clarified that a wand rod holds one core but can take multiple upgrades.
修正遊戲內指引的過時文字：魔杖頁不再指向不存在的「諧振器祭壇」，祭壇說明改用模組化魔杖（裝啟動核心插件）取代已移除的儀式魔杖。並說明一個杖柄只能裝一個核心，但可裝多個升級。

The Mirror Core Shard reward from the Mirror boss is now given on every defeat, not only the first clear.
鏡面 boss 的鏡核碎片獎勵現在每次擊敗都會給予，不再只限首次過關。

Fixed two in-game guide pages that named the Grinder and Infuser inconsistently in Chinese; they now match the blocks' actual display names.
修正指引中兩處中文機器名與實際顯示名不一致：改為「粉碎機」「注入機」（原誤寫「研磨機」「灌注機」）。

Regenerated several Nara voice lines (Mirror boss victory/intro/taunt, research table, punishment, angry) so the audio matches the on-screen dialogue text.
重新生成多條娜拉語音（鏡面 boss 勝利/進場/嘲諷、研究台、懲罰、生氣），讓配音對齊畫面字幕。

### Developer Notes / 開發者備註

- Content (mirror core shard repeatable): PlayerCloneDeathSequence.spawnRewardChest no longer gates the Mirror Core Shard (slot 13) behind the includeShard/firstClear flag; the shard is placed every time the reward chest spawns. The includeShard parameter is kept (it still drives clear-state flow elsewhere), it just no longer gates the shard.
- 內容（鏡核碎片可重複）：PlayerCloneDeathSequence.spawnRewardChest 不再用 includeShard/firstClear 把鏡核碎片（slot 13）鎖在首次；獎勵箱每次生成都放碎片。includeShard 參數保留（仍服務過關狀態等其他流程），只是不再 gate 碎片。

- Feature (in-game sound sliders + reload button): SoundOptionsScreenMixin injects BEFORE the vanilla Options.soundDevice() call in addOptions, pulling the OptionsList via OptionsSubScreenAccessor and adding two OptionInstance<Double> volume sliders (options.koniava.boss_music / nara_voice, UnitDouble, % label) through list.addSmall so they pair-render like vanilla source volumes. The "Reload Sound" button (gui.koniava.reload_sound) is added top-right via a new ScreenAccessor (@Mixin(Screen.class), @Invoker addRenderableWidget): the invoker must target Screen, not OptionsSubScreen, since the method is declared on Screen and @Invoker does not search superclasses. Button calls SoundManager.reload() (re-inits OpenAL only, no texture/model reload). Both mixins registered in koniava.mixins.json client array.
- 功能（遊戲內音量條 + 重載按鈕）：SoundOptionsScreenMixin 在 addOptions 裡 vanilla Options.soundDevice() 呼叫 BEFORE 注入，透過 OptionsSubScreenAccessor 取 OptionsList，用 list.addSmall 加兩條 OptionInstance<Double> 音量條（options.koniava.boss_music / nara_voice，UnitDouble，% 標籤），跟 vanilla source volumes 同款兩兩配對排版。「重載聲音」按鈕（gui.koniava.reload_sound）走新的 ScreenAccessor（@Mixin(Screen.class)、@Invoker addRenderableWidget）加在右上：invoker 必須掛 Screen 不能掛 OptionsSubScreen，因為方法宣告在 Screen 上而 @Invoker 不搜父類。按鈕呼叫 SoundManager.reload()（只重建 OpenAL，不重載貼圖/模型）。兩個 mixin 都註冊在 koniava.mixins.json client 陣列。
- Feature (live BGM volume): BossBgmPacket's nested BgmInstance now extends AbstractTickableSoundInstance (was SimpleSoundInstance.forUI, which baked volume at play time). tick() re-reads ModClientConfig.bossMusicVolume each tick so the slider applies in real time; <=0 calls stop() for instant mute. Kept SoundSource.MASTER + relative + Attenuation.NONE so the boss BGM still plays when the vanilla music slider is at 0 and stays at a fixed global volume. Client-only classes remain confined to the nested ClientHandler for sided-load safety.
- 功能（即時 BGM 音量）：BossBgmPacket 的巢狀 BgmInstance 現在 extends AbstractTickableSoundInstance（原本 SimpleSoundInstance.forUI 在播放當下就把音量定死）。tick() 每 tick 重讀 ModClientConfig.bossMusicVolume 讓滑桿即時生效；<=0 呼叫 stop() 即時靜音。保留 SoundSource.MASTER + relative + Attenuation.NONE，讓 boss BGM 在 vanilla 音樂條歸 0 時仍會播、且維持全域固定音量。client-only 類別仍封在巢狀 ClientHandler 保 sided-load 安全。
- Perf (BGM volume write guard): BgmInstance.tick() now only writes this.volume when the new config value differs from the current one (if (v != this.volume)), skipping a redundant field write every tick when the slider hasn't moved.
- 性能（BGM 音量寫入守衛）：BgmInstance.tick() 現在只在新 config 值跟現值不同時才寫 this.volume（if (v != this.volume)），滑桿沒動時跳過每 tick 的多餘欄位寫入。
- Content (guide pages): filled the previously blank NaraGuideScreen entries (mana chapter: solar collector / charger / deployer / plate press; weapons chapter: upgrade / wand / sandbag cactus) with parity-checked en_us + zh_tw lang keys.
- 內容（指引頁）：補齊 NaraGuideScreen 原本空白的條目（魔力章：太陽能收集器 / 充能器 / 部署器 / 壓板機；武器章：升級 / 魔杖 / 沙包仙人掌），en_us + zh_tw lang 鍵已對齊核對。

- Fix (Mana Plate Press unusable): ModCapabilities never registered MANA_PLATE_PRESS_BE, so neither the MANA cap nor the item handler was exposed; conduit network + neighbor pull saw no consumer. Added manaIO + itemHandlerIO for it (AbstractManaMachineEntityBlock already implements IConfigurableBlock, so the generic helpers apply, same as the grinder/infuser).
- 修復（壓板機不可用）：ModCapabilities 從沒註冊 MANA_PLATE_PRESS_BE，MANA cap 與物品 handler 都沒暴露，導管網路 + 鄰居 pull 看不到消耗端。補上 manaIO + itemHandlerIO（AbstractManaMachineEntityBlock 已 implements IConfigurableBlock，泛型 helper 直接適用，跟粉碎機/注入機同寫法）。
- Fix (Mana Plate Press no animation + jerk): the renderer drove the press-head offset from blockEntity.getPressingProgress(), but progress is only marked with setChanged() and never synced to the client, so client progress stayed 0 and the head never moved. Rewrote the animation to run off the WORKING blockstate (synced via setBlock flag 3): a local game-time press curve (smoothstep down -> hold at bottom -> smoothstep up) with MAX_PRESS_OFFSET 0.6875 (11px, rests on the anvil), cycle length = getMaxPressingTime() so one stamp == one craft, phase locked to the moment WORKING starts (no snap on start). Also fixed the periodic twitch: isWorking() returned progress>0, which flickered false for the one tick progress resets to 0 on each craft completion, toggling the WORKING blockstate every cycle; changed to progress>0 || canGenerate() so it stays working while there is more to press. Model: moved the Y9-10 anvil slab out of the 壓板核心 group into 主體 so only the top head animates. Zero extra network cost.
- 修復（壓板機沒動畫 + 抽動）：renderer 用 blockEntity.getPressingProgress() 算壓頭位移，但 progress 只 setChanged()、不同步到 client，所以 client progress 全程 0、壓頭不動。改成由 WORKING blockstate 驅動（透過 setBlock flag 3 同步）：本地遊戲時間壓印曲線（smoothstep 下壓 → 停底 → smoothstep 回升），MAX_PRESS_OFFSET 0.6875（11px，剛好貼底砧），循環長度 = getMaxPressingTime() 讓一次壓印 = 一次合成，phase 鎖在 WORKING 開始時刻（開工不瞬移）。另修週期抽動：isWorking() 原本 progress>0，每次合成完成那 tick progress 歸 0 會閃 false，導致 WORKING blockstate 每循環閃一下；改成 progress>0 || canGenerate()，還有料就維持 working。Model：把 Y9-10 底砧片從「壓板核心」移到「主體」，只剩上方壓頭會動。零額外封包。

- Feature (training dummy): new TrainingDummyEntity (Mob, NoAi, isPushable=false, KNOCKBACK_RESISTANCE 1.0, MAX_HEALTH 1_000_000 + heal-to-full each tick and on hurt with invulnerableTime reset to 0 so every hit counts). Per-attacker Session map (totalDamage / hitCount / startTick / lastHitTick); recordHit called from TrainingDummySessionHandler on LivingDamageEvent.Post (final damage, same value as the floating numbers). tick() ends a session after SESSION_TIMEOUT_TICKS (200t) with no hit. S2C TrainingDummyStatsPacket (ended / totalDamage / hitCount / durationTicks) drives the TrainingDummyHudOverlay (RenderGuiEvent.Post, lower-center): running timer via a client-local start time so it advances between hits, then a 5s result panel on timeout. TrainingDummyItem.useOn spawns it; TrainingDummyModel/Renderer use generated placeholder cactus textures. Recipe + item model via datagen (run runData). Renamed display name to "Sandbag Cactus" (registry id stays training_dummy). hurt() ignores damage with no attacker (source.getEntity()==null) so lava/fire/fall/block damage is no-op, plus fireImmune()=true. remove() flushes all sessions with an ended packet so the HUD timer stops when the dummy is picked up/removed. Verified by compileJava; in-game behavior + visuals need manual test.
- 功能（訓練假人）：新增 TrainingDummyEntity（Mob、NoAi、isPushable=false、KNOCKBACK_RESISTANCE 1.0、MAX_HEALTH 1_000_000 + 每 tick 與 hurt 時回滿、hurt 把 invulnerableTime 歸 0 讓每下都算）。依攻擊者分開的 Session map（累積傷害 / 命中數 / 起始 / 最後命中 tick）；recordHit 由 TrainingDummySessionHandler 在 LivingDamageEvent.Post 用最終傷害呼叫（與浮動數字同值）。tick() 在 SESSION_TIMEOUT_TICKS（200t）沒命中後結束 session。S2C TrainingDummyStatsPacket（ended / 累積傷害 / 命中數 / 用時 tick）驅動 TrainingDummyHudOverlay（RenderGuiEvent.Post，中下）：進行中用 client 本地起始時間跑計時器，所以兩次命中之間也前進，逾時跳 5 秒結算面板。TrainingDummyItem.useOn 生成；TrainingDummyModel/Renderer 用程式生成的仙人掌 placeholder 貼圖。配方 + 物品模型走 datagen（要跑 runData）。顯示名改為「沙包仙人掌」（registry id 維持 training_dummy）。hurt() 無視沒有攻擊者的傷害（source.getEntity()==null），岩漿/火/掉落/方塊傷害都 no-op，加 fireImmune()=true。remove() 把所有 session 用 ended 封包收尾，拿起/移除時 HUD 計時器才會停。靠 compileJava 驗證；實機行為與視覺待手動測。

- Floating turret hand-mode control shots: FloatingTurretItem.releaseUsing splits the long-press charge-release into three bands via CONTROL_BAND_MIN/MAX (0.40/0.75, shared with the client). The middle band fires a control shot, picking the effect by equal-division index over installedControls(main, off) (union of both hands' control upgrades, enum-ordinal sorted for a stable bar order). New fireControlShot (charged-shot-level cost, single center bolt via shootControl) and the TurretChargeBarOverlay client HUD (RenderGuiEvent.Post, only on dual-wield + using item). Verified by compileJava + 66/66 GameTest; in-game behavior needs manual test.
- 浮游砲手持控制彈：FloatingTurretItem.releaseUsing 把長按蓄力鬆手依 CONTROL_BAND_MIN/MAX（0.40/0.75，與 client 共用）分三段。中段發控制彈，依 installedControls(main, off)（兩手控制升級聯集、按 enum ordinal 排序保證條上順序穩定）等分取 index 選效果。新增 fireControlShot（消耗同蓄力彈、單發中心彈走 shootControl）與 TurretChargeBarOverlay client HUD（RenderGuiEvent.Post，只在雙持 + 蓄力時顯示）。靠 compileJava + 66/66 GameTest 驗證；實機行為待手動測。
- Fix (Nara phantom clipping into ground): NaraPhantomEntity.followSourcePlayer used a 3D direction + setPos (no collision check), so dir.y pulled the phantom through terrain toward the player's feet. Now tracks horizontally only and pins Y to the player's height.
- 修復（娜拉幻影鑲地底）：NaraPhantomEntity.followSourcePlayer 用 3D 方向 + setPos（無碰撞檢測），dir.y 把幻影沿直線往玩家腳底拉穿地形。改成只水平追蹤、Y 貼玩家高度。
- Fix (reward chest re-drops previous run's items): VoidMirrorEvents.resetArena cleared MODIFIED_BLOCKS (which includes the reward chest position) with setBlockAndUpdate(air), triggering Containers.dropContents. Switched to setBlock with UPDATE_SUPPRESS_DROPS (flag 32). See reference_vanilla_chest_drop.
- 修復（獎勵箱重噴上一場物品）：VoidMirrorEvents.resetArena 用 setBlockAndUpdate(air) 清 MODIFIED_BLOCKS（含獎勵箱位置），觸發 Containers.dropContents。改用 setBlock + UPDATE_SUPPRESS_DROPS（flag 32）。見 reference_vanilla_chest_drop。

- Fix (boss death vanilla artifacts): new LivingEntityRendererMixin injects HEAD of getOverlayCoords returning NO_OVERLAY when PlayerCloneEntity.getDeathPhase() > 0 (kills the red hurt/death tint that vanilla applies whenever deathTime > 0, which spanned the whole custom cinematic). PlayerCloneRenderer.getFlipDegrees overridden to 0 to cancel the vanilla sideways fall-over (setupRotations tips the body 90 degrees on Z while deathTime > 0). Both scoped to the clone boss only.
- 修復（boss 死亡原版殘留）：新增 LivingEntityRendererMixin，在 getOverlayCoords HEAD 注入，PlayerCloneEntity.getDeathPhase() > 0 時回 NO_OVERLAY（消掉 vanilla 在 deathTime > 0 全程套的紅色受傷/死亡濾鏡，蓋住整段自製演出）。PlayerCloneRenderer.getFlipDegrees 覆寫回 0，取消 vanilla 側倒（setupRotations 在 deathTime > 0 沿 Z 軸傾 90 度）。兩者都只限分身 boss。

- Fix (Nara phantom blocks player damage): NaraPhantomEntity now overrides isPickable() to return false. While following the player she could stand between the player and the boss; isPickable controlling ray-cast / projectile / turret auto-aim hit detection meant she ate hits meant for the boss. Returning false makes her purely visual.
- 修復（娜拉幻影擋玩家傷害）：NaraPhantomEntity 覆寫 isPickable() 回 false。跟隨玩家時她可能站到玩家與 boss 之間；isPickable 控制近戰 ray-cast / 投射物 / 浮游砲自動瞄準的命中判定，導致她吃掉本要打 boss 的攻擊。回 false 讓她純視覺。

- Config (merge sound settings + fill missing translations): ModClientConfig moved naraVoiceVolume + bossMusicVolume out of the separate "nara"/"bossMusic" sections into one "sound" section; translation keys renamed to koniava.config.sound.{naraVoiceVolume,bossMusicVolume}. Added the missing koniava.config.sound.bossMusicVolume.tooltip, koniava.config.cinematic.skipDontAsk.tooltip, and section headers koniava.configuration.sound + koniava.configuration.cinematic (3-layer: header/tooltip/button), both en_us + zh_tw.
- Config（合併聲音設定 + 補缺翻譯）：ModClientConfig 把 naraVoiceVolume + bossMusicVolume 從各自的 "nara"/"bossMusic" section 併進單一 "sound" section；翻譯鍵改名 koniava.config.sound.{naraVoiceVolume,bossMusicVolume}。補上缺的 koniava.config.sound.bossMusicVolume.tooltip、koniava.config.cinematic.skipDontAsk.tooltip，以及 section header koniava.configuration.sound + koniava.configuration.cinematic（三層：header/tooltip/button），en_us + zh_tw 都補。

- Feature (floating damage number toggle): new ModClientConfig.showDamageNumbers BooleanValue (render section, default true). DamageNumberPacket's client handler now gates DamageNumberRenderer.add on showDamageNumbers.get(), so the toggle is client-side visual only (server still sends the packet). Lang koniava.config.render.showDamageNumbers + .tooltip in both locales.
- 功能（浮動傷害數字開關）：新增 ModClientConfig.showDamageNumbers BooleanValue（render section，預設 true）。DamageNumberPacket 的 client handler 現在以 showDamageNumbers.get() 把關 DamageNumberRenderer.add，純 client 視覺開關（server 仍照發包）。lang koniava.config.render.showDamageNumbers + .tooltip 兩語系都補。

- Refactor (PlayerCloneEntity split, Phase 5): extracted the mirror/turret and phase-state subsystems into two controllers. `PlayerCloneMirrorManager` owns the mirrored turret lists + volley state and holds mirrorFrom (recursive container expansion for anti-cheese), self-propelled/handheld turret spawning, and the boss volley skill (collectContained / armorScore / filterMirroredTurret moved with it). `PlayerClonePhaseAI` owns the phase field + wall/drain cooldowns and holds the NORMAL/WALLING/BERSERK transition, berserk wall-teardown + speed, wall-building, and mana drain (BERSERK_SPEED + WALL/DRAIN constants moved with it). The entity keeps thin delegates for the public mirrorFrom and the intro-called rebuildHandTurretsFromEquipped, routes customServerAiStep + NBT through the controllers via helper methods (isWalling / isBerserk / phaseOrdinal / loadPhaseOrdinal), and keeps placedWalls / takeWallBlock / bossHotbar / updateBossBarName on the hub since they are shared across subsystems. Dropped the dead addMirroredTurret. PlayerCloneEntity: 1360 -> 1034 lines; new controllers 293 + 157 lines. Mechanical extraction only, no logic or ordering changes. Verified by compileJava + the full GameTest suite (66/66 green).
- 重構（PlayerCloneEntity 拆分，Phase 5）：把鏡像/浮游砲與血量階段兩個子系統抽成兩個 controller。`PlayerCloneMirrorManager` 擁有鏡像砲清單 + 齊射 state，持有 mirrorFrom（遞迴展開容器杜絕藏盒規避）、自走/手持砲生成、boss 齊射技能（collectContained / armorScore / filterMirroredTurret 一起搬走）。`PlayerClonePhaseAI` 擁有 phase field + 築牆/吸魔冷卻，持有 NORMAL/WALLING/BERSERK 切換、暴走拆牆 + 加速、築牆、吸魔（BERSERK_SPEED + WALL/DRAIN 常數一起搬走）。本體保留公開 mirrorFrom 與 intro 呼叫的 rebuildHandTurretsFromEquipped 兩個薄委派，customServerAiStep + NBT 透過 helper 方法（isWalling / isBerserk / phaseOrdinal / loadPhaseOrdinal）走 controller，placedWalls / takeWallBlock / bossHotbar / updateBossBarName 因跨子系統共用留在樞紐。刪掉死碼 addMirroredTurret。PlayerCloneEntity：1360 → 1034 行；新 controller 293 + 157 行。純機械搬移，沒動邏輯或順序。靠 compileJava + 完整 GameTest 套件驗證（66/66 綠）。

- Refactor (PlayerCloneEntity split, Phase 4): extracted the boss intro cinematic into `PlayerCloneIntroSequence` (controller pattern with entity back-ref). Moved the intro animation state (introTicks / introX / introBaseY / introZ), the 6 INTRO_* timing constants, the smooth/frac easing helpers, and the tick() / revealEquipment / equipBestWeapon / equipBestOffhand / weaponAttack methods. The introActive flag stays on the entity as a cross-system gate (read by customServerAiStep, shouldBeSaved, respawn, and PlayerCloneCombatSkills.tickDive). activateAfterIntro stays on the entity as the boss-start hub (bossEvent / graceTicks / Nara follow / same-source dedup) and is invoked by the controller when the animation ends. isProperWeapon stays on the entity (shared with mirrorFrom's inventory prioritisation) and is referenced statically by the controller. Added a setNoDrop(EquipmentSlot) wrapper since Mob.setDropChance is protected and the same-package controller can't reach it; widened pendingEquipment + rebuildHandTurretsFromEquipped to package-private. PlayerCloneEntity: 1509 -> 1360 lines; new controller 208 lines. Mechanical extraction only, no logic or ordering changes. Verified by compileJava + the full GameTest suite (66/66 green).
- 重構（PlayerCloneEntity 拆分，Phase 4）：把 boss 進場演出抽到 `PlayerCloneIntroSequence`（controller pattern，持 entity back-ref）。搬走進場動畫 state（introTicks / introX / introBaseY / introZ）、6 個 INTRO_* 時間常數、smooth/frac 緩動 helper，以及 tick() / revealEquipment / equipBestWeapon / equipBestOffhand / weaponAttack。introActive 旗標留本體當跨系統閘門（customServerAiStep / shouldBeSaved / 重生 / PlayerCloneCombatSkills.tickDive 都讀）。activateAfterIntro 留本體當 boss 開戰樞紐（bossEvent / graceTicks / Nara follow / 同源去重），動畫結束時由 controller 呼叫。isProperWeapon 留本體（跟 mirrorFrom 的背包排序共用），controller 靜態引用。因 Mob.setDropChance 是 protected、同 package 的 controller 碰不到，加了 setNoDrop(EquipmentSlot) wrapper；pendingEquipment + rebuildHandTurretsFromEquipped 放寬 package-private。PlayerCloneEntity：1509 → 1360 行；新 controller 208 行。純機械搬移，沒動邏輯或順序。靠 compileJava + 完整 GameTest 套件驗證（66/66 綠）。

## [0.0.1.9-1] - 2026-05-30

### Player Changes / 玩家更新內容

- Pressing R to skip a cinematic now opens a confirmation popup ("Skipping cutscenes will miss the full experience / Skip this scene?") with Yes/No buttons and a "Don't ask again" checkbox. Covers all four R-skip points: boss intro, Phase 2 transition, Nara dialogue, and altar upgrade animation. Checking "Don't ask again" + Yes writes to the client config (`cinematicSkipDontAsk`) so future skips bypass the popup entirely. No = cinematic continues uninterrupted.
- 按 R 跳過 cinematic 現在會跳出確認小視窗（「跳過劇情將會缺少完整遊戲體驗 / 請問是否跳過？」）含「是 / 否」按鈕跟「以後不再提示」勾選框。覆蓋四個 R skip 點：boss 進場、Phase 2 變身、娜拉對話、祭壇升級動畫。勾選「以後不再提示」+ 是 → 寫進 client config (`cinematicSkipDontAsk`)，之後直接跳過確認。否 = cinematic 繼續不中斷。

- The Mirror Dimension no longer has a 501x501 WorldBorder cage. The dimension is being repurposed as a shared boss-arena dimension for future fights, so the hard boundary is gone and each boss will manage its own active range. Center 501x501 still has flat terrain (the rest is void), but you can now walk past the old border. Existing save files with the leftover 501 border are reset to the vanilla maximum on level load.
- 鏡中世界不再有 501x501 的 WorldBorder 籠子了。這個維度規劃改成多 boss 共用的場地維度，所以硬邊界取消，未來每隻 boss 自己管自己的活動範圍。中央 501x501 仍有平地（外圍是虛空），但現在可以走出原本邊界。舊存檔殘留的 501 邊界會在維度載入時自動重設回 vanilla 最大值。

- Floating turrets (slot 0/1 auto-attack mode) now target Slime, Ghast, Phantom, and other hostile Mobs that implement the `Enemy` interface but don't extend `Monster`. Previously the hostile filter used `Monster.class` directly, so non-Monster enemies like slimes were silently ignored and the turret just sat there doing nothing while the player took damage. Now it filters on `Mob` + `instanceof Enemy`, covering the full set of vanilla "hostile mob" types.
- 浮游砲（裝備槽 0/1 自走砲模式）現在會攻擊史萊姆、惡魂、幻翼等實作 `Enemy` 介面但不繼承 `Monster` 的敵對生物了。之前的敵對過濾直接用 `Monster.class`，史萊姆這種非 Monster 的敵對被靜默漏掉，砲就乖乖站著不動看你被打。現在改成 `Mob` + `instanceof Enemy`，完整覆蓋 vanilla 所有「敵對生物」類型。

- Player no longer gets controlled by their own floating turret's control projectiles. The control plug-in projectile (slow / root / levitation) was spawned with the turret entity as its owner, not the player; vanilla `canHitEntity` only skips the projectile's own owner, so the player was a valid target and could get hit by their own bullet on close-range firing arcs. Now the projectile is spawned with the player as owner, matching how the normal attack bullet already worked.
- 玩家現在不會被自己浮游砲的控制彈控住了。控制彈插件（緩速 / 定身 / 漂浮）發射時 owner 是砲實體不是玩家，vanilla `canHitEntity` 只擋 owner，所以玩家是合法目標、近距離弧線會打到自己。現在改成 owner 傳玩家，跟普通攻擊彈本來就是這樣的行為一致。

- Mirror Boss's orbiting / handheld floating turrets are now invulnerable AND non-pickable. Two-layer change: (1) `hurt` rejects any player-rooted damage on clone turrets so they can't be ground down; (2) `isPickable` returns false in clone mode so the player's arrows / turret projectiles / melee ray-cast skip the turret entirely and pass through to hit the boss behind it. Previously the turrets could be used as a shield wall around the boss, ruining the fight; now they're a pure visual / offensive threat that players have to dodge but can't waste attacks on. Same-source projectile self-damage is still ignored, and non-player environmental damage still applies.
- 鏡像 Boss 身邊繞行/手持的浮游砲現在玩家完全打不到也選不到。兩層修法：(1) `hurt` 對玩家來源傷害一律回 false，無法被磨血；(2) `isPickable` 在 clone 模式回 false，玩家的箭/浮游砲彈/近戰 ray-cast 直接穿過砲，命中後面的 boss 本體。之前 boss 會把砲當肉盾擋玩家攻擊，體驗很差；現在砲純粹是視覺威脅 + 火力輸出，玩家要閃但不會浪費攻擊在砲上。同源彈自傷免疫保留，非玩家來源環境傷害仍有效。

### Developer Notes / 開發者備註

- Refactor (PlayerCloneEntity split, controller pattern, Phases 1-2): began breaking down the 2309-line boss megaclass. Phase 1 extracted the death cinematic into `PlayerCloneDeathSequence` (5-phase 400-tick animation, source-player-only sound/particles, BGM stop, reward chest); die()/tickDeath()/remove() stay on the entity as thin delegates. Phase 2 extracted the Phase-2 mecha shell's pure visual geometry into `PlayerCloneArmorRig`, which now owns the display-list fields (armorParts / armorOffsets / armorSpawnOffsets / armorAssembleDelay / armorLegSide / turretMountOffsets) plus the shell silhouette + structure-template build + fly-in assembly + walk-follow + crumble/clear logic. The state hub (enterArmored / breakArmor / rebuildArmor: entityData ARMORED, bossEvent, skill reset, dimensions, armorHp) stays on the entity and delegates only the visual actions. Cross-subsystem fields (phase2TransitionTicks, walkPhase, PHASE2_TRANSITION_LEN) were widened to package-private; clonedInventory reuses its existing getter. Added a package-private `isDeathMarked()` probe so the death helper can read LivingEntity.dead (protected, cross-package). PlayerCloneEntity: 2309 -> ~1870 lines. Mechanical extraction only: no logic branches or tick/event ordering changed. Verified by compileJava + the full GameTest suite (green after both phases).
- 重構（PlayerCloneEntity 拆分，controller pattern，Phase 1-2）：開始拆 2309 行的 boss megaclass。Phase 1 把死亡演出抽到 `PlayerCloneDeathSequence`（5 階段 400-tick 動畫、只發給 source player 的音效/粒子、停 BGM、獎勵箱）；die()/tickDeath()/remove() 留在 entity 當薄委派。Phase 2 把二階段機甲外殼的純視覺幾何抽到 `PlayerCloneArmorRig`，由它擁有 display 清單 field（armorParts / armorOffsets / armorSpawnOffsets / armorAssembleDelay / armorLegSide / turretMountOffsets）以及外殼剪影 + 結構模板建構 + 飛入組裝 + 走路跟隨 + 剝落/清理邏輯。狀態樞紐（enterArmored / breakArmor / rebuildArmor：entityData ARMORED、bossEvent、技能重置、dimensions、armorHp）留在 entity，只委派視覺動作。跨子系統 field（phase2TransitionTicks、walkPhase、PHASE2_TRANSITION_LEN）放寬成 package-private；clonedInventory 沿用既有 getter。加一個 package-private `isDeathMarked()` 探針讓死亡 helper 能讀 LivingEntity.dead（protected、跨 package）。PlayerCloneEntity：2309 → ~1870 行。純機械搬移：沒動任何邏輯分支或 tick/event 順序。靠 compileJava + 完整 GameTest 套件驗證（兩階段都綠）。

- Refactor (PlayerCloneEntity split, Phase 3): extracted the combat skill system into `PlayerCloneCombatSkills` (controller pattern with entity back-ref). Moved the PillarSkill enum + all skill state fields + 12 methods (tickAntiPillar / tickBreakSurroundings / tickDive / tickPillarSkill / executeSkill / knockbackPlayer / tickPendingLaunch / placeSkillBlock / tickSkillBlockClear / clearSkillBlocksNow + horizUnit / pointToSegmentDistSqrXZ). customServerAiStep now delegates to skills.*; placedWalls / introActive / takeWallBlock widened to package-private; added setTelegraphSkill / setSkillTarget setters; enterArmored resets via skills.resetForArmored(). tickMeleeStrafe + KEEP_DISTANCE + updatePhase stay on the entity (re-added a local horizUnit helper since tickMeleeStrafe still calls it). PlayerCloneEntity: ~1870 -> ~1500 lines. Mechanical extraction only, no logic/ordering changes. Verified by compileJava + full GameTest suite (green).
- 重構（PlayerCloneEntity 拆分，Phase 3）：把戰鬥技能系統抽到 `PlayerCloneCombatSkills`（controller pattern，持 entity back-ref）。搬走 PillarSkill enum + 所有技能 state field + 12 個方法（tickAntiPillar / tickBreakSurroundings / tickDive / tickPillarSkill / executeSkill / knockbackPlayer / tickPendingLaunch / placeSkillBlock / tickSkillBlockClear / clearSkillBlocksNow + horizUnit / pointToSegmentDistSqrXZ）。customServerAiStep 改委派 skills.*；placedWalls / introActive / takeWallBlock 放寬 package-private；加 setTelegraphSkill / setSkillTarget setter；enterArmored 改用 skills.resetForArmored() 重置。tickMeleeStrafe + KEEP_DISTANCE + updatePhase 留在 entity（tickMeleeStrafe 仍用 horizUnit，本地補回一份 helper）。PlayerCloneEntity：~1870 → ~1500 行。純機械搬移，沒動邏輯/順序。靠 compileJava + 完整 GameTest 套件驗證（綠）。

- Fixed a long-standing red GameTest: `ManaCraftingTableGameTests.craftingResultAppearsWithSufficientMana` asserted against a non-existent "diamond -> mana_dust (cost 1500)" recipe. The mana crafting table is a 3x3 shaped station (9 input slots) and no single-diamond recipe was ever generated, so updateCraftingResult correctly produced nothing and the test failed. Rewrote both crafting tests to use the real shaped `blank_core` recipe (mana_wire / mana_crystal / mana_ingot, cost 600). The companion `craftingResultEmptyWithoutMana` now places the full valid blank_core grid with zero mana, so it actually exercises the "recipe matches but insufficient mana -> empty output" path instead of passing only because no recipe matched. The full GameTest suite is now green (66/66).
- 修好一個長期紅燈的 GameTest:`ManaCraftingTableGameTests.craftingResultAppearsWithSufficientMana` 對著一個不存在的「diamond → mana_dust(cost 1500)」配方斷言。魔力工作台是 3x3 shaped 合成台(9 格輸入),從來沒有單顆鑽石的配方,所以 updateCraftingResult 正確地產不出東西、測試就紅。把兩個合成測試改用真實存在的 shaped `blank_core` 配方(mana_wire / mana_crystal / mana_ingot,cost 600)。對照組 `craftingResultEmptyWithoutMana` 現在放滿合法的 blank_core 格子但給 0 魔力,真正驗到「配方符合但魔力不足 → 輸出空」的路徑,而不是只因為沒配方匹配才碰巧過。完整 GameTest 套件現在全綠(66/66)。

- Refactor (ArcaneConduitBlockEntity split, controller pattern): the conduit was already heavily componentised (7 managers + VirtualNetwork), so the BE's residual bulk was mostly the mandatory IUnifiedManaHandler / NBT override surface plus two extractable clusters, both moved to package-private helpers holding a BE back-reference. `ConduitNetworkMembership` (128 lines) owns the virtual-network membership lifecycle (tryJoin / createNew / join / leave / isNetworkMaster) and the NBT mana-restore handshake (queueRestoreFromNBT on load, restoreIfNeeded on onLoad), absorbing the former tempNetworkData / needsNetworkRestore fields. `ConduitInteractionHandler` (106 lines) owns onUse (wand IO-cycling) and showConduitInfo (the info panel). The `virtualNetwork` field stays on the BE (read by the mana facade, NBT save, and getVirtualNetwork) but became package-private so the membership helper can manage it. Public onUse stays a thin delegate; signature unchanged. ArcaneConduitBlockEntity: 944 -> 755 lines. Verified by compileJava + compileTestJava and the full 66-test GameTest suite; all 8 conduit tests pass (the NBT round-trip + wand IO-cycling tests added in the previous commit exercise the extracted paths directly).
- 重構（ArcaneConduitBlockEntity 拆分，controller pattern）:導管本來就高度組件化(7 個 manager + VirtualNetwork),所以 BE 殘餘的大塊主要是必須留著的 IUnifiedManaHandler / NBT override 表面,加上兩個可抽的 cluster,都搬到持有 BE back-reference 的 package-private helper。`ConduitNetworkMembership`(128 行)掌管虛擬網路成員生命週期(tryJoin / createNew / join / leave / isNetworkMaster)與 NBT 網路魔力還原握手(load 時 queueRestoreFromNBT、onLoad 時 restoreIfNeeded),吸收原本的 tempNetworkData / needsNetworkRestore 兩個 field。`ConduitInteractionHandler`(106 行)掌管 onUse(扳手切 IO)與 showConduitInfo(資訊面板)。`virtualNetwork` 欄位仍留在 BE(魔力 facade、NBT save、getVirtualNetwork 都直接讀它),但改成 package-private 讓 membership helper 能操作。public onUse 維持薄委派,簽名不變。ArcaneConduitBlockEntity:944 → 755 行。靠 compileJava + compileTestJava 與完整 66 test GameTest 套件驗證;8 個導管測試全 PASS(上一個 commit 加的 NBT round-trip + 扳手 IO 切換測試正好跑過抽出的程式碼路徑)。

- Test net before the ArcaneConduitBlockEntity split: added 2 GameTests to `VirtualNetworkGameTests` covering the two clusters about to be extracted. `nbtRoundTripPreservesBufferAndTier` builds a detached conduit BE, sets tier ADVANCED + 777 buffer mana, round-trips through saveAdditional/loadAdditional, and asserts both survive (protects the NBT path that stays in the BE while network-membership fields move out). `wandRightClickCyclesIoConfig` places a conduit, gives a mock player a basic_tech_wand in DIRECTION_CONFIG mode, calls onUse with a NORTH BlockHitResult, and asserts the face's IO type advances one step (DISABLED->OUTPUT->INPUT->BOTH), protecting onUse/showConduitInfo before it moves to a handler. The existing 6 network-capacity tests already cover virtual-network join/leave/aggregation/tier. Suite now 66 tests; both new pass.
- 拆 ArcaneConduitBlockEntity 前先鋪測試網:在 `VirtualNetworkGameTests` 加 2 個 GameTest 覆蓋即將抽出的兩個 cluster。`nbtRoundTripPreservesBufferAndTier` 建一個脫離 level 的導管 BE,設 tier ADVANCED + 777 buffer 魔力,經 saveAdditional/loadAdditional round-trip,驗兩者都還原(保護網路成員 field 搬走後仍留在 BE 的 NBT 路徑)。`wandRightClickCyclesIoConfig` 放一個導管,給 mock player 一支 DIRECTION_CONFIG 模式的 basic_tech_wand,用 NORTH 的 BlockHitResult 呼叫 onUse,驗該面 IO 型態前進一步(DISABLED→OUTPUT→INPUT→BOTH),保護 onUse/showConduitInfo 在搬到 handler 前的行為。既有 6 個網路容量測試已涵蓋虛擬網路加入/離開/聚合/tier。套件現在 66 test,2 個新的全 PASS。

- Refactor (AspectAltarBlockEntity split, Phase B, controller pattern): extracted the two remaining state machines into package-private helpers that hold a back-reference to the BE, matching the FloatingTurret controller pattern. `AltarRitualProcessor` (225 lines) owns the ritual lifecycle (tryActivate, per-tick mana drain + progress, mana-starvation warning + explosion trigger, completion: ingredient consumption + result output + advancement). `AltarRingManager` (144 lines) owns the upgrade-ring state machine (refreshUpgradeTier, checkRingComplete, applyRingReplace, restoreRingBlocks). The shared state fields (active / ritualTick / ritualMaxTick / manaConsumedSoFar / warningTick / cachedRecipe / progress / activatorUUID / completionAnimTick / completionDuration / upgradeTier / activePedestals / centerPedestal) became package-private so both managers operate on them directly (no getter/setter sprawl); NBT save/load and client sync stay centralised in the BE. Public methods `tryActivate` and `refreshUpgradeTier` are now thin delegates, signatures unchanged. AspectAltarBlockEntity: 655 -> 365 lines (974 -> 365 total across Phases A/C/B). Verified by compileJava + compileTestJava and the full 64-test GameTest suite; all 9 altar tests pass (the 3 ritual/ring tests added in the previous commit exercise the extracted code paths directly).
- 重構（AspectAltarBlockEntity 拆分 Phase B，controller pattern）:把剩下兩個狀態機抽成持有 BE back-reference 的 package-private helper,沿用 FloatingTurret 的 controller pattern。`AltarRitualProcessor`(225 行)掌管儀式生命週期(tryActivate、每 tick 消耗魔力推進進度、魔力耗盡警告 + 觸發爆炸、完成時消耗材料 + 輸出產物 + 給成就)。`AltarRingManager`(144 行)掌管升級環狀態機(refreshUpgradeTier、checkRingComplete、applyRingReplace、restoreRingBlocks)。共享狀態 field(active / ritualTick / ritualMaxTick / manaConsumedSoFar / warningTick / cachedRecipe / progress / activatorUUID / completionAnimTick / completionDuration / upgradeTier / activePedestals / centerPedestal)改成 package-private 讓兩個 manager 直接操作(不用寫一堆 getter/setter);NBT save/load 與客戶端同步仍集中在 BE。public 方法 `tryActivate` 與 `refreshUpgradeTier` 變薄委派,簽名不變。AspectAltarBlockEntity:655 → 365 行(A/C/B 三階段合計 974 → 365)。靠 compileJava + compileTestJava 與完整 64 test GameTest 套件驗證;9 個祭壇測試全 PASS(上一個 commit 加的 3 個 ritual/ring 測試正好跑過抽出的程式碼路徑)。

- Test net before the next altar split: added 3 GameTests to `AspectAltarGameTests` covering the still-inlined ritual / ring state machines, so the upcoming recipe + ring extraction has automated coverage. `ritualCompletesConsumesIngredientsAndDropsResult` runs the full `mana_crystal_ritual` (tier-0) lifecycle: form, load catalyst + 4 fragments, fund 10000 mana, `tryActivate`, then assert after 120-tick processing that the ritual is no longer active, the catalyst pedestal is empty, and a `mana_crystal` item entity dropped near the core. `ritualStarvedOfManaCancelsAfterWarning` funds only 100 mana, activates, and asserts the ritual self-cancels after the 200-tick warning window (exercises the `tickRitual` starvation branch + `AltarExplosionTrigger`). `ringUpgradeAdvancesToTier1AndReplacesBlocks` fills RING_T1 with mana blocks, calls `refreshUpgradeTier`, and asserts tier becomes 1 and the ring block is swapped to `RESONANCE_RING`. Suite now 64 tests; all 3 pass.
- 下次拆祭壇前先鋪測試網:在 `AspectAltarGameTests` 加 3 個 GameTest 覆蓋仍內聯的儀式 / 升級環狀態機,讓接下來抽 recipe + ring 時有自動覆蓋。`ritualCompletesConsumesIngredientsAndDropsResult` 跑完整 `mana_crystal_ritual`(tier-0)生命週期:成形、放催化物 + 4 碎片、灌 10000 魔力、`tryActivate`,120 tick 處理後驗儀式不再 active、催化物底座清空、核心旁掉出 `mana_crystal` 物品實體。`ritualStarvedOfManaCancelsAfterWarning` 只灌 100 魔力啟動,驗 200 tick 警告期後儀式自動取消(走 `tickRitual` 魔力耗盡分支 + `AltarExplosionTrigger`)。`ringUpgradeAdvancesToTier1AndReplacesBlocks` 鋪滿 RING_T1 魔力方塊、呼叫 `refreshUpgradeTier`,驗 tier 升到 1 且環方塊換成 `RESONANCE_RING`。套件現在 64 個 test,3 個全 PASS。

- Refactor (AspectAltarBlockEntity split, Phase A + C, mechanical move only): pure structural extraction with no branch / tick-order / state-field changes. Phase A pulls all multiblock geometry (PILLAR / PEDESTAL offsets, RING_T1..T12, ALL_RINGS, the formation `MultiblockPattern`, PILLAR_PRED) into a new `AltarGeometry` data class; external references in `StructureBuildWandItem`, `WandCoreBehavior`, `AltarMultiblockCategory` (JEI), and `AspectAltarGameTests` were repointed from `AspectAltarBlockEntity.*` to `AltarGeometry.*`. Phase C extracts the mana-starvation explosion (`triggerExplosion`) into `AltarExplosionTrigger.trigger(level, pos, tier, activator, pedestals)`, a parameterised static helper, and moves EXPLOSION_RADIUS / ITEM_PERM_LOSS_CHANCE / ITEM_DROP_CHANCE with it. `AspectAltarBlockEntity` drops from 974 to 655 lines. Public API and the ritual / ring state machine are untouched. Verified by `compileJava` + `compileTestJava` and the full GameTest suite (all altar tests pass; the lone failing `craftingResultAppearsWithSufficientMana` is a pre-existing mana-crafting failure, confirmed identical on a clean HEAD stash).
- 重構（AspectAltarBlockEntity 拆分 Phase A + C，純機械搬移）:無分支 / tick 順序 / state field 改動的純結構抽出。Phase A 把所有多方塊幾何（PILLAR / PEDESTAL 偏移、RING_T1..T12、ALL_RINGS、成形用 `MultiblockPattern`、PILLAR_PRED）抽到新的資料類 `AltarGeometry`；`StructureBuildWandItem`、`WandCoreBehavior`、`AltarMultiblockCategory`（JEI）、`AspectAltarGameTests` 的外部引用從 `AspectAltarBlockEntity.*` 改指向 `AltarGeometry.*`。Phase C 把魔力耗盡的失控爆炸（`triggerExplosion`）抽成參數化 static 方法 `AltarExplosionTrigger.trigger(level, pos, tier, activator, pedestals)`，連 EXPLOSION_RADIUS / ITEM_PERM_LOSS_CHANCE / ITEM_DROP_CHANCE 一起搬走。`AspectAltarBlockEntity` 從 974 行降到 655 行。Public API 與儀式 / 升級環狀態機完全不動。靠 `compileJava` + `compileTestJava` 與完整 GameTest 套件驗證（所有祭壇測試 PASS；唯一失敗的 `craftingResultAppearsWithSufficientMana` 是既有的魔力工作台問題，stash 回乾淨 HEAD 確認同樣失敗）。

- `FloatingTurretEntity` clone-mode immunity: (a) `hurt` early-returns `false` on `source.getEntity() instanceof Player` inside the `cloneOwner != null` branch (before the same-source projectile filter); (b) `isPickable` returns `false` when `cloneOwner != null`, so the entity is excluded from `ProjectileUtil.getEntityHitResult` and the crosshair pick ray, meaning player projectiles and melee skip it geometrically. Two layers because layer (b) alone would still let radial AoE / explicit `hurt` calls land — layer (a) closes that gap. Player-turret path (the second branch of `hurt`) is untouched.
- `FloatingTurretEntity` clone 模式雙重免疫:(a) `hurt` 在 `cloneOwner != null` 區塊頂端對 `source.getEntity() instanceof Player` 直接 return false,放在同源彈過濾之前;(b) `isPickable` 在 cloneOwner != null 時回 false,實體不會進 `ProjectileUtil.getEntityHitResult` 與準星 ray,玩家投射物與近戰直接在幾何層面跳過。兩層是因為單獨 (b) 還是有 AoE 或顯式 `hurt` 呼叫能打到,(a) 補這個破口。玩家砲（hurt 第二段分支）完全不動。

- Refactor (Phase 1+2 of FloatingTurretEntity split): both player-mode and clone-mode logic extracted into package-private controllers. `CloneTurretController` (208 lines) handles boss mirror mode. `PlayerTurretController` (295 lines) handles owner-attached / hand-held mode (tick logic, target selection, passive attack, control shots, healing, durability, item return on death). FloatingTurretEntity drops from 628 → 268 lines and is now a thin entity shell that wires DeferredRegister attributes, EntityDataAccessors, save/load, and dispatches `tick()` / `hurt()` / `die()` to the right controller. Public API (`setupAsCloneTurret` / `getCloneOwner` / `getSourceStack` / `applyUpgradesFromOwner` / `hurt` / `isPickable`) signatures unchanged — all external callers compile and behave identically. Verified by 8 GameTests covering single + multi-mock-player paths, all pass.
- 重構（FloatingTurretEntity 拆分 Phase 1+2）:玩家模式跟 clone 模式邏輯都抽到 package-private controller。`CloneTurretController`(208 行)處理 boss 鏡像模式。`PlayerTurretController`(295 行)處理 owner-attached/手持模式(tick 邏輯、目標選取、被動攻擊、控制彈、治療、耐久、死亡時還物品)。FloatingTurretEntity 從 628 行降到 268 行,現在只是個 thin entity 殼,負責 DeferredRegister attribute、EntityDataAccessor、save/load,並把 `tick()` / `hurt()` / `die()` 分派到對應 controller。Public API(`setupAsCloneTurret` / `getCloneOwner` / `getSourceStack` / `applyUpgradesFromOwner` / `hurt` / `isPickable`)簽名完全不變,所有外部 caller 編譯行為一致。靠 8 個 GameTest(單 + 多 mock player)驗證,全 PASS。

- New `FloatingTurretGameTests` (8 tests under `@GameTestHolder(koniava)`). Single-attacker: `cloneTurret_immuneToPlayerMelee`, `cloneTurret_immuneToPlayerProjectile`, `cloneTurret_takesEnvironmentalDamage`, `cloneTurret_isPickableFalse`, `playerTurret_hurtFromMobStillWorks`. Multi-mock-player (server-side multiplayer coverage via `GameTestHelper.makeMockPlayer` × 2): `cloneTurret_immuneToBothPlayersInMultiplayer` (both players melee + arrow blocked), `twoCloneTurrets_independentOwners_bothImmuneToPlayers` (cross-attack matrix, verifies controllers are per-instance independent), `cloneTurret_immuneAfterFirstAttackerDies` (immunity doesn't depend on first attacker being alive). All 8 pass in `./gradlew runGameTestServer`. GameTest covers server logic only — BGM playback, glow outline, client packet decode still need manual `runServer` + dual-client testing.
- 新增 `FloatingTurretGameTests`(8 個 test 掛在 `@GameTestHolder(koniava)`)。單一攻擊者:`cloneTurret_immuneToPlayerMelee`、`cloneTurret_immuneToPlayerProjectile`、`cloneTurret_takesEnvironmentalDamage`、`cloneTurret_isPickableFalse`、`playerTurret_hurtFromMobStillWorks`。多 mock player(透過 `GameTestHelper.makeMockPlayer` × 2 蓋 server 端多人邏輯):`cloneTurret_immuneToBothPlayersInMultiplayer`(兩個 player 近戰 + 箭傷都 blocked)、`twoCloneTurrets_independentOwners_bothImmuneToPlayers`(cross-attack 矩陣,驗每個 controller 是 per-instance 獨立)、`cloneTurret_immuneAfterFirstAttackerDies`(免疫不依賴第一個攻擊者存活)。`./gradlew runGameTestServer` 八個全 PASS。GameTest 只蓋 server 邏輯,BGM 播放、glow outline、client packet 解碼仍要實機 `runServer` + 兩個 client 驗。

- `BossBgmPacket` sided-class load fix: `SoundInstance` static field + `Minecraft` references moved into a `private static final class ClientHandler` nested inside the record. Java loads nested classes lazily on first reference, so dedicated server bytecode verification no longer drags in `net.minecraft.client.resources.sounds.SoundInstance`, which previously crashed `runGameTestServer` with `Attempted to load class ... for invalid dist DEDICATED_SERVER`. Same behaviour on client (lambda dispatches through `ClientHandler.handle`).
- `BossBgmPacket` 跨側類別載入修法:`SoundInstance` static 欄位 + `Minecraft` 參照都搬進 record 內的 `private static final class ClientHandler` 巢狀類別。Java 規範巢狀類別 lazy load,dedicated server 載 BossBgmPacket 時不會連帶驗證 SoundInstance,先前會炸 `Attempted to load class ... for invalid dist DEDICATED_SERVER` 導致 `runGameTestServer` 起不來,現在過了。client 端行為一樣(lambda 透過 `ClientHandler.handle` 分派)。

## [0.0.1.9] - 2026-05-29

### Player Changes / 玩家更新內容

- Mirror Boss death is now a full 20-second cinematic with a dedicated camera and a Nara phantom epilogue. The camera orbits the boss (slow drift during stagger/glow, faster + handheld shake during shatter), then transitions through a brief black fade to the Nara phantom, who delivers a victory line ("Hmph? Not bad. Looks like the show's over, so I'd better get going.") with voice + dialogue subtitle, before another black fade hands control back to the player. Mouse turning is fully suppressed during the sequence via the same mixin pathway as the entry cutscene, so the camera no longer fights player input.
- 鏡像 Boss 死亡演出現在是完整 20 秒鏡頭 + Nara 結語：相機環繞 boss（搖晃/白熱化期間緩慢漂移，碎裂期間加速 + 手持震動），結束後過短暫黑幕切到 Nara 幻影視角，她說一句勝利台詞（「唉呦? 實力不錯欸? 看起來應該是看完了，那我該走了。」帶配音 + 對白字幕），再一段黑幕淡出歸還控制權。期間滑鼠視角輸入用跟入場過場相同的 mixin 路徑徹底鎖住，不會再跟相機打架。

- The return rift inside the Mirror dimension no longer disappears when the boss dies. Previously the boss death cleanup ran `removeForOwner` on both the overworld AND the mirror dimension, accidentally erasing the player's only way out. Now only overworld entry rifts are cleared, and the pending-removal flag is dimension-aware (mirror dim rifts ignore it).
- 鏡中世界的返回裂縫在 boss 死亡時不再被誤刪了。之前 boss 死亡清理同時跑了 overworld 跟 mirror dim 的 `removeForOwner`，把玩家唯一的出口也一起清掉。現在只清 overworld 入口裂縫，pending 旗標也改成只影響非 mirror 維度。

- `/kill` (or any administrative kill via generic_kill / fell_out_of_world damage types) on the Mirror Boss now skips the 20-second cinematic and removes the boss instantly. The camera no longer locks up while admins / debug devs are wiping a stuck fight.
- 對鏡像 Boss 用 `/kill`（或任何 generic_kill / fell_out_of_world 傷害類型的指令殺死）現在會跳過 20 秒演出直接 remove，玩家相機不會被鎖住，方便管理員 / debug 清場。

- The reward chest at the end of the Mirror World really does keep its old contents from spilling now. The previous fix (clear the BlockEntity's items array) was incomplete because `setBlockAndUpdate` still went through vanilla's "drop contents on removal" path. Now the chest is replaced with `Block.UPDATE_SUPPRESS_DROPS` (flag 32), which tells vanilla to skip the drop step entirely.
- 鏡中世界尾聲的獎勵寶箱舊內容物真的不會噴出來了。之前的修法（清 BlockEntity 的物品陣列）不完整，因為 `setBlockAndUpdate` 仍會走 vanilla 的「remove 時 drop」流程。現在用 `Block.UPDATE_SUPPRESS_DROPS`（flag 32）替換寶箱，告訴 vanilla 整個跳過 drop 步驟。

- Mirror Boss death sequence extended to 20 seconds (65/100/110/90/35 tick phases) to give each phase room to breathe.
- 鏡像 Boss 死亡演出延長到 20 秒（65/100/110/90/35 tick 階段），每階段都有時間鋪陳。

- Mirror Boss death is now a full cinematic sequence instead of vanilla's 20-tick collapse. 5 phases over 6 seconds: (1) Stagger: 20t of jitter + warden heartbeat ramp, (2) Glow Up: 30t of slow rotation, end-rod particle stream, beacon-activate chime, (3) Crack: 30t with a decorative space rift opening at the chest, glass break stutter, soul fire flames, (4) Shatter: 30t of fast spin + scale shrinking + 16-direction ballistic ash burst + ender dragon roar + flash, (5) Final Flash: 10t white flash + rising souls + reward chest spawn. Boss bar persists through the whole sequence; reward chest now spawns at the climax (phase 5), not the instant the boss hits 0 HP, so the pacing actually feels earned.
- 鏡像 Boss 死亡現在是完整 6 秒 5 階段演出，取代 vanilla 20-tick 倒下：(1) Stagger 20t：抖動 + warden 心跳低音逼近，(2) Glow Up 30t：緩慢旋轉、end_rod 光點外噴、信標啟動鐘音，(3) Crack 30t：胸口裝飾性裂縫（重用 SpaceCrack shader）+ 玻璃碎裂連音 + 靈魂火，(4) Shatter 30t：高速旋轉 + scale 收縮 + 16 方向 ballistic 白灰粒子噴射 + 末影龍咆哮 + 閃光，(5) Final Flash 10t：白光 + 靈魂上飄 + 寶箱生成。Boss 血條全程保留；寶箱也改成在 phase 5（演出尾聲）才生成，不是 boss 一掉 HP 就秒出，節奏感對齊視覺。

- Mirror Boss death now has proper feedback: a burst of soul fire + portal + smoke particles, an Ender Dragon death roar layered with a sharp glass-break crack, and the boss bar stays visible during the vanilla 20-tick death animation instead of vanishing the instant the boss hits 0 HP.
- 鏡像 Boss 死亡現在有完整視聽回饋：靈魂火 + 傳送門 + 大煙霧粒子爆環、末影龍死亡咆哮疊玻璃碎裂高頻音，boss 血條也會在 20-tick 死亡動畫期間保留（之前一掉血歸零就秒沒）。

- Reward chest no longer spills its contents on the ground when the player re-enters the Mirror World for another fight. Cause: the previous run's chest was still there, and `setBlockAndUpdate` on it triggered vanilla's drop-on-remove behavior. Fix: clear the old chest's contents before overwriting the block.
- 獎勵寶箱在玩家重進鏡中世界打第二場時不會把舊內容物噴一地了。原因：上次的寶箱還在原位，`setBlockAndUpdate` 蓋過去會觸發 vanilla 的 drop-on-remove 把舊內容物全噴出來。修法：覆蓋前先把舊箱子清空。

- The overworld entry rift now actually disappears after the boss is defeated. Previously it would not, because the chunk it sat in was usually unloaded while the player was in the Mirror dimension, so the cleanup query found nothing. Fix: mark the rift for removal in saved-data, and let the rift self-discard on its next tick once its chunk loads. The flag is cleared when the player re-enters for another run.
- 主世界入口裂縫在 boss 被擊敗後現在會真的消失。之前不會：玩家在鏡中世界打 boss 時，主世界裂縫所在的 chunk 通常已卸載，從 boss 那邊清裂縫的程式 query 不到實體。修法：用 SavedData 標記「該玩家的裂縫待清除」，裂縫自己 tick 時檢查並 self-discard（chunk 載入時自然觸發）。玩家重進挑戰時旗標會清掉，新一場 boss 的裂縫照常生成。

- Mirror Boss also reads the 9-slot NINE_GRID storage pouch now, on top of the 10-slot extra equipment. Hiding a weapon in either of these no longer dodges the boss mirror.
- 鏡像 Boss 現在也會讀取 9 格 NINE_GRID 儲存欄位（之前只讀 10 格額外裝備）。武器藏這兩個地方都不再能逃 boss 鏡像。

- Performance: damage number renderer now distance-culls entries past 64 blocks and caches `camera.rotation()` once per frame instead of fetching it per damage number. In heavy combat (100+ floating numbers) this saves ~300-500µs/frame.
- 性能：傷害數字渲染現在距離剔除（64 格外不畫）+ `camera.rotation()` 每幀只抽一次（之前每個數字都重抽）。100+ 數字的戰鬥場景省 ~300-500µs/frame。

- Performance: magic circle renderer now uses pre-computed unit-circle cos/sin tables for ring geometry, eliminating ~1240 trig calls per active circle per frame. Star polygon and regular polygon rotation now uses one cos/sin per shape + matrix multiply instead of per-vertex trig. Saves ~60µs per active circle (up to 3 circles concurrent).
- 性能：法陣渲染現在用預算的單位圓 cos/sin 表處理 ring 幾何，每個法陣每幀少 ~1240 次三角函數呼叫。星形多邊形 + 正多邊形的旋轉也改成「每個圖形算一次 cos/sin + 旋轉矩陣乘法」取代「每個頂點算一次」。每個法陣省 ~60µs（最多 3 個同時）。

- Mirror Boss now actually mirrors weapons stored in the player's extra equipment slots (10 curio-style slots). Previously a player with a full 36-slot main inventory of junk could hide their best sword/turret/shield in extra equipment and the boss would never see it: `clonedInventory` is fixed at 36 entries and the main inventory filled it up first, pushing extra-equipment items off the end. Fix: priority items (turrets / swords / axes / tridents / shields) from anywhere in the player's belongings (armor, hands, main inventory, extra equipment, shulker contents) now get sorted to the front before clonedInventory is filled, so they cannot be evicted by trash.
- 鏡像 Boss 現在會真正鏡像玩家「額外裝備」10 格槽位裡的武器。之前的逃課：玩家把主背包 36 格塞滿垃圾、把最強武器/浮游砲/盾藏在額外裝備槽，boss 的 clonedInventory 固定 36 格、主背包先填滿就會 break，額外裝備永遠進不去。修正：浮游砲 / 真武器（劍/斧/三叉戟）/ 盾現在會優先排序到前面（不分來源，含額外裝備跟盒子內容），垃圾擠不掉它們。

- Performance: Aspect Altar pedestal scan no longer does 169 block-entity lookups every 2 seconds. The scan now uses a cheap block-state pre-filter and only performs the expensive BE lookup on positions that actually contain a pedestal block (~95%+ skip rate in typical setups). Negligible single-altar impact, but adds up with multiple altars.
- 性能：本源祭壇的底座掃描不再每 2 秒做 169 次 BlockEntity 查找。現在先用便宜的 BlockState 預過濾，只對真的有底座方塊的位置做貴的 BE 查找（一般情況下 95%+ 直接跳過）。單祭壇影響微小，但多祭壇場景會累積。

- English voice for both Mirror Boss lines (intro taunt + death taunt) regenerated with cleaner take selection.
- 鏡像 Boss 的兩條英文配音（開場 + 死亡嘲諷）重新生成並挑選乾淨的 take 覆蓋。

- Mirror Boss now has its own battle BGM ("Mirror Image") that plays when the fight starts and after save/reload while the boss is still alive. The BGM volume has a dedicated slider added to the vanilla sound settings (Esc -> Options -> Sound), placed inline with the standard volume sliders (Master / Music / Records / ... / Boss Battle BGM / Nara Voice / Sound Device / ...), matching vanilla layout so it feels native. Nara's two boss-fight voice lines (intro taunt + death taunt) were also regenerated with cleaner text so the TTS no longer sounds strained when she hits the exclamation-heavy lines.
- 鏡像 Boss 現在有專屬戰鬥 BGM「Mirror Image」，戰鬥開始時播放、存檔重進若 boss 還活著也會繼續播。BGM 音量有專屬滑桿放在 vanilla 聲音設定（Esc → 選項 → 聲音），跟原版音量滑桿並排（主音量 / 音樂 / 唱片 / ... / Boss 戰鬥 BGM / 娜拉配音 / 聲音裝置 / ...），融入原版排版。娜拉的兩條 boss 戰台詞（開場嘲諷 + 死亡嘲諷）也用更柔和的文字重生過，原版 TTS 不再被高密度驚嘆號擠到「夾音」。

- Space Crack (return rift in Mirror World) got a full visual overhaul using a custom GLSL shader pipeline: smooth dark elliptical core via a fragment-shader-computed radial alpha (no banding, no pie-slice seams), three-layer additive purple glow rift with lens shape + zigzag center line, and white-purple lightning bolts radiating outward. The whole thing renders through walls and block entities (chests / altars next to the rift don't occlude it) via a custom RenderLevelStageEvent pass after particles, so the rift behaves like a true tear in space. Billboard yaw snaps to integer degrees to eliminate sub-pixel jitter from camera micro-movements. Lightning bolts also penetrate blocks via a custom NO_DEPTH_TEST render type.
- 空間裂縫（鏡中世界的返回裂縫）視覺全面升級，用自訂 GLSL shader：黑色橢圓核心由 fragment shader 算徑向 alpha（無 banding、無 pie-slice 接縫），紫色撕裂縫光暈用 3 層 additive shader 帶 lens 形狀 + 中央 zigzag，外圍放射白紫雙色閃電。整體透過 RenderLevelStageEvent 在粒子之後渲染，會穿透方塊跟 block entity（裂縫旁邊放箱子或祭壇都不會遮住），表現得像「真正的空間破口」。Billboard yaw 鎖整數度，消除玩家微動導致的 sub-pixel 抖動閃爍。閃電也用自訂 NO_DEPTH_TEST RT 穿透方塊。

- Floating Turret upgrade screen now clips the rotating 3D preview to the center panel (with 6px margin inside the panel border) so the model can't visually overflow into the side panels at high zoom.
- 浮游砲升級畫面的 3D 預覽現在會被裁切在中央面板內（面板邊框內縮 6px），所以高倍率縮放下模型不會溢出到左右兩側。

- Inventory sort buttons moved up 8px so they sit cleanly above the slot grid instead of overlapping the top row.
- 物品欄整理按鈕往上移 8px，現在貼在格子上方而不是壓到第一排。

- Performance: the Space Crack renderer no longer scans a 64-block AABB every frame for crack entities even when there are none. Cracks now self-register on add/remove and the renderer iterates a tracked set, skipping the entire stage when empty.
- 性能：空間裂縫渲染器不再每幀掃 64 格 AABB 找實體（即使場上沒裂縫也照掃）。裂縫實體現在自己 register/unregister，渲染器跑追蹤的 set，沒裂縫時整個 stage 直接跳過。

- Space Crack visual rewrite. The return rift in the Mirror World no longer has a flat white center: it's now a smooth dark elliptical "tear into void" rendered via 36 pie-slice wedges (GPU interpolation, no visible banding), surrounded by purple glow layers and white-purple lightning bolts. Each lightning bolt is drawn as 3 stacked additive strokes (wide dim violet halo + mid bright purple + thin white core) so the bolts have a proper electric-glow look instead of hard quad edges. Render order was fixed so the dark center no longer punches through the surrounding glow.
- 空間裂縫視覺重寫。鏡中世界的返回裂縫不再是中間白色亮片：現在是用 36 片 pie-slice 楔形拼出的平滑黑色橢圓「破口」（GPU 線性插值，無環邊接縫），外面包紫色光暈跟白紫雙色閃電。每支閃電用 3 層 additive 疊加（寬+暗紫光暈/中+亮紫/細+白核），看起來像真正的電弧發光，不再是硬邊 quad。順便修了渲染順序問題（之前黑色核會穿透光暈）。

- Inventory sort buttons moved 8px higher so they sit clearly above the container/inventory slot grids instead of overlapping the top row.
- 物品欄整理按鈕往上移 8px，現在貼在容器/物品欄頂端上方而不是壓到第一排格子。

- Title screen splash pool now uses "Zako." instead of "Pathetic." for the whispered taunt line, matching the existing Nara TTS dialogue (zako is the established anime-fandom English usage for that specific tsundere taunt).
- 主選單 splash 池把「Pathetic.」改成「Zako.」對齊娜拉既有的 TTS 配音（zako 已經是英文 anime 圈通用的傲嬌嘲諷講法）。

- Title screen polish + modpack-friendly behavior: the custom title image now scales down on small windows (anchored at the top so it never overlaps the Singleplayer button), no matter how small the window gets. Two new config options were added in the titleScreen section: showTitleToggleButton (hide the corner toggle button entirely) and deferToOtherMenuMods (auto-defer to FancyMenu / CustomMainMenu / BetterTitleScreen / TitleTweaks if any of them is loaded). Filled in 25+ missing config translation strings (boss section, title screen section, all section headers in the config GUI, and tooltips for older config entries that previously showed raw keys).
- 主選單微調 + 模組包友善：客製標題圖片在小視窗時會自動縮小（錨在頂端，再小也不會壓到「單人遊戲」按鈕）。titleScreen 區段新增兩個配置：showTitleToggleButton（完全隱藏右上角切換按鈕）跟 deferToOtherMenuMods（偵測到 FancyMenu / CustomMainMenu / BetterTitleScreen / TitleTweaks 任一就自動退讓）。補了 25+ 個漏翻的 config 翻譯（boss 區段、主選單區段、所有 config GUI section header、跟舊的 config 條目的 tooltip）。

- The boss got an active turret volley skill: every 10 seconds it telegraphs for 1 second (END_ROD particles + beacon-activate sound on each turret), then all 4 of its mirrored turrets fire charged shots at you simultaneously. The mirror world now blocks ender chests too (in addition to shulker boxes) so you can't sneak gear through. Arena resets are also more thorough now: blocks broken by the player or destroyed by explosions inside the mirror world are tracked and restored when the dimension resets, so repeat fights don't leave craters or pickaxe holes behind. Several boss behaviours got config toggles in voidMirrorBoss section: turret volley skill, hotbar weapon switching, shield blocking, and the phase 2 transformation cinematic (disable for instant mecha appearance). All default ON.
- Boss 新增主動發砲齊射技能：每 10 秒前搖 1 秒（每門砲噴 END_ROD 粒子 + 信標啟動音效），然後 4 門鏡像砲一起對你發蓄力彈。鏡中世界現在也擋終界箱（之前只擋界伏盒），不能藉此偷帶裝備。場地重製更徹底：玩家挖掉或爆炸炸掉的方塊都會被追蹤、維度重置時還原原狀，反覆挑戰不會留下坑洞。多項 boss 行為加了配置開關在 voidMirrorBoss 區段：齊射技能、戰鬥中換武器、盾擋、二階段變身過場（關掉就直接顯示機甲不演動畫）。預設全開。

- The main menu got a full makeover: the vanilla Minecraft logo is replaced with a custom image (src/main/resources/assets/koniava/textures/gui/title.png) with a subtle scale pulse, and the yellow splash text uses a custom pool of ~37 mixed Chinese/English lines pulled from the mod's lore documents. A toggle button appears in the top-right corner of the title screen so you can switch back to the vanilla menu instantly without restarting (no game restart needed). The custom title screen has its own config toggle in the titleScreen section. Default ON.
- 主選單大改造：vanilla 的 Minecraft logo 被替換成自訂圖片（src/main/resources/assets/koniava/textures/gui/title.png），有輕微縮放脈動，黃色 splash 改用從模組設定文件抽出的 37 條中英混合台詞池。右上角會有一顆切換按鈕讓你即時切回原版選單，不用重開遊戲。客製主選單也有自己的開關在 titleScreen 區段。預設開啟。

- The mirror clone now equips weapons more like a player: it picks the strongest available weapon in the order Floating Turret > sword/axe/trident > tool (so it no longer grabs a netherite pickaxe over an iron sword just because the pickaxe has higher attack damage). It also fills its offhand intelligently: Floating Turret first, then shield, falling back to whatever the player had. If a Floating Turret ends up in either hand (from inventory if hands were empty), it now actually spawns a hand-mode turret that charges and fires, instead of being a visual prop. Shields equipped on the offhand will block frontal melee/ranged hits (60-degree front cone), consuming durability and playing the vanilla shield sound, so you have to attack from behind or break the shield first. The clone also mirrors your hotbar (slots 0-8) and cycles its main hand through it every ~4 seconds during combat for some weapon variety. Floating turret mirroring now strictly matches your layout: main hand and offhand turrets become hand-mode turrets on the boss, and the two Extra Equipment slots map to its two orbit turrets; any remaining backpack turrets fill empty orbit slots so hiding turrets in the inventory doesn't reduce the boss's firepower.
- 鏡像分身現在裝備武器更像玩家：依「浮游砲 > 劍/斧/三叉戟 > 工具」優先序挑最強的拿到主手（不再因為鎬攻擊力高就拿鎬不拿劍）。副手挑選順序：浮游砲 > 盾 > 玩家原副手物品。如果浮游砲是從背包挑到手上的，現在真的會生出手持砲實體會蓄力射擊，不是只有視覺道具。副手有盾時會擋下前方 60 度錐形範圍內的近戰/遠程攻擊，盾消耗耐久並播放原版盾擋音效，所以要繞背攻擊或先打爆盾。分身也鏡像你的快捷欄（0~8 槽），戰鬥中每 4 秒從中切換主手武器增加變化。浮游砲鏡像現在嚴格依照玩家配置：主/副手浮游砲變 boss 手持砲，extra equipment 兩個槽對應 2 個繞行砲；背包裡剩餘的浮游砲補繞行空位，所以藏在背包也不會減少 boss 火力。

- The block mecha appearance is now data-driven and supports more marker blocks. In addition to amethyst (anchor), white/red/blue wool (shell/legs), you can now use yellow wool to mark where the mecha's glowing "eyes" go (rendered as Sea Lantern, never replaced by inventory materials), and lime wool to mark where Floating Turrets should sit on the mecha (sorted left-to-right matching turret slot 0..N). Four-legged mech designs are fully supported: paint diagonal leg pairs in red/blue for a natural trot gait. The turrets now rotate with the boss's yaw so they always stay on their mounts.
- 方塊機甲外觀現在資料驅動且支援更多標記方塊。除了紫水晶（錨點）、白/紅/藍羊毛（外殼/腿），現在還可以用黃羊毛標記機甲眼位置（會生成海洋燈籠永遠發光，不會被背包材質取代），用淺綠羊毛標記浮游砲固定位置（左到右排序對應砲 slot 0..N）。完整支援四足機甲：對角腿配對用紅藍羊毛就會自動 trot 步態。砲會跟著 boss 朝向旋轉，永遠待在它們的掛點上。

- The mecha shape can now be authored in-game with a Structure Block instead of being hardcoded. Build any shape you want with marker blocks: amethyst block = body anchor (1 required, marks where the boss body slots in), white wool = static shell piece, red wool = left-leg block (walking animation), blue wool = right-leg block. Save it with the vanilla Structure Block as koniava:mecha_shell, drop the .nbt into src/main/resources/data/koniava/structure/, and the mecha will use your custom shape on next launch. If the .nbt is missing, the boss falls back to a built-in 7-wide humanoid silhouette. The boss body now sits deeper inside the mecha shell so its head pokes through the eye gap and its feet are hidden behind the chest armor (rather than floating on top), giving a "fused/piloting" connection feel. The floating turrets also freeze attacks during the phase 2 transformation cinematic instead of continuing to shoot through the animation.
- 機甲形狀現在可以直接在遊戲裡用結構方塊蓋出來，不再寫死。用標記方塊：紫水晶塊=本體錨點（必須有 1 個，標記 boss 本體要嵌入的位置）、白羊毛=一般外殼、紅羊毛=左腿（會走路擺動）、藍羊毛=右腿。用 vanilla 結構方塊存成 koniava:mecha_shell、把 .nbt 丟到 src/main/resources/data/koniava/structure/，下次啟動就會用你的形狀。.nbt 不存在會 fallback 到內建的 7 寬人形剪影。Boss 本體現在深嵌進機甲外殼裡，頭從眼縫探出、腳被胸甲遮住（不再浮在機甲頭頂上），有「融合/駕駛」的連結感。二階段變身過場期間，浮游砲也會凍結不射擊，不再過動畫時還在打。

- Redesigned the block mecha to look like an actual humanoid mech: 7 blocks wide overall with a narrow 3-block head, broad shoulders flaring to full 7-wide, a narrow 3-wide waist, and wider hips opening into thicker legs with mid-leg gaps where the knees would be. There's a single head-top antenna block and visible eye blocks too. The mecha is no longer themed obsidian: instead, its shell is built from blocks copied from the player's own inventory (so the mech is literally assembled out of your stolen materials), then from the surrounding terrain in the Mirror World if your inventory runs out, with obsidian as a last-resort fallback. Terrain blocks mined to build the shell are tracked separately and restored when the Mirror World resets, so the dimension goes back to its original state instead of being left full of holes. The floating turrets in the mecha phase now perch on top of the mecha's head instead of sitting at its feet. The phase 2 transformation also has its own cinematic: the camera locks for about 11 seconds and watches the shell pieces fly in one at a time from scattered sky positions, each accelerating as it travels (ease-in) and the overall stream getting denser toward the end. The camera uses three held shots (low front, side mid, high overview) with short transitions between them instead of continuously orbiting, so it doesn't make you dizzy. The boss is invulnerable and frozen during the cinematic, and you can press the skip key (default R) just like the intro to cut it short.
- 重新設計方塊機甲外觀，改成真正像人形的機甲：整體 7 格寬、頭部收窄到 3 格、肩線拉到 7 格全寬、腰部收到 3 格、髖部再展開到 5 格、腿部加粗並有膝關節中段斷縫；頭頂單格天線、頭部兩側可見眼睛方塊。機甲不再用固定的黑曜石主題，外殼改成「優先從複製自玩家的背包中取方塊，背包用完就從鏡中世界周圍環境挖方塊」（機甲變成你被搶走的材料堆出來的），都沒了再用黑曜石 fallback。挖走的環境方塊會單獨記錄原始狀態，鏡中世界重置時還原，不會留下空洞。機甲階段的浮游砲也移到頭頂（不再卡腳下）。二階段變身過場：相機鎖住約 11 秒，外殼方塊從天空隨機位置「一塊接一塊」飛來，每塊飛行由慢到快，整體飛來節奏越來越密。鏡頭改成 3 個定點（低位前方／側面中位／高位俯瞰）+ 短轉場，不再連續環繞，比較不會頭暈。過場期間 boss 無敵且凍結；可按跳過鍵（預設 R）跟進場一樣中斷。

- Defeating the mirror clone now actually drops loot in the arena chest: a centrepiece Mirror Core Shard (memento, RARE) only on your first clear so it stays meaningful, plus mana ingots, mana dust, and corrupted mana dust scattered around it every clear (so repeat fights still give materials). The shard's texture is not in yet (will show as the missing-texture pattern until the PNG is dropped in).
- 擊敗鏡中分身後,場地寶箱現在會有獎勵:中央一個鏡核碎片(紀念物,稀有,只在首次擊敗時掉以保留紀念意義),周圍的魔力錠/魔力粉/腐化魔力粉每次都有(可重複挑戰刷材料)。碎片的圖還沒畫,目前會顯示成 missing texture 的紫黑格,等補上 PNG 就好。

- The clone's launch skills can now actually be dodged: instead of locking onto you (the telegraph followed you and was unavoidable), each skill now locks a spot on the ground with the telegraph fixed there, so you can run out of that spot during the 1-second wind-up to avoid it entirely.
- 分身的擊飛技能現在真的可以閃躲了:不再鎖定你本人(之前預兆會跟著你跑、躲不掉),而是鎖定地上一個點、預兆固定在那,你在 1 秒前搖內跑出那個點就能完全閃過。

- During the mecha phase the boss bar now turns red and shows the shell's health so you can see your mining progress, and breaking the shell scatters block-crumble particles per piece. Reloading mid-fight while the shell is still up now rebuilds the shell and keeps the boss in the mecha phase (instead of dropping it to a 50%-HP first phase), and no longer leaves orphan shell blocks behind.
- 機甲階段的 boss 血條現在會變紅並顯示外殼血量,讓你看到挖外殼的進度;外殼破掉時每塊會噴方塊碎屑。在外殼還在時存檔重進,現在會重建外殼、維持機甲階段(而不是掉回 50% 血的第一階段),也不再殘留孤兒外殼方塊。

- The Mirror World is no longer flatly grayscale: it keeps a hint of color most of the time, and the screen briefly regains color and tints red as a warning when the clone is dangerous (winding up a skill, in its mirror-reflect state, or transformed into the mecha). Color becomes a danger signal instead of everything being dead gray.
- 鏡中世界不再死板的全灰:平時保留一點顏色,當分身有威脅時(蓄力技能、鏡反狀態、變身成機甲)畫面會短暫回色並泛紅當警示。顏色變成危險的訊號,而不是一切都死灰。

- At half health the clone now transforms into a giant block mecha: it disguises entities as blocks to build a large humanoid shell around itself (front and back layers), wrapping its body so you can't hit it directly. You have to mine the shell off with a pickaxe (other tools barely scratch it); once the shell is destroyed the mecha falls apart and it reverts to its player form to keep fighting.
- 分身在半血時會變身成巨大的方塊機甲:它用實體偽裝成方塊,在自己周圍組出大型人形外殼(前後兩層),把本體包住讓你打不到。你必須用稿子把外殼挖爆(其他工具幾乎敲不動);外殼被破壞後機甲會崩解,它變回玩家型態繼續戰鬥。

- The clone's damage reflect is no longer a random 25% chance: it now periodically enters a "mirror reflect" state (a bright mirror frame appears around it for ~1.5s) and only reflects your damage during that window, so you can learn to read it and stop hitting, instead of randomly getting punished.
- 分身的傷害反彈不再是隨機 25%:它現在會週期性進入「鏡反」狀態(身上出現亮白鏡框約 1.5 秒),只有在這段時間打它才會反傷,所以你可以看著它學會停手,而不是被隨機懲罰。
- The cinematic now shows a "Press R to skip" hint at the bottom the whole time, and the in-world restriction messages were reworded to sound like the dimension warping your gear rather than an admin notice.
- 進場過場現在全程在底部顯示「按 R 跳過」提示;鏡中世界的限制提示也改寫成「鏡面空間扭曲你的裝備」這種世界觀語氣,而不是像管理員公告。

- Reworked the clone's pressure to come from behavior instead of raw numbers: it now keeps a 2-3 block threat distance (chases in when far, backs off when too close, no more face-hugging) and circles you, attacks with skills about twice as often (cooldown halved), and its melee damage was lowered. So it feels like a stalking opponent constantly threatening skills, rather than a high-damage stat-stick glued to you.
- 重做分身的壓迫感,讓它來自行為而非純數值:它現在會保持 2~3 格的威脅距離(太遠追近、太近退開,不再貼臉)並繞著你徘徊,技能頻率約翻倍(冷卻減半),近戰傷害則調低。所以它打起來像一個一直在你身邊伺機放技能的獵人,而不是貼著你猛砍的高傷數值怪。

- Each clone skill now telegraphs during its wind-up: a bright white outline shows the incoming attack (a wall frame in front of you for the ram, a ring at your feet for the lift, a path line for the charge) plus a distinct charge-up sound per skill. Because the Mirror World is grayscale these use shape + brightness instead of color, so you can tell which skill is coming and dodge.
- 分身的每個招式在前搖時會顯示預兆:亮白輪廓標出即將來的攻擊(墊牆是你面前的牆框、頂飛是你腳下的圈、衝刺是一條路徑線),每招還有不同的蓄力音效。因為鏡中世界是灰階,預兆用形狀 + 亮度而非顏色,所以你能看出要來哪招並閃避。

- The clone is no longer absurdly fast: its base move speed is slightly lower and the anti-kite chase boost was cut from +100% to +45%, so it still catches up if you run but no longer instantly glues itself to you.
- 分身不再快得離譜:基礎移速略降,防風箏追擊加速從 +100% 砍到 +45%,所以你逃跑時它還是追得上,但不會瞬間貼死你。

- Fixed: skipping the intro cinematic left the clone without its mirrored armor/weapon (the reveal moment was skipped). The equipment is now applied when the boss activates, so skipping still shows full gear.
- 修正:跳過進場過場後分身沒有顯現鏡像的盔甲/武器(因為跳過了顯現那一刻)。裝備改成在 boss 啟動時套上,所以跳過後也會帶齊裝備。

- The clone no longer stops to swing when attacking: in melee range it now circles around you (strafes) while striking, instead of planting itself and standing still, so it feels like a real opponent.
- 分身近戰不再停下來揮擊:進入攻擊距離後會繞著你側移、邊繞邊打,而不是站定原地揮,打起來更像真正的對手。

- The clone's pillar-charge skills now have a 1-second wind-up: it stands still and charges (facing you with intensifying particles) before striking, giving you time to react or dodge.
- 分身的墊方塊衝撞技能現在有 1 秒前搖:發動前會站定蓄力(面向你、粒子漸強),給你時間反應或閃避。

- The wall-ram knockback is now much more dramatic: the clone first pops you off the ground, then hits you with a strong horizontal blast while you are airborne (no ground friction to eat it), so you actually get flung far back instead of sliding a block.
- 墊牆擊退現在誇張多了:分身會先把你拋離地面,趁你在空中時再給一記強力水平轟飛(沒有地面摩擦吃掉力道),所以你會真的被轟飛很遠,而不是滑一格就停。

- The clone's lift skill now spawns the block at your body and launches you upward off its impact, instead of placing a block above your head that you bonk into and get stuck on.
- 分身的頂起技能現在改成在你身體位置冒出方塊、用衝擊把你往上頂飛,不再是在你頭頂擺一塊讓你往上撞卡住。

- Fixes: the cinematic skip now uses the mod's R keybind (it was wired to the wrong vanilla key before); the wall-ram knockback now launches you back through the air instead of stopping after one block; and the clone's floating turrets no longer blow each other or themselves up with their charged-shot explosions.
- 修正:過場跳過改用模組的 R 鍵(之前綁錯成原版鍵);墊牆擊退現在會把你往後轟飛(離地飛遠),不再貼地一格就停;分身的浮游砲也不再被自己或同伴的蓄力彈爆炸炸掉。

- After the intro cinematic the clone now stands still for 2 seconds before attacking, instead of immediately ambushing you the moment it finishes. You can also press the skip key (default R) during the cinematic to fade to black and hand control back right away.
- 進場過場結束後,分身會先站定 2 秒才開始攻擊,不再一播完就偷襲你。過場中也可以按跳過鍵(預設 R)直接轉黑、立刻把控制交還給你。

- More work on the clone's stutter: the pillars it builds while air-chasing are now cleared once it lands back within reach (they used to pile up and clutter the arena with columns it stumbled over), and its charging skill now dashes smoothly instead of teleporting.
- 繼續改善分身的卡頓:它空中追擊時墊的柱現在會在落回攻擊距離後清除(之前會堆積、把場地塞滿讓它走起來一頓一頓),衝刺技能也改成平滑移動而非瞬移。

- The Mirror World clone can now chase you into the air: when you get more than 3 blocks above it, it pillar-jumps up by placing blocks under itself (like a player MLG pillar) to reach your height. It takes no fall damage when coming back down.
- 鏡中世界分身現在會追進空中:當你高出它 3 格以上時,它會像玩家那樣往腳下墊方塊往上跳,爬到你的高度。落下時不受墜落傷害。

- The Mirror World clone's block skills no longer dig up or cover the arena floor: all three variants now place blocks in the air at body or head height. The clone also moves more smoothly (its anti-kite speed boost no longer flickers on and off around the 10-block mark, which caused a stop-start stutter).
- 鏡中世界分身的墊方塊技能不再破壞或覆蓋 arena 地板:三種形態現在都改在空中(身體或頭部高度)墊方塊。分身移動也更順(防風箏加速不再在約 10 格距離忽開忽關,先前那會造成一頓一頓的卡頓)。

- Fixed duplicate or stale Nara phantoms and return rifts after reloading the world or dying and re-entering. The phantom no longer saves to disk, and the boss now keeps exactly one phantom and one return rift, recreating them if missing and clearing any extras.
- 修正重載世界或死亡後重新進入時,娜拉幻影與返回裂縫重複或殘留的問題。幻影改為不存檔,並由 boss 維持恰好一支娜拉與一個返回裂縫,缺少就重建、多餘就清除。

- The space rift now stands upright and only turns to face you horizontally, instead of always flattening to your camera so it looked like it followed you. Nara's death taunt now says "Zako" in English (was "Small fry").
- 空間裂縫現在直立、只水平轉向你,不再永遠貼平你的視角(之前看起來像黏著你走)。娜拉死亡嘲諷的英文版改為「Zako」(原為「Small fry」)。

- In the Mirror World, the leggings multi-jump and boots dash abilities are now disabled (with an on-screen notice), so you cannot use them to escape the fight; the existing container restriction now also shows a notice when blocked. Fixed: Nara did not taunt after you died inside, because the return point was lost on death.
- 鏡中世界內現在禁止使用護腿多段跳與鞋子衝刺(會有畫面提示),避免用來逃離戰鬥;既有的容器限制被擋時現在也會提示。修正:在裡面死亡後娜拉沒有出來嘲諷的問題(返回點在死亡時遺失)。

- Fixed: the Mirror World clone could be killed by its own charged-shot explosions; it is now immune to its own turret damage. The arena return rift now appears when the boss starts attacking (after the cinematic) instead of during it, so it no longer shows up twice.
- 修正:鏡中世界分身會被自己蓄力彈的爆炸炸死,現在免疫自己浮游砲的傷害。arena 返回裂縫改成 boss 開始進攻時(過場結束後)才出現,不再在過場中重複顯示兩個。

- The clone's pillar-charge skill now has a variant that rams a row of 3 blocks out from you toward the clone (a random 1 or 2 high) to knock you back, first sideways then with a short upward pop, and all three variants' placed blocks are now quickly torn down by the clone in sequence one second after the hit, so they do not litter the arena.
- 分身的墊方塊衝撞技能新增一種:從你朝分身方向墊出一排 3 格長、隨機 1 或 2 格高的方塊把你往反方向擊退(先橫向、再短暫上彈),而且三種技能墊的方塊現在都會在擊中一秒後被分身依序快速打掉,不會殘留在場地上。

- Mirror World entry cinematic polish: the opening camera no longer pulls back as far, Nara now watches from farther away, and her phantom is rendered semi-transparent with a "(Phantom)" name tag so you will not mistake it for the real Nara.
- 鏡中世界進場過場微調：開場鏡頭不再拉那麼遠，娜拉改在更遠處旁觀，她的幻影現在半透明顯示、名牌標示「（幻影）」，避免被當成娜拉本體。

- The Mirror World clone's charged turret shots no longer destroy the arena terrain (only the clone's shots; your own turrets are unchanged).
- 鏡中世界分身的蓄力砲彈不再破壞 arena 地形（只限分身的砲彈，你自己的浮游砲不受影響）。

- You can now leave the Mirror World on your own: a return rift opens at the arena center, right-click it to go back to the overworld. Fixed a bug where the boss vanished if you left the Mirror World and re-entered. The entry camera now moves together with your walk-out instead of staying still.
- 現在可以自己離開鏡中世界：arena 中心會開一道返回裂縫，右鍵即可回到主世界。修正離開鏡中世界後再進入時 boss 消失的問題。進場鏡頭現在會和你走出來的動作同步運鏡，不再呆在原地。

- The Mirror World clone gained a signature pillar-charge skill with three random variants: it slams blocks toward you and launches you into the air. Crouch the moment it hits to greatly soften the knockback. The entry cinematic is now longer (about 31 seconds) with denser charge-up particles, and you now walk out forward from the rift behind you instead of rising up out of the ground.
- 鏡中世界的分身獲得招牌的墊方塊衝撞技能，三種形態隨機發動：朝你的方向墊方塊把你擊飛到空中。被擊中的瞬間蹲下可大幅減輕擊退。進場過場加長到約 31 秒、集氣粒子更密，而且你現在是從身後的裂縫往前走出來，不再從地下升起。

- The Mirror World clone now fights cheese harder. It breaks the blocks you pillar up on in every phase (not only mid fight), and if you get more than 3 blocks above it, it tears down the whole support column at once so you drop back to the ground. When it mirrors you it now also reads inside your shulker boxes, bundles, and nested containers, then arms itself with the strongest gear you own: best armor in each slot, best weapon, and any floating turrets. Hiding equipment in a box no longer makes the clone weaker. Placing or opening shulker boxes is also blocked inside the Mirror World.
- 鏡中世界的分身現在更會反逃課。它在每個階段都會破壞你墊高用的方塊（不再只在戰鬥中段），而且你只要高過它 3 格以上，它就會一次拆掉整段支撐柱，讓你直接摔回地面。它鏡像你時，現在還會翻看你的界伏盒子、收納袋與巢狀容器，用你擁有的最強裝備武裝自己：每個部位最強的防具、最強的武器，以及所有浮游砲。把裝備藏進盒子已經無法讓分身變弱。鏡中世界內也禁止放置或開啟界伏盒子。

- Added a floating turret upgrade system. Hold a Floating Turret and press the upgrade key (U) to open its customization screen, which has 6 upgrade slots. Install USB-shaped upgrade plugins: Mana Capacity (Mk0-3, +3000/6000/10000/15000 max mana), Healing (Mk0-3, heals you +1/2/3/4 HP every 2 seconds while it has mana, works hand-held or in the turret slot), Reinforced Frame (Mk0-2, turret entity +50/100/200 HP, autonomous mode only), and Plating (Mk0-1, 5%/10% damage reduction on the turret entity, autonomous mode only). Each upgrade type can only be installed once. All craftable at the Mana Crafting Table. Requires runData.
- 新增浮游砲升級系統。手持浮游砲按升級鍵（U）開啟自訂介面，共 6 個升級槽。可安裝 USB 形狀的升級插件：魔力容量（Mk0-3，+3000/6000/10000/15000 最大魔力）、治療（Mk0-3，有魔力時每 2 秒治療你 +1/2/3/4 HP，手持與浮游砲槽皆適用）、強化骨架（Mk0-2，自走砲實體 +50/100/200 HP，僅自走型態）、裝甲板（Mk0-1，自走砲實體受傷減免 5%/10%，僅自走型態）。每種升級只能裝一個。皆可在魔力合成台製作。需跑 runData。

- Added more floating turret upgrade plugins. Universal: Auto-Aim (locks the nearest hostile when firing), Block Guard (charged shots stop breaking blocks). Autonomous-mode (Turret slot) only: Player Lock (targets players who attacked you), Guardian Mk0-1 (mobs target the turret; Mk1 also positions between you and the nearest enemy). Control rounds (autonomous mode): Slowing (slow 3s, every 3s), Binding (root 1s, every 3s), Levitation (levitate 2s, every 5s); these fire alongside normal shots, bosses are immune to all control effects, and the same control cannot stack on one target. Binding also blocks the target's dash and double-jump. All craftable at the Mana Crafting Table. Requires runData.
- 新增更多浮游砲升級插件。通用：自動瞄準（發射時鎖定最近敵人）、護方塊（蓄力彈不再破壞方塊）。僅自走型態：玩家鎖定（鎖定攻擊過你的玩家）、守護 Mk0-1（怪物改打浮游砲；Mk1 還會擋在你與最近敵人之間）。控制彈（自走型態）：緩速（緩速 3 秒、每 3 秒一發）、定身（定身 1 秒、每 3 秒一發）、漂浮（漂浮 2 秒、每 5 秒一發）；與普通彈同時發射，所有 boss 免疫全部控制效果，同種控制不能在同一目標疊加。定身還會封鎖目標的衝刺與多段跳。皆可在魔力合成台製作。需跑 runData。

- Reworked the Mana Alloy Chestplate shield into installable, mana-powered plugins (two mutually exclusive types, Mk0-3, chestplate only). Both work only while the chestplate has mana; when mana runs out the shield stops, and refilling mana reactivates it. Type 1 Damage Reduction Shield: reduces all incoming damage by 40%/50%/60%/60%, costing 7.5 mana per point of post-reduction damage; Mk3 also returns 50% of the damage you actually take as health over 3 seconds (free). Type 2 Value Shield: blocks 100% of damage by spending mana per point blocked (150/100/75/75 mana per damage by Mk); Mk3 additionally blocks a flat 20 damage per hit for free. Only one shield type can be installed, and only one of it. The old always-on 40% passive shield is removed. All craftable at the Mana Crafting Table. Requires runData.
- 把魔力合金胸甲的護盾改成可安裝、靠魔力驅動的插件（兩種互斥、各 Mk0-3、胸甲專屬）。兩種都只在胸甲有魔力時生效，魔力耗盡護盾失效，補魔力後恢復。類型一減傷護盾：減免所有傷害 40%/50%/60%/60%，每 1 點減傷後傷害消耗 7.5 魔力；Mk3 額外把你實際受到傷害的 50% 在 3 秒內回血（免費）。類型二數值護盾：100% 擋傷，每擋 1 點傷害消耗 150/100/75/75 魔力（依 Mk）；Mk3 還會每次受擊免費免傷 20 點。只能裝一種護盾、且只能裝一個。移除舊的常駐 40% 被動護盾。皆可在魔力合成台製作。需跑 runData。

- Started the Mirror World boss: a clone of you (the "Mirror Self") now spawns when you enter through the Space Crack. It looks like you (your skin, armor, and held item), copies your full inventory and hotbar, has 300 HP, fights you in melee, and has a 25% chance to reflect the damage you deal back at you. It shows a boss health bar labeled with your own name. The fight has phases: above 60% HP it fights normally; at 60% it starts walling off your escape with blocks (using copies of blocks from your inventory, falling back to stone); at 30% it tears down its own walls, speeds up, goes all-out, and drains mana from your equipped gear. Defeating the clone marks you as having cleared the Mirror World, closes your Space Crack, and opens a reward chest near the arena (chest contents are still to be decided). If you die inside, you respawn back outside the crack in the overworld and your clone is reset, so you can re-enter for a fresh fight. The floating turret mirror is not implemented yet.
- 開始製作鏡中世界的 boss：當你從空間裂縫進入時，會生成一個你的分身（鏡中的自己）。它外型與你相同（皮膚、盔甲、手持物），複製你的整個背包與快捷欄，擁有 300 點血量，會近戰攻擊你，且有 25% 機率把你造成的傷害反彈回你身上。會顯示一條以你自己名字為標題的 boss 血條。戰鬥有階段：血量 60% 以上正常戰鬥；60% 起用方塊封住你的退路（用你背包方塊的副本，沒有則用石頭）；30% 起拆掉自己的牆、加速、全力進攻，並吸取你裝備上的魔力。擊敗分身後會標記你已通關鏡中世界、關閉你的空間裂縫，並在 arena 附近開出一個獎勵寶箱（寶箱內容待定）。若你在裡面死亡，會在主世界裂縫外重生且分身重置，可重新進入打一場全新的。浮游砲鏡像尚未實作。

- Added the Mirror World, a grey prerequisite dimension. Its central area is a 501x501 superflat region matching the overworld surface height, surrounded by void, with a world border keeping you inside. Everything is rendered through a full-screen grayscale filter. When a Tier 3 or higher Altar explodes from running out of mana, a Space Crack tears open 10 blocks above the altar core; right-click it to enter. Requires runData. The boss encounter inside is not implemented yet.
- 新增鏡中世界，一個灰色的前置維度。中央為 501x501 超平坦區域（地表高度與主世界相同），外圍是虛空，世界邊界會把你擋在裡面。整個畫面套用全螢幕灰階濾鏡。當 T3 以上的祭壇因魔力耗盡而爆炸時，會在祭壇核心上方 10 格撕開一道空間裂縫，右鍵即可進入。需跑 runData。維度內的 boss 戰尚未實作。

- Redesigned crafting for Mana Alloy Helmet, Chestplate, and Leggings into two steps: first craft a base shell at the Mana Crafting Table (iron armor + Mana Reinforced Plates, costs 800/1200/1000 mana), then activate it at a T1 Altar with thematic materials. Helmet uses Mana Eyes; chestplate uses a Shield; leggings use Rabbit Foot and Slime Balls.
- 魔力合金頭盔、胸甲、護腿的合成改為兩步驟：先在魔力合成台用鐵甲 + 魔力強化板合成底殼（消耗 800/1200/1000 魔力），再在 T1 祭壇搭配主題材料激活。頭盔使用魔力之眼；胸甲使用盾牌；護腿使用兔腳與黏液球。

- Added Mana Eye: a new intermediate crafting material. Craft it at the Mana Crafting Table using an Ender Eye and Mana Crystal Alloy Dust (2000 mana). Used to activate the Mana Alloy Helmet.
- 新增魔力之眼：新的中間合成材料。在魔力合成台用末影之眼 + 魔力水晶合金粉合成（2000 魔力），用於激活魔力合金頭盔。

- Added passive Mana Shield to the Mana Alloy Chestplate. While the chestplate has mana, it automatically absorbs 40% of incoming damage at a cost of 10 mana per 1 damage absorbed. If mana runs out mid-hit, absorption is reduced proportionally. The tooltip shows shield status (active/inactive).
- 魔力合金胸甲新增被動魔力護盾。胸甲有魔力時，自動吸收 40% 傷害，每吸收 1 點傷害消耗 10 魔力。若魔力在受傷過程中耗盡，吸收量按比例縮減。工具提示顯示護盾狀態。

- When the Mana Shield absorbs a hit, it now plays a shield block sound and shows a translucent blue spherical shield bubble that wraps the whole player and fades out.
- 魔力護盾吸收傷害時，現在會播放盾牌格擋音效，並顯示一個包住整個玩家的半透明藍色球形護罩，隨後淡出。

- Renamed "Unfinished Mana Sprint Boots" to "Mana Sprint Boots Base" for consistency with other armor base shells.
- 「未完成的魔力衝刺靴」更名為「魔力衝刺靴底殼」，與其他裝甲底殼命名統一。

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

- Merged all per-armor capacity upgrades into a single universal item. There is now one Mana Capacity Upgrade per Mk level (Mk0-Mk3) that works for all mana armor pieces (helmet, chestplate, leggings, boots). Capacity bonus is now uniform: +1000 / +2000 / +3500 / +5500 per Mk. The item appears in the upgrade items group of the creative tab alongside other upgrade items. Requires runData.
- 將各部位獨立的容量升級合併為通用物品。現在每個 Mk 等級（Mk0-Mk3）只有一個「魔力容量升級」，適用於所有魔力盔甲（頭盔、胸甲、護腿、靴子）。容量加成統一為 +1000 / +2000 / +3500 / +5500。物品會與其他升級物品一起出現在創造模式的升級物品群組中。需跑 runData。

- Merged all per-armor defense upgrades into a single universal item. There is now one Armor Plating Upgrade per Mk level (Mk0-Mk3) that works for all mana armor pieces. Armor bonus is uniform: +1 / +2 / +3 / +4 per Mk. Replaces the old helmet/chestplate/leggings/boots specific armor upgrades. Requires runData.
- 將各部位獨立的防禦升級合併為通用物品。現在每個 Mk 等級（Mk0-Mk3）只有一個「防甲強化板」，適用於所有魔力盔甲。防甲加成統一為 +1 / +2 / +3 / +4。取代舊有的頭盔/胸甲/護腿/靴子專屬防甲升級。需跑 runData。

- Helmet Night Vision upgrade now drains mana while active (5 mana/second). When mana runs out, night vision turns off automatically. Toggling off instantly removes the effect. The upgrade no longer removes potion-based night vision when deactivated.
- 頭盔夜視升級現在開啟時會持續消耗魔力（5 魔力/秒）。魔力耗盡後夜視自動關閉。手動關閉時效果立即移除。關閉升級不再移除藥水提供的夜視效果。

### Developer Notes / 開發者備註

- Bug-fix sweep on the mirror-boss feature (14 issues across three review rounds): `hurt` armored branch routes non-player damage to `super.hurt` so the boss isn't immune to fire/void/mob damage; pickaxe detection uses `ItemTags.PICKAXES` so modded pickaxes also count; `onSyncedDataUpdated(ARMORED)` mirrors `armoredDimensions` to client + `refreshDimensions()` (client-side `getDefaultDimensions` / `getBoundingBoxForCulling` now correct); `readAdditionalSaveData` immediately sets `entityData.set(ARMORED, true)` + `armoredDimensions = true` + `refreshDimensions()` (prevents autosave-writes-false race and first-tick attack bypass before `rebuildArmor` runs); armored branch in `customServerAiStep` now calls `ensureCompanions()` so nara/return-crack stay alive during long shell phases; `CHARGE_RAMP` dodge uses `pointToSegmentDistSqrXZ` (line-shaped attack, not a sphere around the endpoint); `spawnRewardChest` force-overwrites + `chest.clearContent()` (player-placed blocks don't disrupt drops); `die` refactored to `if (getSourceUUID().isPresent())` block so sourceless bosses still drop the reward; `ARMOR_TAG` replaced with owner-specific `armorTag()` (`prefix + getStringUUID()`) so multi-boss arenas don't cross-discard each other's shells; `discardOwnedArmorDisplays(sl)` helper used by `rebuildArmor` / `ensureCompanions` / `die` / `remove` (the last two as fallback when `pendingArmorRebuild` was never consumed before death); `PlayerCloneEntity.ARMORED_BODY_OFFSET_Y` constant replaces magic 13 in `PlayerCloneRenderer.render`.
- 鏡中 boss 功能的 bug 修補（三輪 review 共 14 個）:`hurt` armored 分支對非玩家傷害走 `super.hurt`,boss 不再對火/虛空/生物傷害免疫;鎬子判斷改用 `ItemTags.PICKAXES`,modded 鎬子也算;`onSyncedDataUpdated(ARMORED)` 同步 `armoredDimensions` 到 client + `refreshDimensions()`(client 端 `getDefaultDimensions`/`getBoundingBoxForCulling` 正確);`readAdditionalSaveData` 立刻 `entityData.set(ARMORED, true)` + `armoredDimensions = true` + `refreshDimensions()`(防 autosave 寫 false 與 first-tick 攻擊繞過外殼);armored 期間 `customServerAiStep` 也呼叫 `ensureCompanions()`,長外殼期 nara/裂縫仍會維護;`CHARGE_RAMP` dodge 改用 `pointToSegmentDistSqrXZ`(線狀技能不再用 sphere 判定);`spawnRewardChest` 強制覆蓋 + `chest.clearContent()`,玩家亂放方塊不影響;`die` 重構成 `if (isPresent)` 區塊,sourceless boss 也會掉寶箱;`ARMOR_TAG` 改 owner-specific `armorTag()`(prefix + boss UUID),多 boss 場景不互清外殼;`discardOwnedArmorDisplays(sl)` helper 給 `rebuildArmor` / `ensureCompanions` / `die` / `remove`(後兩者當 `pendingArmorRebuild` 未消費前死亡的 fallback);`PlayerCloneEntity.ARMORED_BODY_OFFSET_Y` 常數取代 `PlayerCloneRenderer.render` 的 magic 13。

- Added `ModItems.MIRROR_CORE_SHARD` (Rarity.RARE, lang + tooltip key); `spawnRewardChest` now populates the arena chest via `ChestBlockEntity.setItem` with the shard at slot 13 (centre) plus mana materials around it. Item texture not provided yet (artwork pending); `ModItemModelProvider` auto-skips when `koniava:item/mirror_core_shard.png` is missing, so run `runData` after the PNG is dropped in to regenerate the model.
- 新增 `ModItems.MIRROR_CORE_SHARD`（Rarity.RARE，含 lang + tooltip 鍵）；`spawnRewardChest` 透過 `ChestBlockEntity.setItem` 填寶箱（slot 13 中央放碎片、周圍放魔力錠/魔力粉/腐化魔力粉）。物品 texture 暫未提供（待補圖）；`ModItemModelProvider` 在 `koniava:item/mirror_core_shard.png` 缺失時會自動 skip，補圖後重跑 `runData` 即會生成 model JSON。

- Launch skills now target a locked ground position instead of the player entity: `tickPillarSkill` records `skillTargetPos` (synced via the `SKILL_TARGET` BlockPos entityData) when the wind-up starts; `executeSkill` places blocks at that spot and only knocks back if the player is still within `SKILL_DODGE_RADIUS` (2.5, horizontal) of it, otherwise it whiffs (blocks still placed). `CloneTelegraphRenderer` draws the telegraph at `getSkillTarget()` (fixed) instead of the local player, so the danger zone reads as a fixed spot the player can walk out of. `SKILL_TARGET` is cleared to `ZERO` after execution.
- 擊飛技能改為鎖定地面定點而非玩家實體:`tickPillarSkill` 在前搖開始時記錄 `skillTargetPos`（透過 `SKILL_TARGET` BlockPos entityData 同步）；`executeSkill` 在該點放方塊，且只有玩家仍在其 `SKILL_DODGE_RADIUS`（2.5，水平）內才擊退，否則撲空（方塊照放）。`CloneTelegraphRenderer` 改在 `getSkillTarget()`（固定）畫預兆而非本機玩家，使危險區呈現為一個玩家能跑出的固定點。執行後 `SKILL_TARGET` 清為 `ZERO`。

- Phase-2 mecha polish: the boss bar shows `armorHp / ARMOR_MAX_HP` and turns RED while armored (back to WHITE on break); `breakArmor` emits a per-part deepslate `BlockParticleOption` crumble before discarding. Armor `Display.BlockDisplay` parts get scoreboard tag `ARMOR_TAG`; `ensureCompanions` (40t) discards tagged displays while not armored. `armorTriggered` / `Armored` / `armorHp` are persisted in NBT: reloading while still shelled runs `rebuildArmor` on the first tick (clears orphan tagged displays, re-enters armored at the saved `armorHp`) so the mecha phase continues instead of dropping to a 50%-HP phase-1; reload after the shell already broke stays in phase-1.
- 二階段機甲收尾:boss 血條顯示 `armorHp / ARMOR_MAX_HP` 且變身期間轉紅（破殼恢復白）；`breakArmor` 在移除前對每塊噴深邃石 `BlockParticleOption` 碎屑。外殼 `Display.BlockDisplay` 加 scoreboard tag `ARMOR_TAG`；`ensureCompanions`（40t）在非變身狀態清掉帶標記的孤兒 display。`armorTriggered` / `Armored` / `armorHp` 都存進 NBT:在外殼仍在時重載會於首次 tick 跑 `rebuildArmor`（清孤兒、以存回的 `armorHp` 重新進入外殼狀態），延續機甲階段而非掉回 50% 血的一階段;外殼已破後重載則維持一階段。

- Mirror World grayscale is now dynamic: `ClientTickHandler` smoothly lerps the `grey.json` post effect's `Saturation` and `RedTint` uniforms (`chain.setUniform`) from a danger level via `computeVoidMirrorDanger` (0 idle, 0.5 armored, 1 telegraph/reflect): Saturation 0.12→0.75, RedTint 0→0.45 (`grey.fsh` mixes toward a red multiplier), reset on exit. Also added a `getBoundingBoxForCulling` override on `PlayerCloneEntity` (inflate 6 while armored) so the head-rendered body plus large shell is not frustum-culled at edge angles.
- 鏡中世界灰階改為動態:`ClientTickHandler` 依 `computeVoidMirrorDanger` 的危險度（0 平時、0.5 變身中、1 預兆/鏡反）平滑驅動 `grey.json` 的 `Saturation` 與 `RedTint` uniform（`chain.setUniform`）：Saturation 0.12→0.75、RedTint 0→0.45（`grey.fsh` 混入偏紅倍率），離開時重置。另在 `PlayerCloneEntity` 加 `getBoundingBoxForCulling` override（變身期間 inflate 6），避免頭部渲染的本體加大型外殼在某些視角被 frustum culling 剔除。

- Added phase-2 block mecha to `PlayerCloneEntity`: synced `ARMORED` bool + `isArmored()`. At <=50% HP `customServerAiStep` triggers `enterArmored` once (`armorTriggered` guard): clears in-progress skill state (so a skill mid-windup at transform time doesn't get stuck repeating), enlarges the hitbox via a `getDefaultDimensions` override (5x16, gated by `armoredDimensions` so it isn't read before entityData is defined) + `refreshDimensions()` (hitbox only; the body model stays normal-size and visible, so no SCALE attribute / no invisibility / no equipment hiding), then `buildArmorShell` spawns hollow single-layer `Display.BlockDisplay` parts from the `MECH_SHAPE` silhouette (lower rows tagged left/right legs). Since `Display.BlockDisplay.setBlockState` is private, the block state is set via NBT (`tag.put("block_state", NbtUtils.writeBlockState(...))` + `Entity.load`). While armored the boss faces the target (`setYRot`), slowly closes (stops within 3 blocks so you can mine the shell), and `tickArmorFollow` rotates each part's local offset by body yaw and swings the leg parts around the hip (`walkPhase`, left/right anti-phase) for a walk cycle; `PlayerCloneRenderer.render` translates the visible body up to the mecha's head while armored. `hurt` routes damage to `armorHp` (pickaxe full, others x0.2, body HP protected, returns false); `armorHp<=0` -> `breakArmor` (discard displays, restore normal dimensions, revert to phase-1). `die`/`remove` call `clearArmorParts`. Known gaps: boss-bar still shows body HP during armor; armor state not persisted across reload; silhouette is single-layer (thin side profile).
- 為 `PlayerCloneEntity` 加上二階段方塊機甲:同步 `ARMORED` bool + `isArmored()`。血量 <=50% 時 `customServerAiStep` 觸發一次 `enterArmored`（`armorTriggered` 守門）:清掉進行中的技能狀態（避免變身瞬間正在前搖的招式卡住重放）、用 `getDefaultDimensions` override 放大碰撞箱（5x16，以 `armoredDimensions` 旗標守門避免 entityData 尚未 define 時讀取）+ `refreshDimensions()`（只放大碰撞箱；本體模型保持正常大小且可見，所以不用 SCALE 屬性、不隱形、不收裝備），再由 `buildArmorShell` 依 `MECH_SHAPE` 剪影生成空心單層 `Display.BlockDisplay`（下半列標記左/右腿）。因 `Display.BlockDisplay.setBlockState` 是 private，方塊狀態改用 NBT 設（`tag.put("block_state", NbtUtils.writeBlockState(...))` + `Entity.load`）。變身期間本體面向目標（`setYRot`）、緩慢逼近（3 格內停下讓玩家挖外殼），`tickArmorFollow` 依本體 yaw 旋轉每個 part 的 local 偏移，並讓腿部繞髖擺動（`walkPhase`，左右反相）做出走路動畫；`PlayerCloneRenderer.render` 在變身期間把可見的本體上移到機甲頭部。`hurt` 把傷害導向 `armorHp`（稿子全傷、其他 x0.2、本體血量受保護、回傳 false）；`armorHp<=0` 觸發 `breakArmor`（discard displays、還原正常碰撞箱、回一階段）。`die`/`remove` 呼叫 `clearArmorParts`。已知缺口:變身期間 boss 血條仍顯示本體血量；機甲狀態未跨重載存盤；剪影為單層（側面薄）。

- Mirror-reflect state replaces the random reflect: synced `REFLECTING` bool, `tickReflect` cycles it (200t initial, then `REFLECT_DURATION` 30t on / `REFLECT_INTERVAL` 160t off) with a glass sound on enter; `hurt` only reflects to a player attacker while `isReflecting()` (removed `REFLECT_CHANCE`). `CloneTelegraphRenderer.drawReflectBox` outlines the boss AABB in bright white during the window. Added skip-hint string drawn bottom-center for the whole cinematic in `onRenderGui`; reworded the ability/container disabled messages to in-world flavor.
- 鏡反狀態取代隨機反傷:同步 `REFLECTING` bool，`tickReflect` 週期切換（初始 200t，之後開 `REFLECT_DURATION` 30t / 關 `REFLECT_INTERVAL` 160t），進入時播玻璃音；`hurt` 只在 `isReflecting()` 時對玩家攻擊者反傷（移除 `REFLECT_CHANCE`）。`CloneTelegraphRenderer.drawReflectBox` 在該窗口以亮白描出 boss AABB。`onRenderGui` 全程在底部置中畫跳過提示字串；改寫 ability/container 禁用訊息為世界觀語氣。

- Reworked melee into a keep-distance pressure pattern: `tickMeleeStrafe` now holds `KEEP_DISTANCE` (2.5): `moveTo` when farther than +1.5, back off (side + away) when closer than -0.6, otherwise strafe sideways; melee only lands if actually within reach. `SKILL_COOLDOWN` 160 to 80 (skills roughly twice as often) and `ATTACK_DAMAGE` 12 to 8. Pressure now comes from constant skill threat at range, not high-damage face-hugging.
- 近戰改為保持距離的壓迫模式:`tickMeleeStrafe` 維持 `KEEP_DISTANCE`（2.5）：距離大於 +1.5 時 `moveTo` 追近、小於 -0.6 時（側移 + 後退）退開、其餘繞圈側移；只有真的進到攻擊距離才近戰。`SKILL_COOLDOWN` 160 改 80（技能約翻倍頻率）、`ATTACK_DAMAGE` 12 改 8。壓迫改由「在距離外持續以技能威脅」提供，而非高傷貼臉。

- Skill telegraphs: `PlayerCloneEntity` syncs a `TELEGRAPH_SKILL` int (0/1/2/3) set during wind-up, cleared on fire/target-loss, plus a per-skill wind-up sound (RAM_WALL warden charge / LIFT_UP piston / CHARGE_RAMP ravager roar). New client `CloneTelegraphRenderer` (`RenderLevelStageEvent.AFTER_TRANSLUCENT_BLOCKS`) draws bright-white outlines via `RenderType.lines()` at the target: ram = wall frame, lift = foot ring, charge = boss->player line. Uses shape + brightness (color is killed by the grayscale post shader). Assumes the local player is the target (1v1); multiplayer would draw on self.
- 招式預兆:`PlayerCloneEntity` 同步 `TELEGRAPH_SKILL` int（0/1/2/3），前搖時設、發動/失去目標時清，並依招式播不同蓄力音（RAM_WALL 監守者蓄力 / LIFT_UP 活塞 / CHARGE_RAMP 劫毀獸吼）。新增 client `CloneTelegraphRenderer`（`RenderLevelStageEvent.AFTER_TRANSLUCENT_BLOCKS`）以 `RenderType.lines()` 在目標位置畫亮白輪廓:墊牆=牆框、頂飛=腳下圈、衝刺=boss→玩家連線。靠形狀+亮度（顏色會被灰階 post shader 吃掉）。假設本機玩家為目標（1對1）；多人會畫在自己身上。

- Clone `MOVEMENT_SPEED` 0.3 to 0.27 and `ANTI_KITE_SPEED` modifier 1.0 to 0.45 (ADD_MULTIPLIED_TOTAL), so the anti-kite chase no longer doubles speed and pins the player.
- 分身 `MOVEMENT_SPEED` 0.3 改 0.27、`ANTI_KITE_SPEED` 修飾 1.0 改 0.45（ADD_MULTIPLIED_TOTAL），防風箏追擊不再翻倍貼死玩家。

- `activateAfterIntro` now calls `revealEquipment()` up front (idempotent). Skipping the cinematic sets `introTicks = INTRO_LEN`, which bypassed the `INTRO_REVEAL_TICK` reveal, so the clone activated without its `pendingEquipment` applied; revealing on activation covers both the normal and skip paths.
- `activateAfterIntro` 開頭補呼叫 `revealEquipment()`（冪等）。跳過過場會把 `introTicks` 設成 `INTRO_LEN`，因而略過 `INTRO_REVEAL_TICK` 的顯現，導致分身啟動時沒套上 `pendingEquipment`；改在啟動時顯現可同時涵蓋正常與跳過路徑。

- Replaced `MeleeAttackGoal` + `LookAtPlayerGoal` with a custom `tickMeleeStrafe` in `customServerAiStep`: out of reach it `moveTo`s the target; in reach it `getNavigation().stop()` then strafes via `getMoveControl().setWantedPosition` (perpendicular to the target line, slight inward bias) while swinging + `doHurtTarget` on a 16t `attackCooldown`. Skipped during grace and skill wind-up.
- 以 `customServerAiStep` 中的自訂 `tickMeleeStrafe` 取代 `MeleeAttackGoal` + `LookAtPlayerGoal`:超出攻擊距離時 `moveTo` 追擊；進入距離則 `getNavigation().stop()` 後用 `getMoveControl().setWantedPosition`（垂直連線方向、略帶內傾）繞圈側移，並以 16t `attackCooldown` 揮手 + `doHurtTarget`。緩衝期與技能前搖期間跳過。

- `SKILL_TELEGRAPH` raised 12 to 20 (1s). During the wind-up the clone zeroes horizontal motion + `getNavigation().stop()` + looks at the player (stand-and-charge), and emits intensifying `CRIT` (count/speed scale with progress) + `ENCHANTED_HIT` particles before the skill fires.
- `SKILL_TELEGRAPH` 由 12 提高到 20（1 秒）。前搖期間分身水平速度歸零 + `getNavigation().stop()` + 面向玩家（站定蓄力），並噴漸強的 `CRIT`（數量/速度隨進度增加）+ `ENCHANTED_HIT` 粒子，再發動技能。

- `RAM_WALL` knockback is now two-stage and physics-aware: first a mostly-vertical pop (0.3/0.85) to get the player off the ground, then after 6t `tickPendingLaunch` applies a strong horizontal blast (`pendingLaunchDir` × 3.0, preserving upward y) while the player is airborne so ground friction doesn't kill it. New `pendingLaunchDir` field stores the away direction.
- `RAM_WALL` 擊退改為兩段且符合物理:先一記偏垂直的拋飛（0.3/0.85）讓玩家離地，6t 後 `tickPendingLaunch` 趁玩家在空中給強力水平轟飛（`pendingLaunchDir` × 3.0，保留向上 y），地面摩擦才不會吃掉力道。新增 `pendingLaunchDir` 欄位存擊退方向。

- `LIFT_UP` now places blocks at the player's body position (`blockPosition()` + above) instead of above the head (`above(2)`), with a stronger upward knockback (0.5 / 1.5), so the player is launched off the spawning block rather than bonking head-first into a ceiling block.
- `LIFT_UP` 改為在玩家身體位置（`blockPosition()` + 上一格）放方塊，而非頭頂（`above(2)`），並提高上拋（0.5 / 1.5），玩家被冒出的方塊頂飛而非往上撞到天花板方塊卡住。

- `VoidMirrorIntroManager` skip now uses `ModKeyMappings.NARA_SKIP` (R) instead of `keySwapOffhand` (vanilla F). `RAM_WALL` knockback raised to 2.2 horizontal / 0.45 vertical so the player leaves the ground and keeps speed (was 1.5 / 0.1, which stopped after one block). `FloatingTurretEntity.hurt` clone branch now ignores damage whose `getDirectEntity()` is a `FloatingTurretProjectile` owned by the same `cloneOwner`, so clone turrets aren't destroyed by their own/sibling charged-shot explosions.
- `VoidMirrorIntroManager` 跳過改用 `ModKeyMappings.NARA_SKIP`（R）取代 `keySwapOffhand`（原版 F）。`RAM_WALL` 擊退提高為水平 2.2 / 垂直 0.45，玩家會離地且保速（原為 1.5 / 0.1，貼地一格就停）。`FloatingTurretEntity.hurt` 的 clone 分支忽略 `getDirectEntity()` 為同一 `cloneOwner` 所屬 `FloatingTurretProjectile` 的傷害，分身砲不再被自己或同伴的蓄力彈爆炸摧毀。

- `activateAfterIntro` sets `graceTicks = 40` and no longer sets a target; `customServerAiStep` returns early (clearing the target) while `graceTicks > 0`, so the clone stands still for 2s after the cinematic. New C2S `VoidMirrorSkipIntroPacket`: pressing `keySwapOffhand` (R) during the cinematic sends it; the server finds the player's clone and calls `skipIntro` (sets `introTicks = INTRO_LEN` so it activates next tick), and the client jumps `ticks` to `FADE_IN_START` to fade out and release control.
- `activateAfterIntro` 設 `graceTicks = 40` 且不再設定目標；`customServerAiStep` 在 `graceTicks > 0` 時提前 return(清除目標),因此分身在過場後站定 2 秒。新增 C2S `VoidMirrorSkipIntroPacket`:過場中按 `keySwapOffhand`(R)送出,server 找到該玩家的分身並呼叫 `skipIntro`(設 `introTicks = INTRO_LEN`,下 tick 啟動),client 則把 `ticks` 跳到 `FADE_IN_START` 轉黑並交還控制。

- Air-chase pillars are tracked in `chaseBlocks` and torn down in `tickAirChase` once the target drops below the 3-block threshold while the clone is grounded (also removed from `placedWalls`). `CHARGE_RAMP` now uses `setDeltaMovement` (smooth dash) instead of `setPos` (teleport), removing the position-jump stutter.
- 空中追擊墊的柱記入 `chaseBlocks`，在 `tickAirChase` 中當目標低於 3 格門檻且分身落地時清除（同時從 `placedWalls` 移除）。`CHARGE_RAMP` 改用 `setDeltaMovement`（平滑衝刺）取代 `setPos`（瞬移），消除位置跳動造成的卡頓。

- Added `tickAirChase`: when the target is 3+ blocks above the grounded clone, it places a block at its feet (`placeChaseBlock`) and jumps (y velocity 0.42), pillar-jumping up to the player. Chase-pillar blocks go into `placedWalls` + `addModifiedBlock` (so anti-pillar won't tear its own column and resetArena clears it) but not into the 1s `skillBlocks` clear. `causeFallDamage` overridden to return false so the clone takes no fall damage from its block mobility.
- 新增 `tickAirChase`:當目標高出在地面的分身 3 格以上時，分身在腳下放方塊（`placeChaseBlock`）並跳躍（y 速度 0.42），像 pillar jump 往玩家爬。追擊柱方塊進 `placedWalls` + `addModifiedBlock`（anti-pillar 不拆自己的柱、resetArena 會清），但不進 1 秒的 `skillBlocks` 清理。覆寫 `causeFallDamage` 回傳 false，分身墊方塊機動時不受墜落傷害。

- `placeSkillBlock` reverted to a `canBeReplaced` check (air/replaceable only), so skill blocks never overwrite the floor or existing blocks. `LIFT_UP` now stacks blocks above the player's head (`above(2)`) with a strong vertical pop; `CHARGE_RAMP` places its row at the clone's body height (`getY()+1`); `RAM_WALL` already sat at body height. Anti-kite now uses hysteresis (enable >144 d², disable <64 d²) instead of a single threshold, removing the boundary flicker; the unused `ANTI_KITE_DIST_SQR` was removed.
- `placeSkillBlock` 改回 `canBeReplaced` 檢查（只放空氣/可替換），技能方塊不再覆蓋地板或既有方塊。`LIFT_UP` 改為在玩家頭頂上方（`above(2)`）疊方塊 + 強垂直上拋；`CHARGE_RAMP` 的方塊排改在分身身體高度（`getY()+1`）；`RAM_WALL` 本來就在身體高度。防風箏改用遲滯（>144 d² 開、<64 d² 關）取代單一閾值，消除邊界抖動；移除未使用的 `ANTI_KITE_DIST_SQR`。

- `NaraPhantomEntity.shouldBeSaved` now returns `false` (no longer persisted). `PlayerCloneEntity.ensureCompanions` (every 40t while active) keeps exactly one same-source Nara phantom and one return-rift `SpaceCrackEntity` (spawns if missing, discards extras); the return rift is no longer spawned in `activateAfterIntro`, so a reload or death-reentry can't leave stale/duplicate companions. `VoidMirrorTeleport.spawnNaraPhantom` is `public static` taking a `UUID` so the boss can reuse it.
- `NaraPhantomEntity.shouldBeSaved` 改為回傳 `false`（不再存檔）。`PlayerCloneEntity.ensureCompanions`（active 時每 40t）維持同源恰好一支娜拉幻影與一個返回裂縫 `SpaceCrackEntity`（缺少則生成、多餘則清除）；返回裂縫不再於 `activateAfterIntro` 生成，因此重載或死亡重進入都不會殘留/重複。`VoidMirrorTeleport.spawnNaraPhantom` 為 `public static` 吃 `UUID`，供 boss 重用。

- `SpaceCrackRenderer` now billboards only around the Y axis (`Axis.YP.rotationDegrees(-camera.getYRot())`) instead of the full `camera.rotation()`, so the rift stays upright. English `nara.dialogue.void_mirror.death_taunt` changed "Small fry" to "Zako" (audio unchanged).
- `SpaceCrackRenderer` 改為只繞 Y 軸 billboard（`Axis.YP.rotationDegrees(-camera.getYRot())`），不再用完整 `camera.rotation()`，使裂縫保持直立。英文 `nara.dialogue.void_mirror.death_taunt` 的「Small fry」改為「Zako」（音檔不變）。

- `LeggingsDoubleJumpHandler.handleDoubleJump` and `ManaSprintBootsItem.performDash` early-return with an actionbar notice when in `VOID_MIRROR`. `VoidMirrorEvents` shulker place/open cancels now call `notifyContainerBlocked`. The `RETURN_POINT` attachment gained `.copyOnDeath()` so `onPlayerRespawn` still sees it and fires `exit` + `NaraTauntPacket` (without it the new player had an empty `RETURN_POINT`, so the taunt never sent). New lang keys `message.koniava.void_mirror.ability_disabled` / `.container_disabled`.
- `LeggingsDoubleJumpHandler.handleDoubleJump` 與 `ManaSprintBootsItem.performDash` 在 `VOID_MIRROR` 內提前 return 並送 actionbar 提示。`VoidMirrorEvents` 的界伏盒放置/開啟取消改為呼叫 `notifyContainerBlocked`。`RETURN_POINT` attachment 加上 `.copyOnDeath()`，否則死亡後新 player 的 `RETURN_POINT` 為空，`onPlayerRespawn` 讀不到、嘲諷不會發送。新增 lang 鍵 `message.koniava.void_mirror.ability_disabled` / `.container_disabled`。

- `PlayerCloneEntity.hurt` now ignores damage whose direct entity is a `FloatingTurretProjectile` it owns (covers charged-shot explosion self-damage). The return-rift `SpaceCrackEntity` is spawned in `activateAfterIntro` (owner = source UUID, arena center) instead of `VoidMirrorTeleport.enter`, so it no longer overlaps the emerge crack during the cinematic; `spawnExitCrack` was removed.
- `PlayerCloneEntity.hurt` 現在忽略 direct entity 為自己擁有的 `FloatingTurretProjectile` 的傷害（涵蓋蓄力彈爆炸自傷）。返回裂縫 `SpaceCrackEntity` 改在 `activateAfterIntro` 生成（owner = 來源 UUID、arena 中心），不再在 `VoidMirrorTeleport.enter`，避免過場中與走出裂縫重疊；移除了 `spawnExitCrack`。

- `PlayerCloneEntity` `RAM_WALL` now places a row of 3 blocks extending from the player toward the clone (`relative(toClone, 1..3)`, a random 1-2 high) and knocks the player back sideways, then `tickPendingLaunch` adds a short upward pop 4t later (`pendingLaunchTarget`); the from-clone-feet ramp version was replaced. `placeSkillBlock` now only skips unbreakable blocks (was `canBeReplaced`) so the wall reliably appears. All skill blocks are tracked in `skillBlocks`; `executeSkill` sets `skillClearTimer = SKILL_BLOCK_LIFETIME` (20t) and `tickSkillBlockClear` destroys one block per tick in placement order; `clearSkillBlocksNow` wipes leftovers at the start of the next skill.
- `PlayerCloneEntity` 的 `RAM_WALL` 改為從玩家朝分身方向排 3 格方塊（`relative(toClone, 1..3)`、隨機 1~2 格高）並先把玩家往反方向橫向擊退，`tickPendingLaunch` 之後 4t 再給一段上彈（`pendingLaunchTarget`）；取代了原本從分身腳邊延伸的斜坡式。`placeSkillBlock` 改為只跳過不可破壞方塊（原為 `canBeReplaced`），確保牆一定放得出來。所有技能方塊記入 `skillBlocks`；`executeSkill` 設 `skillClearTimer = SKILL_BLOCK_LIFETIME`（20t），`tickSkillBlockClear` 每 tick 依放置順序打掉一格；`clearSkillBlocksNow` 在下次技能開始前清掉殘留。

- Mirror World intro tweaks: `VoidMirrorIntroManager.CAM_OFFSETS` A/B pulled in (A to {0,3.2,-12}, B to {0,2.4,-7}) so the opening is less distant; `VoidMirrorTeleport.spawnNaraPhantom` moves Nara to `ARENA_Z - 26` and names her via the new `nara.phantom.name` key (zh "娜拉（幻影）" / en "Nara (Phantom)"). `NaraPhantomRenderer` overrides `getRenderType` to `entityTranslucent` and wraps the buffer with an `AlphaConsumer` that multiplies vertex alpha by 0.75 (25% transparent).
- 鏡中世界進場微調：`VoidMirrorIntroManager.CAM_OFFSETS` A/B 拉近（A 改 {0,3.2,-12}、B 改 {0,2.4,-7}），開場不再那麼遠；`VoidMirrorTeleport.spawnNaraPhantom` 把娜拉移到 `ARENA_Z - 26`，並改用新的 `nara.phantom.name` 鍵命名（中「娜拉（幻影）」/英「Nara (Phantom)」）。`NaraPhantomRenderer` 覆寫 `getRenderType` 為 `entityTranslucent`，並用 `AlphaConsumer` 包裝 buffer 把頂點 alpha 乘 0.75（25% 透明）。

- Clone turret shots (`FloatingTurretEntity.cloneOrbitTick` / `cloneHandTick`) now call `proj.setNoBlockDamage(true)` before spawning, so the charged-shot explosion uses `ExplosionInteraction.NONE` and spares arena terrain. Player-owned turrets are untouched (the flag is set only at the clone call sites, not in `shootAt`).
- 分身砲彈（`FloatingTurretEntity.cloneOrbitTick` / `cloneHandTick`）生成前呼叫 `proj.setNoBlockDamage(true)`，使蓄力爆炸採用 `ExplosionInteraction.NONE`、不破壞 arena 地形。玩家自己的浮游砲不受影響（旗標只在分身呼叫點設定，不在 `shootAt` 內）。

- `SpaceCrackEntity.interact` now branches on dimension: in `VOID_MIRROR` it calls `VoidMirrorTeleport.exit()`, elsewhere `enter()`. `enter()` spawns a non-decorative owner-tagged exit crack at the arena center, calls `clearExistingFor` (discards the player's residual clone / Nara / crack via `getEntitiesOfClass` + `getAllEntities`), and then spawns unconditionally. The old `alreadyExists` skip-guards in `spawnClone` / `spawnNaraPhantom` were removed: they were what skipped boss spawn on re-entry when a residual clone lingered. `activateAfterIntro` adds a same-source dedupe pass as a backstop for chunk-load timing. Intro camera `CAM_OFFSETS` A/B changed from a static pose to a dolly-in ({0,4.5,-22} to {0,2.4,-9}) so the opening segment moves together with the player's walk-out window.
- `SpaceCrackEntity.interact` 改為依維度分支：在 `VOID_MIRROR` 呼叫 `VoidMirrorTeleport.exit()`，否則 `enter()`。`enter()` 會在 arena 中心生成一個非裝飾、帶 owner 的出口裂縫，呼叫 `clearExistingFor`（用 `getEntitiesOfClass` + `getAllEntities` 清掉該玩家殘留的 clone / 娜拉 / 裂縫），然後無條件生成。移除了 `spawnClone` / `spawnNaraPhantom` 舊的 `alreadyExists` 守門：殘留 clone 時它會跳過 boss 生成，正是「再進入 boss 消失」的元兇。`activateAfterIntro` 加同源去重作為 chunk 載入時序的保險。進場鏡頭 `CAM_OFFSETS` A/B 從定點改為推軌（{0,4.5,-22} 到 {0,2.4,-9}），讓開場段與玩家走出窗同步移動。

- `PlayerCloneEntity` gained a `PillarSkill` state machine (`tickPillarSkill`, ~160t cooldown, 12t telegraph): it randomly picks RAM_WALL / LIFT_UP / CHARGE_RAMP, places blocks through the existing `takeWallBlock` / `placedWalls` / `VoidMirrorEvents.addModifiedBlock` path, and launches the player via `knockbackPlayer` (reuses the `setDeltaMovement` + `hurtMarked` pattern from `LeggingsDoubleJumpHandler`; crouch applies a ×0.35 multiplier). Intro timing was rescaled to 620t (~31s): client `VoidMirrorIntroManager.SEG_DURATIONS` {160,90,170,200}, server `INTRO_*` RISE 430/480, FLY 530, REVEAL 570, LEN 620, with denser gather particles. Player emerge changed from vertical `emergeOffset` to horizontal `walkOffset` (PoseStack Z translate from -3.2 to 0) plus a manual `walkAnimation.update` for limb swing; the decorative crack now spawns behind the arena point (`EMERGE_CRACK_BACK`).
- `PlayerCloneEntity` 新增 `PillarSkill` 狀態機（`tickPillarSkill`，約 160t 冷卻、12t 前搖）：隨機選 RAM_WALL / LIFT_UP / CHARGE_RAMP，透過既有 `takeWallBlock` / `placedWalls` / `VoidMirrorEvents.addModifiedBlock` 放方塊，並以 `knockbackPlayer` 擊飛玩家（複用 `LeggingsDoubleJumpHandler` 的 `setDeltaMovement` + `hurtMarked` 寫法；蹲下套 ×0.35）。進場時序重排為 620t（約 31 秒）：client `VoidMirrorIntroManager.SEG_DURATIONS` {160,90,170,200}，server `INTRO_*` RISE 430/480、FLY 530、REVEAL 570、LEN 620，集氣粒子加密。玩家登場從垂直 `emergeOffset` 改為水平 `walkOffset`（PoseStack Z 位移 -3.2 到 0）加手動 `walkAnimation.update` 驅動四肢擺動；裝飾裂縫改生成在登場點後方（`EMERGE_CRACK_BACK`）。

- `PlayerCloneEntity.mirrorFrom` now recursively expands container components (`CONTAINER` + `BUNDLE_CONTENTS`, depth capped at 4) into a pool of owned items, picks the best armor per humanoid slot via summed `ARMOR`/`ARMOR_TOUGHNESS` modifiers (`armorScore` + `Equipable.get`), fills `clonedInventory` from that pool, and collects floating turrets from every source. Block breaking was split: `tickAntiPillar` runs every phase (single support break, or a multi block downward column break when the target is `PILLAR_HEIGHT_TRIGGER`=3+ blocks above the clone) with its own `antiPillarCooldown`; `tickBreakSurroundings` (the old radius 5 scan, cooldown 15 to 10) runs only in WALLING/BERSERK. `VoidMirrorEvents` cancels shulker box place/open via `PlayerInteractEvent.RightClickBlock`/`RightClickItem` when in `VOID_MIRROR`.
- `PlayerCloneEntity.mirrorFrom` 改為遞迴展開容器 component（`CONTAINER` + `BUNDLE_CONTENTS`，深度上限 4）成擁有物品池，依 `ARMOR`/`ARMOR_TOUGHNESS` 修飾總和（`armorScore` + `Equipable.get`）選每個人形防具槽最強的防具，從該池填入 `clonedInventory`，並從所有來源收集浮游砲。破壞邏輯拆分：`tickAntiPillar` 每階段執行（拆單格支撐，或目標高出分身 `PILLAR_HEIGHT_TRIGGER`=3 格以上時往下連拆整段柱），有獨立的 `antiPillarCooldown`；`tickBreakSurroundings`（原半徑 5 掃描，冷卻 15 改 10）僅在 WALLING/BERSERK 執行。`VoidMirrorEvents` 在 `VOID_MIRROR` 維度透過 `PlayerInteractEvent.RightClickBlock`/`RightClickItem` 取消界伏盒子的放置/開啟。

- Floating turret upgrade system foundation. New `TURRET_UPGRADE_DATA` DataComponent (reuses `EquipmentUpgradeData` record, separate from armor's `EQUIPMENT_UPGRADE_DATA`). `TurretUpgradeBehavior` enum (CAPACITY 4mk / HEALING 4mk / HEALTH 3mk / DEFENSE 2mk, with `entityOnly` flag) + `TurretUpgradeItem`; 13 Mk items registered in `ModItems`. `FloatingTurretItem` gained static helpers (`getData/setData`, `recalculateMaxMana`, `getHealthBonus/getDamageReduction/getHealAmount`, `isValidUpgradeItem`, `getUpgradeBehaviorKey`, `getMaxUpgradeSlots`=6). Effects: CAPACITY recalcs MAX_MANA on swap; `FloatingTurretEntity.applyUpgradesFromOwner` sets MAX_HEALTH on spawn (called from `FloatingTurretEventHandler.spawnTurretEntity`); DEFENSE scales damage in `hurt()`; HEALING heals owner every 40t in `serverTick` consuming HEAL_MANA_COST(300). `getSourceStack` resolves the entity's backing ItemStack (hand or EXTRA_EQUIPMENT slot). Client `TurretUpgradeScreen` (wand-style, 6 slots, behavior-key dedup) + C2S `TurretUpgradeSwapPacket` (registered in `ModNetworking`, server-side same-behavior reject). U-key open order in `ClientTickHandler` is now wand -> turret -> armor. Color handler, creative-tab grouping, model names, and recipes added. Deferred (per design `1.md`): laser/auto-aim, wireless charging, taunt AI, omni-control plugin. This unblocks the [[project-boss-turret-mirror-deferred]] mirror.
- 浮游砲升級系統基建。新 `TURRET_UPGRADE_DATA` DataComponent（複用 `EquipmentUpgradeData` record，與盔甲的 `EQUIPMENT_UPGRADE_DATA` 分開）。`TurretUpgradeBehavior` enum（容量 4mk / 治療 4mk / 血量 3mk / 防禦 2mk，含 `entityOnly` 旗標）+ `TurretUpgradeItem`；`ModItems` 註冊 13 個 Mk 物品。效果：容量在安裝時重算 MAX_MANA；血量在實體生成時由 `applyUpgradesFromOwner` 設定 MAX_HEALTH；防禦在 `hurt()` 縮放傷害；治療在 `serverTick` 每 40t 治療擁有者並扣 300 魔力。客戶端 `TurretUpgradeScreen`（法杖風格、6 槽、依 behaviorKey 去重）+ C2S `TurretUpgradeSwapPacket`。U 鍵順序改為 杖 → 浮游砲 → 盔甲。已加染色、創造分頁分組、模型名、配方。延後（依 `1.md`）：激光/自動瞄準、無線充電、保護使用者 AI、萬能控制插件。

- Mirror World dimension (`koniava:void_mirror`) via datagen `RegistrySetBuilder` in `ModDimensionProvider` (biome/dimension_type/level_stem). Terrain uses a custom `BoundedFlatChunkGenerator` (extends `FlatLevelSource`) that clips blocks outside `HALF_SIZE` (250) to air, registered to `BuiltInRegistries.CHUNK_GENERATOR` in `ModChunkGenerators`. World border (500) is applied in `VoidMirrorTeleport.enter` when a player enters (and as a backup in `KoniavacraftMod.onLevelLoad`), because non-overworld dimension borders are delegated to the overworld border and a load-time set can be overwritten during server startup. Grayscale is a full-screen post-processing chain (`shaders/post/grey.json` + `program/grey.fsh`, reusing vanilla `blit` vertex), loaded/unloaded by dimension check in `ClientTickHandler` via `GameRenderer.loadEffect/shutdownEffect`.
- 鏡中世界維度（`koniava:void_mirror`）以 datagen `RegistrySetBuilder` 在 `ModDimensionProvider` 生成。地形用自訂 `BoundedFlatChunkGenerator`（繼承 `FlatLevelSource`），把 `HALF_SIZE`（250）以外的方塊裁切為空氣，註冊於 `ModChunkGenerators`。世界邊界於 level load 時在 `KoniavacraftMod.onLevelLoad` 設為 500。灰階為全螢幕後處理鏈（`shaders/post/grey.json` + `program/grey.fsh`，重用 vanilla `blit` 頂點），由 `ClientTickHandler` 依維度判斷以 `GameRenderer.loadEffect/shutdownEffect` 套用與解除。

- Boss subsystem B/C foundation: `PlayerCloneEntity extends Monster` (300 HP) stores source player UUID/name (synced) and a 36-slot mirrored inventory copy (saved, not synced, not dropped). `mirrorFrom` copies armor + hands (drop chance 0 to prevent dupes) and the full main inventory. `hurt` reflects 25% of damage back to a `Player` attacker. `checkDespawn`/`removeWhenFarAway` overridden so the boss never despawns. Rendered by `PlayerCloneRenderer extends HumanoidMobRenderer<..., PlayerModel<...>>` with armor + item-in-hand layers; skin resolved from source UUID via `getConnection().getPlayerInfo(uuid).getSkin().texture()`, falling back to `DefaultPlayerSkin`. Spawned in `VoidMirrorTeleport.enter` (one per source player, guarded against duplicates). Phase system (`Phase` NORMAL/WALLING/BERSERK driven by health ratio in `customServerAiStep`): WALLING places 2-high wall columns sealing the target's far side (blocks taken from `clonedInventory`, fallback `Blocks.STONE`, tracked in `placedWalls`, capped at 24, only within the world border); BERSERK tears down its own walls, adds a `ADD_MULTIPLIED_TOTAL` movement-speed modifier, and drains `MANA_STORED` from the target's equipped items. Phase and placed walls are saved. Completion (boss subsystem D): `PlayerCloneEntity.die` marks the source player cleared in `VoidMirrorSavedData` (global, overworld data storage), removes that player's `SpaceCrackEntity` in the overworld, and spawns a reward chest at the arena when no clones remain (chest contents TBD). `VoidMirrorEvents` handles player death in the dimension (discards their clone for a fresh re-entry) and respawn (`VoidMirrorTeleport.exit` to the stored `RETURN_POINT`). A `NaraPhantomEntity` (passive, invulnerable, no-AI watcher rendered by `NaraPhantomRenderer` as a player model; skin from the entering player's UUID as a placeholder until a dedicated Nara texture is supplied; nameplate uses `nara.hud.name`) spawns at a distance on entry. Turret mirroring is still blocked because no turret upgrade system exists yet; the entry cinematic is not implemented.
- Boss 子系統 B/C 基礎：`PlayerCloneEntity extends Monster`（300 HP）存來源玩家 UUID/name（同步）與 36 格鏡像背包副本（存盤、不同步、不掉落）。`mirrorFrom` 複製盔甲＋雙手（drop chance 0 防 dupe）與整個主背包。`hurt` 對 `Player` 攻擊者反彈 25% 傷害。覆寫 `checkDespawn`/`removeWhenFarAway` 使 boss 永不消失。由 `PlayerCloneRenderer extends HumanoidMobRenderer<..., PlayerModel<...>>` 渲染（含盔甲與手持物 layer）；皮膚由來源 UUID 經 `getConnection().getPlayerInfo(uuid).getSkin().texture()` 解析，fallback 為 `DefaultPlayerSkin`。於 `VoidMirrorTeleport.enter` 生成（每位來源玩家一隻，防重複）。浮游砲鏡像、疊方塊/破壞 AI、階段與魔力反噬尚未實作。

- Entry mechanism (boss subsystem A): `SpaceCrackEntity extends Entity` stores owner UUID (the altar `activatorUUID`), persists across reload, non-attackable, right-click calls `VoidMirrorTeleport.enter`. Spawned at end of `AspectAltarBlockEntity.triggerExplosion()` when `upgradeTier >= 3` (with `existsNear` dedup). `VoidMirrorTeleport` records a `GlobalPos` return point in the `RETURN_POINT` player data attachment before cross-dimension `teleportTo`. `SpaceCrackEntity.removeForOwner` is a stub for the future completion subsystem. Boss entity, equipment/turret mirroring, and completion/loot are not yet implemented.
- 入口機制（boss 子系統 A）：`SpaceCrackEntity extends Entity` 存 owner UUID（祭壇 `activatorUUID`），跨重載存在、不可攻擊，右鍵呼叫 `VoidMirrorTeleport.enter`。於 `AspectAltarBlockEntity.triggerExplosion()` 末尾在 `upgradeTier >= 3` 時生成（用 `existsNear` 防重複）。`VoidMirrorTeleport` 在跨維度 `teleportTo` 前把 `GlobalPos` 返回點存入 `RETURN_POINT` 玩家 data attachment。`SpaceCrackEntity.removeForOwner` 為後續結算子系統預留的空殼。Boss 實體、裝備/浮游砲鏡像、結算/寶箱尚未實作。

- Two-step armor recipe system: `MANA_ALLOY_HELMET/CHESTPLATE/LEGGINGS_BASE` registered as intermediate `Item` instances. Base shell recipes use `ManaCraftingRecipeBuilder.shaped()` with 800/1200/1000 mana cost; altar activation recipes use the base shell as catalyst with thematic ingredients. Old single-step altar recipes for helmet/chestplate/leggings removed.
- `MANA_EYE` item registered; shapeless mana crafting recipe (Ender Eye + Mana Crystal Alloy Dust, 2000 mana). Model auto-generated by `ModItemModelProvider` forEach loop via texture detection.
- `ChestplateManaShieldHandler`: subscribes to `LivingDamageEvent.Pre` (server-side only). Reads `event.getNewDamage()` (post-armor), absorbs 40% at 10 mana/damage via `event.setNewDamage()` and `ManaArmorItem.setMana()`. Partial absorption when mana insufficient.
- `ManaAlloyChestplateItem`: added `appendHoverText` override showing shield status (40% when mana > 0, inactive when empty).
- Mana Shield hit feedback: `ChestplateManaShieldHandler` plays `SoundEvents.SHIELD_BLOCK` and sends `ManaShieldHitPacket` (S2C) to the hit player. `ManaShieldEffectManager` tracks active effects (15-tick duration keyed by entity id); `ManaShieldEffectRenderer` draws a translucent blue sphere (14x28 lat/long, radius from bbHeight, culling disabled, depth-write off) on `RenderLevelStageEvent.AFTER_LEVEL` via a custom VAO/VBO + `shaders/armor/shield_ring` program. Cleaned up in `ClientTickHandler.onClientLogOut`. Packet registered in `ModNetworking`.
- `MANA_SPRINT_BOOTS_UNFINISHED` renamed to `MANA_SPRINT_BOOTS_BASE` (item ID, lang keys, altar recipe, texture). Old model JSON deleted; datagen forEach handles new model automatically.
- Run `runData` to generate: base shell item models, mana_eye model, new mana crafting table recipe JSONs for base shells, new altar activation recipe JSONs.

- Added `HelmetUpgradeBehavior.NIGHT_VISION` (purple, single item, no Mk scaling). Toggle state stored as `ModDataComponents.NIGHT_VISION_ACTIVE` (Boolean, persistent, networkSynchronized) on the helmet ItemStack.
- `ToggleNightVisionPacket` (C2S): verifies upgrade installed before toggling DataComponent.
- `HelmetNightVisionHandler`: server-side PlayerTickEvent.Post, refreshes Night Vision MobEffect only when duration < 60 ticks. Drains 1 mana per 4 ticks (5/s). Uses `NIGHT_VISION_WE_APPLIED` DataComponent to track whether we applied the effect, preventing removal of potion-based night vision on deactivation. Auto-sets `NIGHT_VISION_ACTIVE` false when mana hits 0.
- `ArmorCapacityUpgradeItem`: universal capacity upgrade class replacing 16 per-armor variants. `BONUS_PER_MK = [1000, 2000, 3500, 5500]`. Each armor piece's `isValidUpgradeItem`/`recalculateMaxMana`/`getUpgradeBehaviorKey` updated to accept it. `COLOR = 0xFF44FFAA` (matches all per-armor CAPACITY behavior colors). Color handler registered in `ModColorHandlers`. `ModItemModelProvider` UPGRADE_NAMES updated: removed 16 dead per-armor capacity entries, added 4 universal names. `ModCreativeModTabs` instanceof check updated to include `ArmorCapacityUpgradeItem` so it sorts into the upgrade items group.
- `ArmorDefenseUpgradeItem`: universal defense upgrade class (mirrors `ArmorCapacityUpgradeItem`) replacing 16 per-armor ARMOR variants. `BONUS_PER_MK = [1, 2, 3, 4]`, `COLOR = 0xFF8888FF`. Each armor piece's `isValidUpgradeItem`/`getUpgradeBehaviorKey`/`getArmorBonus` updated to use it (key `"DEFENSE"`). Removed `ARMOR` entry from `HelmetUpgradeBehavior`/`ChestplateUpgradeBehavior`/`LeggingsUpgradeBehavior`/`BootsUpgradeBehavior`; removed `HELMET/CHESTPLATE/LEGGINGS/BOOTS_UPGRADE_ARMOR_MK0-3` from `ModItems`, added `ARMOR_UPGRADE_DEFENSE_MK0-3`. Recipes in `ManaCraftingRecipeProvider` collapsed from 16 to 4 (`armor_upgrade_defense_mk0-3`). `ModItemModelProvider`, `ModColorHandlers`, and `ModCreativeModTabs` (instanceof check, so it sorts into the upgrade items group) updated accordingly. `ChestplateUpgradeItem`/`ChestplateUpgradeBehavior` now have no registered instances (CAPACITY-only enum) but remain referenced by the chestplate's validity check.
- Refactored `ManaSprintBootsItem` to extend `ManaArmorItem` instead of `ArmorItem`. Removed duplicate static helpers (`getMana`, `setMana`, `getMaxMana`, `getData`, `setData`, `getMaxUpgradeSlots`) and `getDefaultAttributeModifiers` — all inherited from base. Added `@Override` implementations for the three abstract methods and `getArmorBonus`. `BootsUpgradeSwapPacket` now uses instance polymorphism (`isValidUpgradeItem`, `getUpgradeBehaviorKey`, `recalculateMaxMana`) instead of static calls. `UnifiedArmorUpgradeScreen` boots special-case branches removed; all four armor slots now handled uniformly via `ManaArmorItem`.
- `ModKeyMappings`: added TOGGLE_HELMET/CHESTPLATE/LEGGINGS/BOOTS for future extensibility.
- Run `runData` to generate model JSONs for: `helmet_upgrade_night_vision`, `leggings_upgrade_multi_jump_mk0-mk3`, `armor_upgrade_capacity_mk0-mk3`.
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