---
layout: docs
title: Rock
---

The Rock tool generates natural-looking rock formations by combining noise-based displacement with Gaussian smoothing and meld blending. The result is placed at the cursor with a live preview before confirmation.

## Controls

| Input | Action |
|---|---|
| RMB | Place a rock formation at the cursor |

## Parameters

| Parameter | Description |
|---|---|
| Noise radius | Base radius of the generated rock in blocks |
| Noisiness | How jagged and irregular the surface is (0 = smooth sphere, 1 = highly irregular) |
| Noise seed | Random seed; changing it produces a different rock shape |
| Smoothing std dev | Gaussian smoothing applied after noise displacement — higher values produce rounder rocks |
| Meld strength | How strongly the rock blends into surrounding terrain at its base |

## How it works

A spherical base volume is generated at the cursor position. Simplex noise displaces the surface outward or inward at each point, scaled by `noisiness`. A Gaussian blur with the given standard deviation is then applied to smooth the displaced surface. Finally, `meldStrength` controls a weighted blend at the rock's base to avoid hard flat edges where it meets the ground.

## Tips

- Low noisiness with high smoothing produces boulders; high noisiness with low smoothing produces jagged crags.
- Reseed to quickly iterate through different shapes at the same position.
- Use meld strength 0 for floating or cliff-face rocks.
