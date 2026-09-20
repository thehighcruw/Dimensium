---
layout: docs
title: Noise
---

The Noise tool replaces blocks within the selection according to a noise field. The noise value at each block's position is mapped to a block from the active palette. Multiple noise types and octave stacking allow everything from smooth Perlin variation to sharp Voronoi patterns.

## Controls

| Input | Action |
|---|---|
| Apply button | Apply noise painting to the current selection |

## Parameters

| Parameter | Description |
|---|---|
| Noise type | `Simplex`, `Perlin`, `Voronoi Edges`, `Worley`, `Metaball`, `White`, `Splatter` |
| Scale | Noise frequency — larger values produce coarser patterns |
| Octaves | Number of noise layers stacked for fractal detail |
| Lacunarity | Frequency multiplier per octave (default 2.0) |
| Gain | Amplitude multiplier per octave (default 0.5) |
| Seed | Random seed for the noise field |
| Jitter | Cell jitter for Voronoi/Worley types (0.0 = grid, 1.0 = fully random) |
| Metaball range | Influence radius for Metaball noise type |
| Surface only | When enabled, only surface blocks are painted |
| 3D noise | When enabled, noise is sampled in 3D (varying by Y) rather than 2D (uniform column) |

## How it works

Each block position in the selection is fed into the selected noise function. The resulting value (0–1) is used to index into the active palette, selecting which block type to place. With multiple octaves, higher-frequency layers add progressively finer detail at reduced amplitude.

## Tips

- Simplex/Perlin with 2–3 octaves produces natural-looking stone or dirt variation.
- Voronoi Edges creates cracked, mosaic-like patterns.
- White noise gives a fully random per-block distribution.
- Enable 3D for geological layering that varies with depth.

## See also

- [Gradient](/tools/painting/gradient) — directional gradient painting along an axis
