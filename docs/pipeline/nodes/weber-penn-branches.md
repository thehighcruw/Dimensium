---
layout: docs
title: Weber–Penn Branches
---

Attaches Weber–Penn style multi-level branches to every leaf node of the input skeleton, producing natural-looking deciduous crowns.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| skeleton | Input | Skeleton | Yes |
| skeleton | Output | Skeleton | — |

## Parameters

| Parameter | Description |
|---|---|
| Levels | Number of recursive sub-branching levels (1–4) |
| Branch count | Number of branches spread evenly around each node (1–8) |
| Branch angle spread | Maximum spread angle from the parent direction in degrees (5.0–80.0) |
| Initial length | Length of the primary branches in blocks (1–20) |
| Length ratio | Multiplier applied to branch length at each level (0.2–0.9) |
| Radius factor | Multiplier applied to branch radius at each level (0.2–0.8) |
