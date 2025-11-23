package com.predixcode.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.predixcode.board.Board;
import com.predixcode.board.MoveResult;
import com.predixcode.board.pieces.Piece;
import com.predixcode.colors.Color;

/**
 * UI adapter for a Board.
 * Owns:
 *  - Selection state
 *  - Cached legal targets
 *  - Last move coordinates
 *  - Move history (e.g. "e2-e4")
 *  - Last error message (for rejected moves)
 * Exposes:
 *  - handleClick(x,y): processes user clicks and returns a ClickOutcome event
 *    (used e.g. by JavaFX for animation)
 *  - getViewState(): immutable BoardViewState snapshot for rendering
 */
public class BoardController {

    private final Board board;

    private int[] selectedSquare = null;              // [x,y]
    private Set<String> cachedLegalTargets = Set.of();// current highlight set
    private int[] lastFromXY = null, lastToXY = null; // last move
    private final List<String> moveHistory = new ArrayList<>();
    private String lastError = null;

    public BoardController(Board board) {
        if (board == null) {
            throw new IllegalArgumentException("Board cannot be null");
        }
        this.board = board;
    }

    public Board getBoard() {
        return board;
    }

    /**
     * Main UI entry point: user clicked on board square (x,y).
     * Returns a ClickOutcome describing what happened (for
     * animation / feedback).
     * After calling this, ALWAYS call getViewState() to
     * re-render from the new state.
     */
    public ClickOutcome handleClick(int x, int y) {
        lastError = null;

        // First click = selection
        if (selectedSquare == null) {
            Piece p = board.getPieceAt(x, y);
            if (p == null) {
                return ClickOutcome.noop();
            }

            Color active = board.getActiveColor();
            if (active != null && !active.equals(p.getColor())) {
                return ClickOutcome.noop();
            }

            selectedSquare = new int[] { x, y };
            cachedLegalTargets = board.computeLegalTargets(p);
            return ClickOutcome.selection(copyXY(selectedSquare), Set.copyOf(cachedLegalTargets));
        }

        // Second click = reselect or attempt move
        Piece selPiece = board.getPieceAt(selectedSquare[0], selectedSquare[1]);
        Piece clicked  = board.getPieceAt(x, y);

        if (selPiece == null) {
            clearSelection();
            return ClickOutcome.noop();
        }

        // Reselect same-color piece
        if (clicked != null && clicked.getColor().equals(selPiece.getColor())) {
            selectedSquare = new int[] { x, y };
            cachedLegalTargets = board.computeLegalTargets(clicked);
            return ClickOutcome.selection(copyXY(selectedSquare), Set.copyOf(cachedLegalTargets));
        }

        // Attempt to apply move
        String fromAlg = board.toAlg(selectedSquare[0], selectedSquare[1]);
        String toAlg   = board.toAlg(x, y);

        try {
            MoveResult result = board.applyTurnWithResult(fromAlg, toAlg);

            lastFromXY = result.getFrom();
            lastToXY   = result.getTo();

            recordMoveInHistory(lastFromXY, lastToXY);

            clearSelection(); // also clears legal targets

            return ClickOutcome.moveApplied(
                copyXY(lastFromXY),
                copyXY(lastToXY),
                result.getCaptured()
            );
        } catch (Exception ex) {
            clearSelection();
            lastError = ex.getMessage();
            return ClickOutcome.moveRejected(lastError);
        }
    }

    /**
     * Immutable snapshot for rendering. Both Android and desktop
     * should use this as the single source of truth for view state.
     */
    public BoardViewState getViewState() {
        return new BoardViewState(
            copyXY(selectedSquare),
            Set.copyOf(cachedLegalTargets),
            copyXY(lastFromXY),
            copyXY(lastToXY),
            List.copyOf(moveHistory),
            lastError
        );
    }

    public void clearSelection() {
        selectedSquare = null;
        cachedLegalTargets = Set.of();
    }

    private void recordMoveInHistory(int[] from, int[] to) {
        if (from == null || to == null) return;
        String ply = board.toAlg(from[0], from[1]) + "-" + board.toAlg(to[0], to[1]);
        moveHistory.add(ply);
    }

    private static int[] copyXY(int[] src) {
        if (src == null) return null;
        return new int[] { src[0], src[1] };
    }
}