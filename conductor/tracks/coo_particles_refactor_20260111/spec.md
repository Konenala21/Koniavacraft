# Track Specification: CooParticlesAPI Refactor & Completion

## 1. Overview
本任務旨在完善已初步移植到 `Koniavacraft` 的 `CooParticlesAPI` 粒子系統。
目前的實作位於 `com.github.nalamodikk.particle`，但功能尚未完全對齊原 Kotlin 參考專案。
本軌道將專注於重構現有代碼以符合原框架的設計模式 (特別是幾何構建與樣式系統)，並補完缺失的 Shader 渲染功能。

## 2. Functional Requirements

### 2.1 幾何系統重構 (Geometry Refactor)
- [ ] **PointsBuilder 升級**: 檢查並擴充現有的幾何生成邏輯，確保支援參考專案中的所有形狀 (圓、多邊形、螺旋、貝茲曲線)。
- [ ] **Math Util 整合**: 確認 `utils` 套件中的數學工具是否完整，補足缺失的 3D 旋轉與矩陣運算功能。

### 2.2 樣式與控制增強 (Style & Control Enhancement)
- [ ] **ParticleStyle 系統**: 實作或重構 `ParticleStyle` 介面，使其能像參考專案一樣靈活組合 (例如 `ParticleShapeStyle` + `PointsBuilder`)。
- [ ] **群組控制**: 強化 `ParticleGroup` 邏輯，確保能處理複雜的複合特效 (如多層魔法陣)。

### 2.3 Shader 渲染系統實作 (Shader Implementation)
- [ ] **Shader Infrastructure**: 引入 `GlShader`, `FileShader` 等基礎設施 (目前似乎缺失)。
- [ ] **Custom Render Types**: 實作支援自定義 Shader 的 `RenderType`。
- [ ] **Pipeline Integration**: 將 Shader 渲染邏輯整合進 NeoForge 的渲染管線。

### 2.4 範例移植與驗證 (Migration & Verification)
- [ ] **RomaMagicTestStyle**: 完整移植「羅馬魔法陣」範例，作為驗證框架功能的基準 (Benchmark)。
- [ ] **現有特效遷移**: 檢查 `effects` 目錄下的現有特效，將其重構為使用新的 Builder/Style 系統。

## 3. Non-Functional Requirements
- **重構優先**: 盡量重用現有類別，避免產生重複代碼。
- **Java 21**: 保持純 Java 實作。
- **註解規範**: 為新加入或重構的 API 補上完整的繁體中文 Javadoc。

## 4. Acceptance Criteria
- [ ] `PointsBuilder` 能流暢生成複雜幾何圖形。
- [ ] 成功渲染帶有 Shader 特效 (如發光/流動) 的粒子。
- [ ] 遊戲內能透過指令召喚出與參考影片/專案一致的「羅馬魔法陣」。
