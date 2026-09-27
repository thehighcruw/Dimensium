---
layout: docs
title: Selection Operations
---

Selection operations modify the active selection or the blocks within it. They are accessible from the **Selection** and **Operations** windows.

## Selection Shape Operations

These operations change which blocks are selected without touching the world.

| Operation | Description |
|---|---|
| **Clear** | Deselect all blocks |
| **Expand** | Add one layer of blocks outward on all faces |
| **Shrink** | Remove one layer of blocks inward on all faces |
| **Bounding Box** | Replace the selection with the axis-aligned bounding box of the current selection |
| **Convex Hull** | Replace the selection with the convex hull of the current selection |
| **Filter** | Remove blocks from the selection by block type |
| **Distort** | Displace the selection boundary using 3D noise |
| **Smooth** | Smooth the selection boundary |

### Filter

Scans the selected blocks and presents all unique block types as an icon grid. Check or uncheck types, then apply to keep or remove those types from the selection.

| Option | Description |
|---|---|
| **Keep matching** | Keep the checked types; remove the rest |
| **Exact meta** | Match block metadata exactly (uncheck to match any meta variant) |

### Distort

Displaces the selection boundary with Perlin noise, producing organic irregular edges.

| Parameter | Description |
|---|---|
| **Scale** | Noise frequency — higher values produce finer, more chaotic distortion (1–100) |
| **Distance X/Y/Z** | Maximum displacement per axis in blocks (0–20) |
| **Randomize Seed** | Generate a new random seed for a different noise pattern |

### Smooth

Rounds the selection boundary by expanding and contracting based on neighbour density.

| Parameter | Description |
|---|---|
| **Strength** | Number of smoothing passes (1–8) |
| **Threshold** | Minimum neighbour fill fraction for a block to be included (0.01–1.0) — lower = more inclusive |

## Block Operations

These operations write to the world. All require an active selection.

| Operation | Where |
|---|---|
| **Delete** (erase to air) | Operations window |
| **Fill** (fill with active block) | Operations window |
| **Fill…** (modal, choose block + mode) | Operations window |
| **Fill Nearest** | Operations window |
| **Replace** | Operations window |
| **Type Replace** | Operations window |
| **Hollow** | Operations window |
| **Fill Gaps** | Operations window |
| **Drain** | Operations window |
| **Simulate Gravity** | Operations window |
| **Trigger Updates** | Operations window |

### Fill (modal)

Opens a dialog to pick a block and a fill mode.

| Mode | Description |
|---|---|
| **Fill** | Fill every selected block |
| **Outline** | Fill only blocks that have at least one face-adjacent block outside the selection |
| **Walls** | Fill only the vertical perimeter (north/south/east/west outline, excluding top/bottom) |
| **Top** | Fill only the topmost layer of the selection |
| **Bottom** | Fill only the bottommost layer of the selection |

### Replace

Replace one specific block with another throughout the selection.

| Option | Description |
|---|---|
| **Find** | Block to search for |
| **Replace with** | Block to place instead |
| **Exact meta** | When checked, only matches the exact metadata variant |

Supports multiple find→replace mappings in a single operation.

### Type Replace

Replace all instances of a block type with another block, optionally preserving existing metadata.

| Option | Description |
|---|---|
| **Source** | Block type to replace |
| **Target** | Block type to place |
| **Preserve meta** | When checked, keeps the original block's metadata instead of using the target's metadata |

Supports multiple source→target mappings.

### Hollow

Removes interior blocks, leaving only the outer shell (surface blocks with at least one air or liquid neighbour).

### Fill Gaps

Fills air blocks that are fully surrounded by solid blocks on all 6 faces.

### Drain

Replaces all liquid blocks in the selection with air.

### Simulate Gravity

Drops all non-air blocks in the selection downward until they rest on a solid block, simulating gravity.

### Trigger Updates

Forces a block update event on every block in the selection. Useful for activating redstone, flowing liquids, or updating connected blocks after a bulk edit.

## See also

- [Box Select](/tools/selecting/box) — create a rectangular selection
- [Masks](/tools/masks) — restrict which blocks operations affect
