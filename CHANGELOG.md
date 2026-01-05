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

## [0.0.1.5 Preview] - 2026-01-04
### Changed
- Removed RPG command registration and class.
- Prefixed test and UI commands with `koniava`.
- Fixed mana generator GUI mode sync index to reflect actual mode state.
