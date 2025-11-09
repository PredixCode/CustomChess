package com.predixcode.core.board.pieces;

import java.util.Collections;
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
    protected List<Integer[]> getMoves() {
        return Collections.emptyList();
    }
}