package com.predixcode.core.rules;

import com.predixcode.core.board.Board;

public class DoubleMove extends StandardRule {

    boolean hasMoved = false;
    
    @Override
    protected void switchPlayer(Board board) {
        if (board.activeColor != null && hasMoved) {
            board.activeColor = board.activeColor.opposite();
            hasMoved = false;
        }
        hasMoved = true;
    }
    
}
