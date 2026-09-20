---
layout: docs
title: Melt
---

The Melt tool erodes the edges of a selection by removing exposed blocks that fall below a neighbourhood density threshold. Each application smooths and rounds the selection boundary, simulating melting or weathering.

## Controls

| Input | Action |
|---|---|
| Apply button | Apply one melt pass to the current selection |

## Parameters

| Parameter | Description |
|---|---|
| Smooth strength | Number of smoothing passes applied per operation (higher = more erosion per click) |
| Threshold | Minimum neighbour density (0.0–1.0) required to retain a block; blocks below this are removed |

## Tips

- Low threshold removes only the most isolated exposed blocks; high threshold aggressively erodes edges.
- Apply multiple times to iteratively round a blocky selection.
- Combine with Weld to fill gaps, then Melt to smooth the result.

## See also

- [Smooth](/tools/manipulating/smooth) — surface smoothing without net erosion
- [Weld](/tools/manipulating/weld) — fills gaps rather than removing blocks
