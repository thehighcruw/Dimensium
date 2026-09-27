---
layout: docs
title: Modify
---

The Modify tool applies structural transformations to a selection: creating translated copies, revolving a cross-section around an axis, or twisting a selection along its own axes. A live preview shows the result before confirming.

## Controls

| Input | Action |
|---|---|
| RMB click | Confirm (Translate Copies / Twist); place revolve center (Revolve) |
| LMB drag (Revolve) | Drag the revolve center gizmo |

## Modes

### Translate Copies

Creates one or more copies of the selection, each offset from the last by a fixed step.

| Parameter | Description |
|---|---|
| Offset type | `Relative` — offset is a fraction of the selection size; `Absolute` — offset in blocks |
| Offset X/Y/Z | Step distance per copy along each axis |
| Count | Number of copies to produce (1–64) |

Set offset type to `Relative` and offset X to `1.0` to stack copies edge-to-edge along the X axis.

### Revolve

Rotates copies of the selection around a center point, distributing them evenly across an arc. Useful for round towers, pillars, or radially symmetric structures.

| Parameter | Description |
|---|---|
| Axis | Axis of rotation: X, Y, or Z |
| Angle | Total arc in degrees (1–360); `360` produces a full ring |
| Count | Number of copies placed around the arc (1–64) |
| Add translation | When enabled, each copy is also offset by a translation vector, producing a spiral |
| Translation X/Y/Z | Per-copy translation step applied when Add translation is on |

**Placing the revolve center:** RMB-click a block face in the world to plant the center point. Drag the translation gizmo that appears to reposition it. The preview updates live as you move it.

### Twist

Rotates each block in the selection by an amount that increases from one face to the opposite face, producing a twisting or spiralling distortion.

| Parameter | Description |
|---|---|
| Twist angle X | Total rotation around the X axis from bottom to top of the selection (−360–360°) |
| Twist angle Y | Total rotation around the Y axis from left to right (−360–360°) |
| Twist angle Z | Total rotation around the Z axis from front to back (−360–360°) |

## Tips

- For Revolve at 360°, set count to the number of evenly-spaced copies wanted around the ring (e.g. `8` for octagonal symmetry).
- Combine Revolve with a small translation to produce helical staircases or spiral pillars.
- Twist with only one axis set and a moderate angle (30–90°) gives a subtle organic lean; large angles produce full corkscrews.
- The preview is ghost-rendered over the world — confirm with RMB when satisfied.
