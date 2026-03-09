package org.bir;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FeatureVector {
    private final Map<String, Double> numeric = new LinkedHashMap<>();
    private final Map<String, String> text = new LinkedHashMap<>();

    public void addNumeric(String name, double value) {
        numeric.put(name, value);
    }

    public void addText(String name, String value) {
        text.put(name, value);
    }

    public double getNumeric(String name) {
        return numeric.get(name);
    }

    public String getText(String name) {
        return text.get(name);
    }

    public Map<String, Double> numeric() {
        return Collections.unmodifiableMap(numeric);
    }

    public Map<String, String> text() {
        return Collections.unmodifiableMap(text);
    }
}
