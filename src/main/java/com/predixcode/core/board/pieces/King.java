package com.predixcode.core.board.pieces;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece {
    private boolean canCastleKingSide;
    private boolean canCastleQueenSide;

    public King() { this.fenSymbol = "k"; }

    public boolean canCastleKingSide() { return canCastleKingSide; }
    public boolean canCastleQueenSide() { return canCastleQueenSide; }

    public void setCastleKingSide(boolean canCastle) { this.canCastleKingSide = canCastle; }
    public void setCastleQueenSide(boolean canCastle) { this.canCastleQueenSide = canCastle; }

    @Override
    protected List<Integer[]> getMoves(int matrixX, int matrixY) {
        List<Integer[]> moves = new ArrayList<>();

        // Normal king moves (one square in any direction), bounded by board edges
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int tx = this.x + dx;
                int ty = this.y + dy;
                if (tx >= 0 && tx < matrixX && ty >= 0 && ty < matrixY) {
                    moves.add(new Integer[]{dx, dy});
                }
            }
        }

        // Castling moves (no blocking/check validation here)
        // King-side: move two squares to the right
        if (canCastleKingSide) {
            int tx = this.x + 2;
            if (tx >= 0 && tx < matrixX) {
                moves.add(new Integer[]{2, 0});
            }
        }

        // Queen-side: move two squares to the left
        if (canCastleQueenSide) {
            int tx = this.x - 2;
            if (tx >= 0 && tx < matrixX) {
                moves.add(new Integer[]{-2, 0});
            }
        }

        return moves;
    }
}