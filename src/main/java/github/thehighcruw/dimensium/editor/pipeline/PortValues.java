/*
 * Copyright (c) 2026 TheHighcruw
 * SPDX-License-Identifier: MIT
 */
package github.thehighcruw.dimensium.editor.pipeline;

import java.util.HashMap;
import java.util.Map;

public final class PortValues {

    private final Map<String, Object> values = new HashMap<>();

    public void set(String port, Object value) {
        values.put(port, value);
    }

    public boolean has(String port) {
        return values.containsKey(port);
    }

    public <T> T get(String port, Class<T> type) {
        Object value = values.get(port);
        if (value == null) return null;
        return type.cast(value);
    }

    public Object getRaw(String port) {
        return values.get(port);
    }
}
