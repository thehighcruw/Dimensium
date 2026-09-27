---
layout: docs
title: Helix Curve
---

Generates a helical curve rising from the pipeline origin — useful for spiral staircases and vines.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| origin | Input | Vec3 | No |
| curve | Output | Curve | — |

## Parameters

| Parameter | Description |
|---|---|
| Radius | Helix radius in blocks (0.5–32.0) |
| Height | Total vertical rise in blocks (1–128) |
| Turns | Number of full rotations over the height (0.25–16.0) |
