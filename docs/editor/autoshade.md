---
layout: docs
title: Autoshade
---

Autoshade replaces blocks in the current selection with shaded variants from a palette based on how much light each block receives. It simulates directional lighting and ambient occlusion to make structures look more three-dimensional without manual painting.

## Opening Autoshade

Open the **Operations** window and click **Autoshade**, or use the menu bar. Autoshade requires an active selection.

## Parameters

### Light source

| Option | Description |
|---|---|
| **Sun** (checkbox) | Enable directional lighting. Uncheck to use only AO and GI. |
| **Light from** | `Player position` — sun direction follows your look direction. `Sun angle` — set yaw (0–360°) and elevation (0–90°) manually. |
| **Sun yaw** | Horizontal angle of the sun (degrees, 0–360). |
| **Sun elevation** | Vertical angle of the sun above the horizon (degrees, 0–90). |

### Shading

| Parameter | Description |
|---|---|
| **AO strength** | Ambient occlusion intensity (0.0–1.0). Higher = deeper crevices. |
| **GI strength** | Global illumination (sky fill light) intensity (0.0–1.0). |
| **Dither** | Add noise to break up hard banding at palette boundaries. |

### Palette

Add blocks to the palette using **Add Block**. Each block gets a **Weight** slider (0.0–1.0) that controls how strongly it represents the bright end of the range.

Blocks with higher weight are placed on more brightly lit faces; lower weight blocks on darker faces. Providing a gradient from light to dark (e.g. White Wool → Light Grey Wool → Grey Wool → Dark Grey Wool) produces the most natural result.

### Presets

Enter a name and click **Save preset** to store the current palette and settings. Saved presets appear in the list below — click **Load** to restore, **Del** to remove.

## Applying

Click **Apply** to replace blocks in the selection. The operation is recorded in history and can be undone with **Ctrl+Z**.

## Tips

- Use a 4–6 block gradient ordered from lightest to darkest for best results.
- Match the sun angle to your world's actual sun direction for consistent lighting.
- Run Autoshade before applying a mask to restrict which faces are shaded.
