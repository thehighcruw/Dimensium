/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.mask;

import java.util.ArrayList;
import java.util.List;

public class MaskFolder implements MaskEntry {

    private String name;
    public final List<MaskEntry> entries = new ArrayList<>();
    public boolean expanded = true;

    public MaskFolder(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
