package com.awahiid;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.text.similarity.LevenshteinDistance;

/**
 * Stores all indexed terms and their occurrences.
 *
 * It also keeps a FAT (File Allocation Table) that maps internal file IDs to absolute paths,
 * and supports serialization for persistence.
 * */
public class Dictionary implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    private final Map<Integer, String> FAT = new HashMap<>();
    private int nextFileId = 0;
    private static final double MIN_SIMILARITY = 0.5;

    private final TreeMap<String, Occurrence> tokens = new TreeMap<>();

    public int getSize(){
        return this.tokens.size();
    }

    public List<String> tokenize(String query) {
        List<String> tokenized = new ArrayList<>();
        LevenshteinDistance leven = LevenshteinDistance.getDefaultInstance();

        for (String term : query.split("\\s+")) {
            String bestToken = null;
            double bestSimilarity = 0.0;

            for (String token : this.tokens.keySet()) {
                int distance = leven.apply(term, token);
                double similarity = 1.0 - ((double) distance / Math.max(term.length(), token.length()));

                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestToken = token;
                }
            }

            if (bestToken != null && bestSimilarity >= MIN_SIMILARITY) {
                tokenized.add(bestToken);
            }
        }

        return tokenized;
    }

    public List<Occurrence> search(String regex){
        return tokens.values()
                .stream()
                .filter(occurrence -> occurrence.token.matches(regex))
                .sorted(Comparator.comparingInt(Occurrence::getGF).reversed())
                .toList();
    }

    public void put(String term, Integer fileCode) {
        // Ensure file code exists before adding occurrences.
        if (!FAT.containsKey(fileCode)) {
            throw new NoSuchElementException("File with code " + fileCode + " does not exist");
        }
        
        Occurrence occurrence = tokens.get(term);
        if(occurrence == null) {
            occurrence = new Occurrence(term);
            tokens.put(term, occurrence);
        }
        occurrence.put(fileCode);
    }

    public void save(Path output) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(output))) {
            oos.writeObject(this);
        }
    }

    public void load(Path source) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(source))) {
            Dictionary loaded = (Dictionary) ois.readObject();

            this.tokens.clear();
            this.FAT.clear();
            
            this.tokens.putAll(loaded.tokens);
            this.FAT.putAll(loaded.FAT);
            // Keep ID allocation consistent for future insertions.
            this.nextFileId = loaded.nextFileId;
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String toString() {
        return tokens.values()
            .stream()
            .sorted(Comparator.comparingInt(Occurrence::getGF).reversed())
            .map(this::occurrenceToString)
            .collect(Collectors.joining("\n"));
    }

        public String occurrenceToString(Occurrence occurrence) {
        StringBuilder summary = new StringBuilder()
            .append(occurrence.GF)
            .append(" - ")
            .append(occurrence.token)
            .append("\n");

        List<Map.Entry<Integer, Integer>> sortedPaths = occurrence.files.entrySet()
                .stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .toList();

        for (Map.Entry<Integer, Integer> e : sortedPaths) {
            summary.append("-> ")
                    .append(e.getValue())
                    .append(" | ")
                    .append(FAT.get(e.getKey()))
                    .append("\n");
        }

        return summary.toString();
    }

    public Integer addToFAT(File file) {
        int id = nextFileId++;
        FAT.put(id, file.getAbsolutePath());
        return id;
    }

    public record RankedResult(Map<String, Integer> terms, String path, int freq, double score) {}

    /**
        * Returns a list of ranked search results sorted by descending score using:<br>
        * - Number of query terms present in the file.<br>
        * - Term frequency in that file.<br>
        * - Token rarity in the corpus (IDF).<br>
        * - Matches in the file name.<br>
        * - File recency (newer files receive a slight bonus).<br>
     *
        * @param occurrences Occurrences to score and rank.
     */
    public List<RankedResult> rank(Collection<Occurrence> occurrences) {
        int totalFiles = FAT.size();
        Map<String, RankedResult> ranking = new LinkedHashMap<>();

        for (Occurrence occurrence : occurrences) {
            double idf = Math.log((double) totalFiles / occurrence.files.size());

            for (Map.Entry<Integer, Integer> occurrenceFile : occurrence.files.entrySet()) {
                String path = FAT.get(occurrenceFile.getKey());
                File file = new File(path);

                RankedResult prev = ranking.get(path);

                Map<String, Integer> terms = prev != null ? new HashMap<>(prev.terms()) : new HashMap<>();
                terms.put(occurrence.token, terms.getOrDefault(occurrence.token, 0) + occurrenceFile.getValue());

                int freq = occurrenceFile.getValue();
                double nameBonus = Utils.normalize(file.getName()).contains(occurrence.token) ? 10.0 : 0.0;
                long daysOld = (System.currentTimeMillis() - file.lastModified()) / (1000 * 60 * 60 * 24);

                double termBonus = 1 + 0.1 * terms.size();
                double scoreIncrement = freq * idf * (1 + nameBonus + 1.0 / (daysOld + 1)) * termBonus;

                int totalFreq = (prev != null ? prev.freq() : 0) + freq;
                double totalScore = (prev != null ? prev.score() : 0) + scoreIncrement;

                ranking.put(path, new RankedResult(terms, path, totalFreq, totalScore));
            }
        }

        return ranking.values()
                .stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();
    }
}