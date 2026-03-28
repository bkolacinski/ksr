package org.bir.knn;

import java.util.List;

public record ExperimentResult(
        int k,
        double testSizePercent,
        DistanceMetric metric,
        List<String> featureIds,
        double accuracy
) {
}

