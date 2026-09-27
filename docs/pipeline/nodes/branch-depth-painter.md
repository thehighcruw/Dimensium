---
layout: docs
title: Branch Depth Painter
---

Repaints skeleton segments at or beyond a minimum branch depth with a new block palette, useful for highlighting thin outer branches.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| blocks | Input | Block map | Yes |
| skeleton | Input | Skeleton | Yes |
| blocks | Output | Block map | — |

## Parameters

| Parameter | Description |
|---|---|
| Min depth | Branch depth threshold at which repainting begins — segments shallower than this are left unchanged (1–10) |
| Palette | Block palette applied to segments at or beyond the min depth |
