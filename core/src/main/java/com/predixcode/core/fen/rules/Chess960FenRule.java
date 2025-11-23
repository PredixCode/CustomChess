package com.predixcode.core.fen.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.predixcode.core.GameConfig;

/**
 * Chess960 variant of BoardSizeFenRule.
 *
 * It uses the same height / middle-empty-row logic, but overrides how
 * the back rank is generated:
 *
 *  - 2 bishops on opposite-colored squares
 *  - 2 rooks with the king somewhere between them
 *  - remaining squares: at least 1 queen, rest knights
 *
 * Works for arbitrary width >= 5 (for width < 8, you may have no knights
 * or queen depending on the number of files).
 */
public class Chess960FenRule extends BoardSizeFenRule {

    @Override
    protected String applyToBoard(String boardPart, GameConfig cfg, Random random) {
        // We still respect boardWidth/Height from cfg, but back-rank layout
        // itself is randomized.
        return super.applyToBoard(boardPart, cfg, random);
    }

    @Override
    protected char[] createWhiteBackRank(int width, Random random) {
        if (width < 5) {
            throw new IllegalArgumentException(
                    "Chess960 requires width >= 5, got " + width);
        }

        char[] rank = new char[width];
        Arrays.fill(rank, '\0');

        // Partition files by color (assuming a1 is dark, but only parity matters).
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
            if (rank[i] == '\0') {
                remaining.add(i);
            }
        }

        // --- King between two rooks ---
        // Pick three distinct positions, then sort them so rook-king-rook.
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

        // Remove those from remaining pool.
        remaining.remove(Integer.valueOf(rookLeft));
        remaining.remove(Integer.valueOf(kingIndex));
        remaining.remove(Integer.valueOf(rookRight));

        // --- Remaining squares: 1 queen (if any slot) + knights ---
        int extraSlots = remaining.size();             // width - 5
        int queens = extraSlots > 0 ? 1 : 0;
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

        // Safety: any leftover (shouldn't happen) becomes a knight.
        while (idx < remaining.size()) {
            int pos = remaining.get(idx++);
            if (rank[pos] == '\0') {
                rank[pos] = 'N';
            }
        }

        // Final sanity: fill any still-empty cells as knights.
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
