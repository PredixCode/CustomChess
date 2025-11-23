package com.predixcode.desktop;

import java.util.List;

import com.predixcode.core.GameConfig;
import com.predixcode.core.ScenarioMeta;
import com.predixcode.core.board.Board;
import com.predixcode.core.fen.StartPositionService;
import com.predixcode.core.rules.Rule;
import com.predixcode.core.rules.RuleBuilder;
import com.predixcode.desktop.ui.ConfigMenuScreen;
import com.predixcode.desktop.ui.GameScreen;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static final String STANDARD_FEN =
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private static final String BUREAUCRAT_FEN =
        "rnbqkbnr/pppppppp/3c4/8/8/4C3/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    // Presets only: they just pre-fill the config screen
    private static final List<ScenarioMeta> PRESETS = List.of(
        // name, defaultFEN, bureaucrat, multiMove, whiteMoves, blackMoves
        new ScenarioMeta("Standard",           STANDARD_FEN,   false, 1, 1),
        new ScenarioMeta("Double move x2",     STANDARD_FEN,   false, 2, 2),
        new ScenarioMeta("Bureaucrat",         BUREAUCRAT_FEN, true,  1, 1),
        new ScenarioMeta("Bureaucrat + DM x2", BUREAUCRAT_FEN, true,  2, 2)
    );

    private ScenarioMeta selectedPreset = PRESETS.get(0);

    // The last-used configuration (so the UI remembers choices)
    private GameConfig currentConfig = new GameConfig(
        null,   // fenOverride
        false,  // bureaucratRule
        1,      // whiteMovesPerTurn
        1       // blackMovesPerTurn
    );

    private GameScreen activeGui;  // currently running game (if any)

    @Override
    public void start(Stage primaryStage) {
        showConfigMenu(primaryStage, false);
    }

    private void showConfigMenu(Stage stage, boolean canResume) {
        Scene scene = ConfigMenuScreen.create(
            stage,
            PRESETS,
            selectedPreset,
            currentConfig,
            // onStart: user pressed "Start new game"
            (preset, cfg) -> {
                selectedPreset = preset;
                currentConfig = cfg;
                startNewGame(stage);
            },
            // onResume: user pressed "Resume"
            () -> resumeGame(stage),
            // onExit
            Platform::exit,
            canResume && activeGui != null && activeGui.getGameScene() != null
        );

        stage.setTitle("Custom Chess – Menu");
        stage.setScene(scene);
        stage.show();
    }

    private void startNewGame(Stage stage) {
        try {
            Board board = createBoardFromConfig(selectedPreset, currentConfig);
            List<Rule> rules = RuleBuilder.buildRules(currentConfig);
            board.setRules(rules);          // we'll add setRules to Board

            GameScreen gameScreen = new GameScreen(board);

            // When user clicks "← Menu" in the game, go back to config
            gameScreen.setBackToMenuHandler(() -> showConfigMenu(stage, true));

            gameScreen.start(stage);
            activeGui = gameScreen;
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    private void resumeGame(Stage stage) {
        if (activeGui != null && activeGui.getGameScene() != null) {
            stage.setScene(activeGui.getGameScene());
            stage.show();
        }
    }

    /**
     * Decide which FEN to use and create the Board.
     * Right now: fenOverride (if any) or preset FEN.
     */
    private Board createBoardFromConfig(ScenarioMeta preset, GameConfig cfg) {
        String fen = cfg.fenOverride();
        if (fen == null || fen.isBlank()) {
            fen = preset.getDefaultFen();
        }
        
        String finalFen = StartPositionService.buildStartingFen(fen, cfg);
        return Board.fromFen(finalFen);
    }

    public static void main(String[] args) {
        launch(args);
    }
}