package org.bir.specs;

import org.bir.FeatureSpec;
import org.bir.FeatureType;
import org.bir.ReutersArticle;

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

