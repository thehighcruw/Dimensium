---
layout: docs
title: Distort
---

The Distort tool displaces blocks within the current selection using 3D noise-based offsets. Each block is mapped to a new position determined by the noise field, producing organic warping and deformation effects. Smooth edges optionally blends the distorted boundary.

## Controls

| Input | Action |
|---|---|
| Apply button | Apply distortion to the current selection |

## Parameters

| Parameter | Description |
|---|---|
| Scale | Noise frequency — larger values produce broader, more gradual distortion |
| Seed | Random seed; changing it produces a different distortion pattern |
| Distance X | Maximum displacement in the X axis in blocks |
| Distance Y | Maximum displacement in the Y axis in blocks |
| Distance Z | Maximum displacement in the Z axis in blocks |
| Separate axes | When enabled, X/Y/Z distances can be set independently; otherwise a single distance applies to all axes |
| Smooth edges | When enabled, a blending pass softens the boundary of the distorted region |

## Tips

- Set Y distance to 0 to distort only horizontally, useful for warping walls without changing height.
- Use a low scale with large distance for chaotic displacement; high scale with small distance for gentle surface noise.
- Apply multiple times with different seeds for cumulative organic irregularity.
