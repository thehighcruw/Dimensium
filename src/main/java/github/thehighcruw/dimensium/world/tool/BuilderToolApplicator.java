/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.world.tool;

import java.util.EnumMap;
import java.util.Map;

import github.thehighcruw.dimensium.shared.SelectionState;
import github.thehighcruw.dimensium.tool.BuilderTool;
import github.thehighcruw.dimensium.tool.BuilderToolState;
import github.thehighcruw.dimensium.world.tool.strategy.BuilderToolStrategy;
import github.thehighcruw.dimensium.world.tool.strategy.CloneStrategy;
import github.thehighcruw.dimensium.world.tool.strategy.EraseStrategy;
import github.thehighcruw.dimensium.world.tool.strategy.MoveStrategy;
import github.thehighcruw.dimensium.world.tool.strategy.SmearStrategy;
import github.thehighcruw.dimensium.world.tool.strategy.StackStrategy;

public class BuilderToolApplicator {

    private static final Map<BuilderTool, BuilderToolStrategy> STRATEGIES = new EnumMap<>(BuilderTool.class);

    static {
        STRATEGIES.put(BuilderTool.MOVE, new MoveStrategy());
        STRATEGIES.put(BuilderTool.CLONE, new CloneStrategy());
        STRATEGIES.put(BuilderTool.STACK, new StackStrategy());
        STRATEGIES.put(BuilderTool.SMEAR, new SmearStrategy());
        STRATEGIES.put(BuilderTool.ERASE, new EraseStrategy());
    }

    public static BuilderToolStrategy get(BuilderTool tool) {
        return STRATEGIES.get(tool);
    }

    public static boolean needsCapture(BuilderTool tool) {
        BuilderToolStrategy s = STRATEGIES.get(tool);
        return s != null && s.needsCapture();
    }

    public static void confirm(BuilderTool tool, BuilderToolState bts, SelectionState sel) {
        BuilderToolStrategy s = STRATEGIES.get(tool);
        if (s != null) s.confirm(bts, sel);
    }
}
