---
layout: docs
title: Lasso Select
---

The Lasso Select tool selects blocks by drawing a freehand polygon on screen. Hold RMB and move the cursor to trace the lasso outline; release to project the polygon into the world and select all blocks within it up to the configured depth.

## Controls

| Input | Action |
|---|---|
| RMB drag | Draw the lasso polygon on screen |
| Release RMB | Project the polygon and apply the selection |

## Parameters

| Parameter | Description |
|---|---|
| Depth | How many blocks deep the lasso projects from the screen into the world |
| Include non-solid | When enabled, air and non-solid blocks inside the polygon are also selected |

## Tips

- Draw slowly for a more precise outline; the polygon is sampled from your cursor path.
- Increase depth to select through thick structures.
- Use Include non-solid to capture air spaces inside a lassoed region.

## See also

- [Box Select](/tools/selecting/box) — axis-aligned rectangular selection with gizmo controls
- [Freehand Select](/tools/selecting/freehand) — 3D brush-based selection
