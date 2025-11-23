package com.predixcode.rules;

import com.predixcode.board.Board;
import com.predixcode.board.pieces.Piece;
import com.predixcode.colors.Color;

public class StandardEndConditionRule extends Rule {

    @Override
    public void afterMove(Board board, MoveContext ctx) {
        Piece movingPiece = ctx.piece;
        Color opponent = movingPiece.getColor().opposite();
        boolean opponentInCheck = board.isInCheck(opponent);

        if (opponentInCheck && board.hasNoLegalMoves(opponent)) {
            throw new IllegalStateException("Checkmate! " + movingPiece.getColor() + " wins.");
        }

        if (!opponentInCheck && board.hasNoLegalMoves(opponent)) {
            throw new IllegalStateException("Stalemate!");
        }
    }
}