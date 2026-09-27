---
layout: docs
title: Curved Path
---

Generates a leaning skeleton chain — like Line Path but offset in a configurable direction.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| origin | Input | Vec3 | No |
| skeleton | Output | Skeleton | — |

## Parameters

| Parameter | Description |
|---|---|
| Height | Number of skeleton nodes in the chain (2–40) |
| Base radius | Radius at the base (0.2–6.0) |
| Taper | Fraction of base radius kept at the tip (0.0–1.0) |
| Lean angle | Angle in degrees away from vertical (0.0–80.0) |
| Lean direction | Compass azimuth of the lean direction in degrees (0.0–360.0) |
