package org.bir.knn;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;

public final class CsvResultWriter {
    public void write(Path outputPath, List<ExperimentResult> results) throws IOException {
        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(
                outputPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            writer.write("k,test_size_pct,metric,features,accuracy");
            writer.newLine();

            for (ExperimentResult result : results) {
                writer.write(String.format(
                        Locale.US,
                        "%d,%.2f,%s,%s,%.6f",
                        result.k(),
                        result.testSizePercent(),
                        result.metric(),
                        escapeCsv(String.join("|", result.featureIds())),
                        result.accuracy()
                ));
                writer.newLine();
            }
        }
    }

    private String escapeCsv(String input) {
        if (input.contains(",") || input.contains("\"") || input.contains("\n")) {
            return "\"" + input.replace("\"", "\"\"") + "\"";
        }
        return input;
    }
}

