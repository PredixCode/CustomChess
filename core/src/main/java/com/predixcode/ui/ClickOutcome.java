package com.predixcode.ui;

import java.util.Set;

import com.predixcode.board.MoveResult;

/**
 * UI-level outcome of a user click.
 *
 * - SELECT:   user selected a piece; includes selection + legal targets.
 * - MOVE_APPLIED: a move was successfully applied; wraps a MoveResult.
 * - MOVE_REJECTED: move failed validation; includes error message.
 * - NOOP:     nothing interesting (e.g. clicking empty square with no selection).
 */
public final class ClickOutcome {

    public enum Type { NOOP, SELECT, MOVE_APPLIED, MOVE_REJECTED }

    public final Type type;
    public final int[] selected;       // selection square for highlighting (or null)
    public final Set<String> legalTargets;
    public final MoveResult moveResult; // non-null only for MOVE_APPLIED
    public final String error;         // rejection reason (for MOVE_REJECTED)

    private ClickOutcome(
            Type type,
            int[] selected,
            Set<String> legalTargets,
            MoveResult moveResult,
            String error
    ) {
        this.type = type;
        this.selected = selected;
        this.legalTargets = legalTargets;
        this.moveResult = moveResult;
        this.error = error;
    }

    public static ClickOutcome noop() {
        return new ClickOutcome(Type.NOOP, null, Set.of(), null, null);
    }

    public static ClickOutcome selection(int[] selected, Set<String> legalTargets) {
        return new ClickOutcome(Type.SELECT, selected, legalTargets, null, null);
    }

    public static ClickOutcome moveApplied(MoveResult result) {
        if (result == null) throw new IllegalArgumentException("MoveResult cannot be null");
        return new ClickOutcome(Type.MOVE_APPLIED, null, Set.of(), result, null);
    }

    public static ClickOutcome moveRejected(String error) {
        return new ClickOutcome(Type.MOVE_REJECTED, null, Set.of(), null, error);
    }
}