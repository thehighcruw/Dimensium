---
layout: docs
title: Box Select
---

The Box Select tool defines a rectangular axis-aligned selection by clicking two corner positions. After both corners are placed, axis gizmos appear on each corner and a center gizmo allows moving the whole box. Confirming commits the blocks inside the box to the active selection.

## Controls

| Input | Action |
|---|---|
| RMB | Place the first corner (pos 1) |
| LMB (on pos 1 gizmo axis) | Drag pos 1 along that axis |
| LMB (on pos 2 gizmo axis) | Drag pos 2 along that axis |
| LMB (on center gizmo axis) | Translate the entire box along that axis |
| LMB (on center view-plane gizmo) | Translate the entire box on the camera-facing plane |
| LMB (elsewhere, after box is shown) | Commit the box to the selection |

## Parameters

| Parameter | Description |
|---|---|
| Boolean op | How the new box interacts with the existing selection: `Add`, `Subtract`, `Replace`, `Intersect` |

## Tips

- After placing pos 1, the box previews as you move the cursor before pos 2 is set.
- Use the center gizmo to reposition the box without resizing it.
- Boolean op `Subtract` lets you carve out rectangular holes from an existing selection.

## See also

- [Magic Select](/tools/selecting/magic) — selects by block type rather than position
- [Lasso Select](/tools/selecting/lasso) — freeform screen-space selection
