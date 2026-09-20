---
layout: docs
title: Freehand
---

The Freehand tool paints blocks into the world by dragging across surfaces with a configurable brush. The brush shape, radius, and replace mode are shared with other brush-based tools via the Brush panel.

## Controls

| Input | Action |
|---|---|
| RMB drag | Paint blocks at cursor position continuously |

## Parameters

| Parameter | Description |
|---|---|
| Brush shape | Shape of the brush volume (Sphere, Cube, Cylinder, Ellipsoid, Cuboid, Capsule, Cone, Octahedron) |
| Brush radius | Radius of the brush in blocks (shared with other brush tools) |
| Brush height | Height of the brush for shapes that support a separate height dimension |
| Replace mode | Which existing blocks the brush may overwrite: `Any`, `Air only`, `Solid only`, `Same block` |
| Mask surface | When enabled, only surface blocks are painted |
| Replace solid | When enabled, solid blocks within the brush are replaced |
| Include air | When enabled, air blocks are also added to the brush region |

## Tips

- Combine with a mask to protect specific block types from being overwritten.
- Use `Air only` replace mode to fill gaps without touching existing structures.
- For large strokes, increase brush radius and hold RMB while moving the camera.
