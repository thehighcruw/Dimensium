---
layout: docs
title: Pipeline Editor
---

The Pipeline Editor is a node-based graph editor for procedurally generating structures. Nodes are connected with wires; data flows left-to-right from generator nodes through processing nodes to a final block map that the Tree tool places in the world.

## Opening the editor

The Pipeline Editor opens from the Tree tool panel. Use **Manage** to browse and select saved pipelines, and **Preview** to render the current graph to the viewport without placing blocks.

## Graph concepts

| Concept | Description |
|---|---|
| Node | A single operation with typed input and output ports |
| Port | A typed connection point on a node (input on the left, output on the right) |
| Wire | A connection between an output port and an input port of matching type |
| Block map | The primary output type — a set of blocks at world-space positions |

Data types flow along wires. A port only accepts connections from ports of the same type.

| Type | Description |
|---|---|
| Skeleton | A branching tree structure made of segments and radii |
| Block map | A set of placed blocks at world positions |
| Curve | A 3D spline or arc shape |
| Float | A scalar numeric value |
| Vec3 | A 3D vector |
| Color ramp | A gradient used for palette-based painting |

## Adding and connecting nodes

Right-click the canvas to open the **Add Node** menu, organised by group. Click a node type to place it on the canvas. Drag from an output port to an input port to connect them. Click a wire without dragging to disconnect it.

## Node groups

| Group | Purpose |
|---|---|
| Generate | Create data from nothing — skeleton paths, curves, constant values |
| Branch | Grow a skeleton from an input skeleton — branching algorithms |
| Voxelize | Convert a skeleton or curve into a block map |
| Combine | Boolean operations on two block maps (merge, subtract, intersect) |
| Transform | Reposition, rotate, mirror, or repeat a block map |
| Filter | Modify the shape of a block map (mask, erode, blur) |
| Paint | Recolor blocks without moving them |
| Math | Float arithmetic and range remapping |

## Node parameters

Select a node to edit its parameters in the Node Details panel. Parameters that show an **expose** toggle can be converted to input ports, letting you drive them from a Float node or another source.

## Library

Pipelines are saved to the library with a name and optional folder path. The **Manage** window lists all saved pipelines and built-in presets. Presets are organised under `Presets/Trees` and `Presets/Structures`.

## Preview

The **Preview** window renders the current graph result in the viewport. Toggle **Auto-preview** to update the preview whenever a parameter changes.

## Tips

- Start from a preset — modify an existing tree rather than building from scratch.
- Use **Merge Blocks** to combine independently built parts into one output.
- Expose a float parameter as a port and connect a **Random Float** node to randomise it each time the graph runs.
- The **Ellipsoid Mask** filter trims a block map to an ellipsoidal shape — useful for leaf crowns.

## Nodes

See the [node reference](/pipeline/nodes) for documentation on every available node.
