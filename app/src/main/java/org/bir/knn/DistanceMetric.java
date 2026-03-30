package org.bir.knn;

public enum DistanceMetric {
    MANHATTAN,
    EUCLIDEAN,
    CHEBYSHEV;

    public static DistanceMetric fromString(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return MANHATTAN;
        }

        return DistanceMetric.valueOf(rawValue.trim().toUpperCase());
    }
}

