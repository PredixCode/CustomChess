package com.predixcode.rules;

import com.predixcode.board.Board;
import com.predixcode.board.pieces.Pawn;
import com.predixcode.board.pieces.Piece;
import com.predixcode.colors.Color;

public class StandardTurnRule extends Rule {

    @Override
    public void afterTurn(Board board, MoveContext ctx) {
        Piece movingPiece = ctx.piece;
        boolean isCapture = ctx.isCapture;

        // Halfmove clock
        if (movingPiece instanceof Pawn || isCapture) {
            board.resetHalfmove();
        } else {
            board.increaseHalfmove();
        }

        // Fullmove number (increment after Black's move)
        if (board.getActiveColor() == Color.BLACK) {
            board.increaseFullmove();
        }

        // Switch player
        Color c = board.getActiveColor();
        if (c != null) {
            board.setActiveColor(c.opposite());
        }
    }
}