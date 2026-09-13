/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.stamp;

import java.util.ArrayList;
import java.util.List;

public class StampToolState {

    public static final StampToolState INSTANCE = new StampToolState();

    public static final float BASE_CHANCE_MIN = 0.0f;
    public static final float BASE_CHANCE_MAX = 1.0f;
    public static final float MIN_SPACING_MIN = 0.0f;
    public static final float MIN_SPACING_MAX = 4.0f;
    public static final float ENTRY_CHANCE_MIN = 0.0f;
    public static final float ENTRY_CHANCE_MAX = 1.0f;
    public static final int OFFSET_Y_MIN = -64;
    public static final int OFFSET_Y_MAX = 64;

    public float baseChance = 1.0f;
    /** Fraction of max(clipW, clipD) used as minimum distance between stamp anchors. */
    public float minSpacingPct = 1.0f;

    public boolean randomYaw = false;
    public boolean randomXFlip = false;
    public boolean randomZFlip = false;
    public boolean keepExisting = false;

    public final List<StampEntry> blueprints = new ArrayList<>();

    private StampToolState() {}
}
