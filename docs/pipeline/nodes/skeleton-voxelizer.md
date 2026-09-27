---
layout: docs
title: Skeleton Voxelizer
---

Converts a skeleton into a block map by stamping filled spheres along every segment.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| skeleton | Input | Skeleton | Yes |
| radiusScale | Input | Float | No |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Palette | Block palette used to fill the skeleton segments |
