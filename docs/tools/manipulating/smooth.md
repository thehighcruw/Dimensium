---
layout: docs
title: Smooth
---

The Smooth tool smooths jagged surfaces by redistributing blocks based on neighbourhood density. A modifier controls whether the net volume is preserved, lost (melted), or grown during smoothing. Dragging across the selection accumulates positions; all positions are processed on mouse release.

## Controls

| Input | Action |
|---|---|
| RMB drag | Accumulate positions to smooth; released to apply |

## Parameters

| Parameter | Description |
|---|---|
| Smooth strength | Number of smoothing passes applied per release |
| Block ratio | Percentage of blocks retained per pass (0–100); lower values produce heavier smoothing |
| Modifier | `Stable` — preserve volume; `Melt` — shrink and round; `Grow` — expand while smoothing |
| Fix edges | When enabled, boundary blocks at the selection edge are preserved |

## Tips

- Multiple smooth strengths stack — drag over an area several times for progressive smoothing.
- `Melt` modifier is useful for rounding off blocky corners without the full erosion of the Melt tool.
- `Fix edges` prevents the selection boundary from being eroded back.

## See also

- [Melt](/tools/manipulating/melt) — dedicated erosion tool without a drag interface

## How it works

For each brush position accumulated during the drag, the neighbourhood density of each block is computed. Blocks below the density threshold are candidates for removal; air blocks above it are candidates for filling. The modifier biases whether additions or removals dominate, controlling the net volume change.
