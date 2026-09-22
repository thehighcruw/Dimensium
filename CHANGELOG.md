# Changelog

All notable changes to Dimensium will be documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com). Run `scripts/release.sh <version>` to cut a release.

## [Unreleased]
### Added
- Modify tool: add Twist mode that redistributes selection blocks in-place by rotating each block around the AABB center by an angle proportional to its normalized position along each axis; X, Y, and Z twist angles can be combined simultaneously
- Modify tool: add Revolve mode that rotates a selection N times around a chosen axis by a configurable angle, with optional helix translation; stairs, slabs, and orientable blocks rotate with each copy
- Modify tool: add Translate Copies mode that clones a selection N times along a configurable offset (relative or absolute)
- Path tool: add "Extend to Ground" option that fills each path column downward until a solid surface is reached
- Tool panel: replace category/tool comboboxes with single-click icon grid; column count configurable in Settings
- Path tool: "Use Stairs and Slabs" option smooths sloped paths with correctly oriented stair blocks when using a supported block material
- Tree tool: procedural pipeline-based voxel tree generator with Weber-Penn skeleton and leaf cluster nodes
- Tree tool: DepthPaletteNode for Y-depth driven block palette variation

### Changed
- Shape tool: cuboid defaults to cube with optional separate axes toggle; uniform shapes (sphere, octahedron, supersphere, dodecahedron, icosahedron) show single size slider by default; XZ-symmetric shapes (cylinder, cone, pyramid) link depth to width by default; 2D radial shapes show single radius slider by default
- Tree tool: pipeline nodes now receive PipelineContext carrying seed and typed slots; skeleton no longer passed via thread-local
- Tree tool: log and leaf block selection now uses palette (multi-block) instead of single block

### Deprecated

### Removed

### Fixed
- Shape tool: supersphere exponent slider had no effect (was wired to a dead constant)
- Shape tool: torus Z ring radius ignored the separate-axes toggle, always stretching on one axis
- Shape tool: switching shape types no longer carries over hidden stale dimension values

### Security

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
