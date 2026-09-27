---
layout: docs
title: Curve Scatter
---

Places copies of a block map centred at every Nth point along a curve — useful for colonnades, fence posts, and other evenly-spaced repeated elements.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| curve | Input | Curve | Yes |
| blocks | Input | Block map | Yes |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Stride | Sample every Nth curve point; 1 places a copy at every point, 2 at every other point, etc. (1–64) |
| Align to curve | When enabled, each copy is yaw-rotated to face the curve tangent direction at its placement point |
