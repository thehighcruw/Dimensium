---
layout: docs
title: Sculpt
---

The Sculpt tool raises or lowers terrain under the cursor using a Gaussian-weighted brush. Holding RMB raises terrain; inverting the tool lowers it. Optional Y-axis masking and a denoise pass help produce clean terrain shapes.

## Controls

| Input | Action |
|---|---|
| RMB drag | Raise terrain at the brush position |
| RMB drag (inverted) | Lower terrain when Invert is enabled |

Brush shape, radius, and height are shared with the global Brush state.

## Parameters

| Parameter | Description |
|---|---|
| Strength | Intensity of the raise/lower effect per stroke (0.0–1.0+) |
| Invert | When enabled, the brush lowers terrain instead of raising it |
| Mask Y | When enabled, displacement is restricted to a single Y level |
| Denoise | Applies a smoothing pass to reduce single-block spikes in the result |

## How it works

The Gaussian kernel assigns each block in the brush a weight that decreases with distance from the center. Blocks with weights above a threshold are raised (or lowered) by one block, accumulating fractional displacement over multiple passes until the threshold is crossed.

## Tips

- Use low strength with multiple strokes for gentle slope shaping.
- Enable Denoise when working at high strength to avoid isolated floating blocks.
- Mask Y to sculpt horizontally without creating unintended overhangs.

## See also

- [Elevation](/tools/manipulating/elevation) — brush-based raise/lower on a selection with falloff curves
