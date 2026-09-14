/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.utility.ruler;

import java.util.ArrayList;
import java.util.List;

public class RulerToolState {

    public static final RulerToolState INSTANCE = new RulerToolState();

    public enum Mode {
        DEFAULT("dimensium.ruler.mode.default"),
        CIRCLE("dimensium.ruler.mode.circle");

        public final String label;

        Mode(String l) {
            label = l;
        }
    }

    public final List<int[]> points = new ArrayList<>();
    public Mode mode = Mode.DEFAULT;
}
