package com.predixcode.ui;

import java.util.List;
import java.util.function.BiConsumer;

import com.predixcode.GameConfig;
import com.predixcode.ScenarioMeta;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public final class ConfigMenu {

    private ConfigMenu() {}

    public static Scene create(Stage stage,
                               List<ScenarioMeta> presets,
                               ScenarioMeta selected,
                               GameConfig config,
                               BiConsumer<ScenarioMeta, GameConfig> onStart,
                               Runnable onResume,
                               Runnable onExit,
                               boolean canResume) {

        Label title = new Label("Custom Chess – Configuration");
        title.setFont(Font.font(24));
        title.setTextFill(Paint.valueOf("#333"));

        // Preset selector
        Label presetLabel = new Label("Preset:");
        ComboBox<ScenarioMeta> presetBox = new ComboBox<>();
        presetBox.getItems().addAll(presets);
        presetBox.getSelectionModel().select(selected != null ? selected : presets.get(0));

        HBox presetRow = new HBox(8, presetLabel, presetBox);
        presetRow.setAlignment(Pos.CENTER_LEFT);

        // FEN input
        Label fenLabel = new Label("Start position (FEN):");
        TextField fenField = new TextField();
        fenField.setPromptText("Leave empty to use preset's default FEN");

        ScenarioMeta currentPreset = presetBox.getSelectionModel().getSelectedItem();
        String initialFen = (config != null && config.fenOverride() != null && !config.fenOverride().isBlank())
                ? config.fenOverride()
                : (currentPreset != null ? currentPreset.getDefaultFen() : "");
        fenField.setText(initialFen);

        VBox fenBox = new VBox(4, fenLabel, fenField);

        // Rule toggles
        CheckBox bureaucratChk = new CheckBox("Enable Bureaucrat rule");

        boolean initialBureaucrat = (config != null)
                ? config.bureaucratRule()
                : (currentPreset != null && currentPreset.isDefaultBureaucratRule());

        bureaucratChk.setSelected(initialBureaucrat);

        VBox rulesBox = new VBox(4,
            new Label("Rules:"),
            bureaucratChk
        );

        Label movesLabel = new Label("Moves per turn");

        TextField whiteMovesField = new TextField();
        whiteMovesField.setPromptText("White");
        whiteMovesField.setPrefColumnCount(3);

        TextField blackMovesField = new TextField();
        blackMovesField.setPromptText("Black");
        blackMovesField.setPrefColumnCount(3);

        int initialWhiteMoves = (config != null)
                ? config.whiteMovesPerTurn()
                : (currentPreset != null ? currentPreset.getDefaultWhiteMovesPerTurn() : 1);
        int initialBlackMoves = (config != null)
                ? config.blackMovesPerTurn()
                : (currentPreset != null ? currentPreset.getDefaultBlackMovesPerTurn() : 1);

        whiteMovesField.setText(String.valueOf(initialWhiteMoves));
        blackMovesField.setText(String.valueOf(initialBlackMoves));

        HBox movesRow = new HBox(8,
            new Label("White:"), whiteMovesField,
            new Label("Black:"), blackMovesField
        );
        movesRow.setAlignment(Pos.CENTER_LEFT);

        VBox movesBox = new VBox(4, movesLabel, movesRow);

        // When preset changes, reset fields to preset defaults (unless user edited)
        presetBox.valueProperty().addListener((obs, oldPreset, newPreset) -> {
            if (newPreset == null) return;
            fenField.setText(newPreset.getDefaultFen());
            bureaucratChk.setSelected(newPreset.isDefaultBureaucratRule());
            whiteMovesField.setText(String.valueOf(newPreset.getDefaultWhiteMovesPerTurn()));
            blackMovesField.setText(String.valueOf(newPreset.getDefaultBlackMovesPerTurn()));
        });

        // Buttons
        Button startBtn = new Button("Start new game");
        startBtn.setOnAction(e -> {
            ScenarioMeta meta = presetBox.getSelectionModel().getSelectedItem();
            if (meta == null) return;

            String fen = fenField.getText();
            if (fen != null && fen.isBlank()) fen = null;

            int wMoves = parseIntOrDefault(whiteMovesField.getText(), 1);
            int bMoves = parseIntOrDefault(blackMovesField.getText(), 1);

            GameConfig cfg = new GameConfig(
                fen,
                bureaucratChk.isSelected(),
                wMoves,
                bMoves
            );

            if (onStart != null) onStart.accept(meta, cfg);
        });

        Button resumeBtn = new Button("Resume");
        resumeBtn.setDisable(!canResume);
        resumeBtn.setOnAction(e -> {
            if (onResume != null) onResume.run();
        });

        Button exitBtn = new Button("Exit");
        exitBtn.setOnAction(e -> {
            if (onExit != null) onExit.run();
        });

        HBox buttons = new HBox(10, startBtn, resumeBtn, exitBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(12, 0, 0, 0));

        VBox root = new VBox(16, title, presetRow, fenBox, rulesBox, movesBox, buttons);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_LEFT);

        double w = (stage.getScene() != null) ? stage.getScene().getWidth() : 640;
        double h = (stage.getScene() != null) ? stage.getScene().getHeight() : 360;
        return new Scene(root, w, h);
    }

    private static int parseIntOrDefault(String s, int def) {
        if (s == null || s.isBlank()) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}