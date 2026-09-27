---
layout: docs
title: Line Path
---

Generates a straight vertical skeleton chain — the simplest trunk generator.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| origin | Input | Vec3 | No |
| skeleton | Output | Skeleton | — |

## Parameters

| Parameter | Description |
|---|---|
| Height | Number of skeleton nodes in the chain (2–40) |
| Base radius | Radius at the base of the trunk (0.2–6.0) |
| Taper | Fraction of the base radius kept at the tip — 0.0 tapers to a point, 1.0 keeps constant thickness (0.0–1.0) |
