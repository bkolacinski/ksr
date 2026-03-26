package org.bir.extractor.specs;

import java.util.List;

import org.bir.extractor.FeatureSpec;
import org.bir.extractor.FeatureType;
import org.bir.extractor.ReutersArticle;

public class AverageWordLengthSpec extends FeatureSpec<Double> {

    public AverageWordLengthSpec(Double weight) {
        super(weight);
        this.name = "averageWordLength";
        this.type = FeatureType.NUMERIC;
    }

    @Override
    public Double calculate(ReutersArticle article) {
        List<String> words = SpecWordUtils.extractWords(article.getText());
        if (words.isEmpty()) {
            return 0.0;
        }

        int totalLength = 0;
        for (String word : words) {
            totalLength += word.length();
        }

        return (double) totalLength / words.size();
    }
}

