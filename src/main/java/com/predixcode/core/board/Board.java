package com.predixcode.core.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.predixcode.core.board.colors.Color;
import com.predixcode.core.board.pieces.King;
import com.predixcode.core.board.pieces.Piece;

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
        sb.append("En Passant: ").append(toAlgebraicSquare(enPassant)).append('\n');
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

    private String toAlgebraicSquare(int[] xy) {
        if (xy == null || xy.length != 2 || xy[0] < 0 || xy[1] < 0) return "-";
        int x = xy[0], y = xy[1];
        char file = (char) ('a' + x);
        char rank = (char) ('0' + (yMatrix - y));
        return "" + file + rank;
    }
}