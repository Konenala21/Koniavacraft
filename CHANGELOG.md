# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### Changed
- Expanded render bounding boxes for mana generator and solar mana collector renderers to cover 1x2x1 visuals.
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
- Mana generator, solar collector, ore grinder, and mana infuser now scale output or speed with the owner's RPG intelligence.
- Prevented duplicate payload registration by skipping server-side networking registration on client.
- Registered SyncRPGDataPacket on the server to avoid missing payload types.
- Hid the mana crafting table mana bar background to avoid overlapping the GUI frame.
- Hid the mana infuser mana bar background to avoid overlapping the GUI frame.
- Restricted server-side payload registration to dedicated servers to avoid duplicate registration on integrated client.
- Updated ore grinder GUI size and slot layout to align with the mana infuser layout.
- Added a recipe hint hotspot for the ore grinder and linked it to JEI.
- Updated ore grinder progress drawing to match the mana infuser style and removed the progress background.
- Hid the ore grinder mana bar background to avoid overlapping the GUI frame.
- Fixed JEI grinder recipe rendering by using the client font and translation keys.
- Aligned grinder JEI title with the block translation key and set the JEI background crop to 171x77.
- Updated grinder JEI slot positions to match the new GUI layout and avoided tooltip overlap when JEI is loaded.
- Shifted grinder JEI slot positions by one pixel to match the in-game layout.
- Added a dedicated ore grinder recipe datagen provider and moved grinder recipes there.
- Renamed the ore grinder block to mana grinder in translations.
- Shifted ore grinder GUI and JEI recipe slots two pixels to the right.
- Rendered the grinder JEI mana cost as a bar instead of text.
- Replaced deprecated JEI background override with recipe extras background drawable.
- Ore grinder now uses each recipe's processing time and consumes the recipe mana cost once per craft.
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
