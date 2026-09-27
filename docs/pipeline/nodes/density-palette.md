---
layout: docs
title: Density Palette
---

Repaints blocks based on how many neighbors they have, mapping local density to a palette — dense interior blocks get different colors than sparse surface blocks.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| blocks | Input | Block map | Yes |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Palette | Block palette indexed by neighbor density — first entry for sparse, last for dense |
| Sample radius | Cubic neighborhood half-size used to count filled neighbors (1–4) |
| Only leaves | When enabled, only leaf blocks (id 18, 161) are recolored |
