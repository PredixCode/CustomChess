package com.predixcode.core.rules;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.colors.Color;

public class StandardRule extends Rule {

    @Override
    public void applyOnStart(Board board) {}

    @Override
    public void applyOnTurn(Board board, Piece movingPiece, int[] fromXY, int[] toXY) {
        throwIfIllegal(board, movingPiece, fromXY, toXY);


        // Forbid moves that leave own king in check
        

        // Track capture state up-front
        boolean isCapture = isSquareOccupied(board, toXY);

        // Handle special moves (EP may modify isCapture, castling may move rook)
        isCapture = enPassante(isCapture, board, movingPiece, fromXY, toXY);
        castling(board, movingPiece, fromXY, toXY, isCapture);

        // Handle standard move and capture
        handleCaptureIfAny(board, toXY);

        // Make the move
        movePieceToDestination(movingPiece, toXY);

        // Keep castling rights accurate for all moves (king/rook moves or rook captured)
        board.updateCastlingRights(movingPiece, fromXY[0], fromXY[1], toXY[0], toXY[1], isCapture);

        // Check/checkmate evaluation against the opponent (post-move state)
        Color opponent = movingPiece.getColor().opposite();
        boolean opponentInCheck = isInCheck(board, opponent);

        if (opponentInCheck && !hasAnyLegalMove(board, opponent)) {
            throw new IllegalStateException("Checkmate! " + movingPiece.getColor() + " wins.");
        }

        // Optional stalemate detection (commented out by default)
        // if (!opponentInCheck && !hasAnyLegalMove(board, opponent)) {
        //     // board.activeColor = null;
        //     throw new IllegalStateException("Stalemate!");
        // }

        // Move bookkeeping and switch turn
        nextTurn(board, movingPiece, isCapture);
    }
}