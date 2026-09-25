# Changelog

All notable changes to Dimensium will be documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com). Run `scripts/release.sh <version>` to cut a release.

## [Unreleased]
### Added
- Tool panel: replace category/tool comboboxes with single-click icon grid; column count configurable in Settings
- Path tool: "Use Stairs and Slabs" option smooths sloped paths with correctly oriented stair blocks when using a supported block material

### Changed

### Deprecated

### Removed

### Fixed
- Move, paste, path, and stamp tools: rotate block metadata (stairs, furnaces, dispensers, logs, etc.) to match the applied rotation
- Stamp tool: mirror block metadata when flipX or flipZ is active
- Gizmo: single-axis translation arrows now rotate with the shape's rotation
- Ghost renderer: non-full blocks (slabs, stairs, fences, etc.) now render with correct geometry instead of a full cube
- Camera: LMB drag inside an ImGui window (including tab bars and resize handles) no longer rotates the camera

### Security

## [0.0.6.1] — 2026-09-22
### Added
- Path tool: stamp blueprints and clipboard content along a path

## [0.0.6] — 2026-09-22
### Added
- Path tool: stamp blueprints and clipboard content along a path
