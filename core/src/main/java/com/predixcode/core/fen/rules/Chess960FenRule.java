package com.predixcode.core.fen.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.predixcode.core.GameConfig;
import com.predixcode.core.fen.FenUtils;

/**
 * Chess960 variant of BoardSizeFenRule.
 * Pipeline:
 *   1) Use BoardSizeFenRule's logic to resize the board non-destructively
 *      (only add/remove empty ranks/files when possible).
 *   2) On the resized board, randomize the back rank for each side with a
 *      Chess960-style layout:
 *         - bishops on opposite-colored squares
 *         - king somewhere between the two rooks
 *         - at least one queen if there is enough room
 *         - remaining squares are knights
 * Only the *back ranks* (one for white, one for black) are overwritten.
 * All other ranks are preserved exactly as in the resized position.
 */
public class Chess960FenRule extends BoardSizeFenRule {

    @Override
    protected String applyToBoard(String boardPart, GameConfig cfg, Random random) {
        // 1) First, let BoardSizeFenRule handle width/height non-destructively.
        String resizedBoard = super.applyToBoard(boardPart, cfg, random);

        // 2) Now apply Chess960 back-rank randomization on top of the resized board.
        String[] rankStrings = FenUtils.splitBoard(resizedBoard);
        int height = rankStrings.length;
        if (height == 0) {
            return resizedBoard;
        }

        int width = FenUtils.countSquaresInRank(rankStrings[0]);
        if (width < 5) {
            // Not enough files to do anything meaningful; leave as-is.
            return resizedBoard;
        }

        // Expand to a mutable grid.
        char[][] grid = new char[height][];
        for (int y = 0; y < height; y++) {
            grid[y] = FenUtils.expandRankToArray(rankStrings[y]);
        }

        // Find white & black back-rank indices.
        int whiteBack = findBackRankIndex(grid, /*white=*/true);
        int blackBack = findBackRankIndex(grid, /*white=*/false);

        // Build random white back rank and mirror for black.
        char[] whiteBackRank = createWhiteBackRank(width, random);
        char[] blackBackRank = new char[width];
        for (int x = 0; x < width; x++) {
            blackBackRank[x] = Character.toLowerCase(whiteBackRank[x]);
        }

        grid[whiteBack] = whiteBackRank;
        grid[blackBack] = blackBackRank;

        // Compress back to board-part FEN.
        List<String> outRanks = new ArrayList<>(height);
        for (char[] row : grid) {
            outRanks.add(FenUtils.compressRank(row));
        }
        return String.join("/", outRanks);
    }

    /**
     * Find the "back rank" index for a color:
     *  - For white: search from bottom up for a rank that contains any
     *    white non-pawn piece (uppercase letter other than 'P'). If none,
     *    fall back to the last rank.
     *  - For black: search from top down for a rank that contains any
     *    black non-pawn piece (lowercase letter other than 'p'). If none,
     *    fall back to the first rank.
     */
    private static int findBackRankIndex(char[][] grid, boolean white) {
        int h = grid.length;
        if (white) {
            for (int y = h - 1; y >= 0; y--) {
                if (hasColorNonPawn(grid[y], true)) return y;
            }
            return h - 1;
        } else {
            for (int y = 0; y < h; y++) {
                if (hasColorNonPawn(grid[y], false)) return y;
            }
            return 0;
        }
    }

    private static boolean hasColorNonPawn(char[] row, boolean white) {
        for (char c : row) {
            if (c == '.' || c == ' ') continue;
            if (white) {
                if (Character.isUpperCase(c) && c != 'P') return true;
            } else {
                if (Character.isLowerCase(c) && c != 'p') return true;
            }
        }
        return false;
    }

    protected char[] createWhiteBackRank(int width, Random random) {
        if (width < 5) {
            throw new IllegalArgumentException(
                    "Chess960 requires width >= 5, got " + width);
        }

        char[] rank = new char[width];
        Arrays.fill(rank, '\0');

        // Partition files by color (a1 treated as dark; only parity matters).
        List<Integer> darkSquares = new ArrayList<>();
        List<Integer> lightSquares = new ArrayList<>();
        for (int file = 0; file < width; file++) {
            if ((file & 1) == 0) {
                darkSquares.add(file);
            } else {
                lightSquares.add(file);
            }
        }

        // --- Bishops on opposite colors ---
        int b1Index = pickAndRemove(darkSquares, random);
        int b2Index = pickAndRemove(lightSquares, random);
        rank[b1Index] = 'B';
        rank[b2Index] = 'B';

        // Remaining free indices.
        List<Integer> remaining = new ArrayList<>();
        for (int i = 0; i < width; i++) {
            if (rank[i] == '\0') remaining.add(i);
        }

        // --- King between two rooks ---
        Collections.shuffle(remaining, random);
        int a = remaining.get(0);
        int b = remaining.get(1);
        int c = remaining.get(2);
        int[] triple = new int[]{a, b, c};
        Arrays.sort(triple);
        int rookLeft = triple[0];
        int kingIndex = triple[1];
        int rookRight = triple[2];

        rank[rookLeft] = 'R';
        rank[kingIndex] = 'K';
        rank[rookRight] = 'R';

        remaining.remove(Integer.valueOf(rookLeft));
        remaining.remove(Integer.valueOf(kingIndex));
        remaining.remove(Integer.valueOf(rookRight));

        // --- Remaining squares: 1 queen (if any) + knights ---
        int extraSlots = remaining.size();             // width - 5 (minus bishops & RKRs)
        int queens = (extraSlots > 0) ? 1 : 0;
        int knights = extraSlots - queens;

        Collections.shuffle(remaining, random);
        int idx = 0;
        for (int i = 0; i < queens; i++) {
            int pos = remaining.get(idx++);
            rank[pos] = 'Q';
        }
        for (int i = 0; i < knights; i++) {
            int pos = remaining.get(idx++);
            rank[pos] = 'N';
        }

        // Any leftover (shouldn't happen) becomes a knight.
        while (idx < remaining.size()) {
            int pos = remaining.get(idx++);
            if (rank[pos] == '\0') {
                rank[pos] = 'N';
            }
        }

        // Final safety: fill any still-empty cells as knights.
        for (int i = 0; i < width; i++) {
            if (rank[i] == '\0') {
                rank[i] = 'N';
            }
        }

        return rank;
    }

    private static int pickAndRemove(List<Integer> list, Random random) {
        if (list.isEmpty()) {
            throw new IllegalStateException("No squares available");
        }
        int idx = random.nextInt(list.size());
        int value = list.get(idx);
        list.remove(idx);
        return value;
    }
}