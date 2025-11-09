package com.predixcode.core.board.pieces;

import java.util.List;

public class Pawn extends Piece {

    public Pawn() {
        super();
        this.fenSymbol = "p";
    }

    @Override
    protected List<Integer[]> getMoves() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMoves'");
    }
}
