---
layout: docs
title: Tip Cluster Fill
---

Fills noisy spherical clusters around skeleton tip nodes using a block palette, producing organic leaf canopies.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| blocks | Input | Block map | Yes |
| skeleton | Input | Skeleton | Yes |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Palette | Block palette used to fill the clusters |
| Radius | Radius of each cluster sphere in blocks (1.0–12.0) |
| Noisiness | Blend between a solid sphere (0.0) and a fully noise-shaped cluster (1.0) |
