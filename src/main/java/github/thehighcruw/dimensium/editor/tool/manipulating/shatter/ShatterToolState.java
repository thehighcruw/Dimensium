/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.shatter;

import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseParams;
import github.thehighcruw.dimensium.editor.tool.painting.noise.NoiseToolState;

public class ShatterToolState {

    public static final ShatterToolState INSTANCE = new ShatterToolState();

    public enum AxisMode {

        XYZ("dimensium.shatter.axis.xyz"),
        X("dimensium.shatter.axis.x"),
        Y("dimensium.shatter.axis.y"),
        Z("dimensium.shatter.axis.z");

        public final String label;

        AxisMode(String l) {
            label = l;
        }
    }

    public final NoiseParams noiseParams = new NoiseParams(NoiseToolState.NoiseType.VORONOI_EDGES);

    public float crackWidth = 0.15f;
    public AxisMode axisMode = AxisMode.XYZ;
    public boolean fillMode = false;
}
