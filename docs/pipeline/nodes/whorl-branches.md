---
layout: docs
title: Whorl Branches
---

Attaches radial whorls of branches at regular intervals along a trunk skeleton, producing the characteristic cone silhouette of conifers.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| skeleton | Input | Skeleton | Yes |
| skeleton | Output | Skeleton | — |

## Parameters

| Parameter | Description |
|---|---|
| Branches per whorl | Number of branches evenly spaced around each whorl (2–12) |
| Base angle | Angle from vertical at the lowest whorl in degrees — higher values are more horizontal (10.0–90.0) |
| Tip angle | Angle from vertical at the highest whorl in degrees (5.0–85.0) |
| Base length | Branch length at the lowest whorl in blocks (1–20) |
| Tip length | Branch length at the highest whorl in blocks (1–10) |
| Branch radius | Skeleton radius of each branch segment (0.1–1.5) |
| Spacing | Number of trunk nodes between consecutive whorls (1–6) |
| Skip base | Number of trunk nodes at the bottom to leave branch-free (0–8) |
| Skip tip | Number of trunk nodes at the top to leave branch-free (0–12) |
| Spiral offset | Golden-ratio-like azimuth offset between consecutive whorls to avoid alignment (0.0–1.0) |
