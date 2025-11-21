package com.predixcode.rules;

import java.util.ArrayList;
import java.util.List;

import com.predixcode.GameConfig;

public final class RuleBuilder {

    private RuleBuilder() {}

    /**
     * Build a composable ruleset from GameConfig.
     */
    public static List<Rule> buildRules(GameConfig cfg) {
        List<Rule> rules = new ArrayList<>();

        // Legality first
        rules.add(new StandardLegalityRule());

        // Special capture plugins that want to see the board pre-standard-capture
        if (cfg.bureaucratRule()) {
            rules.add(new BureaucratCaptureRule());
        }

        // Core movement
        rules.add(new StandardMovementRule());

        // End conditions
        rules.add(new StandardEndConditionRule());

        // Turn system
        if (cfg.multipleMoveRule()) {
            int w = Math.max(1, cfg.whiteMovesPerTurn());
            int b = Math.max(1, cfg.blackMovesPerTurn());
            rules.add(new MultipleMoveTurnRule(w, b));
        } else {
            rules.add(new StandardTurnRule());
        }

        return rules;
    }

    /**
     * Default ruleset if Board.ensureRules() is used without config
     * (optional convenience).
     */
    public static List<Rule> defaultRules() {
        GameConfig cfg = new GameConfig(null, false, false, 1, 1);
        return buildRules(cfg);
    }
}