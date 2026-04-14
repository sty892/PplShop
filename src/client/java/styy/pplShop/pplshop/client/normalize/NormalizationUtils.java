package styy.pplShop.pplshop.client.normalize;

import styy.pplShop.pplshop.client.config.ParserRulesConfig;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class NormalizationUtils {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern DECORATIVE_RUN = Pattern.compile("[=~`^*_+#<>|\\u2022\\u00b7\\u2605\\u2606\\u2665\\u2764\\u2661\\u2192\\u279c\\u27a1\\u25c6\\u25c7\\u25b2\\u25bc\\u25a0\\u25a1\\u25cb\\u25cf]+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private static final Map<Character, Character> CYRILLIC_LOOKALIKES = Map.ofEntries(
            Map.entry('a', 'а'),
            Map.entry('A', 'А'),
            Map.entry('b', 'в'),
            Map.entry('B', 'В'),
            Map.entry('c', 'с'),
            Map.entry('C', 'С'),
            Map.entry('e', 'е'),
            Map.entry('E', 'Е'),
            Map.entry('h', 'н'),
            Map.entry('H', 'Н'),
            Map.entry('k', 'к'),
            Map.entry('K', 'К'),
            Map.entry('m', 'м'),
            Map.entry('M', 'М'),
            Map.entry('o', 'о'),
            Map.entry('O', 'О'),
            Map.entry('p', 'р'),
            Map.entry('P', 'Р'),
            Map.entry('t', 'т'),
            Map.entry('T', 'Т'),
            Map.entry('x', 'х'),
            Map.entry('X', 'Х'),
            Map.entry('y', 'у'),
            Map.entry('Y', 'У')
    );

    private NormalizationUtils() {
    }

    public static String normalizeForLookup(String input, ParserRulesConfig rules) {
        if (input == null) {
            return "";
        }

        String normalized = normalizeVisibleText(input);
        for (String ignored : rules.ignored_characters) {
            normalized = normalized.replace(ignored, " ");
        }

        normalized = DECORATIVE_RUN.matcher(normalized).replaceAll(" ");
        normalized = normalized
                .replace('(', ' ')
                .replace(')', ' ')
                .replace('[', ' ')
                .replace(']', ' ')
                .replace('{', ' ')
                .replace('}', ' ')
                .replace('/', ' ')
                .replace('\\', ' ')
                .replace('.', ' ')
                .replace('!', ' ')
                .replace('?', ' ')
                .replace('"', ' ')
                .replace('\'', ' ')
                .replace('*', ' ');
        normalized = NON_ALNUM.matcher(normalized).replaceAll(" ");

        List<String> tokens = tokenize(normalized);
        List<String> mapped = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            mapped.add(applyReplacement(token, rules.token_replacements));
        }

        return compactDisplay(String.join(" ", mapped));
    }

    public static String normalizeWithoutSorting(String input, ParserRulesConfig rules) {
        return normalizeForLookup(input, rules);
    }

    public static String normalizeVisibleText(String input) {
        if (input == null) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC)
                .replace('\u00A0', ' ')
                .replace('\u2014', ' ')
                .replace('\u2013', ' ')
                .replace('\u2212', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ');
        normalized = canonicalizeCommonGlyphs(normalized);
        normalized = canonicalizeMixedScript(normalized);
        normalized = normalized.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replace('Ё', 'Е');
        return compactDisplay(normalized);
    }

    public static List<String> tokenize(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        String[] parts = WHITESPACE.matcher(input.trim()).replaceAll(" ").split(" ");
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            String token = part.trim();
            if (!token.isEmpty()) {
                result.add(token);
            }
        }
        return result;
    }

    public static boolean containsDigits(String input) {
        if (input == null) {
            return false;
        }
        for (int i = 0; i < input.length(); i++) {
            if (Character.isDigit(input.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static String compactDisplay(String input) {
        if (input == null) {
            return "";
        }
        return WHITESPACE.matcher(input.replace('\n', ' ').replace('\r', ' ')).replaceAll(" ").trim();
    }

    public static String stripDecorativeCharacters(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String cleaned = normalizeVisibleText(input);
        cleaned = DECORATIVE_RUN.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replaceAll("[\\[\\](){}<>]", " ");
        cleaned = cleaned.replaceAll("[,;:]+", " ");
        cleaned = cleaned.replaceAll("\\s*[|/\\\\]+\\s*", " ");
        cleaned = NON_ALNUM.matcher(cleaned).replaceAll(" ");
        return compactDisplay(cleaned);
    }

    public static String canonicalizeCommonGlyphs(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        return input
                .replace('ё', 'е')
                .replace('Ё', 'Е')
                .replace('І', 'i')
                .replace('і', 'i')
                .replace('Ї', 'i')
                .replace('ї', 'i')
                .replace('Ј', 'j')
                .replace('ј', 'j')
                .replace('❤', ' ')
                .replace('♥', ' ')
                .replace('♡', ' ')
                .replace('★', ' ')
                .replace('☆', ' ')
                .replace('⚠', ' ')
                .replace('✨', ' ')
                .replace('➡', ' ')
                .replace('➜', ' ')
                .replace('▲', ' ')
                .replace('▼', ' ');
    }

    private static String canonicalizeMixedScript(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        boolean hasCyrillic = input.codePoints().anyMatch(codePoint -> Character.UnicodeBlock.of(codePoint) == Character.UnicodeBlock.CYRILLIC);
        boolean hasLatin = input.codePoints().anyMatch(codePoint -> Character.UnicodeBlock.of(codePoint) == Character.UnicodeBlock.BASIC_LATIN && Character.isLetter(codePoint));
        if (!hasCyrillic || !hasLatin) {
            return input;
        }

        StringBuilder builder = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            builder.append(CYRILLIC_LOOKALIKES.getOrDefault(current, current));
        }
        return builder.toString();
    }

    private static String applyReplacement(String token, Map<String, String> replacements) {
        return replacements.getOrDefault(token, token);
    }
}
