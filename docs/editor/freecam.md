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

| Input | Action |
|---|---|
| Scroll | Move forward/backward along look direction |
| Alt + LMB drag | Rotate camera |
| Alt + RMB drag | Pan camera |
| Alt + MMB drag | Orbit around the block under the cursor |
| Ctrl + LMB drag | Orbit around the block under the cursor |
| **C** | Toggle walk mode (WASD movement) |

## Interaction Limits

While in freecam mode:
- You cannot interact with the world as a normal player (no inventory, no regular block placement).
- Only editor tools placed through the overlay affect the world.
- Your player body does not move, so it can fall, take damage, or suffocate if left in a dangerous position.

## Reach

The freecam raycasts up to **512 blocks** from the camera position. Tools and selections work at any distance within this reach, regardless of your player's physical position.

## Tips

- Use orbit (Alt+MMB) to inspect a specific structure face without losing your viewing angle.
- Walk mode is useful for initial positioning; switch back to editor mode for precise tool use.

## See also

- [Navigation](/editor/navigation) — full camera control reference
