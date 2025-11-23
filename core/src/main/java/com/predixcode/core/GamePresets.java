package com.predixcode.core;

import java.util.List;


public final class GamePresets {

    public static final String STANDARD_FEN =
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static final String BUREAUCRAT_FEN =
        "rnbqkbnr/pppppppp/3c4/8/8/4C3/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    // Presets only: they just pre-fill the config screen
    public static final List<ScenarioMeta> PRESETS = List.of(
        new ScenarioMeta("Standard",           STANDARD_FEN,   false, 1, 1),
        new ScenarioMeta("Double move x2",     STANDARD_FEN,   false, 2, 2),
        new ScenarioMeta("Bureaucrat",         BUREAUCRAT_FEN, true,  1, 1),
        new ScenarioMeta("Bureaucrat + DM x2", BUREAUCRAT_FEN, true,  2, 2)
    );

    public static final GameConfig DEFAULT_CONFIG = new GameConfig(
        null,
        false,
        1,
        1,
        0,      // boardWidth  (0 = auto from FEN)
        0,      // boardHeight (0 = auto from FEN)
        false, 
        false
    );

    private GamePresets() {}
}
