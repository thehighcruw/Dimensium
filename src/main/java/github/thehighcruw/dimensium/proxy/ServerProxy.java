/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.proxy;

import cpw.mods.fml.common.FMLCommonHandler;
import github.thehighcruw.dimensium.editor.handler.PlayerHistoryEventHandler;

@SuppressWarnings("unused")
public class ServerProxy implements IProxy {

    @Override
    public void preInit() {}

    @Override
    public void init() {
        FMLCommonHandler.instance().bus().register(new PlayerHistoryEventHandler());
    }

    @Override
    public void postInit() {}
}
