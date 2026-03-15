package org.bir.specs;

import org.bir.FeatureSpec;
import org.bir.FeatureType;
import org.bir.ReutersArticle;

public class AlnumToOtherCharsRatioSpec extends FeatureSpec<Double> {

    public AlnumToOtherCharsRatioSpec(Double weight) {
        super(weight);
        this.name = "alnumToOtherCharsRatio";
        this.type = FeatureType.NUMERIC;
    }

    @Override
    public Double calculate(ReutersArticle article) {
        String text = article.getText();
        if (text == null || text.isEmpty()) {
            return 0.0;
        }

        int alnum = SpecWordUtils.countAlphanumericChars(text);
        int other = text.length() - alnum;
        return SpecWordUtils.safeRatio(alnum, other);
    }
}

