> **Disclaimer**: Dimensium is currently in early Alpha.
> You may use it, but prepare yourself for bugs, unexpected or underdefined behavior, and lost progress.
> For people using it, please report issues in the GitHub issue-tracker.

# Dimensium

A building mod for Minecraft 1.7.10, inspired by [Axiom](https://modrinth.com/mod/axiom).
Dimensium brings powerful in-game world editing tools — selection, clipboard, shape placement, brush operations to 1.7.10 Minecraft.

> **Warning:** Dimensium can place or destroy thousands of blocks at once.
> Use it **only in creative mode** and keep **frequent backups** of your world.
> One misclick can cause massive changes, which Undo may not always be able to reverse.
> The authors are not responsible for lost progress.

---

## Features

### Selection
- Box, freehand, lasso, and magic-wand selection tools
- Boolean operations (add, subtract, intersect)
- Clipboard: copy, paste, fill

### Manipulation tools
- **Move** — translate selection with optional scaling
- **Extrude** — push faces outward
- **Distort** — warp geometry
- **Slope** — apply a linear slope to a selection
- **Elevation** — raise/lower terrain with custom or preconfigured heightmaps
- **Melt / Smooth / Roughen / Weld / Shatter** — surface refinement brushes

### Creation tools
- Shape placement (sphere, cylinder, and more)
- Fill, freehand, path, rock, sculpt, stamp, modelling brushes
- Brush with configurable shape, radius, and Gaussian falloff

### Painting
- Painter, gradient, and noise paint tools

### Gizmo
- Translate, rotate, and scale gizmo for pastes and moved selections

### Blueprints
- Save, load, and browse named blueprints with thumbnail previews

### Other
- Mask system for filtering block operations
- Undo / redo history
- Freecam for unrestricted camera movement
- Ruler utility
- Noise preview renderer
- ImGui-based floating panel UI
- Full client–server authority: all block operations go through server-side packets

---

## Requirements

| Mod | Version |
|-----|---------|
| Minecraft Forge | 1.7.10-10.13.x |
| [GTNHLib](https://github.com/GTNewHorizons/GTNHLib) | 0.11.35+ |
| [NotEnoughItems (GTNH fork)](https://github.com/GTNewHorizons/NotEnoughItems) | 2.8.118-GTNH+ |

GTNHLib and NEI are required at runtime.
If you are playing GT:NH, both are already included.

---

## Installation

1. Drop the Dimensium jar into your `mods/` folder.
2. Ensure GTNHLib and NEI are present.
3. Launch in creative mode. **Do not use on survival worlds without a backup.**

---

## Building from source

```bash
./gradlew classes      # compile check
./gradlew test         # run unit tests
./gradlew runClient    # launch the game
./gradlew jar          # build distributable jar
```

---

## License

MIT — see [LICENSE](LICENSE).
