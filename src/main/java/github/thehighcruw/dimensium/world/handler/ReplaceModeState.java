/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.handler;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ReplaceModeState {

    public static final ReplaceModeState INSTANCE = new ReplaceModeState();

    public boolean active = false;

    static final int REPLACE_REPEAT_DELAY = 4;
    public int replaceHoldCooldown = 0;

    public void seedReplaceCooldown() {
        replaceHoldCooldown = REPLACE_REPEAT_DELAY;
    }
}
