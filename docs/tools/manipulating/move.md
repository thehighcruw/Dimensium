---
layout: docs
title: Move
---

The Move tool translates and rotates the current selection using interactive gizmos. The selection is captured as a block snapshot on activation. Dragging the translation gizmo moves the ghost preview; the rotation gizmo rotates around the selection's center of mass. RMB confirms the move and applies the change to the world.

## Controls

| Input | Action |
|---|---|
| LMB drag (translation gizmo axis) | Translate the selection along that axis |
| LMB drag (rotation gizmo axis) | Rotate the selection around that axis |
| LMB drag (view-plane gizmo) | Translate the selection on the camera-facing plane |
| RMB | Confirm the move and apply to the world |

## Parameters

| Parameter | Description |
|---|---|
| (none) | The Move tool has no panel parameters — all transforms are applied through gizmos in the viewport |

## Tips

- The ghost preview updates in real time as you drag.
- Rotation pivots around the selection's center of mass.
- Press **Escape** to cancel before confirming — this discards all gizmo movement.
- Scale handles resize the selection via nearest-neighbour resampling, which may simplify detail at small scales.
- Rotated blocks have their facing metadata updated automatically where supported.
