# Dimensium — Development Guide

Minecraft 1.7.10 building mod. Java 8. RetroFuturaGradle. Main package: `github.thehighcruw.dimensium`.

> **Java version note**: The header says Java 8 but the project uses [Jabel](https://github.com/bsideup/jabel) to compile modern Java syntax (records, sealed classes, switch expressions, etc.) down to Java 8 bytecode. Annotate records with `@com.github.bsideup.jabel.Desugar`. All modern syntax is fair game.

---

## Source Layout

| Path                                          | Purpose                                                                                               |
|-----------------------------------------------|-------------------------------------------------------------------------------------------------------|
| `src/main/java/.../tool/`                     | `BuilderTool` item, `BuilderToolState`, `ChangeProposal`                                              |
| `src/main/java/.../editor/blueprint/`         | Blueprint save/load, registry, thumbnail cache                                                        |
| `src/main/java/.../editor/clipboard/`         | Clipboard utilities                                                                                   |
| `src/main/java/.../editor/freecam/`           | Freecam entity, state, and utilities                                                                  |
| `src/main/java/.../editor/handler/`           | FML event handlers: key input, tick, mouse, selection ops, anchor snap, extrude helper                |
| `src/main/java/.../editor/history/`           | Undo/redo: `EditHistory`, `ClientEditHistory`, server capture/edit queues                             |
| `src/main/java/.../editor/overlay/`           | HUD overlay, menu bar, status bar, editing mode screen, layout presets, view state                    |
| `src/main/java/.../editor/tool/`              | Tool enum/registry, `BrushInput`/`BrushInputRegistry`, `BrushApplicator`, `ToolStates`                |
| `src/main/java/.../editor/tool/brushes/`      | Shared brush infrastructure: `BrushState`, `BrushStrategy`, `BrushShape`, Gaussian kernel             |
| `src/main/java/.../editor/tool/creating/`     | Block-creating tools: fill, freehand, modelling, path, rock, sculpt, shape, stamp                     |
| `src/main/java/.../editor/tool/gizmo/`        | Gizmo interaction interfaces (translate, rotate, scale)                                               |
| `src/main/java/.../editor/tool/manipulating/` | Selection-manipulating tools: distort, elevation, extrude, melt, move, roughen, shatter, smooth, weld |
| `src/main/java/.../editor/tool/mask/`         | Mask nodes, serializer, registry, and utilities                                                       |
| `src/main/java/.../editor/tool/noise/`        | `NoiseSampler`, noise preview renderer                                                                |
| `src/main/java/.../editor/tool/painting/`     | Paint tools: gradient, noise, painter                                                                 |
| `src/main/java/.../editor/tool/selecting/`    | Selection tools: box, freehand, lasso, magic; boolean ops and shared state                            |
| `src/main/java/.../editor/tool/state/`        | Shared per-tool state: `PaletteState`, `PaletteRegistry`, `ClipboardPlacementState`                   |
| `src/main/java/.../editor/tool/utility/`      | Utility tools: ruler                                                                                  |
| `src/main/java/.../editor/window/`            | All ImGui windows (palette, clipboard, selection, operations, autoshade, history, …)                  |
| `src/main/java/.../editor/window/imgui/`      | ImGui core: `ImGuiWindow`, `ImGuiWindowRegistry`, `ImGuiManager`, GL renderer, icon/item caches       |
| `src/main/java/.../editor/window/panel/`      | Shared panel draw utilities and `PanelSection` base                                                   |
| `src/main/java/.../editor/window/popup/`      | ImGui popup/modal windows (block picker, blueprint browser, create blueprint, conflict, settings)     |
| `src/main/java/.../editor/window/viewport/`   | Viewport capture, panel, registry, state, and world-space renderers                                   |
| `src/main/java/.../network/`                  | Client↔server packets (FML SimpleImpl)                                                                |
| `src/main/java/.../proxy/`                    | Client / server proxy split                                                                           |
| `src/main/java/.../shared/`                   | `SelectionState`, `SelectionTransforms`, `BoundingBox`, `InputHandler`, `KeyConstants`                |
| `src/main/java/.../shared/math/`              | `Vec3DInt/Float/Double`, `Vec2DFloat/Double`, `Mat3DFloat`, tri-int functional interfaces             |
| `src/main/java/.../shared/util/`              | `BlockUtils`, `WorldUtils`, `RenderUtils`, `UIUtils`, `PerfTrace`                                     |
| `src/main/java/.../world/handler/`            | Server-side FML event handlers for builder tool and mouse                                             |
| `src/main/java/.../world/inventory/`          | Legacy Minecraft GUI screens: colour picker, gradient helper                                          |
| `src/main/java/.../world/tool/`               | Server-side `BuilderToolApplicator` and placement strategies (clone, erase, move, smear, stack)       |

---

## Read-only Context Libraries — DO NOT EDIT

`.context/` is **gitignored and read-only**. Shallow clones of upstream GTNH libraries for reference only.

| Directory              | Library    | Purpose                                                              |
|------------------------|------------|----------------------------------------------------------------------|
| `.context/GTNHLib/`    | GTNHLib    | GTNH utility library — config, networking, keybinds, color, geometry |
| `.context/RegionLib/`  | RegionLib  | Region/cubic-chunk file I/O library                                  |
| `.context/GTNHExtLib/` | GTNHExtLib | Bootstrap/classloader utility                                        |

To refresh a clone: `git -C .context/<dir> pull`.

---

## Design Tenets

### 1. i18n — no hardcoded strings in UI

All user-visible text goes through `I18n.format("dimensium.some.key")`. Add keys to `src/main/resources/assets/dimensium/lang/en_US.lang`. Never inline English strings in render code.

### 2. Strategy + Registry over static switch/if-chains

New tool behaviours attach via registry, not by editing a central switch. Current registries:

| Registry             | Maps                 | Purpose                        |
|----------------------|----------------------|--------------------------------|
| `BrushInputRegistry` | `Tool → BrushInput`  | mouse/click handling per tool  |
| `BrushViewRegistry`  | `Tool → BrushView`   | brush outline preview per tool |
| `ToolStates`         | `Tool → ToolSection` | panel section per tool         |

Adding a new tool = register in each relevant registry. Never add `if (tool == Tool.X)` branches in shared code paths.

### 3. UI and state are separate layers

Tool state lives in `tool/state/` (e.g. `NoiseToolState`, `ShapeToolState`). Panel sections in `render/panel/sections/` read and mutate those state objects — they do not own state themselves. State objects are reusable from server logic, packets, and preview renderers independently of the panel.

### 4. Prefer instance fields over static fields

Static fields are only appropriate for:
- **Constants**: `static final` primitives and immutable objects
- **Singleton instances**: `public static final INSTANCE` on true singletons (one instance for the lifetime of the game)

Do **not** use static fields for:
- Mutable state that belongs to a single object's lifecycle (e.g. open/close flags on popup classes, cached render state in renderers)
- Shared mutable state as a shortcut to avoid passing objects (e.g. `static activeDrag`, `static lastExtrudeX`)
- State that would be testable or resettable if it were an instance field

`*ToolState` singletons (`public static final INSTANCE`) are acceptable because exactly one state object exists per tool for the lifetime of the mod. Everything else: make it an instance field.

### 5. Client/server side discipline

Annotate client-only classes with `@SideOnly(Side.CLIENT)`. Never call client-only APIs (Minecraft, GL11, I18n, etc.) from server-side paths. Network boundary = `network/` packets. Server logic lives in `handler/` classes that only touch the world/player API.

### 6. Always use imports — never fully-qualified names in code

Use `import` statements. Never write fully-qualified class names inline (e.g. `com.foo.Bar.baz()`). Java circular imports are not a problem as long as there are no circular *static initializer* dependencies — and following Tenet 4 (no mutable statics) prevents those.

### 7. No magic numbers — use named constants

Never use bare numeric literals for values that have semantic meaning. Declare `static final` constants (or interface constants) with descriptive names. Common examples in this codebase:

- Mouse buttons: `InputHandler.LMB = 0`, `InputHandler.RMB = 1` — never write `button == 0` or `button != 1`, and never redeclare these locally
- Keycodes: bind to a named key constant, not a raw integer
- Block/meta IDs: assign a descriptive constant, not an inline literal

Exception: `0` and `1` used as loop indices or array sizes where the value *is* the concept (e.g. `i = 0`, `array.length - 1`).

### 8. Remove dead code and redundant dependencies

Delete unused code rather than commenting it out or leaving it in place. This includes:

- Unused imports, fields, methods, and classes
- Gradle plugins, configurations, and dependencies that no longer serve a purpose
- Feature flags, compatibility shims, or workarounds that have been superseded

When a tool, library, or plugin is replaced by a better alternative, remove the old one in the same change. Do not leave disabled-but-present infrastructure (e.g. `isEnabled = false` blocks, commented-out plugin applications).

### 9. Make use of Vec3D{Int/Float/Double} and Mat3DFloat

These should be used for representing vectors and matrices and methods on them should be preferred for vector/matrix math.
Unpacking the scalar values embedded in vectors should be avoided as long as possible.

### 10. Naming

Java variables and methods should be camelcase and abbreviations and shorthands should be avoided.
Single character names are not allowed.

### 11. Re-use of code

Before creating a function that may be a utility - especially static functions -, ensure to check whether an implementation
already exists. Make sure to re-use existing utility functions whenever possible.

### 12. Boyscout rule

When you touch a piece of code, review it for structural issues, technical debt, high complexity.
Leave it cleaner than you touched it when possible.

### 13. Put static functions in Utils classes

When you have a function that may be used in various classes and should not be expressed as a member function,
it should be created as a static function as part of a utility class.
A few of such classes already exist. If a fitting place exists, make sure to place the static function in that class.
Those utility classes should not have mutable static state.

---

## Development Workflow

### Build & compile check

```bash
./gradlew classes
```

### Run tests

```bash
./gradlew test
```

### Run the game (client)

```bash
./gradlew runClient
```

### CPD (copy-paste detection)

```bash
./gradlew cpdCheck
```

### Apply spotless formatter

```bash
./gradlew spotlessApply
```

Always run `./gradlew spotlessApply` and `./gradlew classes` after edits to confirm no compile errors before reporting a task complete. Run `./gradlew test` when touching geometry, selection, or raycast logic.
