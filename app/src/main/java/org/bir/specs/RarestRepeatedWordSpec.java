package org.bir.specs;

import org.bir.FeatureSpec;
import org.bir.FeatureType;
import org.bir.ReutersArticle;

import java.util.List;
import java.util.Map;

public class RarestRepeatedWordSpec extends FeatureSpec<String> {

    public RarestRepeatedWordSpec(Double weight) {
        super(weight);
        this.name = "rarestRepeatedWord";
        this.type = FeatureType.TEXT;
    }

    @Override
    public String calculate(ReutersArticle article) {
        List<String> words = SpecWordUtils.extractWords(article.getText());
        if (words.isEmpty()) {
            return "None";
        }

        Map<String, Integer> counts = SpecWordUtils.countWordFrequencies(words);

        String bestWord = null;
        int bestCount = Integer.MAX_VALUE;

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            int count = entry.getValue();
            if (count < 2) {
                continue;
            }

            String word = entry.getKey();
            if (count < bestCount || (count == bestCount && (bestWord == null || word.compareTo(bestWord) < 0))) {
                bestCount = count;
                bestWord = word;
            }
        }

        return bestWord == null ? "None" : bestWord;
    }
}

