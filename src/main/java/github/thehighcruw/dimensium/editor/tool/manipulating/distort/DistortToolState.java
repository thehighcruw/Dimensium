/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.manipulating.distort;

import github.thehighcruw.dimensium.shared.math.Vec3DFloat;
import java.util.concurrent.ThreadLocalRandom;

public class DistortToolState {

    public static final DistortToolState INSTANCE = new DistortToolState();

    public float distortScale = 10f;
    public long distortSeed = ThreadLocalRandom.current().nextLong();
    public Vec3DFloat distortDistance = Vec3DFloat.from(3f, 3f, 3f);
    public boolean distortSeparateAxis = false;
    public boolean distortSmoothEdges = true;
}
