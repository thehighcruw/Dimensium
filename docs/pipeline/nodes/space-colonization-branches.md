---
layout: docs
title: Space Colonization Branches
---

Grows organic branches from leaf nodes toward a cloud of attractors scattered in an ellipsoid above the skeleton tip, producing natural crown shapes.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| skeleton | Input | Skeleton | Yes |
| skeleton | Output | Skeleton | — |

## Parameters

| Parameter | Description |
|---|---|
| Attractor count | Number of attraction points scattered in the crown ellipsoid (50–1000) |
| Crown radius X / Y / Z | Half-extents of the ellipsoid containing attractors (2.0–20.0) |
| Crown offset Y | Vertical offset of the crown ellipsoid center above the skeleton's highest tip (0.0–20.0) |
| Influence radius | Distance within which an attractor influences branch growth (1.0–15.0) |
| Kill radius | Distance at which an attractor is consumed and removed (0.5–5.0) |
| Step size | Length of each growth step per iteration (0.5–3.0) |
| Max iterations | Maximum number of growth iterations before stopping (10–200) |
| Branch radius | Skeleton radius assigned to newly grown branch nodes (0.2–3.0) |
