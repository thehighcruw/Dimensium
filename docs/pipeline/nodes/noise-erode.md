---
layout: docs
title: Noise Erode
---

Removes blocks based on 3D simplex noise, producing irregular eroded surfaces.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| blocks | Input | Block map | Yes |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Strength | Fraction of blocks removed — 0.0 removes nothing, 1.0 removes nearly all blocks (0.0–1.0) |
| Noise scale | Spatial frequency of the noise — smaller values produce larger eroded patches (0.01–0.5) |
