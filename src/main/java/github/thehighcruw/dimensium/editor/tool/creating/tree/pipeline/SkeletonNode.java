/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline;

import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.ArrayList;
import java.util.List;

public class SkeletonNode {

    public final Vec3DInt position;
    public final float radius;
    public final List<SkeletonNode> children = new ArrayList<>();

    public SkeletonNode(Vec3DInt position, float radius) {
        this.position = position;
        this.radius = radius;
    }
}
