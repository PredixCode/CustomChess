package com.predixcode.core.board.pieces;

import java.util.LinkedHashSet;
import java.util.Set;

import com.predixcode.core.board.Board;

public class Knight extends Piece {

    public Knight() {
        super();
        this.fenSymbol = "n";
    }

    @Override
    public Set<String> pseudoLegalTargets(Board board) {
        Set<String> out = new LinkedHashSet<>();
        int[][] deltas = new int[][]{
            { 1,  2}, { 2,  1}, { 2, -1}, { 1, -2},
            {-1, -2}, {-2, -1}, {-2,  1}, {-1,  2}
        };
        for (int[] d : deltas) {
            int tx = this.x + d[0];
            int ty = this.y + d[1];
            if (!board.inBounds(tx, ty)) continue;
            var at = board.getPieceAt(tx, ty);
            if (at == null || at.getColor() != this.color) {
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
            int tx = this.x + d[0];
            int ty = this.y + d[1];
            if (board.inBounds(tx, ty)) out.add(new int[]{tx, ty});
        }
        return out;
    }
}