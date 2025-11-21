package com.predixcode.board.pieces;

import java.util.LinkedHashSet;
import java.util.Set;

import com.predixcode.board.Board;

public class Knight extends Piece {

    public Knight() {
        super();
        this.fenSymbol = "n";
    }

    @Override
    public Set<String> getLegalMoves(Board board) {
        Set<String> out = new LinkedHashSet<>();
        int[][] deltas = new int[][]{
            { 1,  2}, { 2,  1}, { 2, -1}, { 1, -2},
            {-1, -2}, {-2, -1}, {-2,  1}, {-1,  2}
        };
        for (int[] d : deltas) {
            int tx = this.posX + d[0];
            int ty = this.posY + d[1];
            if (!board.inBounds(tx, ty)) continue;
            Piece at = board.getPieceAt(tx, ty);
            if (at == null || !at.getColor().equals(this.color)) {
                out.add(board.toAlg(tx, ty));
            }
        }
        return out;
    }

    @Override
    public Set<int[]> attackedSquares(Board board) {
        Set<int[]> out = new LinkedHashSet<>();
        int[][] deltas = new int[][]{
            { 1,  2}, { 2,  1}, { 2, -1}, { 1, -2},
            {-1, -2}, {-2, -1}, {-2,  1}, {-1,  2}
        };
        for (int[] d : deltas) {
            int tx = this.posX + d[0];
            int ty = this.posY + d[1];
            if (board.inBounds(tx, ty)) out.add(new int[]{tx, ty});
        }
        return out;
    }
}