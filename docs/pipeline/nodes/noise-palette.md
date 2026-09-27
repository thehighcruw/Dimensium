---
layout: docs
title: Noise Palette
---

Repaints every block by sampling 3D simplex noise at its position and mapping the noise value to a palette.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| blocks | Input | Block map | Yes |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Palette | Block palette indexed by noise value — first entry maps to noise 0, last to noise 1 |
| Noise scale | Spatial frequency of the noise — smaller values produce larger blotches (0.01–0.5) |
