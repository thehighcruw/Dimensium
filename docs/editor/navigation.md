---
layout: docs
title: Navigation
---

The editor uses a freecam viewport that detaches from the player body.

## Opening the Editor

Press **RShift** (or your configured toggle key) while holding the Builder Tool in creative mode. The editor overlay opens and the camera detaches from the player. Press the same key again to close.

## Camera Controls

The cursor is a software pointer — the mouse does not rotate the camera unless a drag or modifier is used.

The **camera modifier key** is platform-specific: **Ctrl** on Windows and Linux, **Option** on macOS.

| Input | Action |
|---|---|
| LMB drag | Rotate camera (yaw / pitch) |
| CameraMod + LMB drag | Orbit around the block under the cursor |
| CameraMod + RMB drag | Pan camera |
| Scroll wheel | Zoom (move along look direction) |
| Shift + Scroll | Change brush size |

## Freecam Movement

WASD movement is always available while the editor is open.

| Input | Action |
|---|---|
| W / A / S / D | Move forward / left / backward / right |
| Space | Move up |
| Shift | Move down |
| Ctrl (hold) | Sprint (5× speed) |
| Shift (hold) | Sneak (0.2× speed) |
| Scroll wheel | Increase / decrease movement speed |

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
| Ctrl+F | Fill selection with active block |
| Ctrl+P | Save blueprint |
| Ctrl+B | Open blueprint browser |
| Ctrl+. | Open settings |
| Arrow keys / PgUp / PgDn | Nudge active gizmo 1 block |

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

- Orbit (CameraMod + LMB) always pivots around the first block hit by a ray from the camera — useful for inspecting a specific structure.
- In Editor mode, brush size can be changed without switching tools: hold Shift and scroll.
- Freecam movement speed persists between sessions; scroll to adjust it for precise navigation.
