/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.fill;

public class FloodfillToolState {

    public static final FloodfillToolState INSTANCE = new FloodfillToolState();

    public enum FloodfillDir {
        DOWN("dimensium.floodfill_dir.down"),
        UP("dimensium.floodfill_dir.up");

        public final String label;

        FloodfillDir(String l) {
            label = l;
        }
    }

    public static final int FILL_MAX = 2000;

    public int floodfillLimit = 100000;
    public FloodfillDir floodfillDir = FloodfillDir.DOWN;
    public boolean floodfillCorners = false;
}
