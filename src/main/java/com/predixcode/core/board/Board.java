package com.predixcode.core.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.predixcode.core.board.pieces.King;
import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.colors.Color;

public class Board {
    private int xMatrix = 8;
    private int yMatrix = 8;

    private int halfmove;
    private int fullmove;
    // enPassant as board coordinates [x, y]; -1 means none
    private int[] enPassant = new int[] { -1, -1 };

    private Color activeColor;

    List<Piece> pieces = new ArrayList<>();

    public static Board fromFen(String fen) {
        return FenAdapter.setupBoard(fen);
    }

    // Setters used by FenAdapter
    public void setDimensions(int xMatrix, int yMatrix) { this.xMatrix = xMatrix; this.yMatrix = yMatrix; }
    public void setHalfmove(int halfmove) { this.halfmove = halfmove; }
    public void setFullmove(int fullmove) { this.fullmove = fullmove; }
    public void setEnPassant(int[] enPassant) { this.enPassant = enPassant != null ? enPassant : new int[]{-1, -1}; }
    public void setActiveColor(Color activeColor) { this.activeColor = activeColor; }
    public void setPieces(List<Piece> pieces) { this.pieces = pieces != null ? pieces : new ArrayList<>(); }

    public List<Piece> getPieces() { return pieces; }

    public Piece getPieceAt(int x, int y) {
        for (Piece p : pieces) {
            if (p.getX() == x && p.getY() == y) return p;
        }
        return null;
    }

    public void nextTurn() {
        halfmove++; // Simplified; in real chess, reset on pawn move or capture
        if (activeColor == Color.BLACK) {
            fullmove++;
        }
        if (activeColor != null) {
            activeColor = activeColor.opposite();
        }
    }

    public void move(String from, String to) {
        int[] fromXY = FenAdapter.parseAlgebraicSquare(from);
        int[] toXY = FenAdapter.parseAlgebraicSquare(to);
        if (fromXY == null || toXY == null) throw new IllegalArgumentException("Invalid move coordinates");

        Piece movingPiece = null;
        for (Piece p : pieces) {
            if (p.getX() == fromXY[0] && p.getY() == fromXY[1]) {
                movingPiece = p;
                break;
            }
        }
        if (movingPiece == null) throw new IllegalArgumentException("No piece at source square: " + from);

        // Simple move (no legality checks)
        movingPiece.setPosition(toXY[0], toXY[1]);
        nextTurn();
    }

    // Public API: pseudo-legal targets for a piece (algebraic like "e4")
    public Set<String> getPseudoLegalTargets(Piece p) {
        Set<String> targets = new LinkedHashSet<>();
        if (p == null) return targets;

        boolean isWhite = p.getColor() != null && p.getColor().getCode() == 1;
        char t = Character.toLowerCase(p.symbol().charAt(0));
        int x = p.getX(), y = p.getY();

        switch (t) {
            case 'n' -> {
                int[][] offs = {{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2}};
                for (int[] o : offs) addIfValidTarget(x + o[0], y + o[1], isWhite, targets);
            }
            case 'k' -> {
                for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    addIfValidTarget(x + dx, y + dy, isWhite, targets);
                }
                // Castling omitted for brevity (add here if needed)
            }
            case 'b' -> slideTargets(x, y, isWhite, targets, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}});
            case 'r' -> slideTargets(x, y, isWhite, targets, new int[][]{{1,0},{-1,0},{0,1},{0,-1}});
            case 'q' -> slideTargets(x, y, isWhite, targets, new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}});
            case 'p' -> {
                int dir = isWhite ? -1 : 1;
                // Single push
                if (empty(x, y + dir)) targets.add(toAlg(x, y + dir));
                // Double push from start rank
                int startRank = isWhite ? 6 : 1; // using 0-top, 7-bottom indexing
                if (y == startRank && empty(x, y + dir) && empty(x, y + 2*dir)) {
                    targets.add(toAlg(x, y + 2*dir));
                }
                // Captures
                addIfCapture(x + 1, y + dir, isWhite, targets);
                addIfCapture(x - 1, y + dir, isWhite, targets);
                // En passant omitted
            }
        }
        return targets;
    }

    // Helpers (Board-internal)
    private void slideTargets(int x, int y, boolean isWhite, Set<String> out, int[][] dirs) {
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            while (inBounds(nx, ny)) {
                Piece at = getPieceAt(nx, ny);
                if (at == null) {
                    out.add(toAlg(nx, ny));
                } else {
                    if (at.getColor() != null && (at.getColor().getCode() != (isWhite ? 1 : 0))) {
                        out.add(toAlg(nx, ny)); // capture
                    } else if (at.getColor() == null) {
                        out.add(toAlg(nx, ny));
                    }
                    break; // stop ray on first piece
                }
                nx += d[0];
                ny += d[1];
            }
        }
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < xMatrix && y >= 0 && y < yMatrix;
    }

    private boolean empty(int x, int y) {
        return inBounds(x, y) && getPieceAt(x, y) == null;
    }

    private void addIfValidTarget(int x, int y, boolean isWhite, Set<String> out) {
        if (!inBounds(x, y)) return;
        Piece at = getPieceAt(x, y);
        if (at == null) out.add(toAlg(x, y));
        else if (at.getColor() != null && at.getColor().getCode() != (isWhite ? 1 : 0)) out.add(toAlg(x, y));
    }

    private void addIfCapture(int x, int y, boolean isWhite, Set<String> out) {
        if (!inBounds(x, y)) return;
        Piece at = getPieceAt(x, y);
        if (at != null && at.getColor() != null && at.getColor().getCode() != (isWhite ? 1 : 0)) {
            out.add(toAlg(x, y));
        }
    }

    // Algebraic from board coords
    public String toAlg(int x, int y) {
        char file = (char) ('a' + x);
        int rank = yMatrix - y;
        return "" + file + rank;
    }

    public int[] fromAlg(String alg) {
        int x = alg.charAt(0) - 'a';
        int rank = alg.charAt(1) - '0'; // 1..8
        int y = yMatrix - rank;
        return new int[]{x, y};
    }

    @Override
    public String toString() {
        // Use configured dimensions (defaults 8x8)
        char[][] grid = new char[yMatrix][xMatrix];
        for (int r = 0; r < yMatrix; r++) Arrays.fill(grid[r], '.');

        for (Piece p : pieces) {
            int x = p.getX();
            int y = p.getY();
            if (x >= 0 && x < xMatrix && y >= 0 && y < yMatrix) {
                grid[y][x] = p.symbol().charAt(0);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("  +-----------------+\n");
        for (int row = 0; row < yMatrix; row++) {
            int rank = yMatrix - row;
            sb.append(rank).append(" | ");
            for (int col = 0; col < xMatrix; col++) {
                sb.append(grid[row][col]).append(' ');
            }
            sb.append("|\n");
        }
        sb.append("  +-----------------+\n");
        sb.append("    a b c d e f g h\n\n");

        sb.append("Active: ").append(activeColor != null ? activeColor : "unknown").append('\n');
        sb.append("Castling: ").append(currentCastlingString()).append('\n');
        sb.append("En Passant: ").append(toAlg(enPassant[0], enPassant[1])).append('\n');
        sb.append("Halfmove: ").append(halfmove).append('\n');
        sb.append("Fullmove: ").append(fullmove).append('\n');

        return sb.toString();
    }

    private String currentCastlingString() {
        boolean K = false, Q = false, k = false, q = false;
        for (Piece p : pieces) {
            if (p instanceof King king) {
                boolean isWhite = p.getColor() != null && p.getColor().getCode() == 1;
                if (isWhite) {
                    if (king.canCastleKingSide()) K = true;
                    if (king.canCastleQueenSide()) Q = true;
                } else {
                    if (king.canCastleKingSide()) k = true;
                    if (king.canCastleQueenSide()) q = true;
                }
            }
        }
        String s = (K ? "K" : "") + (Q ? "Q" : "") + (k ? "k" : "") + (q ? "q" : "");
        return s.isEmpty() ? "-" : s;
    }
}