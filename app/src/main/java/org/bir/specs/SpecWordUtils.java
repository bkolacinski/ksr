package org.bir.specs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SpecWordUtils {
    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}+");

    private SpecWordUtils() {
    }

    static List<String> extractWords(String text) {
        List<String> words = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return words;
        }

        Matcher matcher = WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            words.add(matcher.group());
        }

        return words;
    }

    static boolean startsWithUppercase(String word) {
        return !word.isEmpty() && Character.isUpperCase(word.charAt(0));
    }

    static int countProperNounsInsideSentence(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        Matcher matcher = WORD_PATTERN.matcher(text);
        int previousEnd = 0;
        boolean firstToken = true;
        int count = 0;

        while (matcher.find()) {
            String separator = text.substring(previousEnd, matcher.start());
            boolean sentenceStart = firstToken || containsSentenceBreak(separator);

            String token = matcher.group();
            if (!sentenceStart && startsWithUppercase(token)) {
                count++;
            }

            firstToken = false;
            previousEnd = matcher.end();
        }

        return count;
    }

    static Map<String, Integer> countWordFrequencies(List<String> words) {
        Map<String, Integer> counts = new HashMap<>();
        for (String word : words) {
            String normalized = word.toLowerCase(Locale.ROOT);
            counts.put(normalized, counts.getOrDefault(normalized, 0) + 1);
        }
        return counts;
    }

    static int countAlphanumericChars(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetterOrDigit(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    static int countUppercaseChars(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isUpperCase(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    static int countLowercaseChars(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLowerCase(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    static double safeRatio(int numerator, int denominator) {
        if (denominator <= 0) {
            return numerator == 0 ? 0.0 : (double) numerator;
        }
        return (double) numerator / denominator;
    }

    private static boolean containsSentenceBreak(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                return true;
            }
        }
        return false;
    }
}

