/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.shared.math;

@FunctionalInterface
public interface TriIntPredicate {

    boolean test(int x, int y, int z);
}
