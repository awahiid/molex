package com.awahiid;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;

/**
 * MolexCLI - command-line interface for Molex.
 * Implemented with Picocli for subcommand and argument management.
 *
 * <p>Typical Linux workflow:</br>
 * 1. Build: {@code ./build.sh}</br>
 * 2. Index: {@code molex count <root> <output>} (or {@code countr} for recursive mode)</br>
 * 3. Search: {@code molex search <output>}</br>
 */
@Command(
        name = "molex",
        mixinStandardHelpOptions = true,
    description = "Molex CLI: explore directories, index terms, and search dictionaries.",
        subcommands = {
                MolexCLI.ListCmd.class,
                MolexCLI.CountCmd.class,
                MolexCLI.CountRCmd.class,
                MolexCLI.SearchCmd.class
        }
)
public class MolexCLI {

    public static void main(String[] args) {
        System.exit(new CommandLine(new MolexCLI()).execute(args));
    }

    @Command(name = "list", description = "Lists directory contents similarly to the tree command")
    static class ListCmd implements Runnable {
        private final MolexCrawler pcCrawler = new MolexCrawler();

        @Parameters(index = "0", description = "Root directory to list")
        Path root;

        @Override
        public void run() {
            try {
                pcCrawler.list(root);
            } catch (Exception e) {
                System.err.println("ERROR >> " + e.getMessage());
                System.err.println("Usage: molex list <root>");
            }
        }
    }

    @Command(name = "count", description = "(Iterative) Builds a term dictionary from root and saves it to output")
    static class CountCmd implements Runnable {
        private final MolexCrawler pcCrawler = new MolexCrawler();

        @Parameters(index = "0", description = "Root directory")
        Path root;

        @Parameters(index = "1", description = "Output dictionary file")
        Path output;

        @CommandLine.Option(names = {"--inverso", "--stopwords", "-i"}, description = "Enable stopword filtering while indexing")
        boolean useStopwords = false;

        @Override
        public void run() {
            try {
                if(useStopwords) {
                    System.out.println("[Crawler iterative] Stopword filtering enabled for this dictionary build.");
                    pcCrawler.stopwords.load();
                }
                pcCrawler.countTF(root, output);
            } catch (Exception e) {
                System.err.println("ERROR >> " + e.getMessage());
                System.err.println("Usage: molex count [--stopwords] <root> <output>");
            }
        }
    }

    @Command(name = "countr", description = "(Recursive) Builds a term dictionary from root and saves it to output")
    static class CountRCmd implements Runnable {
        private final MolexCrawler pcCrawler = new MolexCrawler();

        @Parameters(index = "0", description = "Root directory")
        Path root;

        @Parameters(index = "1", description = "Output dictionary file")
        Path output;

        @CommandLine.Option(names = {"--inverso", "--stopwords", "-i"}, description = "Enable stopword filtering while indexing")
        boolean useStopwords = false;

        @Override
        public void run() {
            try {
                if(useStopwords) {
                    System.out.println("[Crawler recursive] Stopword filtering enabled for this dictionary build.");
                    pcCrawler.stopwords.load();
                }
                pcCrawler.countTFR(root, output);
            } catch (Exception e) {
                System.err.println("ERROR >> " + e.getMessage());
                System.err.println("Usage: molex countr [--stopwords] <root> <output>");
            }
        }
    }

    @Command(name = "search", description = "Search terms in a dictionary file")
    static class SearchCmd implements Runnable {
        private final MolexCrawler pcCrawler = new MolexCrawler();

        @Parameters(index = "0", description = "Dictionary file to load")
        Path root;

        @CommandLine.Option(names = {"--thesaurus", "-t"}, description = "Enable thesaurus-based query expansion", required = false)
        boolean useThesaurus = false;

        @Override
        public void run() {
            try {
                if (useThesaurus) pcCrawler.thesaurus.load();
                pcCrawler.search(root);
            } catch (Exception e) {
                System.err.println("ERROR >> " + e.getMessage());
                System.err.println("Usage: molex search [-t] <root>");
            }
        }
    }
}