package org.bir;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.tartarus.snowball.ext.englishStemmer;

public class TextParser {
    private List<String> stopwords;
    private englishStemmer stemmer;

    public TextParser(String stopwordFilePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream("org/bir/stoplist.txt");
        
        stopwords = new BufferedReader(new InputStreamReader(is)).lines().toList();

        stemmer = new englishStemmer();
    }

    public List<ReutersArticle> filter(List<ReutersArticle> articles) {
        for (ReutersArticle article : articles) {
            String text = article.getText();
            text = text.replaceAll("'s\\b", "");
            String[] tokens = text.split("[^a-z]+");

            List<String> parsed = new ArrayList<>(tokens.length);

            for (String token : tokens) {
                if (!token.isEmpty() && !stopwords.contains(token)) {
                    parsed.add(token);
                }
            }

            article.setText(String.join(" ", parsed));
        }

        return articles;
    }


    public List<ReutersArticle> stem(List<ReutersArticle> articles) {
        for (ReutersArticle article : articles) {
            String text = article.getText();
            String[] words = text.split("\\s+");

            StringBuilder stemmedText = new StringBuilder();

            for (String word : words) {
                stemmer.setCurrent(word);
                stemmer.stem();
                stemmedText.append(stemmer.getCurrent()).append(" ");
            }

            article.setText(stemmedText.toString().trim());
        }

        return articles;
    }

}
