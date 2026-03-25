package com.awahiid;

import java.io.Serializable;
import java.util.TreeMap;

/**
 * Links a token to the FAT indexes of files where it appears,
 * along with the term frequency in each file.
 * */
public class Occurrence implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
        * Token represented by this occurrence record.
     * */
    String token;

    /**
        * Global token frequency across all indexed files.
     */
    Integer GF = 0;

    /**
        * Maps file IDs to token frequency in each file.
     */
    TreeMap<Integer, Integer> files = new TreeMap<>();

    public Occurrence(String token) { this.token = token; }

    public int getGF() { return GF; }

    public void put(Integer fileCode) {
        GF++;
        files.put(fileCode, files.getOrDefault(fileCode, 0) + 1);
    }
}