package com.predixcode.core.rules;

import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.colors.Color;

/**
 * Shared move state visible to all rules during a single move.
 */
public final class MoveContext {

    public final Piece piece;
    public final int[] fromXY;
    public final int[] toXY;
    public final Color movingColor;

    // Capture info – rules can set or modify these.
    public boolean isCapture = false;
    public Piece capturedPiece = null;
    public boolean captureHandled = false;  // if true, other rules should not remove it again

    // Special move flags
    public boolean isEnPassant = false;
    public boolean isCastling  = false;

    // Turn control: if a TurnRule wants multiple moves, it can set endsTurn = false
    public boolean endsTurn = true;

    public MoveContext(Piece piece, int[] fromXY, int[] toXY) {
        this.piece = piece;
        this.fromXY = fromXY;
        this.toXY   = toXY;
        this.movingColor = piece.getColor();
    }

    // --- New helper methods used by Board ---

    public Piece getCapturedPiece() {
        return capturedPiece;
    }

    public void setCapturedPiece(Piece capturedPiece) {
        this.capturedPiece = capturedPiece;
    }
}