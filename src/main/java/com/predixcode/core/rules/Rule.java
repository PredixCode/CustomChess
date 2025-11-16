package com.predixcode.core.rules;


import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Piece;

public abstract class Rule {

    public abstract void applyOnStart(Board board);

    public abstract void applyOnTurn(Board board, Piece movingPiece, int[] fromXY, int[] toXY);

    public void applyOnTurn(Board board, Piece movingPiece, String from, String to) {
        int[] fromXY = board.fromAlg(from);
        int[] toXY   = board.fromAlg(to);
        applyOnTurn(board, movingPiece, fromXY, toXY);
    }

    protected abstract void checkIllegal(Board board, Piece movingPiece, int[] fromXY, int[] toXY);

    protected abstract boolean enPassant(boolean isCapture, Board board, Piece movingPiece, int[] fromXY, int[] toXY);

    protected abstract void castling(Board board, Piece movingPiece, int[] fromXY, int[] toXY, boolean isCapture);

    protected abstract void updateSpecialMoveStates(Board board, Piece movingPiece, int[] fromXY, int[] toXY, boolean isCapture);

    protected abstract void checkForEndConditions(Board board, Piece movingPiece);

    protected abstract void submitNextTurn(Board board, Piece movingPiece, boolean isCapture);
    
}
