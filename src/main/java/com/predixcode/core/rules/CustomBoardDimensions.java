package com.predixcode.core.rules;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Piece;

public class CustomBoardDimensions implements Rule {

    private final int width;
    private final int height;
    private boolean wasApplied = false;

    public CustomBoardDimensions(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void apply(Board board, Piece movingPiece, String from, String to) {
        if (wasApplied) {
            wasApplied = false;
            return;
        }
        board.width = this.width;
        board.height = this.height;
        wasApplied = true;
    }
    
}
