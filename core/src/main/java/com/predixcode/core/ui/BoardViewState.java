package com.predixcode.core.ui;

import java.util.List;
import java.util.Set;

/**
 * Immutable snapshot of UI-relevant board state.
 * Both desktop and Android render purely from this.
 * Coordinates are board-space [x,y] (0-based, origin at top-left),
 * legal targets are in algebraic notation ("e4", "a1", ...),
 * moveHistory is a simple list of ply strings like "e2-e4".
 */
public final class BoardViewState {

    private final int[] selectedSquare;   // [x,y] or null
    private final Set<String> legalTargets;
    private final int[] lastFrom;         // [x,y] or null
    private final int[] lastTo;           // [x,y] or null
    private final List<String> moveHistory;
    private final String lastError;       // null if none

    public BoardViewState(
            int[] selectedSquare,
            Set<String> legalTargets,
            int[] lastFrom,
            int[] lastTo,
            List<String> moveHistory,
            String lastError
    ) {
        this.selectedSquare = selectedSquare;
        this.legalTargets = legalTargets;
        this.lastFrom = lastFrom;
        this.lastTo = lastTo;
        this.moveHistory = moveHistory;
        this.lastError = lastError;
    }

    /**
     * Selected square coordinates or null.
     */
    public int[] getSelectedSquare() {
        return selectedSquare;
    }

    /**
     * Legal target squares in algebraic notation (e.g. "e4").
     * Set is immutable.
     */
    public Set<String> getLegalTargets() {
        return legalTargets;
    }

    /**
     * Last move "from" square [x,y] or null if no moves yet.
     */
    public int[] getLastFrom() {
        return lastFrom;
    }

    /**
     * Last move "to" square [x,y] or null if no moves yet.
     */
    public int[] getLastTo() {
        return lastTo;
    }

    /**
     * Move history as a list of ply strings like "e2-e4".
     * List is immutable.
     */
    public List<String> getMoveHistory() {
        return moveHistory;
    }

    /**
     * Last error message (e.g. from a rejected move), or null.
     */
    public String getLastError() {
        return lastError;
    }
}