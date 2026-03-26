package org.bir.extractor.specs;

import java.util.List;

import org.bir.extractor.FeatureSpec;
import org.bir.extractor.FeatureType;
import org.bir.extractor.ReutersArticle;

public class LongWordToOtherWordsRatioSpec extends FeatureSpec<Double> {
    private static final int DEFAULT_LONG_WORD_MIN_LENGTH = 5;

    public LongWordToOtherWordsRatioSpec(Double weight) {
        super(weight);
        this.name = "longWordToOtherWordsRatio";
        this.type = FeatureType.NUMERIC;
    }

    @Override
    public Double calculate(ReutersArticle article) {
        List<String> words = SpecWordUtils.extractWords(article.getText());
        if (words.isEmpty()) {
            return 0.0;
        }

        int longWords = 0;
        for (String word : words) {
            if (word.length() >= DEFAULT_LONG_WORD_MIN_LENGTH) {
                longWords++;
            }
        }

        int otherWords = words.size() - longWords;
        return SpecWordUtils.safeRatio(longWords, otherWords);
    }
}

