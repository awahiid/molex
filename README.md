# Molex

Command-line text crawler and search indexer for local files.

Created by Abdel Wahed Mahfoud Mouhandizi ([awahiid](https://github.com/awahiid)).

## Overview

Molex scans files from a root directory, builds a serialized dictionary of terms, and lets you run interactive searches against that dictionary. It is designed as a lightweight CLI workflow for indexing and querying document collections.

## Requirements

- Java 17+
- Maven 3.8+
- Linux/macOS shell or Windows PowerShell/CMD

## Build

### Linux/macOS

```bash
chmod +x build.sh molex.sh
./build.sh
```

After running `build.sh`, the `molex` command should be available globally in your shell.

### Windows

```powershell
mvn clean package
```

## Run

### Linux/macOS (with global command)

```bash
molex list /path/to/directory
molex count /path/to/source dictionary.bin
molex countr /path/to/source dictionary.bin
molex search dictionary.bin
```

### Windows (running the generated JAR)

```powershell
java -jar target/molex-1.0-SNAPSHOT.jar list C:\Docs
java -jar target/molex-1.0-SNAPSHOT.jar countr C:\Docs dictionary.bin
java -jar target/molex-1.0-SNAPSHOT.jar search dictionary.bin
```

Optional PowerShell alias:

```powershell
Set-Alias molex "java -jar C:\path\to\project\target\molex-1.0-SNAPSHOT.jar"
```

## Project Structure

```text
.
├── build.sh
├── molex.sh
├── pom.xml
├── resources/
├── src/main/java/com/awahiid/
│   ├── Main.java
│   ├── MolexCLI.java
│   ├── MolexCrawler.java
│   ├── Dictionary.java
│   ├── Occurrence.java
│   ├── Stopwords.java
│   ├── Thesaurus.java
│   └── Utils.java
├── tests/
└── target/
```

## Notes

- `count` runs indexing with an iterative strategy.
- `countr` runs indexing with a recursive strategy.
- The generated dictionary file can be reused across search sessions.