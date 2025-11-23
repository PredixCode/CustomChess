package com.predixcode.core;

import java.util.List;

import com.predixcode.core.board.Board;
import com.predixcode.core.fen.StartPositionService;
import com.predixcode.core.rules.Rule;
import com.predixcode.core.rules.RuleBuilder;
import com.predixcode.core.ui.BoardController;

public final class GameFactory {

    private GameFactory() {}

    /**
     * Create a fully initialized BoardController for a given preset + config:
     *  - resolve base FEN (preset vs override)
     *  - apply board-size + Chess960 rules
     *  - build rules and call onGameStart
     */
    public static BoardController createGame(ScenarioMeta preset, GameConfig cfg) {
        // 1) Resolve base FEN
        String fenOverride = cfg.fenOverride();
        String baseFen = (fenOverride == null || fenOverride.isBlank())
                ? preset.getDefaultFen()
                : fenOverride;

        // 2) Apply start-position rules (size + Chess960)
        String finalFen = StartPositionService.buildStartingFen(baseFen, cfg);

        // 3) Build board + rules
        Board board = Board.fromFen(finalFen);
        List<Rule> rules = RuleBuilder.buildRules(cfg);
        board.setRules(rules);

        board.ensureRules();
        for (Rule r : board.getRules()) {
            r.onGameStart(board);
        }

        // 4) Wrap in controller (shared UI contract)
        return new BoardController(board);
    }
}
