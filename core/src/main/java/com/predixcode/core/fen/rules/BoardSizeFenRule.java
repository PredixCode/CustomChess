package com.predixcode.core.fen.rules;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.predixcode.core.GameConfig;
import com.predixcode.core.fen.FenUtils;

/**
 * Rebuilds the starting position to fit a requested board width / height.
 *
 * - Height: adds/removes empty ranks between the pawn ranks (no new pieces).
 * - Width: builds a sensible symmetric back-rank for the given width and
 *          fills pawns on all files.
 *
 * This assumes a "standard-style" game with:
 *   black pieces / black pawns / empty middle / white pawns / white pieces.
 * For non-standard FEN this will overwrite the layout with a standard-style
 * one that fits the requested dimensions.
 */
public class BoardSizeFenRule extends AbstractStartFenRule {

    @Override
    protected String applyToBoard(String boardPart, GameConfig cfg, Random random) {
        String[] originalRanks = FenUtils.splitBoard(boardPart);
        int currentHeight = originalRanks.length;
        if (currentHeight == 0) {
            throw new IllegalArgumentException("Empty board part: " + boardPart);
        }
        int currentWidth = FenUtils.countSquaresInRank(originalRanks[0]);

        int targetWidth = cfg.boardWidth() > 0 ? cfg.boardWidth() : currentWidth;
        int targetHeight = cfg.boardHeight() > 0 ? cfg.boardHeight() : currentHeight;

        // No-op if nothing changes.
        if (targetWidth == currentWidth && targetHeight == currentHeight) {
            return boardPart;
        }

        if (targetHeight < 4) {
            throw new IllegalArgumentException(
                    "Board height must be at least 4, got " + targetHeight);
        }
        if (targetWidth < 5) {
            throw new IllegalArgumentException(
                    "Board width must be at least 5, got " + targetWidth);
        }

        // Build white back rank for the requested width using a deterministic
        // algorithm; black is just lowercase mirror.
        char[] whiteBackRank = createWhiteBackRank(targetWidth, random);
        char[] blackBackRank = new char[targetWidth];
        for (int i = 0; i < targetWidth; i++) {
            blackBackRank[i] = Character.toLowerCase(whiteBackRank[i]);
        }

        // Pawn ranks (full files of pawns).
        char[] whitePawns = new char[targetWidth];
        char[] blackPawns = new char[targetWidth];
        Arrays.fill(whitePawns, 'P');
        Arrays.fill(blackPawns, 'p');

        String whiteBackFen = FenUtils.compressRank(whiteBackRank);
        String blackBackFen = FenUtils.compressRank(blackBackRank);
        String whitePawnFen = FenUtils.compressRank(whitePawns);
        String blackPawnFen = FenUtils.compressRank(blackPawns);
        String emptyRankFen = FenUtils.emptyRank(targetWidth);

        List<String> resultRanks = new ArrayList<>();
        resultRanks.add(blackBackFen);
        resultRanks.add(blackPawnFen);
        for (int i = 0; i < targetHeight - 4; i++) {
            resultRanks.add(emptyRankFen);
        }
        resultRanks.add(whitePawnFen);
        resultRanks.add(whiteBackFen);

        return String.join("/", resultRanks);
    }

    /**
     * Deterministic "classic" back-rank layout for arbitrary width >= 5.
     *
     * Guarantees:
     *  - 2 rooks on the outer files
     *  - 1 king near the center
     *  - 2 bishops near the king (one on each side if possible)
     *  - 1 queen placed as close to the king as possible (if there is room)
     *  - Remaining squares are filled with knights.
     */
    protected char[] createWhiteBackRank(int width, Random random) {
        if (width < 5) {
            throw new IllegalArgumentException(
                    "Board width must be at least 5, got " + width);
        }

        char[] rank = new char[width];
        // Start with all knights as filler.
        Arrays.fill(rank, 'N');

        // Put rooks on the extremes.
        rank[0] = 'R';
        rank[width - 1] = 'R';

        // King near the center (bias slightly to the left for even widths).
        int kingIndex = (width % 2 == 0) ? (width / 2 - 1) : (width / 2);
        rank[kingIndex] = 'K';

        // Bishops just to the left/right of the king if possible, otherwise
        // fallback inside the rooks.
        int leftBishop = Math.max(1, kingIndex - 1);
        int rightBishop = Math.min(width - 2, kingIndex + 1);

        // Avoid overriding rooks/king if width is very small.
        if (rank[leftBishop] == 'N') {
            rank[leftBishop] = 'B';
        }
        if (rank[rightBishop] == 'N') {
            rank[rightBishop] = 'B';
        }

        // Place one queen as close to the king as possible on a remaining knight
        // square (if any). Remaining knights stay as knights.
        int queenIndex = -1;
        for (int offset = 1; offset < width; offset++) {
            int li = kingIndex - offset;
            int ri = kingIndex + offset;
            if (li >= 0 && rank[li] == 'N') {
                queenIndex = li;
                break;
            }
            if (ri < width && rank[ri] == 'N') {
                queenIndex = ri;
                break;
            }
        }
        if (queenIndex >= 0) {
            rank[queenIndex] = 'Q';
        }

        return rank;
    }
}
