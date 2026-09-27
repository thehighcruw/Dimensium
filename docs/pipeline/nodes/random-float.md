---
layout: docs
title: Random Float
---

Outputs a seeded uniform-random float — deterministic for a given pipeline seed, so the same graph always produces the same value.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| value | Output | Float | — |

## Parameters

| Parameter | Description |
|---|---|
| Min | Lower bound of the random range (-1000.0–1000.0) |
| Max | Upper bound of the random range (-1000.0–1000.0) |
