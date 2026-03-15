package org.bir.specs;

import org.bir.FeatureSpec;
import org.bir.FeatureType;
import org.bir.ReutersArticle;

public class LengthSpec extends FeatureSpec<Double> {

    public LengthSpec(Double weight) {
        super(weight);
        this.type = FeatureType.NUMERIC;
        //TODO Auto-generated constructor stub
    }

    public Double calculate(ReutersArticle article) {
        return ((double)article.getText().length());
    }
    
}
