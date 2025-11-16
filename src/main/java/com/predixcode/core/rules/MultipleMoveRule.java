package com.predixcode.core.rules;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Piece;

/**
 * Each side must make two moves before the turn passes.
 * - activeColor stays the same until its 2-move budget is consumed.
 * - fullmove increments only when Black completes its second move (end of a ply).
 */
public class MultipleMoveRule extends StandardRule {

    // Tracks how many moves remain for the current active side
    private Integer movesPerSide;
    private Integer movesLeftForActive = null; // null => initialize on first use

    public MultipleMoveRule(int movesPerSide) {
        this.movesPerSide = movesPerSide;
    }

    @Override
    public void applyOnStart(Board board) {
        // Initialize the budget for the side to move from FEN
        movesLeftForActive = 2;
    }

    @Override
    protected void submitNextTurn(Board board, Piece movingPiece, boolean isCapture) {
        // Keep Standard half-move clock behavior
        updateHalfmove(board, movingPiece, isCapture);

        // Initialize budget if needed (e.g., game restored)
        if (movesLeftForActive == null) {
            movesLeftForActive = movesPerSide;
        }

        // Consume one move from the active side's budget
        movesLeftForActive = Math.max(0, movesLeftForActive - 1);

        if (movesLeftForActive == 0) {
            updateFullMove(board);
            switchPlayer(board);
            movesLeftForActive = movesPerSide;
        }
        // else: keep the same activeColor for the second move; do not switch or bump fullmove
    }
}