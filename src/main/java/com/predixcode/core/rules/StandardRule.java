package com.predixcode.core.rules;

import java.util.Set;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.King;
import com.predixcode.core.board.pieces.Pawn;
import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.colors.Color;

public class StandardRule extends Rule {

    @Override
    public void applyOnStart(Board board) {}

    @Override
    public void applyOnTurn(Board board, Piece movingPiece, int[] fromXY, int[] toXY) {
        checkIllegal(board, movingPiece, fromXY, toXY);

        // Track capture state
        boolean isCapture = board.isSquareOccupied(toXY);

        // Handle special moves
        isCapture = enPassant(isCapture, board, movingPiece, fromXY, toXY);
        castling(board, movingPiece, fromXY, toXY, isCapture);

        handleCaptureIfAny(board, toXY);
        movingPiece.setPosition(toXY[0], toXY[1]);
        updateSpecialMoveStates(board, movingPiece, fromXY, toXY, isCapture);

        checkForEndConditions(board, movingPiece);
        submitNextTurn(board, movingPiece, isCapture);
    }

    @Override
    protected void checkIllegal(Board board, Piece movingPiece, int[] fromXY, int[] toXY) {
        if (board.getActiveColor() != null && !movingPiece.getColor().equals(board.getActiveColor())) {
            throw new IllegalStateException("It is not " + movingPiece.getColor() + "'s turn");
        }

        if (board.wouldLeaveOwnKingInCheck(movingPiece, fromXY, toXY)) {
            throw new IllegalStateException("Illegal move: would leave own king in check");
        }

        String from = board.toAlg(fromXY[0], fromXY[1]); 
        String to = board.toAlg(toXY[0], toXY[1]);
        Set<String> targets = movingPiece.getLegalMoves(board);
        String toAlg = to.toLowerCase();
        if (!targets.contains(toAlg)) {
            throw new IllegalArgumentException("Destination " + to + " is not a legal target for " + from);
        }
    }

    @Override
    protected boolean enPassant(boolean isCapture, Board board, Piece movingPiece, int[] fromXY, int[] toXY) {
        boolean enPassanteDone = board.performEnPassantIfApplicable(movingPiece, fromXY, toXY);
        if (enPassanteDone) {
            isCapture = true;
        }
        return isCapture;
    }

    @Override
    protected void castling(Board board, Piece movingPiece, int[] fromXY, int[] toXY, boolean isCapture) {
        boolean isKing = movingPiece instanceof King;
        if (isKing && board.isCastlingMove(fromXY, toXY)) {
            board.handleCastling((King) movingPiece, fromXY, toXY);
        } 
    }

    @Override
    protected void updateSpecialMoveStates(Board board, Piece movingPiece, int[] fromXY, int[] toXY, boolean isCapture) {
        board.updateEnPassantTargetIfApplicable(movingPiece, fromXY, toXY);
        board.updateCastlingRights(movingPiece, fromXY[0], fromXY[1], toXY[0], toXY[1], isCapture);
    }

    @Override
    protected void checkForEndConditions(Board board, Piece movingPiece) {
        Color opponent = movingPiece.getColor().opposite();
        boolean opponentInCheck = board.isInCheck(opponent);

        // Check mate
        if (opponentInCheck && !board.hasAnyLegalMove(opponent)) {
            throw new IllegalStateException("Checkmate! " + movingPiece.getColor() + " wins.");
        }
        // Stalemate
        if (!opponentInCheck && !board.hasAnyLegalMove(opponent)) {
             throw new IllegalStateException("Stalemate!");
        }
    }

    @Override
    protected void submitNextTurn(Board board, Piece movingPiece, boolean isCapture) {
        updateHalfmove(board, movingPiece, isCapture);
        updateFullMove(board);
        switchPlayer(board);
    }

    protected void handleCaptureIfAny(Board board, int[] toXY) {
        board.handleCaptureIfAny(toXY);
    }

    protected void updateHalfmove(Board board, Piece movingPiece, boolean isCapture) {
        if (movingPiece instanceof Pawn || isCapture) {
            board.resetHalfmove();
        } else {
            board.increaseHalfmove();
        }
    }

    protected void updateFullMove(Board board) {
        if (board.getActiveColor() == Color.BLACK) {
            board.increaseFullmove();
        }
    }

    protected void switchPlayer(Board board) {
        Color c = board.getActiveColor();
        if (c != null) {
            board.setActiveColor(c.opposite());
        }
    }
}