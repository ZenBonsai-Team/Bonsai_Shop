package com.example.bonsai_shop.customer.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ProfanityFilterService {

    // Comprehensive Vietnamese & English profanity dictionary
    private static final List<String> PROFANITY_PATTERNS = Arrays.asList(
            "đm", "dm", "đmm", "dmm", "dkm", "đkm", "vcl", "vcc", "clgt", "cc",
            "địt", "dit", "đẹt", "đẹc", "cặc", "cac", "lồn", "lon", "buồi", "buoi",
            "cứt", "cut", "chó đẻ", "cho de", "đám chó", "ngu lồn", "ngu lon", "mẹ kiếp",
            "con mẹ", "con me", "thằng chó", "thang cho", "mẹ thằng", "me thang",
            "fuck", "shit", "bitch", "bastard", "asshole", "dick", "pussy", "crap"
    );

    /**
     * Removes Vietnamese accents / diacritics and converts 'đ' -> 'd'.
     */
    public String removeAccents(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(normalized).replaceAll("");
        return result.replace('đ', 'd').replace('Đ', 'D');
    }

    /**
     * Removes spaces, dots, and special punctuation used for obfuscation (e.g. 'đ.ị.t' -> 'dit').
     */
    public String stripObfuscation(String input) {
        if (input == null) return "";
        return removeAccents(input.toLowerCase()).replaceAll("[^a-z0-9]", "");
    }

    /**
     * Checks if text contains any profane terms (using original text & normalized forms).
     */
    public boolean containsProfanity(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String lower = text.toLowerCase();
        String normalized = removeAccents(lower);
        String stripped = stripObfuscation(lower);

        for (String patternWord : PROFANITY_PATTERNS) {
            String patternNorm = removeAccents(patternWord.toLowerCase());

            // 1. Direct match in original lower text
            if (lower.contains(patternWord)) {
                return true;
            }

            // 2. Normalized accentless match
            if (normalized.contains(patternNorm)) {
                return true;
            }

            // 3. Obfuscation stripped match (for multi-character terms)
            if (patternNorm.length() > 1 && stripped.contains(patternNorm.replaceAll("[^a-z0-9]", ""))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces any detected profane terms with '***'.
     */
    public String maskProfanity(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String result = text;
        for (String patternWord : PROFANITY_PATTERNS) {
            // Case-insensitive replace for exact match
            result = Pattern.compile(Pattern.quote(patternWord), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                    .matcher(result).replaceAll("***");

            // Also check accent-removed forms if different
            String normWord = removeAccents(patternWord);
            if (!normWord.equalsIgnoreCase(patternWord)) {
                result = Pattern.compile(Pattern.quote(normWord), Pattern.CASE_INSENSITIVE)
                        .matcher(result).replaceAll("***");
            }
        }
        return result;
    }
}
