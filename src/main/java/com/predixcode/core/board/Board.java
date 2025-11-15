package com.predixcode.core.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.predixcode.core.board.pieces.King;
import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.board.pieces.Rook;
import com.predixcode.core.colors.Color;
import com.predixcode.core.rules.Rule;

public class Board {
    public int width;
    public int height;

    public int halfmove;
    public int fullmove;
    public int[] enPassant = new int[] { -1, -1 }; // enPassant as board coordinates [x, y]; -1 means none

    public Color activeColor;

    public List<Piece> pieces = new ArrayList<>();
    public List<Rule> rules = new ArrayList<>();

    //  ========= Board initialization ==========
    public static Board fromFen(String fen) {
        return FenAdapter.boardFromFen(fen);
    }

    public void setPieces(List<Piece> pieces) { this.pieces = pieces != null ? pieces : new ArrayList<>(); }

    // En Passant
    public void setEnPassant(int[] enPassant) { this.enPassant = enPassant != null ? enPassant : new int[]{-1, -1}; }
    public int[] getEnPassantXY() { return new int[]{ enPassant[0], enPassant[1] }; }
    public void clearEnPassant() { this.enPassant[0] = -1; this.enPassant[1] = -1; }

    public String getEnPassantAlgebraic() {
        if (enPassant[0] < 0 || enPassant[1] < 0) return "-";
        return toAlg(enPassant[0], enPassant[1]);
    }

    // ========== Piece access and board state ==========
    public void move(String from, String to) { 
        int[] fromXY = fromAlg(from);
        int[] toXY = fromAlg(to);
        if (fromXY == null || toXY == null) throw new IllegalArgumentException("Invalid move coordinates");

        Piece movingPiece = getPieceAt(fromXY[0], fromXY[1]);
        if (movingPiece == null) throw new IllegalArgumentException("No piece at source square: " + from);

        for (Rule rule : rules) {
            rule.apply(this, movingPiece, from, to);
        }
    }

    public Piece getPieceAt(int x, int y) {
        for (Piece p : pieces) {
            if (p.getX() == x && p.getY() == y) return p;
        }
        return null;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean isEmpty(int x, int y) {
        return inBounds(x, y) && getPieceAt(x, y) == null;
    }

    public void nextTurn() {
        if (activeColor == Color.BLACK) {
            fullmove++;
        }
        if (activeColor != null) {
            activeColor = activeColor.opposite();
        }
    }

    public String toAlg(int x, int y) {
        if (x < 0 || y < 0) return "-";
        char file = (char) ('a' + x);
        int rank = height - y;
        return "" + file + rank;
    }

    public int[] fromAlg(String alg) {
        if (alg == null) throw new IllegalArgumentException("Square is null");
        alg = alg.trim();
        if (alg.equals("-")) return new int[]{-1, -1};
        if (alg.length() < 2) throw new IllegalArgumentException("Invalid algebraic square: " + alg);

        char fileCh = Character.toLowerCase(alg.charAt(0));
        if (fileCh < 'a' || fileCh >= ('a' + width)) {
            throw new IllegalArgumentException("File out of range: " + alg);
        }
        int file = fileCh - 'a';

        // Support standard chess ranks 1..8 (single digit). If you later support >9 ranks, parse substring(1).
        char rankCh = alg.charAt(1);
        if (rankCh < '1' || rankCh > '0' + height) {
            throw new IllegalArgumentException("Rank out of range: " + alg);
        }
        int rank = rankCh - '0';
        int y = height - rank;
        return new int[]{file, y};
    }
    // ========== Attack detection and helpers ==========

    public boolean isSquareAttacked(Color byColor, int x, int y) {
        for (Piece p : pieces) {
            if (p.getColor() == byColor) {
                for (int[] sq : p.attackedSquares(this)) {
                    if (sq[0] == x && sq[1] == y) return true;
                }
            }
        }
        return false;
    }

    public Piece findFirstRookOnRay(int sx, int sy, int dx, int dy, Color color) {
        int x = sx + dx, y = sy + dy;
        while (inBounds(x, y)) {
            Piece at = getPieceAt(x, y);
            if (at != null) {
                if (at instanceof Rook && at.getColor() == color) return at;
                return null;
            }
            x += dx; y += dy;
        }
        return null;
    }

    public void updateCastlingRightsAfterMove(Piece mover, int fromX, int fromY, int toX, int toY, boolean isCapture) {
        // If king moved, clear its rights
        if (mover instanceof King king) {
            king.setCastleKingSide(false);
            king.setCastleQueenSide(false);
        }

        // If rook moved from initial squares, clear that side for that color
        if (mover instanceof Rook) {
            if (mover.getColor() == Color.WHITE) {
                King wk = getKing(Color.WHITE);
                if (wk != null) {
                    if (fromX == 0 && fromY == 7) wk.setCastleQueenSide(false);
                    if (fromX == 7 && fromY == 7) wk.setCastleKingSide(false);
                }
            } else if (mover.getColor() == Color.BLACK) {
                King bk = getKing(Color.BLACK);
                if (bk != null) {
                    if (fromX == 0 && fromY == 0) bk.setCastleQueenSide(false);
                    if (fromX == 7 && fromY == 0) bk.setCastleKingSide(false);
                }
            }
        }

        // If a rook was captured on its original square, clear that side for that color
        if (isCapture) {
            // The captured piece has already been removed; check destination square coordinates
            // against original rook squares to clear the opponent's rights.
            King wk = getKing(Color.WHITE);
            King bk = getKing(Color.BLACK);
            if (toX == 0 && toY == 7 && wk != null) wk.setCastleQueenSide(false);
            if (toX == 7 && toY == 7 && wk != null) wk.setCastleKingSide(false);
            if (toX == 0 && toY == 0 && bk != null) bk.setCastleQueenSide(false);
            if (toX == 7 && toY == 0 && bk != null) bk.setCastleKingSide(false);
        }
    }

    private King getKing(Color color) {
        for (Piece p : pieces) {
            if (p instanceof King && p.getColor() == color) return (King)p;
        }
        return null;
    }

    @Override
    public String toString() {
        // Use configured dimensions (defaults 8x8)
        char[][] grid = new char[height][width];
        for (int r = 0; r < height; r++) Arrays.fill(grid[r], '.');

        for (Piece p : pieces) {
            int x = p.getX();
            int y = p.getY();
            if (x >= 0 && x < width && y >= 0 && y < height) {
                grid[y][x] = p.getSymbol().charAt(0);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("  +-----------------+\n");
        for (int row = 0; row < height; row++) {
            int rank = height - row;
            sb.append(rank).append(" | ");
            for (int col = 0; col < width; col++) {
                sb.append(grid[row][col]).append(' ');
            }
            sb.append("|\n");
        }
        sb.append("  +-----------------+\n");
        sb.append("    a b c d e f g h\n\n");

        sb.append("Active: ").append(activeColor != null ? activeColor : "unknown").append('\n');
        sb.append("Castling: ").append(FenAdapter.getCastlingString(this)).append('\n');
        sb.append("En Passant: ").append(getEnPassantAlgebraic()).append('\n');
        sb.append("Halfmove: ").append(halfmove).append('\n');
        sb.append("Fullmove: ").append(fullmove).append('\n');

        return sb.toString();
    }
}