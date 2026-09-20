---
layout: docs
title: Navigation
---

The editor uses a freecam viewport that detaches from the player body. Two camera modes are available: **Editor mode** (default) and **Walk mode**.

## Opening the Editor

Press **RShift** (or your configured toggle key) while holding the Builder Tool in creative mode. The editor overlay opens and the camera detaches from the player. Press the same key again to close.

## Editor Mode

Default mode. The cursor is a software pointer — the mouse does not rotate the camera unless a modifier is held.

| Input | Action |
|---|---|
| Scroll wheel | Zoom (move along look direction) |
| Shift + Scroll | Change brush size |
| Alt + LMB drag | Rotate camera |
| Alt + RMB drag | Pan camera |
| Alt + MMB drag | Orbit around the block under the cursor |
| Ctrl + LMB drag | Orbit around the block under the cursor |

## Walk Mode

Press **C** to toggle walk mode. The mouse always rotates the camera, like standard first-person Minecraft.

| Input | Action |
|---|---|
| W / A / S / D | Move forward / left / backward / right |
| Space | Move up |
| Shift | Move down |
| Ctrl (hold) | Sprint (5× speed) |
| Shift (hold) | Sneak (0.2× speed) |
| Scroll wheel | Increase / decrease movement speed |

Press **C** again to return to Editor mode.

## Editor Shortcuts

| Shortcut | Action |
|---|---|
| Ctrl+Z | Undo |
| Ctrl+Y | Redo |
| Ctrl+C | Copy selection |
| Ctrl+X | Cut selection |
| Ctrl+V | Paste clipboard |
| Delete / Backspace | Erase selection |
| Enter / Numpad Enter | Confirm placement (shape, clipboard, path, modelling) |
| Escape | Cancel active drag or placement; deselect tool |
| RShift | Toggle editor on / off |

## Tool Shortcuts

| Shortcut | Default Tool |
|---|---|
| Configurable | Select |
| Configurable | Freehand Draw |
| Configurable | Noise |
| Configurable | Smooth |
| Configurable | Extrude |

Tool shortcuts are remappable in **Settings** (opened via the settings keybind or the menu bar).

## Tips

- Orbit (Alt+MMB or Ctrl+LMB) always pivots around the first block hit by a ray from the camera — useful for inspecting a specific structure.
- In Editor mode, brush size can be changed without switching tools: hold Shift and scroll.
- Walk mode speed persists between sessions; scroll to adjust it before entering walk mode for precise navigation.
