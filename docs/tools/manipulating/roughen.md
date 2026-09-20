---
layout: docs
title: Roughen
---

The Roughen tool adds surface irregularity to a selection by randomly displacing exposed surface blocks outward or inward. The number of affected faces and the displacement probability are configurable.

## Controls

| Input | Action |
|---|---|
| Apply button | Apply roughening to the current selection |

## Parameters

| Parameter | Description |
|---|---|
| Faces | Number of surface block displacements to attempt per application |
| Roughening ratio | Probability (0.0–1.0) that each candidate surface block is displaced |

## Tips

- High faces count with a low ratio gives a scattered, speckled texture.
- Low faces count with high ratio produces heavy localized bumps.
- Apply repeatedly to accumulate roughness without changing the overall form.

## See also

- [Distort](/tools/manipulating/distort) — noise-field displacement of the whole volume
