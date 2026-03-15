package org.bir.specs;

import org.bir.FeatureSpec;
import org.bir.FeatureType;
import org.bir.ReutersArticle;

import java.util.List;

public class ProperNounRatioSpec extends FeatureSpec<Double> {

    public ProperNounRatioSpec(Double weight) {
        super(weight);
        this.name = "properNounMidSentenceRatio";
        this.type = FeatureType.NUMERIC;
    }

    @Override
    public Double calculate(ReutersArticle article) {
        String text = article.getText();
        List<String> words = SpecWordUtils.extractWords(text);
        if (words.isEmpty()) {
            return 0.0;
        }

        int properNouns = SpecWordUtils.countProperNounsInsideSentence(text);
        return (double) properNouns / words.size();
    }
}

