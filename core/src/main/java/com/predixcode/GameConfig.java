package com.predixcode;

/**
 * High-level configuration of a game, coming from the config UI.
 */
public record GameConfig(
        String fenOverride,        // null/blank => use preset FEN
        boolean bureaucratRule,    // enable Bureaucrat capture behavior
        boolean multipleMoveRule,  // enable multi-move turn rule
        int whiteMovesPerTurn,     // used if multipleMoveRule is true
        int blackMovesPerTurn      // used if multipleMoveRule is true
) {
}