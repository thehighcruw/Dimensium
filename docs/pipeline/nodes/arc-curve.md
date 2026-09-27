---
layout: docs
title: Arc Curve
---

Generates a partial arc curve through a configurable angle sweep and orientation plane — useful for bridge arches, doorway frames, and partial ring shapes.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| origin | Input | Vec3 | No |
| curve | Output | Curve | — |

## Parameters

| Parameter | Description |
|---|---|
| Radius | Arc radius in blocks (1.0–64.0) |
| Start angle | Angle in degrees where the arc begins (-180.0–180.0) |
| Sweep angle | Total angle swept in degrees; 360 produces a full circle (1.0–360.0) |
| Plane | Orientation plane: Horizontal (XZ), Vertical N–S (XY), or Vertical E–W (YZ) |
