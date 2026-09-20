---
layout: docs
title: Path
---

The Path tool builds a corridor of blocks along a multi-point spline. Each control point carries its own radius and optional block type. The path between points is computed using the selected curve algorithm, and a live preview updates as points are moved.

## Controls

| Input | Action |
|---|---|
| RMB | Place a new path point at the cursor position |
| LMB | Select the nearest existing path point |
| LMB drag (on gizmo) | Move the selected point along a gizmo axis |

## Parameters

| Parameter | Description |
|---|---|
| Curve type | How points are connected: `Bresenham`, `DDA`, `Catenary`, `Catmull-Rom`, `Bézier` |
| Looped | When enabled, the last point connects back to the first |
| Catenary slack | Slack factor for the Catenary curve type (how much the path sags) |
| Interpolation | Radius/block interpolation between points: `Nearest`, `Linear`, `Bézier` |

## Tips

- Catmull-Rom produces the smoothest natural-looking paths.
- Catenary is useful for chains, ropes, or hanging bridge shapes.
- Set different block types per point to create block transitions along the path.
- Each point has its own radius, so paths can taper or flare.

## See also

- [Modelling](/tools/creating/modelling) — for surface construction from control points
