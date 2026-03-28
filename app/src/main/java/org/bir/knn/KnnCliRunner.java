package org.bir.knn;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class KnnCliRunner {
    private KnnCliRunner() {
    }

    public static void run(String[] args) throws Exception {
        Map<String, String> params = parseArgs(args);

        int k = Integer.parseInt(params.getOrDefault("k", "5"));
        double testSizePercent = Double.parseDouble(params.getOrDefault("testPercent", "20"));
        long seed = Long.parseLong(params.getOrDefault("seed", "42"));
        DistanceMetric metric = DistanceMetric.fromString(params.get("metric"));

        List<String> featureIds = params.containsKey("features")
                ? parseList(params.get("features"))
                : KnnFeatureCatalog.defaultFeatureIds();

        KnnRunConfig config = new KnnRunConfig(k, featureIds, testSizePercent, metric, seed);
        KnnExperimentService service = new KnnExperimentService();
        ExperimentResult result = service.run(config);

        System.out.printf(
                "k=%d, test=%.2f%%, metric=%s, features=%d, accuracy=%.4f%n",
                result.k(),
                result.testSizePercent(),
                result.metric(),
                result.featureIds().size(),
                result.accuracy()
        );
    }

    static Map<String, String> parseArgs(String[] args) {
        return Arrays.stream(args)
                .filter(arg -> arg.startsWith("--") && arg.contains("="))
                .map(arg -> arg.substring(2).split("=", 2))
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1], (left, right) -> right));
    }

    static List<String> parseList(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
    }
}

