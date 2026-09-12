/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.tool;

import java.util.EnumMap;
import java.util.Map;

import github.thehighcruw.dimensium.tool.builder.BuilderToolStrategy;
import github.thehighcruw.dimensium.tool.builder.CloneStrategy;
import github.thehighcruw.dimensium.tool.builder.EraseStrategy;
import github.thehighcruw.dimensium.tool.builder.MoveStrategy;
import github.thehighcruw.dimensium.tool.builder.SmearStrategy;
import github.thehighcruw.dimensium.tool.builder.StackStrategy;
import github.thehighcruw.dimensium.tool.state.SelectionState;

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
