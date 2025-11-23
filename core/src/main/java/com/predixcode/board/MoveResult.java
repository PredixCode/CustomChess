package com.predixcode.board;

import com.predixcode.board.pieces.Piece;

/**
 * Pure model-level result of applying a move on the Board.
 */
public final class MoveResult {
    private final int[] from;
    private final int[] to;
    private final Piece captured;

    public MoveResult(int[] from, int[] to, Piece captured) {
        this.from = from;
        this.to = to;
        this.captured = captured;
    }

    public int[] getFrom() { return from; }
    public int[] getTo() { return to; }
    public Piece getCaptured() { return captured; }
}