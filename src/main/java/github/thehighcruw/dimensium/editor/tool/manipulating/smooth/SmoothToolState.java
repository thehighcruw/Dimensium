/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.smooth;

public class SmoothToolState {

    public static final SmoothToolState INSTANCE = new SmoothToolState();

    public enum SmoothModifier {
        STABLE("dimensium.smooth_modifier.stable"),
        MELT("dimensium.smooth_modifier.melt"),
        GROW("dimensium.smooth_modifier.grow");

        public final String label;

        SmoothModifier(String l) {
            label = l;
        }
    }

    public int smoothStrength = 2;
    public int smoothBlockRatio = 100;
    public SmoothModifier smoothModifier = SmoothModifier.STABLE;
    public boolean smoothFixEdges = true;
}
