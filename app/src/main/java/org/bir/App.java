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

    static void main() throws Exception {
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
