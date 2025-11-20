package com.predixcode.core.rules;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Pawn;
import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.colors.Color;

/**
 * Turn rule where each side gets N moves per turn (can be different for White/Black).
 */
public class MultipleMoveTurnRule extends Rule {

    private final int whiteMovesPerTurn;
    private final int blackMovesPerTurn;

    private int movesLeftForActive;  // budget for current activeColor

    public MultipleMoveTurnRule(int whiteMovesPerTurn, int blackMovesPerTurn) {
        this.whiteMovesPerTurn = Math.max(1, whiteMovesPerTurn);
        this.blackMovesPerTurn = Math.max(1, blackMovesPerTurn);
    }

    @Override
    public void onGameStart(Board board) {
        Color active = board.getActiveColor();
        movesLeftForActive = getBudgetFor(active);
    }

    private int getBudgetFor(Color color) {
        return (color == Color.WHITE) ? whiteMovesPerTurn : blackMovesPerTurn;
    }

    @Override
    public void afterTurn(Board board, MoveContext ctx) {
        Piece movingPiece = ctx.piece;
        boolean isCapture = ctx.isCapture;

        // Halfmove clock (same as classic)
        if (movingPiece instanceof Pawn || isCapture) {
            board.resetHalfmove();
        } else {
            board.increaseHalfmove();
        }

        Color active = board.getActiveColor();
        if (active == null) {
            // Fallback: behave as classic
            board.increaseFullmove();
            switchPlayer(board);
            movesLeftForActive = getBudgetFor(board.getActiveColor());
            return;
        }

        if (movesLeftForActive <= 0) {
            movesLeftForActive = getBudgetFor(active);
        }

        movesLeftForActive--;

        if (movesLeftForActive == 0) {
            // End of this side's turn
            if (active == Color.BLACK) {
                board.increaseFullmove();
            }
            switchPlayer(board);
            movesLeftForActive = getBudgetFor(board.getActiveColor());
        }
        // else: keep same activeColor; next move is still this side
    }

    private void switchPlayer(Board board) {
        Color c = board.getActiveColor();
        if (c != null) board.setActiveColor(c.opposite());
    }
}