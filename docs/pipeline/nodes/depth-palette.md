---
layout: docs
title: Depth Palette
---

Repaints blocks by mapping their vertical height to a palette, creating a gradient from bottom to top.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| blocks | Input | Block map | Yes |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Palette | Block palette indexed by height — the first entry is used at the bottom, the last at the top |
| Gradient start | Normalized height at which palette sampling begins (0.0–1.0) |
| Gradient end | Normalized height at which palette sampling ends (0.0–1.0) |
