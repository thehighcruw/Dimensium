---
layout: docs
title: Stamp
---

The Stamp tool scatters blueprint instances across a brush stroke. As you drag, blueprint copies are placed at positions sampled from the brush area according to probability, spacing, and optional random rotation and flipping. A live preview shows the result during the drag.

## Controls

| Input | Action |
|---|---|
| RMB drag | Scatter blueprint stamps across the brush stroke area |

Brush shape and radius control the sampling area. Each position inside the brush is a candidate anchor for a stamp instance.

## Parameters

| Parameter | Description |
|---|---|
| Base chance | Probability (0.0–1.0) that any candidate position spawns a stamp |
| Min spacing | Minimum distance between stamp anchors as a fraction of the blueprint's max(width, depth) (0.0–4.0) |
| Random yaw | When enabled, each instance is rotated by a random yaw angle |
| Random X flip | When enabled, each instance may be mirrored on the X axis |
| Random Z flip | When enabled, each instance may be mirrored on the Z axis |
| Keep existing | When enabled, the stamp does not overwrite existing solid blocks |
| Blueprints | List of blueprints to stamp; each entry has its own placement probability |

## Tips

- Lower base chance with a large brush creates natural-looking scattered distributions.
- Use multiple blueprints in the list for variety — each instance randomly picks one.
- Min spacing 1.0 ensures stamps do not overlap each other.

## See also

- [Blueprint browser](/tools) — manage and save blueprints used by the Stamp tool
