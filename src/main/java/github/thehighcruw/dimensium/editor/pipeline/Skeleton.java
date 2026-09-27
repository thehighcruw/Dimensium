/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Skeleton {

    public final List<SkeletonNode> roots;

    public Skeleton(List<SkeletonNode> roots) {
        this.roots = Collections.unmodifiableList(new ArrayList<>(roots));
    }

    public Skeleton(SkeletonNode singleRoot) {
        this.roots = Collections.singletonList(singleRoot);
    }
}
