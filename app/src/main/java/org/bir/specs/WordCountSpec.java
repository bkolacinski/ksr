package org.bir.specs;

import org.bir.FeatureSpec;
import org.bir.FeatureType;
import org.bir.ReutersArticle;

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

