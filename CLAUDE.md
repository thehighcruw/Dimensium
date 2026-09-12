# Dimensium — Development Guide

Minecraft 1.7.10 builder-wand mod. Java 8. RetroFuturaGradle. Main package: `github.thehighcruw.dimensium`.

---

## Source Layout

| Path | Purpose |
|------|---------|
| `src/main/java/.../tool/` | Core state: selection, clipboard, tool enum, shape math |
| `src/main/java/.../tool/state/` | Per-tool state singletons |
| `src/main/java/.../network/` | Client↔server packets (FML SimpleImpl) |
| `src/main/java/.../render/` | Client-only renderers and overlay GUI |
| `src/main/java/.../render/panel/sections/` | Per-tool panel UI sections |
| `src/main/java/.../render/brushes/` | Brush preview views per tool |
| `src/main/java/.../handler/` | FML event handlers (key input, tick, item) |
| `src/main/java/.../handler/brushes/` | Brush input handlers per tool |
| `src/main/java/.../item/` | BuilderWand item |
| `src/main/java/.../proxy/` | Client / server proxy split |

---

## Read-only Context Libraries — DO NOT EDIT

`.context/` is **gitignored and read-only**. Shallow clones of upstream GTNH libraries for reference only.

| Directory | Library | Purpose |
|-----------|---------|---------|
| `.context/GTNHLib/` | GTNHLib | GTNH utility library — config, networking, keybinds, color, geometry |
| `.context/RegionLib/` | RegionLib | Region/cubic-chunk file I/O library |
| `.context/GTNHExtLib/` | GTNHExtLib | Bootstrap/classloader utility |

To refresh a clone: `git -C .context/<dir> pull`.

---

## Design Tenets

### 1. i18n — no hardcoded strings in UI

All user-visible text goes through `I18n.format("dimensium.some.key")`. Add keys to `src/main/resources/assets/dimensium/lang/en_US.lang`. Never inline English strings in render code.

### 2. Strategy + Registry over static switch/if-chains

New tool behaviours attach via registry, not by editing a central switch. Current registries:

| Registry | Maps | Purpose |
|---|---|---|
| `BrushInputRegistry` | `Tool → BrushInput` | mouse/click handling per tool |
| `BrushViewRegistry` | `Tool → BrushView` | brush outline preview per tool |
| `ToolStates` | `Tool → ToolSection` | panel section per tool |

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

### 8. Prefer GTNHLib over reinventing

Before writing new infrastructure, check `.context/GTNHLib/`. Open integration opportunities below.

---

## GTNHLib Integration Opportunities

### 1. `SyncedKeybind` — replace manual key sync

**Current**: `KeyHandler` reads raw `InputEvent.KeyInputEvent`, then sends custom packets to tell the server which keys are held.

**GTNHLib path**: `com.gtnewhorizon.gtnhlib.keybind.SyncedKeybind`

**What to do**: Register toggle key and builder-mode modifier keys as `SyncedKeybind.createConfigurable(...)`. Remove `PacketKeyDown`-style manual sync. Server-side code calls `syncedKeybind.isKeyDown(player)` directly.

---

### 2. `HSVColor` / `RGBColor` — replace raw-int color in BlockColorCache

**Current**: `BlockColorCache` computes average icon colors as raw `int` RGB, manually bit-shifting channels (`>> 16`, `>> 8`, `& 0xFF`).

**GTNHLib path**: `com.gtnewhorizon.gtnhlib.color.{RGBColor,HSVColor,ImmutableColor}`

**What to do**: Wrap computed average colors in `RGBColor`. Use `.toHSV()` for hue-sorted palette display.

---

### 3. `CubeIterator` — replace triple-nested loops in shape operations

**Current**: `ShapeMath` and `BuilderTool` operations use nested `for x / y / z` loops over the selection bounding box.

**GTNHLib path**: `com.gtnewhorizon.gtnhlib.geometry.CubeIterator`

**What to do**: For sphere/brush-shaped selections (`BrushShape`), replace nested loops with `CubeIterator` centered on the brush origin. AABB selections keep nested loops.

---

### 4. `MutableXYZ` / `ImmutableXYZ` — typed spatial coords

**Current**: `SelectionState`, `ShapePlacementState`, and packet classes pass block positions as bare `int x, y, z` triples or `int[]` arrays.

**GTNHLib path**: `com.gtnewhorizon.gtnhlib.space.{MutableXYZ,ImmutableXYZ}`

**What to do**: Use `ImmutableXYZ` for clipboard origin and selection corners. Use `MutableXYZ` as scratch inside loops to avoid allocating `int[]` tuples (e.g. in `PacketBlockList`).

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

Always run `./gradlew classes` after edits to confirm no compile errors before reporting a task complete. Run `./gradlew test` when touching geometry, selection, or raycast logic.

---

## Adding a New ImGui Window

Extend `ImGuiWindow` and add to `ClientProxy`. That is all — mouse routing is automatic.

### 1. Extend `ImGuiWindow`, implement `isOpen()`

```java
public class MyWindow extends ImGuiWindow {

    public static final MyWindow INSTANCE = new MyWindow();

    private boolean open = false;

    private MyWindow() {}   // constructor calls super() → auto-registers with ImGuiWindowRegistry

    @Override
    public boolean isOpen() { return open; }

    public void renderImGui() {
        if (!open) return;
        ImGui.begin("My Window###my_window", ...);
        captureBounds();  // ← must be immediately after ImGui.begin()
        // ... content ...
        ImGui.end();
    }
}
```

### 2. Register in `ClientProxy.postInit()` — touch INSTANCE in the array

Add `MyWindow.INSTANCE` to the windows array in `ClientProxy.postInit()`. This forces class loading (and therefore registry self-registration) before any mouse event fires.

### 3. Call `renderImGui()` from `OverlayRenderer`

Add `MyWindow.INSTANCE.renderImGui()` to the imgui render block alongside the other windows (around line 496).

No changes needed to `InputHandler` — `ImGuiWindowRegistry.INSTANCE.anyContainsMouse()` picks up the new window automatically.
