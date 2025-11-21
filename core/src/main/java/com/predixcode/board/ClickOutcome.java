package com.predixcode.board;

import java.util.Set;

import com.predixcode.board.pieces.Piece;

public final class ClickOutcome {
    public enum Type { NOOP, SELECT, MOVE_APPLIED, MOVE_REJECTED }

    public final Type type;
    public final int[] selected;       // selection square for highlighting
    public final Set<String> legalTargets;
    public final int[] from;           // move from (if applied)
    public final int[] to;             // move to (if applied)
    public final Piece captured;       // captured piece identity (if any)
    public final String error;         // rejection reason (optional)

    private ClickOutcome(Type t, int[] sel, Set<String> lt, int[] f, int[] to, Piece cap, String err) {
        this.type = t; this.selected = sel; this.legalTargets = lt;
        this.from = f; this.to = to; this.captured = cap; this.error = err;
    }
    public static ClickOutcome noop() { return new ClickOutcome(Type.NOOP, null, Set.of(), null, null, null, null); }
    public static ClickOutcome selection(int[] sel, Set<String> lt) { return new ClickOutcome(Type.SELECT, sel, lt, null, null, null, null); }
    public static ClickOutcome moveApplied(int[] from, int[] to, Piece cap) { return new ClickOutcome(Type.MOVE_APPLIED, null, Set.of(), from, to, cap, null); }
    public static ClickOutcome moveRejected(String err) { return new ClickOutcome(Type.MOVE_REJECTED, null, Set.of(), null, null, null, err); }
}