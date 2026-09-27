---
layout: docs
title: Gaussian Blur
---

Fills empty voxels adjacent to existing blocks when they have enough filled neighbors, rounding and smoothing the block map surface.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| blocks | Input | Block map | Yes |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Passes | Number of blur iterations to apply (1–3) |
| Threshold | Minimum number of filled face-neighbors required to fill a gap (1–6) |
| Only leaves | When enabled, only leaf blocks (id 18, 161) are considered when counting neighbors and filling |
