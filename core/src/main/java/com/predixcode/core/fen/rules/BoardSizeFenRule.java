package com.predixcode.core.fen.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.predixcode.core.GameConfig;
import com.predixcode.core.fen.FenUtils;

/**
 * Resizes a position to a requested board width / height while preserving
 * the existing layout as much as possible.
 * Width:
 *  - Grows by inserting new files around the existing ones:
 *      first on the right, then on the left, then right, then left...
 *    so the original columns remain roughly centered.
 *  - Shrinks only if the columns being removed are completely empty.
 * Height:
 *  - Grows by inserting empty ranks around the vertical middle.
 *  - Shrinks only by removing completely empty ranks.
 * If cfg.fillExpandedFiles() is true:
 *  - New files in pawn ranks are filled with pawns.
 *  - New files in back ranks are filled with extra pieces, so that:
 *      +1 file -> 1 piece, knight or bishop
 *      +2      -> knight + bishop
 *      +3      -> knight + bishop + rook
 *      +4      -> knight + bishop + rook + queen
 *      +>4     -> above + more knights/bishops (more N/B than R, more R than Q)
 * For Chess960 games (cfg.chess960() == true) we still handle board size,
 * but we skip back-rank filling; Chess960FenRule will replace the back ranks.
 */
public class BoardSizeFenRule extends AbstractStartFenRule {

    @Override
    protected String applyToBoard(String boardPart, GameConfig cfg, Random random) {
        String[] rankStrings = FenUtils.splitBoard(boardPart);
        int currentHeight = rankStrings.length;
        if (currentHeight == 0) {
            throw new IllegalArgumentException("Empty board part: " + boardPart);
        }

        int currentWidth = FenUtils.countSquaresInRank(rankStrings[0]);
        for (String r : rankStrings) {
            if (FenUtils.countSquaresInRank(r) != currentWidth) {
                throw new IllegalArgumentException("Inconsistent rank widths in FEN: " + boardPart);
            }
        }

        int targetWidth  = cfg.boardWidth()  > 0 ? cfg.boardWidth()  : currentWidth;
        int targetHeight = cfg.boardHeight() > 0 ? cfg.boardHeight() : currentHeight;

        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException(
                    "Board dimensions must be positive, got " +
                    targetWidth + "x" + targetHeight);
        }

        // Expand to char grid
        char[][] grid = new char[currentHeight][];
        for (int y = 0; y < currentHeight; y++) {
            grid[y] = FenUtils.expandRankToArray(rankStrings[y]);
        }

        // --- WIDTH -----------------------------------------------------------

        int extraWidth = targetWidth - currentWidth;
        if (extraWidth > 0) {
            // Grow width with centered expansion: right, left, right, left...
            int padLeft = 0;
            int padRight = 0;
            for (int i = 0; i < extraWidth; i++) {
                if ((i & 1) == 0) padRight++; // 1st extra on the right
                else padLeft++;
            }

            int newWidth = currentWidth + extraWidth;
            char[][] newGrid = new char[currentHeight][newWidth];

            // First fill everything with '.' and copy old content into the center block.
            for (int y = 0; y < currentHeight; y++) {
                Arrays.fill(newGrid[y], '.');
                System.arraycopy(grid[y], 0, newGrid[y], padLeft, currentWidth);
            }

            grid = newGrid;
            currentWidth = newWidth;

            // If we don't want to auto-fill, we're done with width.
            if (cfg.fillExpandedFiles()) {
                // Compute indices of the new files in "right, left, right, left..." order.
                int[] newFileIndices = computeNewFileIndices(
                        padLeft, currentWidth - extraWidth, extraWidth); // old width = new - extra

                // Identify back/pawn ranks (top/bottom model).
                int h = currentHeight;
                int blackBack  = 0;
                int whiteBack  = h - 1;
                int blackPawn  = (h >= 4) ? 1 : -1;
                int whitePawn  = (h >= 4) ? h - 2 : -1;

                // 1) Always fill new files in pawn ranks with pawns.
                if (blackPawn >= 0) {
                    for (int col : newFileIndices) {
                        grid[blackPawn][col] = 'p';
                    }
                }
                if (whitePawn >= 0) {
                    for (int col : newFileIndices) {
                        grid[whitePawn][col] = 'P';
                    }
                }

                // 2) Fill back ranks with extra pieces only for non-Chess960 games.
                if (!cfg.chess960()) {
                    char[] extraPieces = buildExtraBackRankPieces(extraWidth, random);
                    for (int i = 0; i < extraWidth; i++) {
                        int col = newFileIndices[i];
                        char p = extraPieces[i];

                        // Only overwrite new empty files (safety check).
                        if (grid[whiteBack][col] == '.' || grid[whiteBack][col] == ' ') {
                            grid[whiteBack][col] = p;
                        }
                        if (grid[blackBack][col] == '.' || grid[blackBack][col] == ' ') {
                            grid[blackBack][col] = Character.toLowerCase(p);
                        }
                    }
                }
            }

        } else if (extraWidth < 0) {
            // Try to shrink width non-destructively (only if cut columns are empty).
            char[][] shrunk = shrinkWidthIfPossible(grid, currentWidth, targetWidth);
            if (shrunk != null) {
                grid = shrunk;
                currentWidth = targetWidth;
            }
            // If shrinking was unsafe (pieces present), keep original width.
        }

        // --- HEIGHT ----------------------------------------------------------

        int extraHeight = targetHeight - currentHeight;
        if (extraHeight > 0) {
            grid = growHeight(grid, currentWidth, targetHeight);
            currentHeight = targetHeight;
        } else if (extraHeight < 0) {
            char[][] shrunkH = shrinkHeightIfPossible(grid, targetHeight);
            if (shrunkH != null) {
                grid = shrunkH;
                currentHeight = targetHeight;
            }
        }

        // --- Back to FEN -----------------------------------------------------

        List<String> outRanks = new ArrayList<>(currentHeight);
        for (char[] row : grid) {
            outRanks.add(FenUtils.compressRank(row));
        }
        return String.join("/", outRanks);
    }

    // ---------------------------------------------------------------------
    // NEW helper: which columns are newly added? (right, left, right, left)
    // ---------------------------------------------------------------------
    private static int[] computeNewFileIndices(int padLeft, int oldWidth, int extraWidth) {
        int[] indices = new int[extraWidth];
        int left  = padLeft - 1;           // rightmost of the left-new block
        int right = padLeft + oldWidth;    // leftmost of the right-new block
        for (int i = 0; i < extraWidth; i++) {
            if ((i & 1) == 0) {
                // even index: right side
                indices[i] = right++;
            } else {
                // odd index: left side
                indices[i] = left--;
            }
        }
        return indices;
    }

    // ---------------------------------------------------------------------
    // NEW helper: extra back-rank piece distribution
    // ---------------------------------------------------------------------
    /**
     * Build the list of extra back-rank pieces for the new files, obeying:
     *  +1 -> 1 piece, knight OR bishop
     *  +2 -> knight AND bishop
     *  +3 -> knight, bishop, rook
     *  +4 -> knight, bishop, rook, queen
     *  +>4-> above + more knights/bishops (more N/B than R, more R than Q)
     */
    private static char[] buildExtraBackRankPieces(int extra, Random random) {
        if (extra <= 0) return new char[0];

        List<Character> pool = new ArrayList<>();

        // First 1–2 ensure at least 1 knight and 1 bishop in some order.
        if (extra >= 1) {
            pool.add(random.nextBoolean() ? 'N' : 'B');
        }
        if (extra >= 2) {
            char first = pool.get(0);
            pool.add(first == 'N' ? 'B' : 'N');
        }
        if (extra >= 3) {
            pool.add('R');
        }
        if (extra >= 4) {
            pool.add('Q');
        }

        // For extra > 4, keep adding more N/B to dominate counts over R/Q.
        int baseSize = pool.size();
        for (int i = 0; i < extra - baseSize; i++) {
            pool.add((i & 1) == 0 ? 'N' : 'B');
        }

        // Shuffle to avoid always clustering the same way.
        pool.sort((a, b) -> 0); // dummy to keep deterministic if you prefer
        // If you actually want randomness, use:
        // java.util.Collections.shuffle(pool, random);

        char[] result = new char[extra];
        for (int i = 0; i < extra; i++) {
            result[i] = pool.get(i);
        }
        return result;
    }

    // ---------------------------------------------------------------------
    // Width-shrink helper (unchanged logic)
    // ---------------------------------------------------------------------
    private static char[][] shrinkWidthIfPossible(char[][] grid, int currentWidth, int targetWidth) {
        if (targetWidth >= currentWidth) return grid;

        int h = grid.length;
        // Check all cut columns are empty.
        for (char[] row : grid) {
            if (row.length != currentWidth) {
                throw new IllegalStateException("Row width mismatch while shrinking width");
            }
            for (int x = targetWidth; x < currentWidth; x++) {
                char c = row[x];
                if (c != '.' && c != ' ') {
                    return null; // unsafe to shrink
                }
            }
        }

        char[][] out = new char[h][targetWidth];
        for (int y = 0; y < h; y++) {
            System.arraycopy(grid[y], 0, out[y], 0, targetWidth);
        }
        return out;
    }

    // ---------------------------------------------------------------------
    // Height helpers (same as before)
    // ---------------------------------------------------------------------
    private static char[][] growHeight(char[][] grid, int width, int targetHeight) {
        int currentHeight = grid.length;
        int extra = targetHeight - currentHeight;
        if (extra <= 0) return grid;

        char[][] out = new char[targetHeight][width];
        int insertIndex = currentHeight / 2;

        int outRow = 0;
        for (int y = 0; y < currentHeight; y++) {
            if (y == insertIndex) {
                for (int k = 0; k < extra; k++) {
                    char[] emptyRow = new char[width];
                    Arrays.fill(emptyRow, '.');
                    out[outRow++] = emptyRow;
                }
            }
            out[outRow++] = Arrays.copyOf(grid[y], width);
        }
        return out;
    }

    private static char[][] shrinkHeightIfPossible(char[][] grid, int targetHeight) {
        int h = grid.length;
        if (targetHeight >= h) return grid;

        int toRemove = h - targetHeight;

        List<Integer> emptyIndices = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            if (isRowEmpty(grid[y])) {
                emptyIndices.add(y);
            }
        }
        if (emptyIndices.size() < toRemove) {
            return null; // not enough empty ranks to remove
        }

        final double mid = (h - 1) / 2.0;
        emptyIndices.sort(Comparator.comparingDouble(i -> Math.abs(i - mid)));

        boolean[] remove = new boolean[h];
        for (int i = 0; i < toRemove; i++) {
            remove[emptyIndices.get(i)] = true;
        }

        char[][] out = new char[targetHeight][grid[0].length];
        int outRow = 0;
        for (int y = 0; y < h; y++) {
            if (!remove[y]) {
                out[outRow++] = grid[y];
            }
        }
        return out;
    }

    private static boolean isRowEmpty(char[] row) {
        for (char c : row) {
            if (c != '.' && c != ' ') return false;
        }
        return true;
    }

    // NOTE: createWhiteBackRank(...) left as-is for Chess960FenRule reuse, but
    // no longer used by BoardSizeFenRule itself.
    @SuppressWarnings("unused")
    protected char[] createWhiteBackRank(int width, Random random) {
        // your old implementation here if Chess960FenRule extends this
        return new char[width];
    }
}