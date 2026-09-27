---
layout: docs
title: Masks
---

Masks restrict which blocks a tool may affect. When a mask is active, every block position is tested against the mask before any edit is applied — only positions that pass are modified. Masks work globally across all tools.

## Using Masks

Open the **Mask List** window from the editor overlay. Create a mask with the **New** button, then open it in the mask editor to build its filter tree. Activate a mask by selecting it and clicking **Set Active**. The active mask name appears in the status bar. Click **Clear** to disable masking.

Each mask has a **Role** that controls where it applies:

| Role | Behaviour |
|---|---|
| Both | Applied to both source and destination positions |
| Destination | Applied only to the positions being written |
| Source | Applied only to source positions (copy/move source) |

## Mask Types

### Block filters

| Mask | Passes when… |
|---|---|
| **Block** | The block at the position matches the specified block (and optionally meta) |
| **Above** | The block directly above the position matches the specified block |
| **Below** | The block directly below the position matches the specified block |
| **Adjacent** | Any of the 6 face-adjacent neighbours matches the specified block |
| **Neighbour** | Any of the 26 surrounding blocks (faces + edges + corners) matches the specified block |
| **Near** | Any block within the given radius matches the specified block |

### Terrain filters

| Mask | Passes when… |
|---|---|
| **Surface** | At least one of the 26 neighbours is air or liquid |
| **CanSeeSky** | The position has an unobstructed line to the sky |
| **Angle** | The slope at the position is within `angle ± range` degrees |

### Position filters

| Mask | Passes when… |
|---|---|
| **Y** | The Y coordinate satisfies the comparison (`=`, `<`, `<=`, `>`, `>=`) against a value |
| **InSelection** | The position is within the current active selection |

### Logic nodes

Logic nodes combine child masks. They can hold any number of children.

| Node | Behaviour |
|---|---|
| **AND** | Passes only if all children pass |
| **OR** | Passes if any child passes |
| **NOT** | Passes only if the single child does not pass |
| **OFFSET** | Evaluates all children at `coord + offset` instead of the actual position |

## Building a Mask Tree

The mask editor shows the tree structure. Right-click a node to add children or change its type. Leaf nodes (Block, Angle, etc.) are evaluated at the candidate position. Logic nodes group leaves and other logic nodes.

Example — restrict to surface grass blocks:

```
AND
├── Block: Grass
└── Surface
```

Example — paint only on slopes between 20° and 60°:

```
AND
├── Angle: 40 ±20
└── Surface
```

## Tips

- A mask with an empty AND root passes every position (no filters active).
- Use **InSelection** to combine mask logic with selection-based restriction.
- The **OFFSET** node is useful for patterns like "place only where there is air one block above": put a `Block: Air` child inside an `OFFSET(0,1,0)`.
