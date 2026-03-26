package org.bir.extractor;
import java.util.List;

public class ReutersArticle {
    private final List<String> places;
    private String text;

    public ReutersArticle(List<String> places, String text) {
        this.places = places;
        this.text   = text;
    }

    public List<String> getPlaces() { return places; }
    public String getText()         { return text;   }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "ReutersArticle{places=" + places + ", text='" + text.substring(0, Math.min(60, text.length())) + "...'}";
    }
}