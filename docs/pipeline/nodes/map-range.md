---
layout: docs
title: Map Range
---

Remaps a float value from one range to another, with optional clamping.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| value | Input | Float | Yes |
| value | Output | Float | — |

## Parameters

| Parameter | Description |
|---|---|
| In min | Lower bound of the input range (-10.0–10.0) |
| In max | Upper bound of the input range (-10.0–10.0) |
| Out min | Lower bound of the output range (-10.0–10.0) |
| Out max | Upper bound of the output range (-10.0–10.0) |
| Clamp | When enabled, the output is clamped to [Out min, Out max] |
