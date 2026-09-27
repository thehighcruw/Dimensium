---
layout: docs
title: Blueprints
---

Blueprints are saved block structures. They are stored as files in your Minecraft folder and can be loaded back into the world at any time via the Stamp tool or the blueprint browser.

## Saving a Blueprint

1. Select the blocks you want to save.
2. Copy the selection (**Ctrl+C**) to put it on the clipboard.
3. Open the **Clipboard** window (or press your save-blueprint keybind).
4. Click **Save Blueprint** — the Create Blueprint dialog opens.

In the dialog:
- Enter a **Name** for the blueprint.
- Optionally add **Tags** (comma-separated) to make it easier to find later. Tags from your existing blueprints are suggested as toggle buttons below the field.
- Drag on the preview thumbnail to adjust the camera angle; scroll to zoom.
- Click **Save** to write the file.

The registry reloads automatically — the new blueprint appears immediately in the browser.

## Browsing and Loading Blueprints

Open the **Blueprint Browser** from the Clipboard window or via your blueprint-browser keybind. The browser lists all blueprints from the blueprints folder. Use the search box to filter by name or tag.

Click a blueprint to select it, then click **Load** (or double-click) to copy it to the clipboard. From there you can paste it, or use it with the **Stamp** tool.

## Using Blueprints with the Stamp Tool

The Stamp tool scatters blueprint instances with a brush. After loading a blueprint into the clipboard you can add it to the Stamp tool's blueprint list in the tool panel. See [Stamp](/tools/creating/stamp) for details.

## Storage

Blueprints are stored in `.minecraft/dimensium/blueprints/`. Each file is a self-contained blueprint including a thumbnail PNG. You can share files from this directory with other players.

The registry watches the directory and reloads headers on a background thread when the directory changes, so adding or removing files on disk is reflected without restarting the game.

## See also

- [Stamp](/tools/creating/stamp) — scatter blueprint instances with a brush
- [Clipboard](/editor/clipboard) — copy/paste workflow
