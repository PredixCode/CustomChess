package com.predixcode.board.pieces;

import java.util.LinkedHashSet;
import java.util.Set;

import com.predixcode.board.Board;
import com.predixcode.colors.Color;

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
                int tx = this.posX + dx;
                int ty = this.posY + dy;
                if (!board.inBounds(tx, ty)) continue;
                Piece at = board.getPieceAt(tx, ty);
                if (at == null || !at.getColor().equals(this.color)) {
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
            for (int xx = this.posX + 1; xx <= this.posX + 2; xx++) {
                if (!board.inBounds(xx, this.posY)) { pathClear = false; break; }
                if (board.getPieceAt(xx, this.posY) != null) { pathClear = false; break; }
            }
            // Find rook to the right with no pieces in between
            Piece rook = findFirstRookOnRay(board, +1, 0);
            boolean rookOk = (rook instanceof Rook) && rook.getColor().equals(this.color);
            boolean safe = !board.isSquareAttacked(opponent, this.posX, this.posY)
                        && !board.isSquareAttacked(opponent, this.posX + 1, this.posY)
                        && !board.isSquareAttacked(opponent, this.posX + 2, this.posY);
            if (pathClear && rookOk && safe) {
                out.add(board.toAlg(this.posX + 2, this.posY));
            }
        }

        // Queen-side: move two squares to the left
        if (canCastleQueenSide) {
            boolean pathClear = true;
            for (int xx = this.posX - 1; xx >= this.posX - 2; xx--) {
                if (!board.inBounds(xx, this.posY)) { pathClear = false; break; }
                if (board.getPieceAt(xx, this.posY) != null) { pathClear = false; break; }
            }
            // Find rook to the left with no pieces in between
            Piece rook = findFirstRookOnRay(board, -1, 0);
            boolean rookOk = (rook instanceof Rook) && rook.getColor().equals(this.color);
            boolean safe = !board.isSquareAttacked(opponent, this.posX, this.posY)
                        && !board.isSquareAttacked(opponent, this.posX - 1, this.posY)
                        && !board.isSquareAttacked(opponent, this.posX - 2, this.posY);
            if (pathClear && rookOk && safe) {
                out.add(board.toAlg(this.posX - 2, this.posY));
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
                int tx = this.posX + dx;
                int ty = this.posY + dy;
                if (board.inBounds(tx, ty)) out.add(new int[]{tx, ty});
            }
        }
        return out;
    }

    private Piece findFirstRookOnRay(Board board, int dx, int dy) {
        int nx = this.posX + dx;
        int ny = this.posY + dy;
        while (board.inBounds(nx, ny)) {
            Piece at = board.getPieceAt(nx, ny);
            if (at != null) return at;
            nx += dx; ny += dy;
        }
        return null;
    }
}