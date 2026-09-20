---
layout: docs
title: Shape
---

The Shape tool places geometric primitives — cuboids, spheres, cylinders, toruses, and many more — with interactive gizmo controls for position, rotation, and scale. RMB places the anchor; then drag gizmos to resize, rotate, and reposition before confirming.

## Controls

| Input | Action |
|---|---|
| RMB (when inactive) | Anchor the shape at the cursor block |
| LMB drag (translation gizmo) | Move the shape along an axis |
| LMB drag (rotation gizmo) | Rotate the shape |
| LMB drag (scale gizmo) | Scale the shape on a plane |
| LMB drag (view-plane gizmo) | Move the shape on the camera-facing plane |
| RMB (when active) | Confirm and place the shape |

## Parameters

| Parameter | Description |
|---|---|
| Shape type | `Cuboid`, `Sphere`, `Cylinder`, `Pyramid`, `Cone`, `Torus`, `Octahedron`, `Supersphere`, `Tube`, `Dodecahedron`, `Icosahedron`, `Disk`, `Plane`, `Superellipse`, `Regular Polygon`, `Archimedean Spiral` |
| Width / Height / Depth | Dimensions in blocks (1–64) |
| Hollow | When enabled, only the shell is placed |
| Keep existing | When enabled, existing solid blocks are not replaced |
| Exponent | Surface exponent for Supersphere/Superellipse (0.5–10) |
| Separate axes | Unlock independent X/Z scaling for shapes that default to uniform axes |
| Torus ring radius | Radius of the torus ring (1–32) |
| Torus tube radius | Radius of the torus tube (1–16) |
| Tube wall thickness | Shell thickness for the Tube shape (1–16) |
| Polygon sides | Number of sides for Regular Polygon (3–32) |
| Spiral spacing | Gap between spiral arms for Archimedean Spiral (0.5–10) |
| Spiral turns | Number of turns for Archimedean Spiral (1–20) |

## Tips

- Hold the shape gizmo on the center handle to move the whole shape freely.
- Hollow + large shapes produce arches, domes, and tubes efficiently.
- Supersphere with exponent 1 is an octahedron; exponent 2 is a sphere; exponent 10 approaches a cube.
