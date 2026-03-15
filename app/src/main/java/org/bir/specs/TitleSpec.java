package org.bir.specs;

import org.bir.FeatureSpec;
import org.bir.FeatureType;
import org.bir.ReutersArticle;

public class TitleSpec extends FeatureSpec<String> {

    public TitleSpec(Double weight) {
        super(weight);
        this.type = FeatureType.TEXT;
        //TODO Auto-generated constructor stub
    }

    @Override
    public String calculate(ReutersArticle article) {
        return article.getText().split(" ")[0];
    }
    
}
