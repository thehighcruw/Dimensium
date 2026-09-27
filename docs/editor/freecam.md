---
layout: docs
title: Freecam
---

The freecam is the detached camera entity that the editor uses. When you open the editor (press **RShift** while holding the Builder Tool), a freecam entity spawns at your player position and becomes the render view. All editor interactions happen through this camera — your player body remains stationary.

## Entering and Exiting

| Action | Keybind |
|---|---|
| Toggle editor (enters/exits freecam) | **RShift** (configurable) |

Closing the editor returns the render view to your player. The camera's last position and orientation are remembered for the next session.

## Camera Controls

See [Navigation](/editor/navigation) for the full control reference. Key controls:

The **camera modifier key** is platform-specific: **Ctrl** on Windows and Linux, **Option** on macOS.

| Input | Action |
|---|---|
| LMB drag | Rotate camera (yaw / pitch) |
| CameraMod + LMB drag | Orbit around the block under the cursor |
| CameraMod + RMB drag | Pan camera |
| Scroll | Zoom (move along look direction) |
| W / A / S / D / Space / Shift | Move (always active while editor is open) |

## Interaction Limits

While in freecam mode:
- You cannot interact with the world as a normal player (no inventory, no regular block placement).
- Only editor tools placed through the overlay affect the world.
- Your player body does not move, so it can fall, take damage, or suffocate if left in a dangerous position.

## Reach

The freecam raycasts up to **512 blocks** from the camera position. Tools and selections work at any distance within this reach, regardless of your player's physical position.

## Tips

- Use orbit (CameraMod + LMB) to inspect a specific structure face without losing your viewing angle.
- WASD is useful for initial positioning; use mouse controls for precise camera adjustment.

## See also

- [Navigation](/editor/navigation) — full camera control reference
