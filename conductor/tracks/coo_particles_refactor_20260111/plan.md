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
- [x] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Style & Control System (Logic Layer)
本階段目標：建構樣式系統與粒子控制器，連結幾何數據與視覺表現。

- [ ] Task: Particle Style System
    - [ ] Sub-task: Define `ParticleStyle` interface.
    - [ ] Sub-task: Implement `ParticleShapeStyle` (integrates PointsBuilder).
    - [ ] Sub-task: Implement `ParticleGroupStyle`.
- [ ] Task: Particle Controller Enhancement
    - [ ] Sub-task: Refactor `ParticleController` to support `preTickAction` and `postTickAction` (as seen in reference).
    - [ ] Sub-task: Ensure `ControlableParticle` respects Style settings.
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

## Phase 4: Shader Integration & Rendering
本階段目標：將 Shader 系統整合進 Minecraft 渲染管線。

- [ ] Task: Render Type & Pipeline
    - [ ] Sub-task: Create custom `ParticleRenderType` that uses our ShaderProgram.
    - [ ] Sub-task: Implement `RenderLevelStageEvent` handler to dispatch shader drawing.
- [ ] Task: Buffer Management
    - [ ] Sub-task: Port `VertexBuffer` / `DynamicVertexBuffer` logic for efficient batch rendering.
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)

## Phase 5: Verification & Polish (The "Roma Magic" Test)
本階段目標：實作羅馬魔法陣範例，驗證所有系統協同運作。

- [ ] Task: Port RomaMagicTestStyle
    - [ ] Sub-task: Create `RomaMagicTestStyle` class in Java.
    - [ ] Sub-task: Register necessary textures/assets.
    - [ ] Sub-task: Create a debug item/command to trigger this style.
- [ ] Task: Final Polish
    - [ ] Sub-task: Verify visual effects in-game.
    - [ ] Sub-task: Add Javadoc (Traditional Chinese) for public APIs.
- [ ] Task: Conductor - User Manual Verification 'Phase 5' (Protocol in workflow.md)
