---
layout: docs
title: Weld
---

The Weld tool fills gaps and bridges disconnected regions within a selection. It adds blocks to air positions that have sufficient solid neighbours, effectively closing holes and connecting nearby structures. Optionally, existing solid blocks can be replaced.

## Controls

| Input | Action |
|---|---|
| Apply button | Apply weld to the current selection |

## Parameters

| Parameter | Description |
|---|---|
| Smooth strength | Number of passes applied; more passes fill larger gaps |
| Threshold | Minimum neighbour density (0.0–1.0) for an air block to be filled |
| Replace solid | When enabled, solid blocks may also be replaced during the weld pass |

## Tips

- High threshold only fills air blocks nearly surrounded by solid; low threshold fills more open gaps.
- Multiple passes weld progressively larger gaps — iterate until satisfied.
- Combine with Melt: Weld first to close gaps, then Melt to smooth the result.

## See also

- [Melt](/tools/manipulating/melt) — removes blocks rather than filling them
