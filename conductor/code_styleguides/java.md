# Java Style Guide (NeoForge 1.21.1)

## 1. Naming Conventions
*   **Classes:** `PascalCase` (e.g., `ManaGeneratorBlockEntity`)
*   **Methods:** `camelCase` (e.g., `tickMachine`, `getManaStored`)
*   **Variables:** `camelCase` (e.g., `maxProgress`, `playerEntity`)
*   **Constants:** `UPPER_SNAKE_CASE` (e.g., `MAX_MANA_CAPACITY`, `MOD_ID`)
*   **Interfaces:** `PascalCase`, optionally prefixed with `I` if widely used as a capability or API (e.g., `IManaStorage`), but modern Java prefers descriptive names (e.g., `ManaHandler`).
*   **Packages:** `lowercase.separated.by.dots` (e.g., `com.github.nalamodikk.common.block`)

## 2. Formatting
*   **Indentation:** 4 spaces (No tabs).
*   **Braces:** K&R style (opening brace on the same line).
*   **Imports:** explicit imports preferred over wildcards (`*`). Grouped by:
    1.  `java.*` / `javax.*`
    2.  `net.minecraft.*`
    3.  `net.neoforged.*`
    4.  Project classes (`com.github.nalamodikk.*`)
    5.  Static imports

## 3. NeoForge Specifics
*   **Registration:** Use `DeferredRegister` for all registries (Blocks, Items, BlockEntities, etc.).
*   **Networking:** Use `Payload` based packet system for 1.20.5+.
*   **Annotations:** 
    *   Use `@Override` whenever possible.
    *   Use `@Nullable` and `@Nonnull` (from `javax.annotation`) for API boundaries.
    *   Use `@Sync` for fields requiring Auto-Sync.

## 4. Documentation & Comments
*   **Javadoc:** Required for all `public` methods and classes in `coreapi` and `utils`.
*   **Language:** Documentation comments should be in **Traditional Chinese (繁體中文)** as per project requirements.
*   **Inline Comments:** Use sparingly, focus on *why* complex logic exists.

## 5. Project Patterns
*   **Common vs Client:** Strictly separate logic. Client-only code must be in `client` package or isolated via `Dist.CLIENT`.
*   **Mixin:** Use Mixins only when API/Events are insufficient. Keep Mixins minimal and robust.
*   **Logging:** Use `SLF4J` logger provided by `LogUtils.getLogger()`. Avoid `System.out.println`.

## 6. Error Handling
*   Fail gracefully. Avoid crashing the game for non-critical errors.
*   Log errors with context (e.g., "Failed to load recipe for Mana Infuser at [x,y,z]").

## 7. AI & Tooling Safety
*   **No Code Abbreviation:** When writing or replacing files, NEVER use placeholders like `// ... existing code ...`. Always provide the full, valid code to prevent accidental deletion of file contents.
*   **Verify Replacements:** Double-check `replace` tool calls to ensure `old_string` matches exactly and `new_string` is complete.
