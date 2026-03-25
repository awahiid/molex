package com.awahiid;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Loads a thesaurus file in OpenThesaurus format (';' separated).
 * It enables query expansion with synonyms.
 * Ignores comments (#), multi-word terms, and inline annotations like (fig.), (vulg.),
 * (loc.), (p. us.), and (NoRAE).
 */
public class Thesaurus implements Serializable {
    private static final Path sourcePath = Paths.get("resources", "thesaurus.txt");
    private static final Path objectPath = Paths.get("resources", "thesaurus.bin");

    private static final Pattern ANNOTATION = Pattern
            .compile("\\s*\\((fig\\.|vulg\\.|loc\\.|p\\.\\s*us\\.|NoRAE)\\)\\s*$", Pattern.CASE_INSENSITIVE);

    public final Map<String, Set<String>> synonymMap = new HashMap<>();

    private void loadFromSource() throws IOException {
        synonymMap.clear();
        try (BufferedReader br = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;

                List<String> tokens = new ArrayList<>();

                // Extract the term and synonyms from each line.
                for (String word : line.split(";")) {
                    String synonym = ANNOTATION.matcher(word.trim()).replaceAll("").trim();
                    if (!synonym.isBlank() && !synonym.contains(" ")) tokens.add(Utils.normalize(synonym));
                }

                if (tokens.size() < 2) continue;

                // Build the synonym adjacency map for all terms in the group.
                for (int i = 0; i < tokens.size(); i++)
                    for (int j = 0; j < tokens.size(); j++)
                        if (i != j) synonymMap.computeIfAbsent(tokens.get(i), k -> new LinkedHashSet<>()).add(tokens.get(j));
            }
        }
        this.save();
    }

    public void load() throws IOException {
        if(Files.exists(objectPath)){
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(objectPath))) {
                Thesaurus loaded = (Thesaurus) ois.readObject();

                this.synonymMap.clear();
                this.synonymMap.putAll(loaded.synonymMap);
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
            System.out.println("[Thesaurus] Loaded from binary: " + objectPath.toAbsolutePath());
        } else {
            this.loadFromSource();
            System.out.println("[Thesaurus] Loaded from source: " + sourcePath.toAbsolutePath());
        }

        System.out.println("[Thesaurus] Loaded: " + synonymMap.size() + " entries.");
    }

    private void save() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(objectPath))) {
            oos.writeObject(this);
        }
    }

    public Set<String> getSynonyms(String term) {
        return synonymMap.getOrDefault(Utils.normalize(term), Collections.emptySet());
    }

    public boolean isLoaded() { return !synonymMap.isEmpty(); }
}