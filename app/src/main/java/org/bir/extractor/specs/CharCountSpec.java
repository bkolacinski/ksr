package org.bir.extractor.specs;

import org.bir.extractor.FeatureSpec;
import org.bir.extractor.FeatureType;
import org.bir.extractor.ReutersArticle;

public class CharCountSpec extends FeatureSpec<Double> {

    public CharCountSpec(Double weight) {
        super(weight);
        this.name = "Character Count";
        this.type = FeatureType.NUMERIC;
    }

    @Override
    public Double calculate(ReutersArticle article) {
        return (double) article.getText().replaceAll("\\s+", "").length();
    }
    
}
