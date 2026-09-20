---
layout: docs
title: Fill
---

The Fill tool flood-fills a contiguous air region with the active block palette. Click a block face to preview the fill; click again to confirm. The flood follows air connectivity from the clicked face outward, bounded by a configurable limit and direction.

## Controls

| Input | Action |
|---|---|
| RMB (first click) | Preview flood fill from cursor position |
| RMB (second click) | Confirm and place the previewed blocks |
| LMB | Cancel the current preview |

## Parameters

| Parameter | Description |
|---|---|
| Limit | Maximum number of air blocks to fill (default 100,000) |
| Direction | `Down` — flood propagates downward; `Up` — propagates upward |
| Include corners | When enabled, diagonally connected air blocks are included |

## Tips

- Use Direction `Down` to fill caves and enclosed spaces from the top.
- Keep the limit low when filling open areas to avoid unexpectedly large fills.
- The fill only enters air blocks; solid blocks act as walls.

## See also

- [Shape](/tools/creating/shape) — place a solid geometric volume instead of flood-filling
