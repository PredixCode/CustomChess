package com.predixcode.core.board.pieces;

import java.util.LinkedHashSet;
import java.util.Set;

import com.predixcode.core.board.Board;
import com.predixcode.core.colors.Color;

public class King extends Piece {
    private boolean canCastleKingSide;
    private boolean canCastleQueenSide;

    public King() { this.fenSymbol = "k"; }

    public boolean canCastleKingSide() { return canCastleKingSide; }
    public boolean canCastleQueenSide() { return canCastleQueenSide; }

    public void setCastleKingSide(boolean canCastle) { this.canCastleKingSide = canCastle; }
    public void setCastleQueenSide(boolean canCastle) { this.canCastleQueenSide = canCastle; }

    @Override
    public Set<String> getLegalMoves(Board board) {
        Set<String> out = new LinkedHashSet<>();

        // Normal king moves (one square in any direction)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int tx = this.x + dx;
                int ty = this.y + dy;
                if (!board.inBounds(tx, ty)) continue;
                var at = board.getPieceAt(tx, ty);
                if (at == null || at.getColor() != this.color) {
                    // Also ensure we don't step into check
                    if (!board.isSquareAttacked(this.color.opposite(), tx, ty)) {
                        out.add(board.toAlg(tx, ty));
                    }
                }
            }
        }

        // Castling: check rights + path emptiness + not in/through/into check
        Color opponent = this.color.opposite();

        // King-side: move two squares to the right
        if (canCastleKingSide) {
            boolean pathClear = true;
            for (int xx = this.x + 1; xx <= this.x + 2; xx++) {
                if (!board.inBounds(xx, this.y)) { pathClear = false; break; }
                if (board.getPieceAt(xx, this.y) != null) { pathClear = false; break; }
            }
            // Find rook to the right with no pieces in between
            Piece rook = findFirstRookOnRay(board, +1, 0);
            boolean rookOk = (rook instanceof Rook) && rook.getColor() == this.color;
            boolean safe = !board.isSquareAttacked(opponent, this.x, this.y)
                        && !board.isSquareAttacked(opponent, this.x + 1, this.y)
                        && !board.isSquareAttacked(opponent, this.x + 2, this.y);
            if (pathClear && rookOk && safe) {
                out.add(board.toAlg(this.x + 2, this.y));
            }
        }

        // Queen-side: move two squares to the left
        if (canCastleQueenSide) {
            boolean pathClear = true;
            for (int xx = this.x - 1; xx >= this.x - 2; xx--) {
                if (!board.inBounds(xx, this.y)) { pathClear = false; break; }
                if (board.getPieceAt(xx, this.y) != null) { pathClear = false; break; }
            }
            // Find rook to the left with no pieces in between
            Piece rook = findFirstRookOnRay(board, -1, 0);
            boolean rookOk = (rook instanceof Rook) && rook.getColor() == this.color;
            boolean safe = !board.isSquareAttacked(opponent, this.x, this.y)
                        && !board.isSquareAttacked(opponent, this.x - 1, this.y)
                        && !board.isSquareAttacked(opponent, this.x - 2, this.y);
            if (pathClear && rookOk && safe) {
                out.add(board.toAlg(this.x - 2, this.y));
            }
        }

        return out;
    }

    @Override
    public Set<int[]> attackedSquares(Board board) {
        Set<int[]> out = new LinkedHashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int tx = this.x + dx;
                int ty = this.y + dy;
                if (board.inBounds(tx, ty)) out.add(new int[]{tx, ty});
            }
        }
        return out;
    }

    private Piece findFirstRookOnRay(Board board, int dx, int dy) {
        int nx = this.x + dx;
        int ny = this.y + dy;
        while (board.inBounds(nx, ny)) {
            Piece at = board.getPieceAt(nx, ny);
            if (at != null) return at;
            nx += dx; ny += dy;
        }
        return null;
    }
}