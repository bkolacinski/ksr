package org.bir.extractor.specs;

import org.bir.extractor.FeatureSpec;
import org.bir.extractor.FeatureType;
import org.bir.extractor.ReutersArticle;

public class WordCountSpec extends FeatureSpec<Double> {

    public WordCountSpec(Double weight) {
        super(weight);
        this.name = "wordCount";
        this.type = FeatureType.NUMERIC;
    }

    @Override
    public Double calculate(ReutersArticle article) {
        return (double) SpecWordUtils.extractWords(article.getText()).size();
    }
}

