package com.predixcode.core.rules;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.colors.Color;
import com.predixcode.core.board.pieces.Piece;

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