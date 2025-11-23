package com.predixcode.core.fen.rules;

import java.util.Random;

import com.predixcode.core.GameConfig;
import com.predixcode.core.fen.FenUtils;

/**
 * Base class that handles splitting/joining the full FEN.
 * Subclasses only implement board-part logic.
 */
public abstract class AbstractStartFenRule implements StartFenRule {

    @Override
    public final String apply(String baseFen, GameConfig cfg, Random random) {
        String[] fields = FenUtils.splitFen(baseFen);
        String boardPart = fields[0];

        String transformedBoard = applyToBoard(boardPart, cfg, random);
        fields[0] = transformedBoard;

        return FenUtils.joinFen(fields);
    }

    /**
     * Transform only the board description (field 0 of FEN).
     */
    protected abstract String applyToBoard(
            String boardPart,
            GameConfig cfg,
            Random random
    );
}