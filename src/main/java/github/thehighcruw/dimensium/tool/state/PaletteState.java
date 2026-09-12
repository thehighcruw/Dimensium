/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.item.ItemStack;

public class PaletteState {

    public static final PaletteState INSTANCE = new PaletteState();

    public final List<ItemStack> palette = new ArrayList<>();
    public final List<Integer> paletteWeights = new ArrayList<>();

    public int getWeight(int i) {
        if (i < 0 || i >= paletteWeights.size()) return 1;
        return Math.max(1, paletteWeights.get(i));
    }

    public void setWeight(int i, int w) {
        while (paletteWeights.size() <= i) paletteWeights.add(50);
        paletteWeights.set(i, Math.max(0, Math.min(100, w)));
    }

    public int totalPaletteWeight() {
        int total = 0;
        for (int i = 0; i < palette.size(); i++) total += getWeight(i);
        return total;
    }

    public ItemStack samplePalette(Random rand) {
        if (palette.isEmpty()) return null;
        int total = totalPaletteWeight();
        if (total == 0) return null;
        int roll = rand.nextInt(total);
        int cum = 0;
        for (int i = 0; i < palette.size(); i++) {
            cum += getWeight(i);
            if (roll < cum) return palette.get(i);
        }
        return null;
    }
}
