# Implementation Plan - CooParticlesAPI Refactor & Completion

## Phase 1: Analysis & Infrastructure Prep (Gap Analysis)
本階段目標：徹底盤點現有 `com.github.nalamodikk.particle` 代碼與參考專案的差異，並建立 Shader 基礎建設。

- [x] Task: Audit Existing Codebase
    - [x] Sub-task: Analyze `com.github.nalamodikk.particle.utils` vs Reference `Math3DUtil/GraphMathHelper`.
    - [x] Sub-task: Analyze `PointsBuilder` (if exists) or geometry logic vs Reference `PointsBuilder`.
    - [x] Sub-task: Analyze `ControlableParticle` structure vs Reference.
- [x] Task: Shader Infrastructure Setup
    - [x] Sub-task: Create `com.github.nalamodikk.particle.render.shader` package structure.
    - [x] Sub-task: Port `GlShader`, `GlShaderType`, `FileShader` (Java implementation).
    - [x] Sub-task: Port `SimpleShaderProgram` and `ShaderProgramBuilder`.
- [x] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md) [checkpoint: df0b7f4]

## Phase 2: Geometry & Math System (Core Refactor)
本階段目標：確保數學與幾何生成核心功能完整。

- [x] Task: Math Utilities Completion
    - [x] Sub-task: Implement/Update `RotationMatrix`, `Math3DUtil`, `GraphMathHelper` (Interpolation logic).
- [x] Task: PointsBuilder Implementation
    - [x] Sub-task: Port `PointsBuilder` core logic (add, rotate, scale).
    - [x] Sub-task: Implement shape generators: Circle, Polygon, Spiral.
    - [x] Sub-task: Test `PointsBuilder` with a simple unit test or debug output.
- [x] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md) [checkpoint: 81fb563]

## Phase 3: Style & Control System (Logic Layer)
本階段目標：建構樣式系統與粒子控制器，連結幾何數據與視覺表現。

- [x] Task: Particle Style System
    - [x] Sub-task: Define `ParticleStyle` interface.
    - [x] Sub-task: Implement `ParticleShapeStyle` (integrates PointsBuilder).
    - [x] Sub-task: Implement `ParticleGroupStyle`.
- [x] Task: Particle Controller Enhancement
    - [x] Sub-task: Refactor `ParticleController` to support `preTickAction` and `postTickAction` (as seen in reference).
    - [x] Sub-task: Ensure `ControlableParticle` respects Style settings.
- [x] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md) [checkpoint: f7a05aa]

## Phase 4: Shader Integration & Rendering
本階段目標：將 Shader 系統整合進 Minecraft 渲染管線。

- [x] Task: Render Type & Pipeline
    - [x] Sub-task: Create custom `ParticleRenderType` that uses our ShaderProgram.
    - [x] Sub-task: Implement `RenderLevelStageEvent` handler to dispatch shader drawing.
- [x] Task: Buffer Management
    - [x] Sub-task: Port `VertexBuffer` / `DynamicVertexBuffer` logic for efficient batch rendering.
- [x] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md) [checkpoint: 20b7cc8]

## Phase 5: Verification & Polish (The "Roma Magic" Test)
本階段目標：實作羅馬魔法陣範例，並補完缺失的動畫與圖形邏輯。

- [x] Task: Port RomaMagicTestStyle
    - [x] Sub-task: Implement `MathPresets` (Roma Numbers).
    - [x] Sub-task: Implement `ScaleHelper` (Animation).
    - [x] Sub-task: Create `RomaMagicTestStyle` class in Java.
    - [x] Sub-task: Register necessary textures/assets.
    - [x] Sub-task: Create a debug item/command to trigger this style.
- [x] Task: Final Polish
    - [x] Sub-task: Verify visual effects in-game.
    - [x] Sub-task: Add Javadoc (Traditional Chinese) for public APIs.
- [x] Task: Conductor - User Manual Verification 'Phase 5' (Protocol in workflow.md) [checkpoint: 48ee2d4]

## Phase 6: Framework Completion (Full Port)
本階段目標：回應「必須重現該框架」的要求，補完網路同步、渲染管線與進階數學功能。

- [x] Task: Network Synchronization System
    - [x] Sub-task: Port `PacketParticleStyleS2C` and networking logic.
    - [x] Sub-task: Implement `ServerControler` interface and logic.
    - [x] Sub-task: Ensure server-side triggered styles sync to all clients.
- [x] Task: Shader Pipe System (Advanced Rendering)
    - [x] Sub-task: Port `ShaderPipe` and `ShaderPipeManager`.
    - [x] Sub-task: Implement `GraphPipeLinker` for complex shader chaining.
- [x] Task: Advanced Math & Presets
    - [x] Sub-task: Port `BezierCurve` and `FourierSeries` logic.
    - [x] Sub-task: Implement `ImagePointBuilder` (if applicable).
- [x] Task: Conductor - User Manual Verification 'Phase 6' (Protocol in workflow.md) [checkpoint: 0d52577]

## Phase 7: The Missing Features (Barrage, PathMotion, Advanced VFX)
本階段目標：補完框架中剩餘的高階功能：彈幕系統、路徑動畫、圖片粒子與進階發光渲染。

- [x] Task: Barrage System (Physics & Collision)
    - [x] Sub-task: Implement `HitBox` and `Barrage` abstract class.
    - [x] Sub-task: Implement `BarrageManager` for handling collision ticks.
- [x] Task: Path Motion System (Advanced Animation)
    - [x] Sub-task: Port `PathMotion` interface and implementations.
    - [x] Sub-task: Integrate PathMotion into `ParticleController`.
- [x] Task: Lightning & Image Builder
    - [x] Sub-task: Implement `Math3DUtil` lightning generation logic.
    - [x] Sub-task: Implement `ImagePointBuilder` using Minecraft's `NativeImage`.
- [x] Task: Advanced Shader Pipes (Bloom/Glow)
    - [x] Sub-task: Implement `PingPongShaderPipe`.
    - [x] Sub-task: Setup basic Bloom effect infrastructure.
- [x] Task: Conductor - User Manual Verification 'Phase 7' (Protocol in workflow.md) [checkpoint: dc52ca6]

## Phase 8: Reference Parity Audit & Refinement
本階段目標：根據參考庫 `AI-context\別人的渲染框架-僅參考` 進行最終審計，確保功能與參考庫的核心邏輯一致，並補完潛在缺失。

- [x] Task: Audit & Refine Shader Pipeline
    - [x] Sub-task: Compare `PingPongShaderPipe` and Bloom logic with reference `renderer/shader/pipe`.
    - [x] Sub-task: Verify `ShaderPipeManager` logic against reference.
- [x] Task: Audit & Refine Network System
    - [x] Sub-task: Check `PacketParticleStyleS2C` and `PacketParticleGroupS2C` fields against reference `network/packet`.
- [x] Task: Audit & Refine Math Utilities
    - [x] Sub-task: Verify `Math3DUtil` and `RotationMatrix` completeness.
- [x] Task: Conductor - User Manual Verification 'Phase 8' (Protocol in workflow.md)

## Phase 9: Deep Consistency Check & Refinement (Post-Build Audit)
響應用戶要求，進行更深度的功能一致性檢查與移植完善，重點在於修復測試錯誤、補全遺漏的輔助方法，以及驗證動畫與發射器系統的完整性。

- [ ] Task: Fix Test Suite & Config Initialization
    - [ ] Sub-task: Investigate and fix `ModCommonConfig` initialization error in unit tests.
    - [ ] Sub-task: Ensure all tests in `ParticleManagerTest`, `PointsBuilderTest`, etc., pass.
- [ ] Task: Structural Parity Audit (Missing Methods Check)
    - [ ] Sub-task: Scan `Math3DUtil` and `RotationMatrix` again for any other missing helper methods used in Reference.
    - [ ] Sub-task: Check `Animate` and `AnimateManager` against reference for completeness.
    - [ ] Sub-task: Check `ParticleEmitters` and `EmitterOption` logic.
- [ ] Task: Final Polish & Verification
    - [ ] Sub-task: Verify `ParticleStyle` serialization logic (Packet consistency).
    - [ ] Sub-task: Ensure strict type mapping between Kotlin `Int` and Java `Integer` in buffers.
- [ ] Task: Conductor - User Manual Verification 'Phase 9'