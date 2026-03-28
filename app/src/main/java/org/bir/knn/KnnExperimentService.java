package org.bir.knn;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.bir.extractor.FeatureSpec;
import org.bir.extractor.FeatureVector;
import org.bir.extractor.ReutersArticle;
import org.bir.extractor.ReutersParser;
import org.bir.extractor.TextParser;

public final class KnnExperimentService {
    private static final Set<String> ALLOWED_PLACES = Set.of(
            "west-germany", "usa", "france", "uk", "canada", "japan"
    );

    private final Map<String, List<ReutersArticle>> byPlace;

    public KnnExperimentService() throws Exception {
        this(resolveDataDir("data/reuters21578"));
    }

    public KnnExperimentService(Path dataDir) throws Exception {
        ReutersParser parser = new ReutersParser();
        List<ReutersArticle> articles = parser.parseDirectory(dataDir, "reut2-*.sgm");

        List<ReutersArticle> filtered = articles.stream()
                .filter(a -> a.getPlaces().size() == 1)
                .filter(a -> ALLOWED_PLACES.contains(a.getPlaces().getFirst()))
                .toList();

        TextParser textParser = new TextParser("org/bir/stoplist.txt");
        List<ReutersArticle> stopped = textParser.filter(filtered);
        List<ReutersArticle> stemmed = textParser.stem(stopped);

        byPlace = stemmed.stream()
                .collect(Collectors.groupingBy(a -> a.getPlaces().getFirst()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    public ExperimentResult run(KnnRunConfig config) {
        PreparedDataset preparedDataset = prepareDataset(config.featureIds(), config.testSizePercent(), config.splitSeed());
        return evaluate(preparedDataset, config.k(), config.metric());
    }

    public PreparedDataset prepareDataset(List<String> featureIds, double testSizePercent, long splitSeed) {
        List<FeatureSpec> featureSpecs = KnnFeatureCatalog.buildFeatures(featureIds);
        List<LabeledVector> trainSamples = new ArrayList<>();
        List<LabeledVector> testSamples = new ArrayList<>();

        for (Map.Entry<String, List<ReutersArticle>> entry : byPlace.entrySet()) {
            String label = entry.getKey();
            List<ReutersArticle> samples = new ArrayList<>(entry.getValue());
            if (samples.size() < 2) {
                continue;
            }

            Collections.shuffle(samples, new Random(splitSeed + label.hashCode()));

            int testCount = (int) Math.round(samples.size() * (testSizePercent / 100.0));
            testCount = Math.max(1, Math.min(testCount, samples.size() - 1));
            int trainCount = samples.size() - testCount;

            for (int i = 0; i < trainCount; i++) {
                trainSamples.add(new LabeledVector(new FeatureVector(featureSpecs, samples.get(i)), label));
            }

            for (int i = trainCount; i < samples.size(); i++) {
                testSamples.add(new LabeledVector(new FeatureVector(featureSpecs, samples.get(i)), label));
            }
        }

        if (testSamples.isEmpty()) {
            throw new IllegalStateException("Brak próbek testowych po podziale danych");
        }

        return new PreparedDataset(
                List.copyOf(featureIds),
                testSizePercent,
                List.copyOf(featureSpecs),
                List.copyOf(trainSamples),
                List.copyOf(testSamples)
        );
    }

    public ExperimentResult evaluate(PreparedDataset dataset, int k, DistanceMetric metric) {
        KnnClassifier classifier = new KnnClassifier(k, dataset.featureSpecs(), metric);

        for (LabeledVector sample : dataset.trainSamples()) {
            classifier.train(sample.vector(), sample.label());
        }

        int correct = 0;
        for (LabeledVector sample : dataset.testSamples()) {
            if (sample.label().equals(classifier.test(sample.vector()))) {
                correct++;
            }
        }

        double accuracy = (double) correct / dataset.testSamples().size();
        return new ExperimentResult(
                k,
                dataset.testSizePercent(),
                metric,
                dataset.featureIds(),
                accuracy
        );
    }

    private record LabeledVector(FeatureVector vector, String label) {
    }

    public record PreparedDataset(
            List<String> featureIds,
            double testSizePercent,
            List<FeatureSpec> featureSpecs,
            List<LabeledVector> trainSamples,
            List<LabeledVector> testSamples
    ) {
    }

    private static Path resolveDataDir(String relative) {
        Path candidate = Path.of(relative);
        if (candidate.toFile().isDirectory()) {
            return candidate;
        }

        candidate = Path.of("..").resolve(relative);
        if (candidate.toFile().isDirectory()) {
            return candidate;
        }

        return Path.of(relative);
    }
}


