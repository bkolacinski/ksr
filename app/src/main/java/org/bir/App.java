package org.bir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bir.specs.LengthSpec;
import org.bir.specs.TitleSpec;

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
                new LengthSpec(1.0),
                new TitleSpec(8.0)
        );

        ReutersArticle article1 = new ReutersArticle(List.of("usa"), "test1");
        ReutersArticle article2 = new ReutersArticle(List.of("usa"), "test2");
        ReutersArticle article3 = new ReutersArticle(List.of("usa"), "test3");
        ReutersArticle article4 = new ReutersArticle(List.of("usa"), "test1");
        ReutersArticle article5 = new ReutersArticle(List.of("usa"), "test2");
        ReutersArticle article6 = new ReutersArticle(List.of("usa"), "test3");
        ReutersArticle article7 = new ReutersArticle(List.of("usa"), "test1");
        ReutersArticle article8 = new ReutersArticle(List.of("usa"), "test2");
        ReutersArticle article9 = new ReutersArticle(List.of("usa"), "test3");
        ReutersArticle article10 = new ReutersArticle(List.of("usa"), "test3");
        ReutersArticle article11 = new ReutersArticle(List.of("usa"), "test3");
        ReutersArticle article12 = new ReutersArticle(List.of("usa"), "test3");
        ReutersArticle article13 = new ReutersArticle(List.of("usa"), "test3");

        KnnClassifier classifier = new KnnClassifier(3, specs);

        classifier.train(new FeatureVector(specs, article1), "biznes");
        classifier.train(new FeatureVector(specs, article2), "biznes");
        classifier.train(new FeatureVector(specs, article3), "biznes");

        classifier.train(new FeatureVector(specs, article4), "sport");
        classifier.train(new FeatureVector(specs, article5), "sport");
        classifier.train(new FeatureVector(specs, article6), "sport");

        classifier.train(new FeatureVector(specs, article7), "technologia");
        classifier.train(new FeatureVector(specs, article8), "technologia");
        classifier.train(new FeatureVector(specs, article9), "technologia");

        printPrediction(classifier, "Próbka 1", new FeatureVector(specs, article10), "biznes");
        printPrediction(classifier, "Próbka 2", new FeatureVector(specs, article11), "sport");
        printPrediction(classifier, "Próbka 3", new FeatureVector(specs, article12), "technologia");
        printPrediction(classifier, "Próbka 4", new FeatureVector(specs, article13), "biznes");

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

    private static void runReutersPreview() throws Exception {
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

        stemmed.stream().limit(5).forEach(a ->
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
