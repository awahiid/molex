package com.awahiid;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core crawler service.
 *
 * Provides directory listing, token indexing (iterative/recursive), and interactive search
 * over a persisted dictionary.
 * */
public class MolexCrawler {
    public final Dictionary dictionary = new Dictionary();
    public final Thesaurus thesaurus = new Thesaurus();
    public final Stopwords stopwords = new Stopwords();

    private record Node(Path path, int depth) {}

    /**
     * Prints directory contents similarly to {@code tree <root>}.
     *
     * @param root Root directory to list.
     * */
    public void list(Path root) throws IOException {
        Objects.requireNonNull(root, "Parameter 'root' cannot be null");
        if (!Files.exists(root)) throw new RuntimeException("Path does not exist: " + root);

        // levelHasSiblings[i] indicates whether depth i still has pending siblings.
        Deque<Node> stack = new ArrayDeque<>();
        List<Boolean> levelHasSiblings = new ArrayList<>();
        stack.push(new Node(root, 0));
        levelHasSiblings.add(false);

        while (!stack.isEmpty()) {
            Node node = stack.pop();
            Path current = node.path;
            int depth = node.depth;

            while (levelHasSiblings.size() <= depth) levelHasSiblings.add(false);

            for (int i = 1; i < depth; i++) System.out.print(levelHasSiblings.get(i) ? "│   " : "    ");
            boolean isLast = stack.isEmpty() || stack.peek().depth < depth;
            String prefix = depth == 0 ? "" : (isLast ? "└── " : "├── ");

            if (Files.isSymbolicLink(current)) {
                System.out.println(prefix + current.getFileName() + " -> " + Files.readSymbolicLink(current));
            } else {
                System.out.println(prefix + current.getFileName());
            }

            levelHasSiblings.set(depth, !isLast);

            // Skip symbolic links while traversing directories.
            if (Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(current)) {
                    ds.forEach(p -> stack.push(new Node(p, depth + 1)));
                }
            }
        }
    }

    /**
     * Token counting/indexing algorithm (iterative version).
     *
     * @param root Root path to crawl.
     * @param output Output path where the serialized dictionary will be saved.
     * */
    public void countTF(Path root, Path output) throws IOException {
        Objects.requireNonNull(root, "Parameter 'root' cannot be null");
        Objects.requireNonNull(output, "Parameter 'output' cannot be null");

        Deque<Path> directory = new ArrayDeque<>();
        directory.push(root);

        while (!directory.isEmpty()) {
            Path current = directory.pop();

            if (Files.isRegularFile(current)) {
                indexFile(current);
            } else if (Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(current)) {
                    ds.forEach(directory::push);
                }
            }
        }

        dictionary.save(output);
        System.out.println("[Crawler iterative] Dictionary created with " + dictionary.getSize() + " terms. Saved to " + output.toAbsolutePath());
    }

    public void countTFR(Path root, Path output) throws IOException {
        Objects.requireNonNull(root, "Parameter 'root' cannot be null");
        Objects.requireNonNull(output, "Parameter 'output' cannot be null");
        countTFRRecursive(root);
        dictionary.save(output);
        System.out.println("[Crawler recursive] Dictionary created with " + dictionary.getSize() + " terms. Saved to " + output.toAbsolutePath());
    }

    /**
     * Token counting/indexing algorithm (recursive version).
     * */
    private void countTFRRecursive(Path root) throws IOException {
        if (Files.isRegularFile(root)) {
            indexFile(root);
        } else if (Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
                ds.forEach(f -> {
                    try {
                        countTFRRecursive(f);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private void indexFile(Path filePath) {
        Integer index = dictionary.addToFAT(filePath.toFile());
        String parsedFile;
        try {
            parsedFile = Utils.fileToString(filePath.toString());
        } catch (Exception e) {
            System.err.println("[SKIP] Could not parse: " + filePath + " - " + e.getMessage());
            return;
        }

        try (Scanner sc = new Scanner(parsedFile)) {
            sc.useDelimiter(Utils.DELIMITER);
            while (sc.hasNext()) {
                String word = Utils.normalize(sc.next());
                if (!stopwords.isStopword(word)) {
                    dictionary.put(word, index);
                }
            }
        }
    }

    /**
     * Runs interactive search against a dictionary loaded from disk.
     *
     * @param root Dictionary file path to load.
     * */
    public void search(Path root) throws IOException {
        Objects.requireNonNull(root, "Parameter 'root' cannot be null");
        if (!Files.isRegularFile(root))
            throw new RuntimeException("Invalid file. Expected a serialized dictionary file");

        dictionary.load(root);
        System.out.println("[Search] Dictionary loaded with " + dictionary.getSize() + " terms.");

        Scanner stdinScanner = System.console() == null ? new Scanner(System.in) : null;

        while (true) {
            System.out.println(">> Enter a search query (type 'exit' to quit):");

            String input = System.console() != null
                    ? System.console().readLine()
                    : (stdinScanner.hasNextLine() ? stdinScanner.nextLine() : "exit");

            if ("exit".equals(input)) break;

            String query = Utils.normalize(input);
            List<String> queryTokens = dictionary.tokenize(query);
            System.out.println(">> Query tokens: " + queryTokens.stream().collect(Collectors.joining(" ")));

            Map<String, Occurrence> results = new LinkedHashMap<>();
            for (String token : queryTokens) {
                if (stopwords.isStopword(token)) continue;
                dictionary.search(token).forEach(o -> results.put(o.token, o));

                if (thesaurus.isLoaded()) {
                    Set<String> synonyms = new LinkedHashSet<>(thesaurus.getSynonyms(token));
                    new ArrayList<>(results.keySet()).forEach(t -> synonyms.addAll(thesaurus.getSynonyms(t)));
                    synonyms.forEach(syn -> dictionary.search(java.util.regex.Pattern.quote(syn)).forEach(o -> results.putIfAbsent(o.token, o)));
                }
            }

            if (!results.isEmpty()) {
                dictionary.rank(results.values()).forEach(r -> {
                    System.out.println("-> Score: " + String.format("%.2f", r.score()) + " | Freq: " + r.freq() + " | " + r.path());
                    r.terms().entrySet().forEach(e -> System.out.println("   Contains " + e.getKey() + " (" + e.getValue() + "): " + Utils.getCoincidence(new File(r.path()), e.getKey())));
                    System.out.println();
                });
            } else {
                System.out.println("No matches found for term: " + query + "\n");
            }
        }
    }
}