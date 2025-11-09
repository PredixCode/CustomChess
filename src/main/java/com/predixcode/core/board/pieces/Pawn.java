package com.predixcode.core.board.pieces;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {

    boolean hasMoved = false;

    public Pawn() {
        super();
        this.fenSymbol = "p";
    }

    @Override
    protected List<Integer[]> getMoves(int matrixX, int matrixY) {
        List<Integer[]> moves = new ArrayList<>();
        moves.add(new Integer[]{0, 1});
        if (!hasMoved)
            moves.add(new Integer[]{0, 2});
        return moves;
    }
}
