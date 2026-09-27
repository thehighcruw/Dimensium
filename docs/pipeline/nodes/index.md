---
layout: docs
title: Pipeline Nodes
---

All available pipeline nodes, grouped by category.

## Generate

| Node | Description |
|---|---|
| [Shape](/pipeline/nodes/shape) | Fills a mathematical solid (sphere, box, cylinder, etc.) at the origin |
| [Line Path](/pipeline/nodes/line-path) | Straight vertical skeleton chain |
| [Curved Path](/pipeline/nodes/curved-path) | Leaning skeleton chain in a configurable direction |
| [Random Walk Path](/pipeline/nodes/random-walk-path) | Organically wandering skeleton chain |
| [Arc Curve](/pipeline/nodes/arc-curve) | Partial arc through a configurable angle sweep and orientation plane |
| [Circle Curve](/pipeline/nodes/circle-curve) | Closed circle in the horizontal XZ plane |
| [Ellipse Curve](/pipeline/nodes/ellipse-curve) | Closed ellipse in the horizontal XZ plane |
| [Helix Curve](/pipeline/nodes/helix-curve) | Helical curve rising from the origin |
| [Constant Float](/pipeline/nodes/constant-float) | Outputs a fixed float value |
| [Random Float](/pipeline/nodes/random-float) | Outputs a seeded uniform-random float |

## Branch

| Node | Description |
|---|---|
| [Whorl Branches](/pipeline/nodes/whorl-branches) | Radial whorls at regular intervals — characteristic conifer silhouette |
| [Simple Recursive Branches](/pipeline/nodes/simple-recursive-branches) | Symmetric recursive branching at leaf nodes |
| [Space Colonization Branches](/pipeline/nodes/space-colonization-branches) | Organic crown grown toward attractor points |
| [Weber–Penn Branches](/pipeline/nodes/weber-penn-branches) | Multi-level deciduous branching at leaf nodes |

## Voxelize

| Node | Description |
|---|---|
| [Skeleton Voxelizer](/pipeline/nodes/skeleton-voxelizer) | Converts a skeleton to blocks by stamping spheres along each segment |
| [Branch Depth Painter](/pipeline/nodes/branch-depth-painter) | Repaints segments at or beyond a minimum branch depth |
| [Tip Cluster Fill](/pipeline/nodes/tip-cluster-fill) | Fills noisy spherical leaf clusters around skeleton tips |
| [Curve Fill](/pipeline/nodes/curve-fill) | Fills a solid tube of blocks along a curve |

## Combine

| Node | Description |
|---|---|
| [Merge Blocks](/pipeline/nodes/merge-blocks) | Unions two block maps; B overwrites A at conflicts |
| [Subtract Blocks](/pipeline/nodes/subtract-blocks) | Removes positions in B from A |
| [Intersect Blocks](/pipeline/nodes/intersect-blocks) | Keeps only positions present in both A and B |

## Transform

| Node | Description |
|---|---|
| [Translate Blocks](/pipeline/nodes/translate-blocks) | Shifts all blocks by a fixed integer offset |
| [Rotate Blocks](/pipeline/nodes/rotate-blocks) | Rotates a block map in 90° increments around the origin |
| [Mirror Blocks](/pipeline/nodes/mirror-blocks) | Reflects a block map across an axis plane |
| [Scatter Blocks](/pipeline/nodes/scatter-blocks) | Places multiple randomly offset copies of a block map |
| [Grid Blocks](/pipeline/nodes/grid-blocks) | Places copies at regular grid positions |
| [Curve Scatter](/pipeline/nodes/curve-scatter) | Places copies at evenly-spaced points along a curve |

## Filter

| Node | Description |
|---|---|
| [Ellipsoid Mask](/pipeline/nodes/ellipsoid-mask) | Removes blocks outside an ellipsoid above the origin |
| [Noise Erode](/pipeline/nodes/noise-erode) | Removes blocks based on 3D noise |
| [Gaussian Blur](/pipeline/nodes/gaussian-blur) | Fills gaps adjacent to enough filled neighbors, smoothing surfaces |

## Paint

| Node | Description |
|---|---|
| [Depth Palette](/pipeline/nodes/depth-palette) | Repaints blocks by mapping height to a palette |
| [Density Palette](/pipeline/nodes/density-palette) | Repaints blocks by mapping local neighbor density to a palette |
| [Noise Palette](/pipeline/nodes/noise-palette) | Repaints blocks by mapping 3D noise to a palette |

## Math

| Node | Description |
|---|---|
| [Map Range](/pipeline/nodes/map-range) | Remaps a float from one range to another |
| [Math](/pipeline/nodes/math) | Applies a binary arithmetic operation (add, multiply, power, etc.) |
