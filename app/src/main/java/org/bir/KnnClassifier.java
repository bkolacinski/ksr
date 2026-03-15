package org.bir;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class KnnClassifier {
    private final int k;
    private final List<FeatureSpec> featureSpecs;
    private final List<LabeledSample> trainingData = new ArrayList<>();

    public KnnClassifier(int k, List<FeatureSpec> featureSpecs) {
        if (k <= 0) {
            throw new IllegalArgumentException("k musi być większe od 0");
        }

        this.featureSpecs = List.copyOf(Objects.requireNonNull(featureSpecs, "Lista cech nie może być nullem"));
        if (this.featureSpecs.isEmpty()) {
            throw new IllegalArgumentException("Lista cech nie może być pusta");
        }

        this.k = k;
    }

    public void train(FeatureVector vector, String category) {
        validateVector(vector);

        String normalizedCategory = Objects.requireNonNull(category, "Kategoria nie może być nullem").trim();
        if (normalizedCategory.isEmpty()) {
            throw new IllegalArgumentException("Kategoria nie może być pusta");
        }

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
                        MixedDistance.calculateDistance(vector, sample.vector(), featureSpecs)
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

