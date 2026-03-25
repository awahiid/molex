package com.awahiid;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.Normalizer;
import java.io.File;
import java.util.Scanner;

/**
 * Utility helpers for text normalization and file content extraction.
 * */
public class Utils {
    public final static String DELIMITER = "[ ,.:;(){}!°?'%/|\\[\\]<=>&#+*$\\-¨^~\n\\r\\t«»¿¡\"\"\"''—–_…]+";

    public static String getCoincidence(File file, String term) {
        String parsedFile;
        try {
            parsedFile = Utils.fileToString(file.getAbsolutePath());
        } catch (Exception e) {
            return "Could not read file content";
        }

        try (Scanner sc = new Scanner(parsedFile)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                int idx = normalize(line).indexOf(term);
                if (idx >= 0) {
                    int start = Math.max(0, idx - 40);
                    int end = Math.min(line.length(), idx + term.length() + 40);
                    return (start > 0 ? "..." : "") + line.substring(start, end).trim() + (end < line.length() ? "..." : "");
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    public static String normalize (String word) {
        if (word == null) return null;
        String normalized = Normalizer.normalize(word, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
    }

    public static String fileToString(String path) throws Exception {
        Tika tika = new Tika();
        Metadata metadata = new Metadata();
        StringBuilder extractedData = new StringBuilder();

        try (InputStream stream = new FileInputStream(path)) {
            String content = tika.parseToString(stream, metadata);
            if (content != null && !content.trim().isEmpty()) {
                extractedData.append(content).append(" ");
            }
            // Include available metadata (description, title, tags, and so on).
            for (String name : metadata.names()) {
                extractedData.append(metadata.get(name)).append(" ");
            }
            return extractedData.toString();
        }
    }
}