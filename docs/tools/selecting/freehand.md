---
layout: docs
title: Freehand Select
---

The Freehand Select tool adds blocks to the selection by dragging a brush across surfaces. The brush shape, radius, and height are shared with other brush tools. Blocks under the brush are added to the selection on each drag tick.

## Controls

| Input | Action |
|---|---|
| RMB click | Select blocks under the brush at the cursor |
| RMB drag | Continuously select blocks as the cursor moves |

## Parameters

| Parameter | Description |
|---|---|
| Brush shape | Shape of the selection brush (Sphere, Cube, Cylinder, Ellipsoid, etc.) |
| Brush radius | Radius of the brush in blocks |
| Brush height | Height for shapes with a separate height dimension |
| Replace mode | Which blocks are eligible for selection: `Any`, `Air only`, `Solid only`, `Same block` |
| Include air | When enabled, air blocks within the brush are also selected |

## Tips

- Use a large sphere brush for fast volumetric selection.
- Enable Include air to select hollow interiors along with solid walls.
- Combine with Boolean ops in other selection tools to add to or subtract from an existing selection.

## See also

- [Lasso Select](/tools/selecting/lasso) — screen-space freeform selection without a 3D brush
