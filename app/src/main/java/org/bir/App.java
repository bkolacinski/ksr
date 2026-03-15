package org.bir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.bir.specs.LongWordToOtherWordsRatioSpec;
import org.bir.specs.RarestRepeatedWordSpec;
import org.bir.specs.AlnumToOtherCharsRatioSpec;
import org.bir.specs.UpperToAllCharsRatioSpec;
import org.bir.specs.UpperToLowerRatioSpec;

public class App {
    private static final double TRAIN_SPLIT_RATIO = 0.8;
    private static final int K = 5;
    private static final long SPLIT_SEED = 42L;

    private static final Set<String> ALLOWED_PLACES = Set.of(
            "west-germany", "usa", "france", "uk", "canada", "japan"
    );

    static void main(String[] args) throws Exception {
        runFeatureSpecsDummyTests();
        runReutersPreview();
    }

    private static void runFeatureSpecsDummyTests() {
        System.out.println("=== Testy nowych speców na dummy danych ===");

        ReutersArticle sampleA = new ReutersArticle(
                List.of("usa"),
                "ALFA alfa 123 !!! kot kot Dom dom DOM"
        );
        ReutersArticle sampleB = new ReutersArticle(
                List.of("uk"),
                "rynek giełdowy dynamicznie rośnie rośnie szybko"
        );

        List<FeatureSpec> specs = List.of(
                new LongWordToOtherWordsRatioSpec(1.0),
                new AlnumToOtherCharsRatioSpec(1.0),
                new RarestRepeatedWordSpec(1.0),
                new UpperToLowerRatioSpec(1.0),
                new UpperToAllCharsRatioSpec(1.0)
        );

        printSpecsForArticle("Sample A", sampleA, specs);
        printSpecsForArticle("Sample B", sampleB, specs);
        System.out.println();
    }

    private static void printSpecsForArticle(String label, ReutersArticle article, List<FeatureSpec> specs) {
        FeatureVector vector = new FeatureVector(specs, article);

        System.out.println(label + " -> text='" + article.getText() + "'");
        System.out.println("  numeric: " + vector.numeric());
        System.out.println("  text: " + vector.text());
    }

    private static void printPrediction(KnnClassifier classifier, String label, FeatureVector vector, String expectedCategory) {
        String predictedCategory = classifier.test(vector);
        String result = expectedCategory.equals(predictedCategory) ? "OK" : "FAIL";

        System.out.printf(
                "%s -> expected=%s, predicted=%s, wynik=%s, numeric=%s, text=%s%n",
                label,
                expectedCategory,
                predictedCategory,
                result,
                vector.numeric(),
                vector.text()
        );
    }

    private static void runReutersPreview() throws Exception {
        System.out.println("=== KNN na prawdziwych danych Reuters ===");

        Path dataDir = resolveDataDir("data/reuters21578");
        String pattern = "reut2-*.sgm";

        ReutersParser parser = new ReutersParser();
        List<ReutersArticle> articles = parser.parseDirectory(dataDir, pattern);

        List<ReutersArticle> filtered = articles.stream()
                .filter(a -> a.getPlaces().size() == 1)
                .filter(a -> ALLOWED_PLACES.contains(a.getPlaces().get(0)))
                .toList();

        TextParser textParser = new TextParser("org/bir/stoplist.txt");

        System.out.printf("Wczytano %d artykułów, po filtracji: %d%n%n", articles.size(), filtered.size());

        List<ReutersArticle> stopped = textParser.filter(filtered);
        List<ReutersArticle> stemmed = textParser.stem(stopped);

        Map<String, List<ReutersArticle>> byPlace = stemmed.stream()
                .collect(Collectors.groupingBy(a -> a.getPlaces().get(0)));

        List<FeatureSpec> realDataSpecs = List.of(
                new LongWordToOtherWordsRatioSpec(1.0),
                new AlnumToOtherCharsRatioSpec(1.0),
                new RarestRepeatedWordSpec(1.0),
                new UpperToAllCharsRatioSpec(1.0)
        );

        KnnClassifier classifier = new KnnClassifier(K, realDataSpecs);
        List<LabeledVector> testSamples = new ArrayList<>();
        List<String> labelsUsedInSplit = new ArrayList<>();
        Map<String, Integer> trainByLabel = new HashMap<>();
        Map<String, Integer> testByLabel = new HashMap<>();

        System.out.println("Liczba artykułów per kraj (po preprocessingu):");
        byPlace.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("  %-15s -> %d%n", e.getKey(), e.getValue().size()));

        for (Map.Entry<String, List<ReutersArticle>> entry : byPlace.entrySet()) {
            String label = entry.getKey();
            List<ReutersArticle> samples = new ArrayList<>(entry.getValue());
            if (samples.size() < 2) {
                continue;
            }

            // Stratyfikowany split: każda klasa ma dane i w train, i w test.
            Collections.shuffle(samples, new Random(SPLIT_SEED + label.hashCode()));

            int trainCount = (int) Math.floor(samples.size() * TRAIN_SPLIT_RATIO);
            trainCount = Math.max(1, Math.min(trainCount, samples.size() - 1));

            labelsUsedInSplit.add(label);
            trainByLabel.put(label, trainCount);
            testByLabel.put(label, samples.size() - trainCount);

            for (int i = 0; i < trainCount; i++) {
                classifier.train(new FeatureVector(realDataSpecs, samples.get(i)), label);
            }

            for (int i = trainCount; i < samples.size(); i++) {
                testSamples.add(new LabeledVector(new FeatureVector(realDataSpecs, samples.get(i)), label));
            }
        }

        if (classifier.trainingSize() == 0 || testSamples.isEmpty()) {
            System.out.println("Za mało danych do treningu/testu po podziale 80/20.");
            System.out.println();
            return;
        }

        System.out.println("Podział train/test per klasa:");
        labelsUsedInSplit.stream().sorted().forEach(label ->
                System.out.printf("  %-15s train=%d test=%d%n",
                        label,
                        trainByLabel.getOrDefault(label, 0),
                        testByLabel.getOrDefault(label, 0))
        );

        int correct = 0;
        Map<String, Integer> actualCounts = new HashMap<>();
        Map<String, Integer> predictedCounts = new HashMap<>();
        Map<String, Integer> truePositives = new HashMap<>();

        for (LabeledVector testSample : testSamples) {
            String actual = testSample.label();
            String predicted = classifier.test(testSample.vector());

            actualCounts.merge(actual, 1, Integer::sum);
            predictedCounts.merge(predicted, 1, Integer::sum);

            if (actual.equals(predicted)) {
                correct++;
                truePositives.merge(actual, 1, Integer::sum);
            }
        }

        double accuracy = (double) correct / testSamples.size();
        System.out.printf("%nKNN: k=%d, cechy=%d, train=%d, test=%d, accuracy=%.2f%%%n",
                K,
                realDataSpecs.size(),
                classifier.trainingSize(),
                testSamples.size(),
                accuracy * 100.0
        );

        double macroF1 = 0.0;
        List<String> labels = labelsUsedInSplit.stream().sorted().toList();
        System.out.println("Per klasa (precision/recall/f1):");
        for (String label : labels) {
            int tp = truePositives.getOrDefault(label, 0);
            int actualTotal = actualCounts.getOrDefault(label, 0);
            int predictedTotal = predictedCounts.getOrDefault(label, 0);

            double precision = predictedTotal == 0 ? 0.0 : (double) tp / predictedTotal;
            double recall = actualTotal == 0 ? 0.0 : (double) tp / actualTotal;
            double f1 = (precision + recall) == 0.0 ? 0.0 : (2.0 * precision * recall) / (precision + recall);
            macroF1 += f1;

            System.out.printf("  %-15s p=%.2f%% r=%.2f%% f1=%.2f%% (actual=%d, predicted=%d)%n",
                    label,
                    precision * 100.0,
                    recall * 100.0,
                    f1 * 100.0,
                    actualTotal,
                    predictedTotal
            );
        }

        macroF1 /= labels.size();
        System.out.printf("Macro-F1: %.2f%%%n%n", macroF1 * 100.0);
    }

    private record LabeledVector(FeatureVector vector, String label) {
    }

    private static Path resolveDataDir(String relative) {
        Path candidate = Path.of(relative);
        if (candidate.toFile().isDirectory()) return candidate;
        candidate = Path.of("..").resolve(relative);
        if (candidate.toFile().isDirectory()) return candidate;
        return Path.of(relative);
    }
}
