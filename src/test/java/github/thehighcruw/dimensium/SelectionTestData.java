/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium;

import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.shared.math.Vec3DInt;
import java.util.HashSet;
import java.util.Set;

class SelectionTestData {

    static Set<Long> pack(int[][] coords) {
        Set<Long> s = new HashSet<>();
        for (int[] c : coords) s.add(SelectionState.pack(Vec3DInt.from(c[0], c[1], c[2])));
        return s;
    }

    static Set<Long> solidCube5x5x5() {
        Set<Long> blocks = new HashSet<>();
        for (int x = 0; x < 5; x++)
            for (int y = 64; y < 69; y++)
                for (int z = 0; z < 5; z++) blocks.add(SelectionState.pack(Vec3DInt.from(x, y, z)));
        return blocks;
    }
}
