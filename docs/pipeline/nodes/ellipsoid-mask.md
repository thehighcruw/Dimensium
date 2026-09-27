---
layout: docs
title: Ellipsoid Mask
---

Removes all blocks outside an axis-aligned ellipsoid centred above the pipeline origin — useful for trimming leaf canopies to a clean shape.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| blocks | Input | Block map | Yes |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Radius X | Ellipsoid half-extent along X (1.0–30.0) |
| Radius Y | Ellipsoid half-extent along Y (1.0–30.0) |
| Radius Z | Ellipsoid half-extent along Z (1.0–30.0) |
| Center offset Y | Vertical offset of the ellipsoid center above the pipeline origin (0.0–50.0) |
