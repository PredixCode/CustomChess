package com.predixcode.core.rules;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Bureaucrat;
import com.predixcode.core.board.pieces.Piece;

public class BureaucratRule extends StandardRule {

    @Override
    public void apply(Board  board, Piece movingPiece, String from, String to) {
        throwIfIllegal(board, movingPiece, to, from);

        // Prepare move
        int[] fromXY = board.fromAlg(from);
        int[] toXY   = board.fromAlg(to);
        boolean isCapture = isSquareOccupied(board, toXY);

        // Handle special moves
        isCapture = enPassante(isCapture, board, movingPiece, fromXY, toXY);
        castling(board, movingPiece, fromXY, toXY, isCapture);

        // Handle standard move and capture
        handleCaptureIfAny(board, toXY);

        movePieceToDestination(movingPiece, toXY);
        nextTurn(board, movingPiece, isCapture);
    }

    @Override
    protected void handleCaptureIfAny(Board board, int[] toXY) {
        Piece captured = board.getPieceAt(toXY[0], toXY[1]);
        boolean isBureaucrat = captured != null && captured instanceof Bureaucrat;

        if (captured != null) {
            if (isBureaucrat) {
                captured.actionOnCapture(board);
            } else {
                board.pieces.remove(captured);
            }
        }
    }
    
}
