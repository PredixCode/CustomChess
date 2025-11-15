package com.predixcode.core.rules;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Piece;

public class DoubleMove extends StandardRule {

    boolean hasMoved = false;

    @Override
    public void applyOnTurn(Board board, Piece movingPiece, String from, String to) {
        super.applyOnTurn(board, movingPiece, from, to);
    }

    @Override
    protected void switchPlayer(Board board) {
        if (board.activeColor != null && hasMoved) {
            board.activeColor = board.activeColor.opposite();
            hasMoved = false;
        }
        hasMoved = true;
    }
    
}
