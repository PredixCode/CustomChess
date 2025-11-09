package com.predixcode.core.board.pieces;

import java.util.Collections;
import java.util.List;

public class Rook extends Piece {

    public Rook() {
        super();
        this.fenSymbol = "r";
    }

    @Override
    protected List<Integer[]> getMoves() {
        // Implement later. Returning empty list so code compiles.
        return Collections.emptyList();
    }
}