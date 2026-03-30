package org.bir.knn;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public final class KnnAutomatedTestRunner {
    private static final Comparator<ExperimentResult> RESULT_ORDER =
            Comparator.comparingDouble(ExperimentResult::accuracy).reversed()
                    .thenComparingInt(ExperimentResult::k)
                    .thenComparingInt(result -> result.featureIds().size())
                    .thenComparingInt(result -> metricRank(result.metric()))
                    .thenComparingDouble(ExperimentResult::testSizePercent);
    private static final Object LOG_LOCK = new Object();

    private KnnAutomatedTestRunner() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> params = KnnCliRunner.parseArgs(args);
        int threads = Integer.parseInt(params.getOrDefault("threads", "4"));
        String scenario = params.getOrDefault("scenario", "all").toLowerCase(Locale.ROOT);
        int maxK = Integer.parseInt(params.getOrDefault("maxK", "200"));
        int fixedK = Integer.parseInt(params.getOrDefault("k", "5"));
        long seed = Long.parseLong(params.getOrDefault("seed", "42"));
        int maxFeatureDrop = Integer.parseInt(params.getOrDefault("maxFeatureDrop", "4"));
        double testPercent = Double.parseDouble(params.getOrDefault("testPercent", "20"));

        List<String> features = params.containsKey("features")
                ? KnnCliRunner.parseList(params.get("features"))
                : KnnFeatureCatalog.defaultFeatureIds();

        List<DistanceMetric> metrics = parseMetrics(params);
        logProgress("main", "Start: scenario=%s, threads=%d".formatted(scenario, threads));
        logProgress("main", "Ladowanie i preprocessing Reuters... to moze chwile potrwac");
        KnnExperimentService service = new KnnExperimentService();
        logProgress("main", "Dane gotowe, start scenariuszy");

        List<Callable<ScenarioRun>> tasks = new ArrayList<>();
        if ("all".equals(scenario) || "ksweep".equals(scenario)) {
            tasks.add(() -> runKSweep(service, metrics, features, testPercent, maxK, seed, threads));
        }
        if ("all".equals(scenario) || "splitsweep".equals(scenario)) {
            tasks.add(() -> runSplitSweep(service, metrics, features, fixedK, seed));
        }
        if ("all".equals(scenario) || "featuresweep".equals(scenario)) {
            tasks.add(() -> runFeatureSweep(service, metrics, features, fixedK, testPercent, maxFeatureDrop, seed));
        }

        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("Nieznany scenariusz: " + scenario);
        }

        List<ScenarioRun> runs = executeInParallel(tasks, threads);

        List<ExperimentResult> allResults = runs.stream()
                .flatMap(run -> run.results().stream())
                .toList();

        Path outputPath = Path.of(params.getOrDefault("out", "knn-results.csv"));
        new CsvResultWriter().write(outputPath, allResults);
        logProgress("main", "Zapisano wyniki do: " + outputPath.toAbsolutePath());

        for (ScenarioRun run : runs) {
            ExperimentResult best = run.best();
            if (best == null) {
                continue;
            }

            System.out.printf(
                    "%s -> best: k=%d, test=%.2f%%, metric=%s, features=%d, accuracy=%.6f%n",
                    run.name(),
                    best.k(),
                    best.testSizePercent(),
                    best.metric(),
                    best.featureIds().size(),
                    best.accuracy()
            );
        }
    }

    private static List<DistanceMetric> parseMetrics(Map<String, String> params) {
        if (params.containsKey("metrics")) {
            return KnnCliRunner.parseList(params.get("metrics")).stream()
                    .map(DistanceMetric::fromString)
                    .toList();
        }

        return List.of(DistanceMetric.fromString(params.get("metric")));
    }

    private static ScenarioRun runKSweep(
            KnnExperimentService service,
            List<DistanceMetric> metrics,
            List<String> features,
            double testPercent,
            int maxK,
            long seed,
            int threads
    ) {
        logProgress("kSweep", "Start: metrics=%d, maxK=%d".formatted(metrics.size(), maxK));
        List<ExperimentResult> results = new ArrayList<>();
        AtomicInteger iteration = new AtomicInteger(0);
        KnnExperimentService.PreparedDataset preparedDataset = service.prepareDataset(features, testPercent, seed);
        logProgress(
                "kSweep",
                "Prepared dataset: train=%d, test=%d"
                        .formatted(preparedDataset.trainSamples().size(), preparedDataset.testSamples().size())
        );

        int poolSize = Math.max(1, Math.min(threads, metrics.size()));
        ExecutorService metricExecutor = Executors.newFixedThreadPool(poolSize);
        try {
            List<Callable<List<ExperimentResult>>> metricTasks = metrics.stream()
                    .<Callable<List<ExperimentResult>>>map(metric -> () -> runKSweepForMetric(
                            service,
                            preparedDataset,
                            metric,
                            maxK,
                            iteration
                    ))
                    .toList();

            List<Future<List<ExperimentResult>>> futures = metricExecutor.invokeAll(metricTasks);
            for (Future<List<ExperimentResult>> future : futures) {
                results.addAll(future.get());
            }
        } catch (ExecutionException e) {
            throw new RuntimeException("Blad podczas rownoleglego ksweep", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Przerwano ksweep", e);
        } finally {
            metricExecutor.shutdownNow();
        }

        return new ScenarioRun("kSweep", results, results.stream().min(RESULT_ORDER).orElse(null));
    }

    private static List<ExperimentResult> runKSweepForMetric(
            KnnExperimentService service,
            KnnExperimentService.PreparedDataset preparedDataset,
            DistanceMetric metric,
            int maxK,
            AtomicInteger iteration
    ) {
        List<ExperimentResult> metricResults = new ArrayList<>();
        int worseStreak = 0;
        Double previousAccuracy = null;

        for (int k = 1; k <= maxK; k++) {
            int currentIteration = iteration.incrementAndGet();
            logProgress("kSweep", "iter=%d START metric=%s k=%d".formatted(currentIteration, metric, k));
            ExperimentResult result = service.evaluate(preparedDataset, k, metric);
            metricResults.add(result);
            logProgress(
                    "kSweep",
                    "iter=%d metric=%s k=%d accuracy=%.6f worseStreak=%d"
                            .formatted(currentIteration, metric, k, result.accuracy(), worseStreak)
            );

            if (previousAccuracy != null && result.accuracy() < previousAccuracy) {
                worseStreak++;
            } else {
                worseStreak = 0;
            }

            previousAccuracy = result.accuracy();
            if (worseStreak >= 3) {
                logProgress("kSweep", "stop dla metric=%s po 3 kolejnych pogorszeniach".formatted(metric));
                break;
            }
        }

        return metricResults;
    }

    private static ScenarioRun runSplitSweep(
            KnnExperimentService service,
            List<DistanceMetric> metrics,
            List<String> features,
            int k,
            long seed
    ) {
        logProgress("splitSweep", "Start: metrics=%d, train od 10%% do 90%% co 5%%".formatted(metrics.size()));
        List<ExperimentResult> results = new ArrayList<>();
        int total = metrics.size() * 17;
        AtomicInteger done = new AtomicInteger(0);

        for (DistanceMetric metric : metrics) {
            for (int trainPercent = 10; trainPercent <= 90; trainPercent += 5) {
                double testPercent = 100.0 - trainPercent;
                ExperimentResult result = service.run(new KnnRunConfig(k, features, testPercent, metric, seed));
                results.add(result);
                logProgress(
                        "splitSweep",
                        "%d/%d metric=%s train=%d%% test=%.2f%% accuracy=%.6f"
                                .formatted(done.incrementAndGet(), total, metric, trainPercent, testPercent, result.accuracy())
                );
            }
        }

        return new ScenarioRun("splitSweep", results, results.stream().min(RESULT_ORDER).orElse(null));
    }

    private static ScenarioRun runFeatureSweep(
            KnnExperimentService service,
            List<DistanceMetric> metrics,
            List<String> features,
            int k,
            double testPercent,
            int maxFeatureDrop,
            long seed
    ) {
        logProgress(
                "featureSweep",
                "Start: metrics=%d, maxFeatureDrop=%d, inputFeatures=%d".formatted(metrics.size(), maxFeatureDrop, features.size())
        );
        List<ExperimentResult> results = new ArrayList<>();
        int dropLimit = Math.min(maxFeatureDrop, features.size() - 1);
        int subsetsPerMetric = 0;
        for (int dropCount = 0; dropCount <= dropLimit; dropCount++) {
            subsetsPerMetric += combinationsCount(features.size(), dropCount);
        }

        int total = subsetsPerMetric * metrics.size();
        AtomicInteger done = new AtomicInteger(0);

        for (DistanceMetric metric : metrics) {
            for (int dropCount = 0; dropCount <= dropLimit; dropCount++) {
                List<Set<Integer>> removalSets = new ArrayList<>();
                collectCombinations(features.size(), dropCount, 0, new HashSet<>(), removalSets);

                for (Set<Integer> removed : removalSets) {
                    List<String> subset = new ArrayList<>();
                    for (int i = 0; i < features.size(); i++) {
                        if (!removed.contains(i)) {
                            subset.add(features.get(i));
                        }
                    }

                    ExperimentResult result = service.run(new KnnRunConfig(k, subset, testPercent, metric, seed));
                    results.add(result);
                    logProgress(
                            "featureSweep",
                            "%d/%d metric=%s removed=%d features=%d accuracy=%.6f"
                                    .formatted(done.incrementAndGet(), total, metric, dropCount, subset.size(), result.accuracy())
                    );
                }
            }
        }

        return new ScenarioRun("featureSweep", results, results.stream().min(RESULT_ORDER).orElse(null));
    }

    private static void collectCombinations(
            int n,
            int targetSize,
            int start,
            Set<Integer> current,
            List<Set<Integer>> output
    ) {
        if (current.size() == targetSize) {
            output.add(Set.copyOf(current));
            return;
        }

        for (int i = start; i < n; i++) {
            current.add(i);
            collectCombinations(n, targetSize, i + 1, current, output);
            current.remove(i);
        }
    }

    private static int combinationsCount(int n, int k) {
        if (k < 0 || k > n) {
            return 0;
        }

        if (k == 0 || k == n) {
            return 1;
        }

        long result = 1;
        int effectiveK = Math.min(k, n - k);
        for (int i = 1; i <= effectiveK; i++) {
            result = result * (n - effectiveK + i) / i;
        }
        return (int) result;
    }

    private static List<ScenarioRun> executeInParallel(List<Callable<ScenarioRun>> tasks, int threads)
            throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<ScenarioRun>> futures = executor.invokeAll(tasks);
            List<ScenarioRun> results = new ArrayList<>();
            for (Future<ScenarioRun> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private static int metricRank(DistanceMetric metric) {
        return switch (metric) {
            case MANHATTAN -> 0;
            case EUCLIDEAN -> 1;
            case CHEBYSHEV -> 2;
        };
    }

    private static void logProgress(String scope, String message) {
        synchronized (LOG_LOCK) {
            System.out.printf("[%s] %s%n", scope, message);
            System.out.flush();
        }
    }

    private record ScenarioRun(String name, List<ExperimentResult> results, ExperimentResult best) {
    }
}






