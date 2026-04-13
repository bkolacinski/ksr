package org.bir.knn;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.bir.extractor.FeatureVector;

public final class RunWithCalculationsRunner {
    private RunWithCalculationsRunner() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> params = KnnCliRunner.parseArgs(args);

        int k = Integer.parseInt(params.getOrDefault("k", "5"));
        double testSizePercent = Double.parseDouble(params.getOrDefault("testPercent", "20"));
        long seed = Long.parseLong(params.getOrDefault("seed", "42"));
        DistanceMetric metric = DistanceMetric.fromString(params.get("metric"));

        List<String> featureIds = params.containsKey("features")
                ? KnnCliRunner.parseList(params.get("features"))
                : KnnFeatureCatalog.defaultFeatureIds();

        String outPath = params.getOrDefault("out", "").trim();
        String format = params.getOrDefault("format", "txt").trim().toLowerCase();
        boolean printToConsole = Boolean.parseBoolean(params.getOrDefault("stdout", "true"));

        KnnExperimentService service = new KnnExperimentService();
        KnnExperimentService.PreparedDataset dataset = service.prepareDataset(featureIds, testSizePercent, seed);

        MetricsReport report = evaluate(dataset, k, metric);
        String rendered = "csv".equals(format) ? report.toCsv() : report.toText();

        if (printToConsole) {
            System.out.println(rendered);
        }

        if (!outPath.isEmpty()) {
            Path outputFile = Path.of(outPath);
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputFile, rendered + System.lineSeparator(), StandardCharsets.UTF_8);
            System.out.println("Zapisano raport do: " + outputFile.toAbsolutePath());
        }
    }

    private static MetricsReport evaluate(KnnExperimentService.PreparedDataset dataset, int k, DistanceMetric metric)
            throws Exception {
        KnnClassifier classifier = new KnnClassifier(k, dataset.featureSpecs(), metric);

        for (Object trainSample : dataset.trainSamples()) {
            classifier.train(extractVector(trainSample), extractLabel(trainSample));
        }

        List<Prediction> predictions = new ArrayList<>();
        for (Object testSample : dataset.testSamples()) {
            String expected = extractLabel(testSample);
            String predicted = classifier.test(extractVector(testSample));
            predictions.add(new Prediction(expected, predicted));
        }

        return MetricsReport.from(predictions, k, metric, dataset.testSizePercent(), dataset.featureIds());
    }

    private static FeatureVector extractVector(Object labeledSample) throws Exception {
        Method vector = labeledSample.getClass().getDeclaredMethod("vector");
        vector.setAccessible(true);
        return (FeatureVector) vector.invoke(labeledSample);
    }

    private static String extractLabel(Object labeledSample) throws Exception {
        Method label = labeledSample.getClass().getDeclaredMethod("label");
        label.setAccessible(true);
        return (String) label.invoke(labeledSample);
    }

    private record Prediction(String expected, String predicted) {
    }

    private record PerClassMetrics(double precision, double recall, double f1, int support) {
    }

    private record MetricsReport(
            int k,
            DistanceMetric metric,
            double testPercent,
            int sampleCount,
            List<String> featureIds,
            double accuracy,
            double macroF1,
            Map<String, PerClassMetrics> perClass
    ) {
        static MetricsReport from(
                List<Prediction> predictions,
                int k,
                DistanceMetric metric,
                double testPercent,
                List<String> featureIds
        ) {
            if (predictions.isEmpty()) {
                throw new IllegalStateException("Brak predykcji do wyliczenia metryk");
            }

            int correct = 0;
            Map<String, Integer> tp = new LinkedHashMap<>();
            Map<String, Integer> fp = new LinkedHashMap<>();
            Map<String, Integer> fn = new LinkedHashMap<>();
            Map<String, Integer> support = new LinkedHashMap<>();
            TreeSet<String> labels = new TreeSet<>();

            for (Prediction prediction : predictions) {
                labels.add(prediction.expected());
                labels.add(prediction.predicted());

                support.merge(prediction.expected(), 1, Integer::sum);

                if (prediction.expected().equals(prediction.predicted())) {
                    correct++;
                    tp.merge(prediction.expected(), 1, Integer::sum);
                } else {
                    fp.merge(prediction.predicted(), 1, Integer::sum);
                    fn.merge(prediction.expected(), 1, Integer::sum);
                }
            }

            Map<String, PerClassMetrics> perClass = new LinkedHashMap<>();
            double f1Sum = 0.0;
            for (String label : labels) {
                int tpCount = tp.getOrDefault(label, 0);
                int fpCount = fp.getOrDefault(label, 0);
                int fnCount = fn.getOrDefault(label, 0);

                double precision = safeDivide(tpCount, tpCount + fpCount);
                double recall = safeDivide(tpCount, tpCount + fnCount);
                double f1 = (precision + recall) == 0.0 ? 0.0 : (2.0 * precision * recall) / (precision + recall);

                f1Sum += f1;
                perClass.put(label, new PerClassMetrics(precision, recall, f1, support.getOrDefault(label, 0)));
            }

            double accuracy = safeDivide(correct, predictions.size());
            double macroF1 = f1Sum / labels.size();

            return new MetricsReport(
                    k,
                    metric,
                    testPercent,
                    predictions.size(),
                    List.copyOf(featureIds),
                    accuracy,
                    macroF1,
                    Map.copyOf(perClass)
            );
        }

        String toText() {
            StringBuilder sb = new StringBuilder();
            sb.append("k=").append(k)
                    .append(", metric=").append(metric)
                    .append(", test=").append(String.format("%.2f", testPercent)).append("%")
                    .append(", features=").append(featureIds.size())
                    .append(", samples=").append(sampleCount)
                    .append(System.lineSeparator());
            sb.append("accuracy=").append(format4(accuracy))
                    .append(", macroF1=").append(format4(macroF1))
                    .append(System.lineSeparator());
            sb.append("class,precision,recall,f1,support").append(System.lineSeparator());

            for (Map.Entry<String, PerClassMetrics> entry : perClass.entrySet()) {
                PerClassMetrics metrics = entry.getValue();
                sb.append(entry.getKey()).append(',')
                        .append(format4(metrics.precision())).append(',')
                        .append(format4(metrics.recall())).append(',')
                        .append(format4(metrics.f1())).append(',')
                        .append(metrics.support())
                        .append(System.lineSeparator());
            }

            return sb.toString().trim();
        }

        String toCsv() {
            StringBuilder sb = new StringBuilder();
            sb.append("k,metric,test_size_pct,features,samples,accuracy,macro_f1").append(System.lineSeparator());
            sb.append(k).append(',')
                    .append(metric).append(',')
                    .append(String.format("%.2f", testPercent)).append(',')
                    .append(featureIds.size()).append(',')
                    .append(sampleCount).append(',')
                    .append(format4(accuracy)).append(',')
                    .append(format4(macroF1))
                    .append(System.lineSeparator());
            sb.append(System.lineSeparator());
            sb.append("class,precision,recall,f1,support").append(System.lineSeparator());
            for (Map.Entry<String, PerClassMetrics> entry : perClass.entrySet()) {
                PerClassMetrics metrics = entry.getValue();
                sb.append(entry.getKey()).append(',')
                        .append(format4(metrics.precision())).append(',')
                        .append(format4(metrics.recall())).append(',')
                        .append(format4(metrics.f1())).append(',')
                        .append(metrics.support())
                        .append(System.lineSeparator());
            }
            return sb.toString().trim();
        }

        private static String format4(double value) {
            return String.format("%.4f", value);
        }

        private static double safeDivide(double numerator, double denominator) {
            if (denominator == 0.0) {
                return 0.0;
            }
            return numerator / denominator;
        }
    }
}


