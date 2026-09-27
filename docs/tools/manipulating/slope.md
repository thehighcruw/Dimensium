---
layout: docs
title: Slope
---

The Slope tool reshapes terrain by projecting blocks onto a sloped surface defined by two anchor points. RMB-click to plant the first anchor, then drag to extend the slope — blocks within the brush radius are raised or lowered to match the surface as you paint.

## Controls

| Input | Action |
|---|---|
| RMB click | Plant anchor (pos1) |
| RMB drag | Define slope direction and paint continuously |
| Clear pos1 button | Remove the anchor and reset the slope |

## Workflow

1. RMB-click a block to set the slope origin (pos1).
2. RMB-drag from a second point to lock the slope angle and begin painting. The slope runs from pos1 through pos2.
3. Release RMB to commit. Hold and drag again to keep painting along the same slope.

## Parameters

| Parameter | Description |
|---|---|
| Shape | `Plane` — flat sloped surface; `Cone` — radially symmetric cone surface centred on pos1 |
| Apply mode | `Raise and lower` — match the surface exactly; `Raise` — only add blocks above the surface; `Lower` — only remove blocks above it |
| Radius | Brush radius in blocks (1–64) |
| Smoothing | Blends the transition at the brush edge (0 = hard cutoff, 1 = fully feathered) |
| Clamp | When enabled, slope effect is capped at pos1 and pos2 — blocks outside that range are not affected |

## Tips

- Use `Cone` shape to build radially symmetric hills or craters: place pos1 at the peak or centre, drag outward to set the slope angle.
- `Raise` mode is useful for adding terrain under a flat build without cutting into anything above the slope.
- Disable `Clamp` to extend the slope indefinitely beyond the two anchor points — useful for long gradual inclines.
- Combine with a mask to restrict the slope to specific block types (e.g. raise only grass, leave stone untouched).

## See also

- [Elevation](/tools/manipulating/elevation) — raises or lowers terrain uniformly without a slope axis
