/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NodeSchema {

    public enum ParamType {
        FLOAT,
        INT,
        BOOL,
        LONG,
        PALETTE,
        ENUM
    }

    public static final class ParamDef {

        public final String key;
        public final ParamType type;
        public final Object defaultValue;
        public final float min;
        public final float max;
        public final String labelKey;
        /** Non-null only for ENUM params. Each entry is a display string (not an i18n key). */
        public final String[] enumOptions;
        /** When false, the port-expose toggle is not shown for this param. */
        public final boolean portExposable;

        public ParamDef(
                String key,
                ParamType type,
                Object defaultValue,
                float min,
                float max,
                String labelKey,
                String[] enumOptions,
                boolean portExposable) {
            this.key = key;
            this.type = type;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.labelKey = labelKey;
            this.enumOptions = enumOptions;
            this.portExposable = portExposable;
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
        paramList.add(new ParamDef(key, ParamType.FLOAT, defaultValue, min, max, labelKey, null, true));
        return this;
    }

    public NodeSchema floatParamNoPort(String key, float defaultValue, float min, float max, String labelKey) {
        paramList.add(new ParamDef(key, ParamType.FLOAT, defaultValue, min, max, labelKey, null, false));
        return this;
    }

    public NodeSchema intParam(String key, int defaultValue, int min, int max, String labelKey) {
        paramList.add(new ParamDef(key, ParamType.INT, defaultValue, (float) min, (float) max, labelKey, null, true));
        return this;
    }

    public NodeSchema intParamNoPort(String key, int defaultValue, int min, int max, String labelKey) {
        paramList.add(new ParamDef(key, ParamType.INT, defaultValue, (float) min, (float) max, labelKey, null, false));
        return this;
    }

    public NodeSchema boolParam(String key, boolean defaultValue, String labelKey) {
        paramList.add(new ParamDef(key, ParamType.BOOL, defaultValue, 0f, 1f, labelKey, null, false));
        return this;
    }

    public NodeSchema longParam(String key, long defaultValue, String labelKey) {
        paramList.add(new ParamDef(key, ParamType.LONG, defaultValue, 0f, 0f, labelKey, null, false));
        return this;
    }

    public NodeSchema paletteParam(String key, List<int[]> defaultValue, String labelKey) {
        paramList.add(
                new ParamDef(key, ParamType.PALETTE, new ArrayList<>(defaultValue), 0f, 0f, labelKey, null, false));
        return this;
    }

    /** Enum param rendered as a combobox. {@code defaultIndex} is the initial selection index. */
    public NodeSchema enumParam(String key, int defaultIndex, String labelKey, String... options) {
        paramList.add(new ParamDef(key, ParamType.ENUM, defaultIndex, 0f, 0f, labelKey, options, false));
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

    /**
     * Returns the full list of input ports for a node instance, including schema-defined ports and
     * any FLOAT ports dynamically exposed from params by the user.
     */
    public static List<InputPortDef> effectiveInputPorts(NodeSchema schema, NodeParams params) {
        Set<String> exposed = params.exposedParamPorts();
        if (exposed.isEmpty()) return schema.inputPorts();
        List<InputPortDef> ports = new ArrayList<>(schema.inputPorts());
        for (String key : exposed) {
            ports.add(new InputPortDef(key, PortType.FLOAT, false));
        }
        return ports;
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
