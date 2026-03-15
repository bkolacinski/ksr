package org.bir.specs;

import org.bir.FeatureSpec;
import org.bir.FeatureType;
import org.bir.ReutersArticle;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LexicalDiversitySpec extends FeatureSpec<Double> {

    public LexicalDiversitySpec(Double weight) {
        super(weight);
        this.name = "lexicalDiversity";
        this.type = FeatureType.NUMERIC;
    }

    @Override
    public Double calculate(ReutersArticle article) {
        List<String> words = SpecWordUtils.extractWords(article.getText());
        if (words.isEmpty()) {
            return 0.0;
        }

        Set<String> unique = new HashSet<>();
        for (String word : words) {
            unique.add(word.toLowerCase(Locale.ROOT));
        }

        return (double) unique.size() / words.size();
    }
}

