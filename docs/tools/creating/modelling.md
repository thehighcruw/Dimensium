---
layout: docs
title: Modelling
---

The Modelling tool constructs block geometry by placing control points in the world and interpolating a solid surface through them. Multiple surface algorithms are available, from simple convex hulls to smooth Catmull-Rom and Bézier patches. For row-based modes, multiple rows of points define a surface grid.

## Controls

| Input | Action |
|---|---|
| RMB | Place a control point at the cursor position |
| Shift + RMB | Start a new row (row-based modes: Flat, Catmull-Rom, Bézier) |
| LMB | Select the nearest existing control point |
| LMB drag (on gizmo) | Move the selected point along a gizmo axis |

## Parameters

| Parameter | Description |
|---|---|
| Mode | Surface algorithm: `Convex Hull`, `Triangle Strip`, `Triangle Fan`, `Flat`, `Catmull-Rom`, `Bézier`, `Smart Surface` |
| Paste mode | `Paste Copy` — overwrite existing blocks; `Keep Existing` — only fill air |
| Offset target point | When enabled, points land on the face outward from the clicked block rather than on the block itself |

## Tips

- Use Convex Hull for simple filled volumes like boulders or hills.
- Catmull-Rom and Bézier give the smoothest results; add more rows for tighter control.
- A live preview shows the result before placement.

## How it works

Each mode computes block positions differently:
- **Convex Hull** — fills the convex hull of all placed points.
- **Triangle Strip / Fan** — tessellates surfaces from ordered point sequences.
- **Catmull-Rom / Bézier** — fits smooth curves through rows of points to generate a smooth surface.
- **Smart Surface** — automatically selects an appropriate interpolation based on point layout.
