/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NodeSchema {

    public enum ParamType {
        FLOAT,
        INT,
        BOOL,
        LONG,
        PALETTE
    }

    public static final class ParamDef {

        public final String key;
        public final ParamType type;
        public final Object defaultValue;
        public final float min;
        public final float max;
        public final String labelKey;

        public ParamDef(String key, ParamType type, Object defaultValue, float min, float max, String labelKey) {
            this.key = key;
            this.type = type;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.labelKey = labelKey;
        }
    }

    private final List<ParamDef> paramList = new ArrayList<>();

    public NodeSchema floatParam(String key, float defaultValue, float min, float max, String labelKey) {
        paramList.add(new ParamDef(key, ParamType.FLOAT, defaultValue, min, max, labelKey));
        return this;
    }

    public NodeSchema intParam(String key, int defaultValue, int min, int max, String labelKey) {
        paramList.add(new ParamDef(key, ParamType.INT, defaultValue, (float) min, (float) max, labelKey));
        return this;
    }

    public NodeSchema boolParam(String key, boolean defaultValue, String labelKey) {
        paramList.add(new ParamDef(key, ParamType.BOOL, defaultValue, 0f, 1f, labelKey));
        return this;
    }

    public NodeSchema longParam(String key, long defaultValue, String labelKey) {
        paramList.add(new ParamDef(key, ParamType.LONG, defaultValue, 0f, 0f, labelKey));
        return this;
    }

    public NodeSchema paletteParam(String key, List<int[]> defaultValue, String labelKey) {
        paramList.add(new ParamDef(key, ParamType.PALETTE, new ArrayList<>(defaultValue), 0f, 0f, labelKey));
        return this;
    }

    public List<ParamDef> params() {
        return Collections.unmodifiableList(paramList);
    }

    public void applyDefaults(NodeParams target) {
        for (ParamDef def : paramList) {
            target.set(def.key, def.defaultValue);
        }
    }
}
