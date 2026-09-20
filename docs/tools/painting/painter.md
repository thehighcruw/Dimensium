---
layout: docs
title: Painter
---

The Painter tool replaces blocks under the brush with blocks from the active palette by dragging across surfaces. It is the simplest painting tool — just drag to paint. Brush shape and radius are shared with other brush-based tools.

## Controls

| Input | Action |
|---|---|
| RMB drag | Replace blocks under the brush with the active palette |

## Parameters

| Parameter | Description |
|---|---|
| Brush shape | Shape of the paint brush (shared with other brush tools) |
| Brush radius | Radius in blocks |
| Replace mode | Which blocks may be replaced: `Any`, `Air only`, `Solid only`, `Same block` |
| Mask surface | When enabled, only surface blocks within the brush are painted |

## Tips

- Use `Same block` replace mode to selectively repaint a single block type without affecting others.
- Combine with a mask to protect specific regions while painting freely.
- For large flat surfaces, use a Cube or Cylinder brush for predictable coverage.

## See also

- [Noise](/tools/painting/noise) — noise-driven painting for natural variation
- [Gradient](/tools/painting/gradient) — gradient-driven painting along an axis
