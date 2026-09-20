---
layout: docs
title: Elevation
---

The Elevation tool raises, lowers, or flattens terrain within a circular brush by holding RMB. A configurable falloff curve controls how the effect tapers toward the brush edge. The tool operates continuously at a rate controlled by the Rate parameter, or fires once per new cursor position in Once mode.

## Controls

| Input | Action |
|---|---|
| RMB drag | Apply elevation change continuously at the cursor |

## Parameters

| Parameter | Description |
|---|---|
| Mode | `Raise` — push terrain up; `Lower` — push terrain down; `Flatten` — level terrain to the average height |
| Apply | `Once` — applies once per new block position; `Continuous` — applies repeatedly at a rate set by Rate |
| Falloff | How effect strength decreases from brush center: `Flat`, `Spherical`, `Linear`, `Logarithmic`, `Normal`, `Peak` |
| Flatten direction | When in Flatten mode: `Both`, `Up only`, or `Down only` |
| Radius | Brush radius in blocks (1–32) |
| Smoothing | 0.0–1.0 blend of the computed result with the original, reducing sharp edges |
| Rate | Applications per second in Continuous mode (0.1–32) |
| Strength | How many blocks are moved per application (1–32) |

## Tips

- `Peak` falloff creates pointed hills; `Normal` produces a Gaussian bell curve shape.
- Use Flatten + `Up only` to raise sunken areas to match surrounding terrain.
- Lower Rate in Continuous mode for precise incremental adjustments.

## See also

- [Sculpt](/tools/creating/sculpt) — freehand Gaussian raise/lower without a selection
