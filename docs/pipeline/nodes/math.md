---
layout: docs
title: Math
---

Applies a binary arithmetic operation to two float inputs.

## Ports

| Port | Direction | Type | Required |
|---|---|---|---|
| valueA | Input | Float | Yes |
| valueB | Input | Float | No |
| value | Output | Float | — |

## Parameters

| Parameter | Description |
|---|---|
| Operation | Arithmetic operation: Add, Subtract, Multiply, Divide, Power, Min, Max |
| Value B | Fallback constant used as the second operand when the valueB port is not connected (-100.0–100.0) |
