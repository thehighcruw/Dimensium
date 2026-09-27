---
layout: docs
title: Shape
---

Fills a mathematical solid at the pipeline origin with blocks from a palette.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| origin | Input | Vec3 | No |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Shape type | Which solid to fill: Sphere, Ellipsoid, Box, Cylinder, Cone, Torus, Pyramid |
| Radius | Sphere radius (1.0–40.0). Only shown when type is Sphere. |
| Radius X / Y / Z | Ellipsoid radii per axis (1.0–40.0). Only shown when type is Ellipsoid. |
| Width / Height / Depth | Box dimensions in blocks (1–80). Only shown when type is Box. |
| Radius | Cylinder cross-section radius (1.0–40.0). Only shown when type is Cylinder. |
| Height | Cylinder height in blocks (1–80). Only shown when type is Cylinder. |
| Axis | Cylinder axis: Y (vertical), X, Z. Only shown when type is Cylinder. |
| Base radius | Cone base radius (1.0–40.0). Only shown when type is Cone. |
| Height | Cone height in blocks (1–80). Only shown when type is Cone. |
| Tip direction | Cone orientation: Tip Up or Tip Down. Only shown when type is Cone. |
| Major radius | Torus ring radius (1.0–40.0). Only shown when type is Torus. |
| Minor radius | Torus tube radius (0.5–20.0). Only shown when type is Torus. |
| Base width / depth | Pyramid base dimensions (2–80). Only shown when type is Pyramid. |
| Height | Pyramid height in blocks (1–80). Only shown when type is Pyramid. |
| Tip direction | Pyramid orientation: Tip Up or Tip Down. Only shown when type is Pyramid. |
| Palette | Block palette used to fill the solid |
