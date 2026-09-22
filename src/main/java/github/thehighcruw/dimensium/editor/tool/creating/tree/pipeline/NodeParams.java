/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.tool.creating.tree.pipeline;

import java.util.HashMap;
import java.util.Map;

public class NodeParams {

    private final Map<String, Object> values = new HashMap<>();

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

    public NodeParams copy() {
        NodeParams copy = new NodeParams();
        copy.values.putAll(values);
        return copy;
    }

    public Map<String, Object> rawValues() {
        return values;
    }
}
