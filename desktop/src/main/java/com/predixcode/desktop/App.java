package com.predixcode.desktop;

import java.util.List;

import com.predixcode.core.GameConfig;
import com.predixcode.core.GameFactory;
import com.predixcode.core.GamePresets;
import com.predixcode.core.ScenarioMeta;
import com.predixcode.core.ui.BoardController;
import com.predixcode.desktop.ui.ConfigMenuScreen;
import com.predixcode.desktop.ui.GameScreen;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static final List<ScenarioMeta> PRESETS = GamePresets.PRESETS;

    private ScenarioMeta selectedPreset = PRESETS.get(0);
    private GameConfig currentConfig = GamePresets.DEFAULT_CONFIG;

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
            (preset, cfg) -> {
                selectedPreset = preset;
                currentConfig = cfg;
                startNewGame(stage);
            },
            () -> resumeGame(stage),
            Platform::exit,
            canResume && activeGui != null && activeGui.getGameScene() != null
        );

        stage.setTitle("Custom Chess – Menu");
        stage.setScene(scene);
        stage.show();
    }

    private void startNewGame(Stage stage) {
        try {
            BoardController controller =
                GameFactory.createGame(selectedPreset, currentConfig);

            GameScreen gameScreen = new GameScreen(controller);
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

    public static void main(String[] args) {
        launch(args);
    }
}