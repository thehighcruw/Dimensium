/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NodeParams {

    private final Map<String, Object> values = new HashMap<>();
    private final Set<String> exposedPorts = new LinkedHashSet<>();

    public void set(String key, Object value) {
        values.put(key, value);
    }

    public float getFloat(String key, float defaultValue) {
        Object value = values.get(key);
        return value instanceof Number ? ((Number) value).floatValue() : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        Object value = values.get(key);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    public boolean getBool(String key, boolean defaultValue) {
        Object value = values.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    public long getLong(String key, long defaultValue) {
        Object value = values.get(key);
        return value instanceof Number ? ((Number) value).longValue() : defaultValue;
    }

    public String getString(String key, String defaultValue) {
        Object value = values.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public List<int[]> getPalette(String key, List<int[]> defaultValue) {
        Object value = values.get(key);
        return value instanceof List ? (List<int[]>) value : defaultValue;
    }

    public void setPortExposed(String key, boolean exposed) {
        if (exposed) {
            exposedPorts.add(key);
        } else {
            exposedPorts.remove(key);
        }
    }

    public boolean isPortExposed(String key) {
        return exposedPorts.contains(key);
    }

    public Set<String> exposedParamPorts() {
        return Collections.unmodifiableSet(exposedPorts);
    }

    public NodeParams copy() {
        NodeParams copy = new NodeParams();
        copy.values.putAll(values);
        copy.exposedPorts.addAll(exposedPorts);
        return copy;
    }

    public Map<String, Object> rawValues() {
        return values;
    }
}
