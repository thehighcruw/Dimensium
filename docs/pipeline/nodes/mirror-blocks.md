---
layout: docs
title: Mirror Blocks
---

Reflects a block map across an axis plane through the pipeline origin, optionally merging the reflection with the original to produce a symmetric result.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| blocks | Input | Block map | Yes |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Axis | Plane of reflection: X (left/right), Y (up/down), or Z (forward/back) |
| Keep original | When enabled, the original blocks are kept alongside the reflection |
