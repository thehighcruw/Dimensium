---
layout: docs
title: Extrude
---

The Extrude tool expands or shrinks a selection by adding or removing blocks on its exposed faces. RMB on a block face applies the extrude at that face's normal direction. Corners can optionally be included for rounder expansion.

## Controls

| Input | Action |
|---|---|
| RMB | Extrude at the clicked block face |

## Parameters

| Parameter | Description |
|---|---|
| Mode | `Expand` — add blocks outward from selection faces; `Shrink` — remove blocks inward |
| Displace | When enabled, the original selection boundary moves with the extrusion |
| Count | Number of extrude steps applied per click |
| Limit | Maximum number of blocks that may be affected per operation |
| Corners | When enabled, corner and edge blocks are also included in the expansion |

## Tips

- Multiple clicks accumulate; use Count to apply several steps in one click.
- Corners enabled produces a more spherical expansion; corners disabled keeps sharp edges.
- Shrink is useful for carving inward ledges and recesses.
