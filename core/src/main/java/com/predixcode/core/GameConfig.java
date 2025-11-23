package com.predixcode.core;

/**
 * High-level configuration of a game, coming from the config UI.
 * boardWidth / boardHeight:
 *   0  => "use whatever the FEN implies" (no resize)
 *  >0  => explicitly resize the starting position to this size.
 * chess960:
 *   true  => randomize starting back rank (and mirror it) in a Chess960 style.
 */
public record GameConfig(
        String fenOverride,        // null/blank => use preset FEN
        boolean bureaucratRule,    // enable Bureaucrat capture behavior
        int whiteMovesPerTurn,
        int blackMovesPerTurn,
        int boardWidth,            // 0 = "auto from FEN"
        int boardHeight,           // 0 = "auto from FEN"
        boolean fillExpandedFiles,
        boolean chess960
) {
    public GameConfig {
        // Basic normalization / clamping
        whiteMovesPerTurn = Math.max(1, whiteMovesPerTurn);
        blackMovesPerTurn = Math.max(1, blackMovesPerTurn);

        // 0 means "use FEN dimensions"; otherwise must be >= 4 / 5.
        boardWidth = Math.max(0, boardWidth);
        boardHeight = Math.max(0, boardHeight);
    }

    /**
     * Backwards-compatible ctor used by existing UI.
     * No explicit board size, classic start layout.
     */
    public GameConfig(String fenOverride,
                      boolean bureaucratRule,
                      int whiteMovesPerTurn,
                      int blackMovesPerTurn) {
        
        this(fenOverride, bureaucratRule,
             whiteMovesPerTurn, blackMovesPerTurn,
             0, 0, false,
              false);
    }
}