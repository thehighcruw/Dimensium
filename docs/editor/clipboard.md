---
layout: docs
title: Clipboard
---

The clipboard holds a block snapshot that you can paste back into the world. Copy, cut, and paste work on the active selection. The Clipboard window shows a live 3D preview of the current contents and provides quick access to blueprint save/load.

## Workflow

| Action | Shortcut |
|---|---|
| Copy selection | **Ctrl+C** |
| Cut selection (copy + erase) | **Ctrl+X** |
| Paste | **Ctrl+V** |

Paste places the clipboard at the block you are pointing at (or at your feet if you are not aiming at a block).

## Clipboard Window

The window shows:
- The bounding box dimensions of the copied region.
- A 3D preview rendered from the clipboard contents.
- **Save Blueprint** — opens the [Create Blueprint](/editor/blueprints) dialog to persist the clipboard as a reusable file.
- **Browse Blueprints** — opens the blueprint browser to load a previously saved structure.

## Placement State

After pasting, an interactive placement mode activates. Gizmos appear on the pasted structure:

| Gizmo | Action |
|---|---|
| Axis arrows | Translate along X, Y, or Z |
| Plane disc | Translate on the camera-facing plane |
| Rotation rings | Rotate around X, Y, or Z |
| Scale handles | Scale along each axis |

Confirm placement with **Enter** to write the blocks to the world, or press **Escape** to cancel.

## See also

- [Blueprints](/editor/blueprints) — save and load clipboard contents as named files
- [History](/editor/history) — undo a paste with Ctrl+Z
