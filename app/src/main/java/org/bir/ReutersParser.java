package org.bir;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ReutersParser {

    public List<ReutersArticle> parseDirectory(Path directory, String globPattern) throws IOException {
        List<ReutersArticle> result = new ArrayList<>();

        try (Stream<Path> files = Files.find(directory, 1,
                (path, attr) -> attr.isRegularFile() &&
                                matchesGlob(path.getFileName().toString(), globPattern))) {

            List<Path> sorted = files.sorted().toList();
            for (Path file : sorted) {
                result.addAll(parseFile(file.toFile()));
            }
        }
        return result;
    }

    public List<ReutersArticle> parseFile(File file) throws IOException {
        String raw = Files.readString(file.toPath(), StandardCharsets.ISO_8859_1);

        Document doc = Jsoup.parse(raw, "", Parser.xmlParser());
        doc.outputSettings().charset(StandardCharsets.ISO_8859_1);

        Elements reutersElements = doc.select("REUTERS");

        List<ReutersArticle> articles = new ArrayList<>();
        for (Element reuters : reutersElements) {
            ReutersArticle article = parseReutersElement(reuters);
            if (article != null) {
                articles.add(article);
            }
        }
        return articles;
    }


    private ReutersArticle parseReutersElement(Element reuters) {
        List<String> places = collectD(reuters, "PLACES");

        Element textParent = reuters.selectFirst("TEXT");
        if (textParent == null) return null;

        String content;
        Element bodyEl = textParent.selectFirst("BODY");

        if (bodyEl != null) {
            content = bodyEl.text();
        } else {
            Element textClone = textParent.clone();

            textClone.select("TITLE, DATELINE, AUTHOR").remove();

            content = textClone.text();
        }

        content = content.trim();

        if (content.isBlank()) return null;

        return new ReutersArticle(places, content);
    }

    private List<String> collectD(Element parent, String parentTag) {
        Element container = parent.selectFirst(parentTag);
        if (container == null) return List.of();

        List<String> result = new ArrayList<>();
        for (Element d : container.select("D")) {
            String text = d.text().trim();
            if (!text.isBlank()) result.add(text);
        }
        return List.copyOf(result);
    }

    private boolean matchesGlob(String filename, String pattern) {
        String regex = "\\Q" + pattern.replace("*", "\\E.*\\Q").replace("?", "\\E.\\Q") + "\\E";
        return filename.matches(regex);
    }
}
