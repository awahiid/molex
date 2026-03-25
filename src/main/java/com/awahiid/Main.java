package com.awahiid;

import picocli.CommandLine;

/**
 * Molex application entry point delegating execution to {@link MolexCLI}.
 **/
public class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new MolexCLI()).execute(args);
        System.exit(exitCode);
    }
}