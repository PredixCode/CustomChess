package com.predixcode.core.fen;

import java.util.Random;

import com.predixcode.core.GameConfig;
import com.predixcode.core.fen.rules.BoardSizeFenRule;
import com.predixcode.core.fen.rules.Chess960FenRule;
import com.predixcode.core.fen.rules.StartFenRule;

/**
 * Single entry point for GUIs: given a preset FEN + GameConfig,
 * compute the final starting FEN after applying board-size and
 * Chess960 rules.
 */
public final class StartPositionService {

    private StartPositionService() {}

    public static String buildStartingFen(String baseFen, GameConfig cfg) {
        return buildStartingFen(baseFen, cfg, new Random());
    }

    public static String buildStartingFen(String baseFen, GameConfig cfg, Random random) {
        System.out.println("BASE FEN : " + baseFen);
        

        if (cfg == null) {
            return baseFen;
        }

        StartFenRule rule = cfg.chess960()
                ? new Chess960FenRule()
                : new BoardSizeFenRule();

        String finalFen = rule.apply(baseFen, cfg, random);
        System.out.println("FINAL FEN: " + finalFen);
        return finalFen;
    }
}