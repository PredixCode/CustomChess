package com.predixcode.rules;

import com.predixcode.board.Board;

/**
 * Composable rule with multiple hooks.
 * Override only what you need.
 */
public abstract class Rule {

    /** Called once when a new game starts (after Board is set up). */
    public void onGameStart(Board board) {}

    /** Called before any validation. Rarely used. */
    public void beforeMove(Board board, MoveContext ctx) {}

    /**
     * Called to validate a move.
     * Throw IllegalArgumentException / IllegalStateException to reject the move.
     */
    public void validateMove(Board board, MoveContext ctx) {}

    /**
     * Called after the piece has been moved (core move performed) and
     * after base capture has been processed (if you do that in the core).
     */
    public void afterMove(Board board, MoveContext ctx) {}

    /**
     * Called at the end of the move. Good place for:
     * - halfmove/fullmove updates
     * - activeColor / multi-move budget
     * - end conditions (checkmate, stalemate, custom wins)
     */
    public void afterTurn(Board board, MoveContext ctx) {}
}