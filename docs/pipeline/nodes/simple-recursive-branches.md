---
layout: docs
title: Simple Recursive Branches
---

Attaches recursive symmetric branches to every leaf node of the input skeleton, suitable for dead trees and bare shrubs.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| skeleton | Input | Skeleton | Yes |
| skeleton | Output | Skeleton | — |

## Parameters

| Parameter | Description |
|---|---|
| Levels | Number of recursive branching iterations (1–5) |
| Branch count | Number of sub-branches spawned at each node (1–6) |
| Branch angle | Spread angle from the parent direction in degrees (5.0–70.0) |
| Initial length | Length of the first level of branches in blocks (1–20) |
| Length decay | Multiplier applied to branch length at each level (0.2–0.9) |
| Radius decay | Multiplier applied to branch radius at each level (0.1–0.9) |
