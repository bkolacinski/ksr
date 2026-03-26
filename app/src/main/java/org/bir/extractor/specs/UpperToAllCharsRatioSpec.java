package org.bir.extractor.specs;

import org.bir.extractor.FeatureSpec;
import org.bir.extractor.FeatureType;
import org.bir.extractor.ReutersArticle;

public class UpperToAllCharsRatioSpec extends FeatureSpec<Double> {

    public UpperToAllCharsRatioSpec(Double weight) {
        super(weight);
        this.name = "upperToAllCharsRatio";
        this.type = FeatureType.NUMERIC;
    }

    @Override
    public Double calculate(ReutersArticle article) {
        String text = article.getText();
        if (text == null || text.isEmpty()) {
            return 0.0;
        }

        int uppercase = SpecWordUtils.countUppercaseChars(text);
        return SpecWordUtils.safeRatio(uppercase, text.length());
    }
}

