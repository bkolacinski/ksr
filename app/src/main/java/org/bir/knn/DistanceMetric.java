package org.bir.knn;

public enum DistanceMetric {
    MANHATTAN,
    EUCLIDEAN;

    public static DistanceMetric fromString(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return MANHATTAN;
        }

        return DistanceMetric.valueOf(rawValue.trim().toUpperCase());
    }
}

