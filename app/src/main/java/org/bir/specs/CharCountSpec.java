package org.bir.specs;

import org.bir.FeatureSpec;
import org.bir.FeatureType;
import org.bir.ReutersArticle;

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
