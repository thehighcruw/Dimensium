---
layout: docs
title: Settings
---

The Settings modal is opened from the menu bar or by pressing your settings keybind (default configurable). It is divided into three categories: **General**, **Navigation**, and **Keybinds**.

## General

| Setting | Description |
|---|---|
| **UI Scale** | Scale factor for all ImGui windows (0.5–3.0). Applied on **Apply**. |
| **UI Scroll Speed** | Mouse wheel scroll speed in ImGui windows (0.1–10). |
| **Rotation Snap** | Snap increment for rotation gizmos in degrees (0 = free, up to 45°). |
| **Shape Threshold** | Voxel fill threshold for the Shape tool (0.0–1.0). Higher = denser fills. |
| **Tool Grid Columns** | Number of columns in the tool picker grid (3–20). |

## Navigation

| Setting | Description |
|---|---|
| **World Scroll Speed** | Mouse wheel scroll speed when zooming the freecam (0.1–10). |
| **Movement Speed** | Walk mode movement speed multiplier (0.1–20×). |
| **Orbit: use cursor position as pivot** | When enabled, orbiting pivots on the block under the software cursor. When disabled, pivots on the crosshair center. |

## Keybinds

The Keybinds tab lists all rebindable actions split into two groups:

**Tool keybinds** — one keybind per tool to switch to that tool directly.

**Action keybinds** — editor-wide shortcuts:

| Action | Default |
|---|---|
| Undo | Ctrl+Z |
| Redo | Ctrl+Y |
| Copy | Ctrl+C |
| Cut | Ctrl+X |
| Paste | Ctrl+V |
| Fill (quick fill with active block) | configurable |
| Erase selection | Delete |
| Confirm | Enter |
| Save Blueprint | configurable |
| Blueprint Browser | configurable |
| Settings | configurable |
| Gizmo nudge forward/back/left/right/up/down | configurable |

To rebind: click the button next to the action, then press the desired key (with optional Ctrl/Shift/Alt modifiers). Press **Escape** to cancel rebinding.

## Applying Changes

Changes in the General and Navigation tabs take effect only after clicking **Apply**. Keybind changes apply immediately when a key is captured.

## See also

- [Navigation](/editor/navigation) — camera controls overview
