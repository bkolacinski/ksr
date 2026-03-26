package org.bir.extractor.specs;

import org.bir.extractor.FeatureSpec;
import org.bir.extractor.FeatureType;
import org.bir.extractor.ReutersArticle;

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
