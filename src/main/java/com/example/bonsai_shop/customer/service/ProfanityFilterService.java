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
     * Checks if text contains any profane terms (using original text & normalized forms).
     */
    public boolean containsProfanity(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // Clean URLs and HTML tags first to avoid false positives inside links and markups
        String cleanedText = text.replaceAll("https?://\\S+\\s?", " ").replaceAll("<[^>]*>", " ");

        String lower = cleanedText.toLowerCase();
        
        // Deobfuscate by removing punctuation (but not spaces) inside words (e.g. "d.i.t" -> "dit", "b.u.ồ.i" -> "buồi")
        String deobfuscated = lower.replaceAll("(?<=\\p{L})[._\\-*]+(?=\\p{L})", "");

        for (String patternWord : PROFANITY_PATTERNS) {
            String pattern = patternWord.toLowerCase();

            // Unicode-compliant word boundary pattern
            String regex = "(?<=^|[^\\p{L}\\p{N}])" + Pattern.quote(pattern) + "(?=$|[^\\p{L}\\p{N}])";
            Pattern compiled = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);

            // 1. Match in original lowercase text
            if (compiled.matcher(lower).find()) {
                return true;
            }

            // 2. Match in deobfuscated text
            if (compiled.matcher(deobfuscated).find()) {
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
            String pattern = patternWord.toLowerCase();
            // Mask exact word matches using regex with boundaries
            String regex = "(?<=^|[^\\p{L}\\p{N}])(?i)" + Pattern.quote(pattern) + "(?=$|[^\\p{L}\\p{N}])";
            result = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS).matcher(result).replaceAll("***");
        }
        return result;
    }
}
