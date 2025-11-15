package com.predixcode.core.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.predixcode.core.board.pieces.King;
import com.predixcode.core.board.pieces.Pawn;
import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.board.pieces.Rook;
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

    public int getXMatrix() { return xMatrix; }
    public int getYMatrix() { return yMatrix; }
    public void setDimensions(int xMatrix, int yMatrix) { this.xMatrix = xMatrix; this.yMatrix = yMatrix; }
    public int getWidth() { return xMatrix; }
    public int getHeight() { return yMatrix; }

    public void setHalfmove(int halfmove) { this.halfmove = halfmove; }
    public int getHalfmove() { return halfmove; }
    public void setFullmove(int fullmove) { this.fullmove = fullmove; }
    public int getFullmove() { return fullmove; }

    public void setEnPassant(int[] enPassant) { this.enPassant = enPassant != null ? enPassant : new int[]{-1, -1}; }
    public int[] getEnPassantXY() { return new int[]{ enPassant[0], enPassant[1] }; }
    public void clearEnPassant() { this.enPassant[0] = -1; this.enPassant[1] = -1; }

    public void setActiveColor(Color activeColor) { this.activeColor = activeColor; }
    public Color getActiveColor() { return activeColor; }

    public void setPieces(List<Piece> pieces) { this.pieces = pieces != null ? pieces : new ArrayList<>(); }
    public List<Piece> getPieces() { return pieces; }

    public String getCastlingString() { return currentCastlingString(); }

    public String getEnPassantAlgebraic() {
        if (enPassant[0] < 0 || enPassant[1] < 0) return "-";
        return toAlg(enPassant[0], enPassant[1]);
    }

    public Piece getPieceAt(int x, int y) {
        for (Piece p : pieces) {
            if (p.getX() == x && p.getY() == y) return p;
        }
        return null;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < xMatrix && y >= 0 && y < yMatrix;
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

    public void move(String from, String to) {
        int[] fromXY = FenAdapter.parseAlgebraicSquare(from);
        int[] toXY = FenAdapter.parseAlgebraicSquare(to);
        if (fromXY == null || toXY == null) throw new IllegalArgumentException("Invalid move coordinates");

        Piece movingPiece = getPieceAt(fromXY[0], fromXY[1]);
        if (movingPiece == null) throw new IllegalArgumentException("No piece at source square: " + from);

        // Turn check (optional; uncomment if enforcing turns)
        if (activeColor != null && !movingPiece.getColor().equals(activeColor)) {
            throw new IllegalStateException("It is not " + movingPiece.getColor() + "'s turn");
        }

        // Validate destination is a pseudo-legal target for this piece
        Set<String> targets = getLegalMoves(movingPiece);
        String toAlg = to.toLowerCase();
        if (!targets.contains(toAlg)) {
            throw new IllegalArgumentException("Destination " + to + " is not a pseudo-legal target for " + from);
        }

        boolean isCapture = (getPieceAt(toXY[0], toXY[1]) != null);
        boolean isPawn = movingPiece instanceof Pawn;
        boolean isKing = movingPiece instanceof King;

        // Handle en passant capture
        boolean enPassantCapture = false;
        if (isPawn && fromXY[0] != toXY[0] && isEmpty(toXY[0], toXY[1])) {
            // Diagonal move into empty square => en passant
            enPassantCapture = true;
            int dir = movingPiece.getColor() == Color.WHITE ? -1 : 1;
            Piece epPawn = getPieceAt(toXY[0], toXY[1] - dir);
            if (epPawn == null || !(epPawn instanceof Pawn) || epPawn.getColor() == movingPiece.getColor()) {
                throw new IllegalStateException("Invalid en passant capture attempted");
            }
            pieces.remove(epPawn);
            isCapture = true;
        }

        // Handle castling (king moves two squares horizontally)
        if (isKing && Math.abs(toXY[0] - fromXY[0]) == 2) {
            King king = (King) movingPiece;
            int rankY = fromXY[1];
            if (toXY[0] > fromXY[0]) {
                // King-side castle: move rook from nearest right rook to f-file (x = fromX+1)
                Piece rook = findFirstRookOnRay(fromXY[0], rankY, +1, 0, movingPiece.getColor());
                if (!(rook instanceof Rook)) throw new IllegalStateException("No rook found for king-side castling");
                int rookToX = fromXY[0] + 1;
                pieces.remove(rook);
                rook.setPosition(rookToX, rankY);
                pieces.add(rook);
            } else {
                // Queen-side castle: move rook from nearest left rook to d-file (x = fromX-1)
                Piece rook = findFirstRookOnRay(fromXY[0], rankY, -1, 0, movingPiece.getColor());
                if (!(rook instanceof Rook)) throw new IllegalStateException("No rook found for queen-side castling");
                int rookToX = fromXY[0] - 1;
                pieces.remove(rook);
                rook.setPosition(rookToX, rankY);
                pieces.add(rook);
            }
            // King loses castling rights
            king.setCastleKingSide(false);
            king.setCastleQueenSide(false);
            clearEnPassant();
        } else {
            // Normal capture if any on destination
            Piece captured = getPieceAt(toXY[0], toXY[1]);
            if (captured != null) {
                pieces.remove(captured);
            }

            // Update en passant target for next move (only after a two-square pawn push)
            clearEnPassant();
            if (isPawn && Math.abs(toXY[1] - fromXY[1]) == 2) {
                int dir = movingPiece.getColor() == Color.WHITE ? -1 : 1;
                enPassant[0] = fromXY[0];
                enPassant[1] = fromXY[1] + dir;
            }
        }

        // Move the piece
        movingPiece.setPosition(toXY[0], toXY[1]);

        // Update halfmove clock
        if (isPawn || isCapture) {
            halfmove = 0;
        } else {
            halfmove++;
        }

        // Update castling rights after king/rook move or rook capture
        updateCastlingRightsAfterMove(movingPiece, fromXY[0], fromXY[1], toXY[0], toXY[1], isCapture);

        nextTurn();
    }

    // Public API: pseudo-legal targets for a piece (algebraic like "e4")
    public Set<String> getLegalMoves(Piece p) {
        if (p == null) return new LinkedHashSet<>();
        return p.getLegalMoves(this);
    }

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

    private Piece findFirstRookOnRay(int sx, int sy, int dx, int dy, Color color) {
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

    private void updateCastlingRightsAfterMove(Piece mover, int fromX, int fromY, int toX, int toY, boolean isCapture) {
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

    private String currentCastlingString() {
        boolean K = false, Q = false, k = false, q = false;
        for (Piece p : pieces) {
            if (p instanceof King king) {
                boolean isWhite = p.getColor() == Color.WHITE;
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
        sb.append("En Passant: ").append(getEnPassantAlgebraic()).append('\n');
        sb.append("Halfmove: ").append(halfmove).append('\n');
        sb.append("Fullmove: ").append(fullmove).append('\n');

        return sb.toString();
    }
}