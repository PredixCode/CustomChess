package com.predixcode.core.board.pieces;

import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece {

    public Knight() {
        super();
        this.fenSymbol = "n";
    }

    @Override
    protected List<Integer[]> getMoves(int matrixX, int matrixY) {
        List<Integer[]> moves = new ArrayList<>();
        int[][] deltas = new int[][]{
            { 1,  2}, { 2,  1}, { 2, -1}, { 1, -2},
            {-1, -2}, {-2, -1}, {-2,  1}, {-1,  2}
        };
        for (int[] d : deltas) {
            int tx = this.x + d[0];
            int ty = this.y + d[1];
            if (tx >= 0 && tx < matrixX && ty >= 0 && ty < matrixY) {
                moves.add(new Integer[]{d[0], d[1]});
            }
        }
        return moves;
    }
}