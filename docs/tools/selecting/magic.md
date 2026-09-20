---
layout: docs
title: Magic Select
---

The Magic Select tool flood-fills a contiguous region of matching blocks from the clicked position. Comparison can match exact block state, block type, solidity, or any block. The result is combined with the existing selection using the active Boolean op.

## Controls

| Input | Action |
|---|---|
| RMB | Flood-fill select from the clicked block |

## Parameters

| Parameter | Description |
|---|---|
| Limit | Maximum number of blocks to select in a single flood fill |
| Range | Flood-fill connectivity radius — how far diagonally adjacent blocks are considered connected |
| Compare type | What counts as a matching block: `Block State` (exact), `Block` (type only), `Solid` (any solid), `Any` (everything) |
| Direction | Which vertical directions the flood travels: `Both`, `Up only`, `Down only` |
| Surface only | When enabled, only surface blocks are included in the selection |
| Corners | When enabled, diagonally connected blocks are included in the flood |
| Boolean op | How the result combines with the existing selection: `Add`, `Subtract`, `Replace`, `Intersect` |

## Tips

- `Block` compare selects all connected blocks of the same type regardless of orientation/metadata.
- `Any` with Direction `Down` quickly selects the floor of a space.
- Lower the limit when selecting near open terrain to avoid runaway fills.

## See also

- [Box Select](/tools/selecting/box) — axis-aligned region selection
