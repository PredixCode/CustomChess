package com.predixcode.core.board.pieces;

import java.util.ArrayList;
import java.util.List;

public class Queen extends Piece {

    public Queen() {
        super();
        this.fenSymbol = "q";
    }

    @Override
    protected List<Integer[]> getMoves(int matrixX, int matrixY) {
        List<Integer[]> moves = new ArrayList<>();
        // Orthogonals
        addRay(moves, matrixX, matrixY,  1,  0);
        addRay(moves, matrixX, matrixY, -1,  0);
        addRay(moves, matrixX, matrixY,  0,  1);
        addRay(moves, matrixX, matrixY,  0, -1);
        // Diagonals
        addRay(moves, matrixX, matrixY,  1,  1);
        addRay(moves, matrixX, matrixY,  1, -1);
        addRay(moves, matrixX, matrixY, -1,  1);
        addRay(moves, matrixX, matrixY, -1, -1);
        return moves;
    }

    private void addRay(List<Integer[]> moves, int matrixX, int matrixY, int dx, int dy) {
        int nx = this.x + dx;
        int ny = this.y + dy;
        while (nx >= 0 && nx < matrixX && ny >= 0 && ny < matrixY) {
            moves.add(new Integer[]{nx - this.x, ny - this.y});
            nx += dx;
            ny += dy;
        }
    }
}