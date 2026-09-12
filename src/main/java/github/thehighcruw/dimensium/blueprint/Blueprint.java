/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.blueprint;

import java.util.ArrayList;
import java.util.List;

public class Blueprint {

    public String name = "";
    public List<String> tags = new ArrayList<>();
    public int clipW, clipH, clipD;
    /** Each entry: {lx, ly, lz, blockId, meta}. */
    public List<int[]> offsets = new ArrayList<>();
    /** PNG-encoded thumbnail bytes, may be null. */
    public byte[] thumbnailPng;
}
