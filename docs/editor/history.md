---
layout: docs
title: History
---

Every block edit sent to the server is recorded as a history entry. You can step backward and forward through your edits at any time.

## Keybinds

| Action | Default |
|---|---|
| Undo | **Ctrl+Z** |
| Redo | **Ctrl+Y** |

These keybinds are remappable in [Settings](/editor/settings).

## History Window

Open the **History** window from the editor overlay. It lists every recorded action from oldest (top) to newest (bottom). The current position in history is highlighted.

- Entries above the current position are in the past (undone).
- The current entry is shown in white.
- Entries below the current position are available to redo (shown in green).

Click **Clear** to erase all history.

## Storage and Limits

History is persisted to disk (per world, gzip-compressed) and survives game restarts. The maximum size is configured by **Edit History Max MB** in the config file (`dimensium.cfg`). When the limit is reached, the oldest entries are dropped automatically.

## How It Works

When you apply a block edit, the server captures the before and after state of every affected block and sends an entry to the client. Undo replays the before-state; redo replays the after-state. All edits — including fill, paste, tool strokes, and selection operations — are recorded.

## See also

- [Settings](/editor/settings) — change the max history size
