package org.bir.knn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.bir.extractor.FeatureSpec;
import org.bir.extractor.specs.AlnumToOtherCharsRatioSpec;
import org.bir.extractor.specs.AverageWordLengthSpec;
import org.bir.extractor.specs.CharCountSpec;
import org.bir.extractor.specs.LexicalDiversitySpec;
import org.bir.extractor.specs.LongWordToOtherWordsRatioSpec;
import org.bir.extractor.specs.ProperNounRatioSpec;
import org.bir.extractor.specs.ProperNounSpec;
import org.bir.extractor.specs.RarestRepeatedWordSpec;
import org.bir.extractor.specs.UpperToAllCharsRatioSpec;
import org.bir.extractor.specs.UpperToLowerRatioSpec;
import org.bir.extractor.specs.WordCountSpec;

public final class KnnFeatureCatalog {
    private static final Map<String, Supplier<FeatureSpec>> REGISTRY = new LinkedHashMap<>();

    static {
        REGISTRY.put("alnumToOtherCharsRatio", () -> new AlnumToOtherCharsRatioSpec(1.0));
        REGISTRY.put("averageWordLength", () -> new AverageWordLengthSpec(1.0));
        REGISTRY.put("charCount", () -> new CharCountSpec(1.0));
        REGISTRY.put("lexicalDiversity", () -> new LexicalDiversitySpec(1.0));
        REGISTRY.put("longWordToOtherWordsRatio", () -> new LongWordToOtherWordsRatioSpec(1.0));
        REGISTRY.put("properNounMidSentenceRatio", () -> new ProperNounRatioSpec(1.0));
        REGISTRY.put("properNounFirst", () -> new ProperNounSpec(1.0));
        REGISTRY.put("rarestRepeatedWord", () -> new RarestRepeatedWordSpec(1.0));
        REGISTRY.put("upperToAllCharsRatio", () -> new UpperToAllCharsRatioSpec(1.0));
        REGISTRY.put("upperToLowerRatio", () -> new UpperToLowerRatioSpec(1.0));
        REGISTRY.put("wordCount", () -> new WordCountSpec(1.0));
    }

    private KnnFeatureCatalog() {
    }

    public static List<String> defaultFeatureIds() {
        return List.copyOf(REGISTRY.keySet());
    }

    public static List<FeatureSpec> buildFeatures(List<String> featureIds) {
        Objects.requireNonNull(featureIds, "Lista cech nie może być nullem");

        List<FeatureSpec> features = new ArrayList<>();
        for (String rawId : featureIds) {
            String id = rawId == null ? "" : rawId.trim();
            Supplier<FeatureSpec> supplier = REGISTRY.get(id);
            if (supplier == null) {
                throw new IllegalArgumentException("Nieznana cecha: " + rawId);
            }

            features.add(supplier.get());
        }

        if (features.isEmpty()) {
            throw new IllegalArgumentException("Lista cech nie może być pusta");
        }

        return List.copyOf(features);
    }
}

