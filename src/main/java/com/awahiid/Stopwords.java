package com.awahiid;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Loads stop words used to filter tokens during indexing and search.
 * Source words can be separated by spaces or line breaks.
 */
public class Stopwords implements Serializable {
    private static final Path sourcePath = Paths.get("resources", "thesaurus_inverso.txt");
    private static final Path objectPath = Paths.get("resources", "thesaurus_inverso.bin");

    public final Set<String> words = new HashSet<>();

    public void load() throws IOException {
        if(!Files.exists(objectPath)) {
            try (BufferedReader br = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8)) {
                String line;
                while ((line = br.readLine()) != null)
                    for (String w : line.trim().split("\\s+"))
                        if (!w.isBlank()) words.add(Utils.normalize(w));
            }
            this.save();
            System.out.println("[Stopwords] Loaded: " + words.size() + " words.");
        } else {
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(objectPath))) {
                Stopwords loaded = (Stopwords) ois.readObject();

                this.words.clear();
                this.words.addAll(loaded.words);
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        }
    }

    private void save() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(objectPath))) {
            oos.writeObject(this);
        }
    }

    public boolean isStopword(String term) { return words.contains(Utils.normalize(term)); }
}