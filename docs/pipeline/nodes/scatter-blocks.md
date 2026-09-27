---
layout: docs
title: Scatter Blocks
---

Places multiple copies of the input block map at random offsets within a volume around an origin point.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| blocks | Input | Block map | Yes |
| origin | Input | Vec3 | No |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Count | Number of copies to place (1–64) |
| Spread X | Maximum random offset along X from the origin in blocks (0–128) |
| Spread Y | Maximum random offset along Y from the origin in blocks (0–128) |
| Spread Z | Maximum random offset along Z from the origin in blocks (0–128) |
