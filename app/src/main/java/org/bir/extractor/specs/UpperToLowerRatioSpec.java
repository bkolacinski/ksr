package org.bir.extractor.specs;

import org.bir.extractor.FeatureSpec;
import org.bir.extractor.FeatureType;
import org.bir.extractor.ReutersArticle;

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

