package org.bir.specs;

import org.bir.FeatureSpec;
import org.bir.FeatureType;
import org.bir.ReutersArticle;

public class UpperToLowerRatioSpec extends FeatureSpec<Double> {

    public UpperToLowerRatioSpec(Double weight) {
        super(weight);
        this.name = "upperToLowerRatio";
        this.type = FeatureType.NUMERIC;
    }

    @Override
    public Double calculate(ReutersArticle article) {
        String text = article.getText();
        int uppercase = SpecWordUtils.countUppercaseChars(text);
        int lowercase = SpecWordUtils.countLowercaseChars(text);
        return SpecWordUtils.safeRatio(uppercase, lowercase);
    }
}

