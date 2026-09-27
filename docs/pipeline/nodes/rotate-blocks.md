---
layout: docs
title: Rotate Blocks
---

Rotates a block map in 90-degree increments around the pipeline origin.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| blocks | Input | Block map | Yes |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Axis | Rotation axis: Y (yaw), X (pitch), or Z (roll) |
| Turns | Amount to rotate: 90°, 180°, or 270° — rotation is clockwise when viewed from the positive end of the axis |
