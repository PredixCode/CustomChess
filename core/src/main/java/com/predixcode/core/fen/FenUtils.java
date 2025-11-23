package com.predixcode.core.fen;

import java.util.ArrayList;
import java.util.List;

/**
 * Small helpers for working with the board-part of FEN strings.
 *
 * All methods are intentionally low-level and side-effect free.
 */
public final class FenUtils {

    private FenUtils() {}

    // ---- Full FEN helpers ---------------------------------------------------

    /** Split a full FEN into its 6 space-separated fields. */
    public static String[] splitFen(String fen) {
        String[] parts = fen.trim().split("\\s+");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Expected 6 fields in FEN, got: " + fen);
        }
        return parts;
    }

    public static String joinFen(String[] fields) {
        if (fields.length != 6) {
            throw new IllegalArgumentException("Expected exactly 6 fields, got " + fields.length);
        }
        return String.join(" ", fields);
    }

    // ---- Board-part helpers -------------------------------------------------

    /** Split the board part (field 0) into rank strings (top -> bottom). */
    public static String[] splitBoard(String boardPart) {
        return boardPart.split("/");
    }

    /** Number of squares represented by a rank string. */
    public static int countSquaresInRank(String rank) {
        return expandRankToArray(rank).length;
    }

    /**
     * Expand a FEN rank like "rnbqkbnr" or "3p3" to a char[] of exact width.
     * Pieces are kept as letters, empty squares as '.'.
     */
    public static char[] expandRankToArray(String rank) {
        List<Character> out = new ArrayList<>();
        int i = 0;
        while (i < rank.length()) {
            char c = rank.charAt(i);
            if (Character.isDigit(c)) {
                int j = i;
                int count = 0;
                while (j < rank.length() && Character.isDigit(rank.charAt(j))) {
                    count = count * 10 + (rank.charAt(j) - '0');
                    j++;
                }
                for (int k = 0; k < count; k++) {
                    out.add('.');
                }
                i = j;
            } else {
                out.add(c);
                i++;
            }
        }
        char[] arr = new char[out.size()];
        for (int k = 0; k < out.size(); k++) {
            arr[k] = out.get(k);
        }
        return arr;
    }

    /**
     * Compress a full-width char[] rank (pieces or '.') back to FEN digits.
     */
    public static String compressRank(char[] squares) {
        StringBuilder sb = new StringBuilder();
        int emptyRun = 0;
        for (char c : squares) {
            if (c == '.' || c == ' ') {
                emptyRun++;
            } else {
                if (emptyRun > 0) {
                    sb.append(emptyRun);
                    emptyRun = 0;
                }
                sb.append(c);
            }
        }
        if (emptyRun > 0) {
            sb.append(emptyRun);
        }
        return sb.toString();
    }

    /** An all-empty rank for a given width, e.g. "8" or "10". */
    public static String emptyRank(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be > 0");
        }
        return Integer.toString(width);
    }
}