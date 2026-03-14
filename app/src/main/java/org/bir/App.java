package org.bir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class App {

    private static final Set<String> ALLOWED_PLACES = Set.of(
            "west-germany", "usa", "france", "uk", "canada", "japan"
    );

    static void main(String[] args) throws Exception {
        runReutersPreview();
        runKnnDummyTests();
    }

    private static void runKnnDummyTests() {
        System.out.println("=== Testy KNN na dummy danych ===");

        List<FeatureSpec> specs = List.of(
                new FeatureSpec("length", FeatureType.NUMERIC, 1.0),
                new FeatureSpec("keyword", FeatureType.TEXT, 8.0)
        );

        KnnClassifier classifier = new KnnClassifier(3, specs);

        classifier.train(articleVector(120, "market"), "biznes");
        classifier.train(articleVector(110, "market"), "biznes");
        classifier.train(articleVector(135, "stocks"), "biznes");

        classifier.train(articleVector(35, "goal"), "sport");
        classifier.train(articleVector(30, "goal"), "sport");
        classifier.train(articleVector(42, "match"), "sport");

        classifier.train(articleVector(85, "ai"), "technologia");
        classifier.train(articleVector(90, "software"), "technologia");
        classifier.train(articleVector(95, "ai"), "technologia");

        printPrediction(classifier, "Próbka 1", articleVector(118, "market"), "biznes");
        printPrediction(classifier, "Próbka 2", articleVector(38, "goal"), "sport");
        printPrediction(classifier, "Próbka 3", articleVector(92, "ai"), "technologia");
        printPrediction(classifier, "Próbka 4", articleVector(125, "stocks"), "biznes");

        System.out.printf("Dane treningowe w klasyfikatorze: %d%n%n", classifier.trainingSize());
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

    private static FeatureVector articleVector(double length, String keyword) {
        FeatureVector vector = new FeatureVector();
        vector.addNumeric("length", length);
        vector.addText("keyword", keyword);
        return vector;
    }

    private static void runReutersPreview() throws Exception {
        Path dataDir = resolveDataDir("data/reuters21578");
        String pattern = "reut2-*.sgm";

        ReutersParser parser = new ReutersParser();
        List<ReutersArticle> articles = parser.parseDirectory(dataDir, pattern);

        List<ReutersArticle> filtered = articles.stream()
                .filter(a -> a.getPlaces().size() == 1)
                .filter(a -> ALLOWED_PLACES.contains(a.getPlaces().get(0)))
                .toList();

        System.out.printf("Wczytano %d artykułów, po filtracji: %d%n%n", articles.size(), filtered.size());

        Map<String, List<ReutersArticle>> byPlace = filtered.stream()
                .collect(Collectors.groupingBy(a -> a.getPlaces().get(0)));

        filtered.stream().limit(5).forEach(a ->
                System.out.printf("miejsca=%-35s tekst=%s%n",
                        a.getPlaces(), a.getText().substring(0, Math.min(80, a.getText().length()))));

        System.out.println("\nLiczba artykułów per kraj:");
        byPlace.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("  %-15s -> %d%n", e.getKey(), e.getValue().size()));
    }

    private static Path resolveDataDir(String relative) {
        Path candidate = Path.of(relative);
        if (candidate.toFile().isDirectory()) return candidate;
        candidate = Path.of("..").resolve(relative);
        if (candidate.toFile().isDirectory()) return candidate;
        return Path.of(relative);
    }
}
