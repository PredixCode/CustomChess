package com.predixcode.core.rules;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Piece;

public class StandardRule extends Rule {

    @Override
    public void applyOnStart(Board board) {}

    @Override
    public void applyOnTurn(Board board, Piece movingPiece, int[] fromXY, int[] toXY) {
        checkIllegal(board, movingPiece, fromXY, toXY);

        // Track capture state
        boolean isCapture = isSquareOccupied(board, toXY);

        // Handle special moves
        isCapture = enPassante(isCapture, board, movingPiece, fromXY, toXY);
        castling(board, movingPiece, fromXY, toXY, isCapture);

        handleCaptureIfAny(board, toXY);
        movePieceToDestination(movingPiece, toXY);
        updateSpecialMoveStates(board, movingPiece, fromXY, toXY, isCapture);

        checkForEndConditions(board, movingPiece);
        submitNextTurn(board, movingPiece, isCapture);
    }
}