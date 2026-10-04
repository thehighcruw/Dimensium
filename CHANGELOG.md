# Changelog

All notable changes to Dimensium will be documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com). Run `scripts/release.sh <version>` to cut a release.

## [Unreleased]
### Added

### Changed

### Deprecated

### Removed

### Fixed
- Copy tool: fix ghost preview appearing hundreds of blocks away and apparently rotated due to float precision loss when large absolute world coordinates were accumulated in the GL float matrix
- Selection highlight: fix same float precision rendering bug affecting the per-block textured and glow passes
- Copy tool: fix ghost wireframe and glow passes sharing the same precision bug via the proposal render path

### Security

## [0.1.0] — 2026-09-27
### Fixed
- Modify tool: fix Twist mode multi-axis block placement disagreeing with the bounding-box visual; forward mapping now places blocks at their exact rotated positions, and the destination AABB is computed per-corner with each corner's own rotation

### Added
- Pipelines: add Blueprint Blocks node (Generate group) — loads a saved blueprint and outputs its blocks as a BlockMap; blueprint is picked via the Blueprint Browser popup
- Pipelines: add ArcCurveNode — partial arc with configurable radius, start angle, sweep angle, and orientation plane (horizontal XZ, vertical XY/YZ); useful for arches and doorway frames
- Pipelines: add Presets/Structures folder with Arch Bridge sample preset (semicircular stone-brick arch with elevated deck)
- Pipelines: CURVE port type renders orange to distinguish it from block map (green) and skeleton (blue) ports
- Pipelines: node picker popups show labelled section separators within large groups (Generate: Shape / Skeleton / Curve / Value; Transform: Move / Repeat)
- Pipelines: Curve gains a `closed` flag; circle and ellipse curves are closed, arc and helix are open; preview renderer and CurveFill respect the flag
- Modify tool: add Twist mode that redistributes selection blocks in-place by rotating each block around the AABB center by an angle proportional to its normalized position along each axis; X, Y, and Z twist angles can be combined simultaneously
- Modify tool: add Revolve mode that rotates a selection N times around a chosen axis by a configurable angle, with optional helix translation; stairs, slabs, and orientable blocks rotate with each copy
- Viewport window: add toggle via Window > Panels menu; state persisted across restarts
- Modify tool: add Translate Copies mode that clones a selection N times along a configurable offset (relative or absolute)
- Path tool: add "Extend to Ground" option that fills each path column downward until a solid surface is reached
- Tool panel: replace category/tool comboboxes with single-click icon grid; column count configurable in Settings
- Path tool: "Use Stairs and Slabs" option smooths sloped paths with correctly oriented stair blocks when using a supported block material
- Pipelines: procedural pipeline-based voxel tree generator with Weber-Penn skeleton and leaf cluster nodes
- Pipelines: DepthPaletteNode for Y-depth driven block palette variation
- Pipelines: ConstantFloatNode and RandomFloatNode as float value sources
- Pipelines: parameter port exposure — toggle a FLOAT input port per parameter to drive values from the graph
- Stamp tool: virtual deterministic 2D grid seed — placement, preset selection, and pipeline seed all derived from tool seed mixed with block x/z, so restamping the same area with the same seed always yields identical results

### Changed
- Pipelines: regroup node toolbar from 11 technical groups into 8 functional groups: Generate, Branch, Voxelize, Combine, Transform, Filter, Paint, Math
- Pipelines: curve generator nodes (circle, ellipse, helix) no longer expose a segments parameter; resolution is auto-computed from arc length (1 sample per block)
- Pipelines: curve preview now renders correctly — executor was not returning Curve outputs from leaf nodes
- Shape tool: cuboid defaults to cube with optional separate axes toggle; uniform shapes (sphere, octahedron, supersphere, dodecahedron, icosahedron) show single size slider by default; XZ-symmetric shapes (cylinder, cone, pyramid) link depth to width by default; 2D radial shapes show single radius slider by default
- Tree tool: pipeline nodes now receive PipelineContext carrying seed and typed slots; skeleton no longer passed via thread-local
- Tree tool: log and leaf block selection now uses palette (multi-block) instead of single block

### Removed
- Pipelines: remove NoiseFieldNode, SplinePathNode, BoxMaskNode, SphereMaskNode — no concrete use cases in current pipeline architecture

### Fixed
- Move tool, clipboard paste, Revolve, Twist: switch to inverse (backward) mapping for all rotations; eliminates gaps that appeared when arc spacing exceeded one block at large radii
- Move tool: fix irrecoverable freeze after confirming a rotation that moved blocks below Y=0; corrupt pack keys from negative Y caused an effectively unbounded AABB iteration
- Shape tool: supersphere exponent slider had no effect (was wired to a dead constant)
- Shape tool: torus Z ring radius ignored the separate-axes toggle, always stretching on one axis
- Shape tool: switching shape types no longer carries over hidden stale dimension values

## [0.0.7] — 2026-09-25
### Added
- Tool panel: replace category/tool comboboxes with single-click icon grid; column count configurable in Settings
- Path tool: "Use Stairs and Slabs" option smooths sloped paths with correctly oriented stair blocks when using a supported block material
- Shape tool: "Use Stairs and Slabs" option smooths shape surfaces with correctly oriented stair and slab blocks when using a supported block material

### Fixed
- Editor: close overlay and open pause menu when the game window loses focus
- Move, paste, path, and stamp tools: rotate block metadata (stairs, furnaces, dispensers, logs, etc.) to match the applied rotation
- Stamp tool: mirror block metadata when flipX or flipZ is active
- Gizmo: single-axis translation arrows now rotate with the shape's rotation
- Path tool: fix jaggedness in stair/slab-smoothed paths caused by integer-snapped sphere centers; spine blocks now always stay solid to prevent gaps in thin paths
- Ghost renderer: non-full blocks (slabs, stairs, fences, etc.) now render with correct geometry instead of a full cube
- Camera: LMB drag inside an ImGui window (including tab bars and resize handles) no longer rotates the camera

## [0.0.6.1] — 2026-09-22
### Added
- Path tool: stamp blueprints and clipboard content along a path

## [0.0.6] — 2026-09-22
### Added
- Path tool: stamp blueprints and clipboard content along a path
