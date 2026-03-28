package org.bir.knn;

import java.util.List;
import java.util.Objects;

public record KnnRunConfig(
        int k,
        List<String> featureIds,
        double testSizePercent,
        DistanceMetric metric,
        long splitSeed
) {
    public KnnRunConfig {
        if (k <= 0) {
            throw new IllegalArgumentException("k musi być większe od 0");
        }

        featureIds = List.copyOf(Objects.requireNonNull(featureIds, "Lista cech nie może być nullem"));
        if (featureIds.isEmpty()) {
            throw new IllegalArgumentException("Lista cech nie może być pusta");
        }

        if (testSizePercent <= 0.0 || testSizePercent >= 100.0) {
            throw new IllegalArgumentException("testSizePercent musi być w zakresie (0, 100)");
        }

        metric = Objects.requireNonNull(metric, "Metryka nie może być nullem");
    }

    public double testRatio() {
        return testSizePercent / 100.0;
    }
}

