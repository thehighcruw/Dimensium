---
layout: docs
title: Shatter
---

The Shatter tool breaks a selection into fragments by carving it along Voronoi cell boundaries. The crack width and the axes along which cuts are made are configurable. An optional fill mode replaces cracks with the active block rather than leaving them as air.

## Controls

| Input | Action |
|---|---|
| Apply button | Apply shatter to the current selection |

## Parameters

| Parameter | Description |
|---|---|
| Crack width | Thickness of the gap between fragments (wider = more visible cracks) |
| Axis mode | Which axes are cut: `XYZ` (all), `X`, `Y`, or `Z` only |
| Fill mode | When enabled, crack voxels are filled with the active block instead of air |
| Noise type | Voronoi variant used to generate fragment boundaries (shared from NoiseParams) |
| Noise scale | Scale of the Voronoi cells — larger = bigger fragments |
| Noise seed | Random seed for the fragment layout |

## Tips

- Axis mode `Y` creates horizontal slabs; `X` or `Z` creates vertical slabs.
- Increase noise scale for fewer, larger fragments.
- Use fill mode to create cracked stone patterns without removing blocks.

## How it works

The selection volume is sampled against a Voronoi noise field. Blocks that fall on Voronoi cell edges (within `crackWidth`) are either removed or filled depending on fill mode. The remaining blocks form the disconnected fragments.
