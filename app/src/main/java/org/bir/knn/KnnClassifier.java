package org.bir.knn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.bir.extractor.FeatureSpec;
import org.bir.extractor.FeatureType;
import org.bir.extractor.FeatureVector;

public final class KnnClassifier {
    private final int k;
    private final List<FeatureSpec> featureSpecs;
    private final DistanceMetric distanceMetric;
    private final List<LabeledSample> trainingData = new ArrayList<>();
    private final Map<String, Double> minNumericValues = new HashMap<>();
    private final Map<String, Double> maxNumericValues = new HashMap<>();

    public KnnClassifier(int k, List<FeatureSpec> featureSpecs) {
        this(k, featureSpecs, DistanceMetric.MANHATTAN);
    }

    public KnnClassifier(int k, List<FeatureSpec> featureSpecs, DistanceMetric distanceMetric) {
        if (k <= 0) {
            throw new IllegalArgumentException("k musi być większe od 0");
        }

        this.featureSpecs = List.copyOf(Objects.requireNonNull(featureSpecs, "Lista cech nie może być nullem"));
        if (this.featureSpecs.isEmpty()) {
            throw new IllegalArgumentException("Lista cech nie może być pusta");
        }

        this.k = k;
        this.distanceMetric = Objects.requireNonNull(distanceMetric, "Metryka nie może być nullem");
    }

    public void train(FeatureVector vector, String category) {
        validateVector(vector);

        String normalizedCategory = Objects.requireNonNull(category, "Kategoria nie może być nullem").trim();
        if (normalizedCategory.isEmpty()) {
            throw new IllegalArgumentException("Kategoria nie może być pusta");
        }

        updateNumericRanges(vector);

        trainingData.add(new LabeledSample(vector, normalizedCategory));
    }

    public String test(FeatureVector vector) {
        validateVector(vector);

        if (trainingData.isEmpty()) {
            throw new IllegalStateException("Brak danych treningowych - najpierw wywołaj train()");
        }

        int neighboursToUse = Math.min(k, trainingData.size());
        List<Neighbour> nearestNeighbours = trainingData.stream()
                .map(sample -> new Neighbour(
                        sample.category(),
                        calculateNormalizedDistance(vector, sample.vector())
                ))
                .sorted(Comparator.comparingDouble(Neighbour::distance))
                .limit(neighboursToUse)
                .toList();

        Map<String, VoteSummary> votes = new HashMap<>();
        for (Neighbour neighbour : nearestNeighbours) {
            VoteSummary summary = votes.computeIfAbsent(neighbour.category(), ignored -> new VoteSummary());
            summary.count++;
            summary.totalDistance += neighbour.distance();
        }

        return votes.entrySet().stream()
                .sorted(Comparator
                        .<Map.Entry<String, VoteSummary>>comparingInt(entry -> -entry.getValue().count)
                        .thenComparingDouble(entry -> entry.getValue().totalDistance)
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
    }

    public int trainingSize() {
        return trainingData.size();
    }

    private void updateNumericRanges(FeatureVector vector) {
        for (FeatureSpec spec : featureSpecs) {
            if (spec.getType() != FeatureType.NUMERIC) {
                continue;
            }

            String featureName = spec.getName();
            double value = vector.getNumeric(featureName);

            minNumericValues.merge(featureName, value, Math::min);
            maxNumericValues.merge(featureName, value, Math::max);
        }
    }

    private double calculateNormalizedDistance(FeatureVector left, FeatureVector right) {
        double sum = 0.0;

        for (FeatureSpec spec : featureSpecs) {
            double weight = spec.getWeight();
            double featureDistance;

            if (spec.getType() == FeatureType.NUMERIC) {
                String featureName = spec.getName();
                double leftNorm = normalizeNumericValue(featureName, left.getNumeric(featureName));
                double rightNorm = normalizeNumericValue(featureName, right.getNumeric(featureName));
                featureDistance = Math.abs(leftNorm - rightNorm);
            } else {
                featureDistance = jaccardDistance2Gram(left.getText(spec.getName()), right.getText(spec.getName()));
            }

            double weightedDistance = weight * featureDistance;
            if (distanceMetric == DistanceMetric.EUCLIDEAN) {
                sum += weightedDistance * weightedDistance;
            } else {
                sum += weightedDistance;
            }
        }

        if (distanceMetric == DistanceMetric.EUCLIDEAN) {
            return Math.sqrt(sum);
        }

        return sum;
    }

    private double jaccardDistance2Gram(String left, String right) {
        Set<String> leftNgrams = build2Grams(left);
        Set<String> rightNgrams = build2Grams(right);

        if (leftNgrams.isEmpty() && rightNgrams.isEmpty()) {
            return 0.0;
        }

        Set<String> union = new HashSet<>(leftNgrams);
        union.addAll(rightNgrams);
        if (union.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(leftNgrams);
        intersection.retainAll(rightNgrams);
        double similarity = (double) intersection.size() / union.size();
        return 1.0 - similarity;
    }

    private Set<String> build2Grams(String value) {
        String safe = value == null ? "" : value.trim().toLowerCase();
        if (safe.isEmpty()) {
            return Set.of();
        }

        if (safe.length() == 1) {
            return Set.of(safe);
        }

        Set<String> grams = new HashSet<>();
        for (int i = 0; i < safe.length() - 1; i++) {
            grams.add(safe.substring(i, i + 2));
        }
        return grams;
    }

    private double normalizeNumericValue(String featureName, double value) {
        Double min = minNumericValues.get(featureName);
        Double max = maxNumericValues.get(featureName);

        if (min == null || max == null) {
            return 0.0;
        }

        double range = max - min;
        if (range == 0.0) {
            return 0.0;
        }

        double normalized = (value - min) / range;
        if (normalized < 0.0) {
            return 0.0;
        }
        return Math.min(normalized, 1.0);
    }

    private void validateVector(FeatureVector vector) {
        Objects.requireNonNull(vector, "Wektor cech nie może być nullem");
        for (FeatureSpec spec : featureSpecs) {
            switch (spec.getType()) {
                case NUMERIC -> {
                    if (!vector.numeric().containsKey(spec.getName())) {
                        throw new IllegalArgumentException("Brakuje cechy numerycznej: " + spec.getName());
                    }
                }
                case TEXT -> {
                    if (!vector.text().containsKey(spec.getName())) {
                        throw new IllegalArgumentException("Brakuje cechy tekstowej: " + spec.getName());
                    }
                }
            }
        }
    }

    private record LabeledSample(FeatureVector vector, String category) {
    }

    private record Neighbour(String category, double distance) {
    }

    private static final class VoteSummary {
        private int count;
        private double totalDistance;
    }
}
