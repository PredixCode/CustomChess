package com.predixcode.core.fen.rules;

import java.util.Random;

import com.predixcode.core.GameConfig;

/**
 * A "start position" rule: transforms the starting FEN based on GameConfig,
 * without touching any runtime move rules.
 */
public interface StartFenRule {

    /**
     * @param baseFen FEN from preset or user override.
     * @param cfg     the game configuration (width/height, chess960, etc.)
     * @param random  RNG for any randomized layouts (e.g., Chess960)
     * @return transformed FEN string (full 6-field FEN)
     */
    String apply(String baseFen, GameConfig cfg, Random random);
}
