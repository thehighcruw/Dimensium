---
layout: docs
title: Random Walk Path
---

Generates an organically wandering skeleton chain where each step randomly deviates from the previous direction.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| origin | Input | Vec3 | No |
| skeleton | Output | Skeleton | — |

## Parameters

| Parameter | Description |
|---|---|
| Steps | Number of skeleton nodes (2–50) |
| Base radius | Radius at the base (0.2–5.0) |
| Taper | Fraction of base radius kept at the tip (0.0–1.0) |
| Deviation angle | Maximum angular deviation per step in degrees (0.0–70.0) |
| Upward bias | Blending weight toward the vertical direction; 0.0 is fully random, 1.0 is straight up (0.0–1.0) |
