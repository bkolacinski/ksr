package org.bir.specs;

import org.bir.FeatureSpec;
import org.bir.FeatureType;
import org.bir.ReutersArticle;

public class ProperNounSpec extends FeatureSpec<String> {

    public ProperNounSpec(Double weight) {
        super(weight);
        this.name = "properNounFirst";
        this.type = FeatureType.TEXT;
    }

    @Override
    public String calculate(ReutersArticle article) {
        for (String word : SpecWordUtils.extractWords(article.getText())) {
            if (SpecWordUtils.startsWithUppercase(word)) {
                return word;
            }
        }

        return "None";
    }
    
}
