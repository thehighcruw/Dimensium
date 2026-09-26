/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

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

    public static final class InputPortDef {

        public final String name;
        public final PortType type;
        public final boolean required;

        public InputPortDef(String name, PortType type, boolean required) {
            this.name = name;
            this.type = type;
            this.required = required;
        }
    }

    public static final class OutputPortDef {

        public final String name;
        public final PortType type;

        public OutputPortDef(String name, PortType type) {
            this.name = name;
            this.type = type;
        }
    }

    private final List<ParamDef> paramList = new ArrayList<>();
    private final List<InputPortDef> inputPorts = new ArrayList<>();
    private final List<OutputPortDef> outputPorts = new ArrayList<>();
    private String descriptionKey = null;

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

    public NodeSchema description(String key) {
        this.descriptionKey = key;
        return this;
    }

    public String descriptionKey() {
        return descriptionKey;
    }

    public NodeSchema inputPort(String name, PortType type) {
        inputPorts.add(new InputPortDef(name, type, true));
        return this;
    }

    public NodeSchema optionalInputPort(String name, PortType type) {
        inputPorts.add(new InputPortDef(name, type, false));
        return this;
    }

    public NodeSchema outputPort(String name, PortType type) {
        outputPorts.add(new OutputPortDef(name, type));
        return this;
    }

    public List<InputPortDef> inputPorts() {
        return Collections.unmodifiableList(inputPorts);
    }

    public List<OutputPortDef> outputPorts() {
        return Collections.unmodifiableList(outputPorts);
    }

    public InputPortDef primaryInput() {
        return inputPorts.isEmpty() ? null : inputPorts.get(0);
    }

    public OutputPortDef primaryOutput() {
        return outputPorts.isEmpty() ? null : outputPorts.get(0);
    }

    public List<ParamDef> params() {
        return Collections.unmodifiableList(paramList);
    }

    @SuppressWarnings("unchecked")
    public void applyDefaults(NodeParams target) {
        for (ParamDef def : paramList) {
            Object value =
                    def.type == ParamType.PALETTE ? new ArrayList<>((List<int[]>) def.defaultValue) : def.defaultValue;
            target.set(def.key, value);
        }
    }
}
