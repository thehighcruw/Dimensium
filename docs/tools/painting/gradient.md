---
layout: docs
title: Gradient
---

The Gradient tool replaces blocks within the selection with a blend from the active palette driven by a linear or spherical gradient. LMB sets the start point; dragging sets the end point and immediately previews the gradient. Releasing confirms the paint. The palette's block distribution is interpolated across the gradient axis.

## Controls

| Input | Action |
|---|---|
| LMB click | Set gradient start position (pos 1) |
| LMB drag | Set end position (pos 2) as you drag; preview updates live |
| Release LMB | Confirm and apply the gradient |

## Parameters

| Parameter | Description |
|---|---|
| Shape | `Plane` — linear gradient from pos 1 to pos 2; `Sphere` — radial gradient centered at pos 1 |
| Interpolation | How palette blocks transition: `Nearest` (hard steps), `Linear` (smooth blend), `Bézier` (ease in/out) |
| Mask surface | When enabled, only surface blocks are painted |
| Clamp to edge | When disabled, gradient repeats beyond pos 2; when enabled, it stops at pos 2 |
| Seed | Random seed for the stochastic palette sampling |

## Tips

- Use Linear interpolation for smooth stone-to-grass transitions on hillsides.
- Bézier gives a slower start and end, useful for subtle colour grading.
- Spherical shape with a small radius creates a radial highlight or stain effect.

## See also

- [Noise](/tools/painting/noise) — noise-driven block replacement without a fixed axis
