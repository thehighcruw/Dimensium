/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.overlay;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayDeque;
import java.util.Deque;

@SideOnly(Side.CLIENT)
public final class ViewState {

    public static final ViewState INSTANCE = new ViewState();

    private ViewState() {}

    public boolean showSelection = true;
    public boolean showKeyPresses = false;
    public boolean flipCanvas = false;

    // Key press log for the "Show Key Presses" overlay.
    private static final int MAX_LOG = 8;
    public static final long FADE_MS = 2500;

    public static final class KeyPressEntry {

        public final String label;
        public final long timeMs;

        KeyPressEntry(String label, long timeMs) {
            this.label = label;
            this.timeMs = timeMs;
        }
    }

    private final Deque<KeyPressEntry> keyLog = new ArrayDeque<>();

    public void logKey(String label) {
        if (keyLog.size() >= MAX_LOG) keyLog.pollFirst();
        keyLog.addLast(new KeyPressEntry(label, System.currentTimeMillis()));
    }

    public Deque<KeyPressEntry> keyLog() {
        return keyLog;
    }
}
