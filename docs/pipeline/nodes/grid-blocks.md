---
layout: docs
title: Grid Blocks
---

Places copies of the input block map at regular grid positions centred at the pipeline origin — useful for columns, fence arrays, and repeating structural elements.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| blocks | Input | Block map | Yes |
| origin | Input | Vec3 | No |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Columns | Number of grid columns along X (1–16) |
| Rows | Number of grid rows along Z (1–16) |
| Spacing X | Distance between columns in blocks (1–64) |
| Spacing Z | Distance between rows in blocks (1–64) |
