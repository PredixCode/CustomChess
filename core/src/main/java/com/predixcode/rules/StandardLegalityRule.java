package com.predixcode.rules;

import java.util.Set;

import com.predixcode.board.Board;
import com.predixcode.board.pieces.Piece;
import com.predixcode.colors.Color;

public class StandardLegalityRule extends Rule {

    @Override
    public void validateMove(Board board, MoveContext ctx) {
        Piece movingPiece = ctx.piece;
        int[] fromXY = ctx.fromXY;
        int[] toXY   = ctx.toXY;

        Color active = board.getActiveColor();
        if (active != null && !movingPiece.getColor().equals(active)) {
            throw new IllegalStateException("It is not " + movingPiece.getColor() + "'s turn");
        }

        // Would leave own king in check?
        if (board.wouldLeaveOwnKingInCheck(movingPiece, fromXY, toXY)) {
            throw new IllegalStateException("Illegal move: would leave own king in check");
        }

        // Ensure destination is in piece's legal moves
        String from = board.toAlg(fromXY[0], fromXY[1]);
        String to   = board.toAlg(toXY[0], toXY[1]);
        Set<String> targets = movingPiece.getLegalMoves(board);
        String toAlg = to.toLowerCase();
        if (!targets.contains(toAlg)) {
            throw new IllegalArgumentException("Destination " + to + " is not a legal target for " + from);
        }
    }
}
