package com.predixcode.desktop.ui;

import java.util.List;
import java.util.function.BiConsumer;

import com.predixcode.core.GameConfig;
import com.predixcode.core.ScenarioMeta;

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

public final class ConfigMenuScreen {

    private ConfigMenuScreen() {}

    public static Scene create(Stage stage,
                               List<ScenarioMeta> presets,
                               ScenarioMeta selected,
                               GameConfig config,
                               BiConsumer<ScenarioMeta, GameConfig> onStart,
                               Runnable onResume,
                               Runnable onExit,
                               boolean canResume) {

        // ---------------------------------------------------------------------
        // Title
        // ---------------------------------------------------------------------
        Label title = new Label("Custom Chess – Configuration");
        title.setFont(Font.font(24));
        title.setTextFill(Paint.valueOf("#333"));

        // ---------------------------------------------------------------------
        // Preset selector
        // ---------------------------------------------------------------------
        Label presetLabel = new Label("Preset:");
        ComboBox<ScenarioMeta> presetBox = new ComboBox<>();
        presetBox.getItems().addAll(presets);
        ScenarioMeta initialPreset = (selected != null && presets.contains(selected))
                ? selected
                : (presets.isEmpty() ? null : presets.get(0));
        if (initialPreset != null) {
            presetBox.getSelectionModel().select(initialPreset);
        }

        HBox presetRow = new HBox(8, presetLabel, presetBox);
        presetRow.setAlignment(Pos.CENTER_LEFT);

        // ---------------------------------------------------------------------
        // Initial values from config
        // ---------------------------------------------------------------------
        ScenarioMeta currentPreset = presetBox.getSelectionModel().getSelectedItem();

        String initialFen = (config != null && config.fenOverride() != null && !config.fenOverride().isBlank())
                ? config.fenOverride()
                : (currentPreset != null ? currentPreset.getDefaultFen() : "");

        boolean initialBureaucrat = (config != null)
                ? config.bureaucratRule()
                : (currentPreset != null && currentPreset.isDefaultBureaucratRule());

        int initialWhiteMoves = (config != null)
                ? config.whiteMovesPerTurn()
                : (currentPreset != null ? currentPreset.getDefaultWhiteMovesPerTurn() : 1);

        int initialBlackMoves = (config != null)
                ? config.blackMovesPerTurn()
                : (currentPreset != null ? currentPreset.getDefaultBlackMovesPerTurn() : 1);

        int initialHeight = (config != null && config.boardHeight() > 0)
                ? config.boardHeight()
                : 0;

        int initialWidth = (config != null && config.boardWidth() > 0)
                ? config.boardWidth()
                : 0;

        boolean initialFillExpanded = (config != null) && config.fillExpandedFiles();

        boolean initialChess960 = (config != null) && config.chess960();

        // ---------------------------------------------------------------------
        // FEN input
        // ---------------------------------------------------------------------
        Label fenLabel = new Label("Start position (FEN):");
        TextField fenField = new TextField();
        fenField.setPromptText("Leave empty to use preset's default FEN");
        fenField.setText(initialFen);

        VBox fenBox = new VBox(4, fenLabel, fenField);

        // ---------------------------------------------------------------------
        // Rules: dimensions, moves, Chess960
        // ---------------------------------------------------------------------

        // Dimensions
        Label dimsLabel = new Label("Dimensions (0=auto)");

        TextField heightField = new TextField();
        heightField.setPromptText("Height");
        heightField.setPrefColumnCount(3);
        heightField.setText(String.valueOf(initialHeight));

        TextField widthField = new TextField();
        widthField.setPromptText("Width");
        widthField.setPrefColumnCount(3);
        widthField.setText(String.valueOf(initialWidth));

        CheckBox fillExpandedFilesBox = new CheckBox("Fill extra files");
        fillExpandedFilesBox.setSelected(initialFillExpanded);

        HBox dimsRow = new HBox(8,
                new Label("Height:"), heightField,
                new Label("Width:"), widthField,
                fillExpandedFilesBox
        );
        dimsRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(fillExpandedFilesBox, new Insets(0, 0, 0, 8));

        VBox dimsBox = new VBox(4, dimsLabel, dimsRow, fillExpandedFilesBox);

        // Moves per turn
        Label movesLabel = new Label("Moves per turn");

        TextField whiteMovesField = new TextField();
        whiteMovesField.setPromptText("White");
        whiteMovesField.setPrefColumnCount(3);
        whiteMovesField.setText(String.valueOf(initialWhiteMoves));

        TextField blackMovesField = new TextField();
        blackMovesField.setPromptText("Black");
        blackMovesField.setPrefColumnCount(3);
        blackMovesField.setText(String.valueOf(initialBlackMoves));

        HBox movesRow = new HBox(8,
                new Label("White:"), whiteMovesField,
                new Label("Black:"), blackMovesField
        );
        movesRow.setAlignment(Pos.CENTER_LEFT);

        VBox movesBox = new VBox(4, movesLabel, movesRow);

        // Chess960 toggle
        CheckBox chess960Chk = new CheckBox("Chess960 (Fischer Random)");
        chess960Chk.setSelected(initialChess960);

        // Rules container (like Android "Rules" card)
        VBox rulesBox = new VBox(8,
                new Label("Rules:"),
                dimsBox,
                movesBox,
                chess960Chk
        );

        // ---------------------------------------------------------------------
        // Custom pieces: Bureaucrat
        // ---------------------------------------------------------------------
        CheckBox bureaucratChk = new CheckBox("Bureaucrat");
        bureaucratChk.setSelected(initialBureaucrat);

        Label customPiecesLabel = new Label("Custom pieces:");
        Label customPiecesDesc = new Label("Enable experimental custom piece rules.");
        VBox customPiecesBox = new VBox(4,
                customPiecesLabel,
                customPiecesDesc,
                bureaucratChk
        );

        // ---------------------------------------------------------------------
        // Preset change behaviour (match Android)
        //   - reset: FEN, Bureaucrat, moves
        //   - keep: dimensions, Chess960
        // ---------------------------------------------------------------------
        presetBox.valueProperty().addListener((obs, oldPreset, newPreset) -> {
            if (newPreset == null) return;
            fenField.setText(newPreset.getDefaultFen());
            bureaucratChk.setSelected(newPreset.isDefaultBureaucratRule());
            whiteMovesField.setText(String.valueOf(newPreset.getDefaultWhiteMovesPerTurn()));
            blackMovesField.setText(String.valueOf(newPreset.getDefaultBlackMovesPerTurn()));
            // heightField, widthField, chess960Chk are intentionally left as-is
        });

        // ---------------------------------------------------------------------
        // Buttons
        // ---------------------------------------------------------------------
        Button startBtn = new Button("Start new game");
        startBtn.setOnAction(e -> {
            ScenarioMeta meta = presetBox.getSelectionModel().getSelectedItem();
            if (meta == null) return;

            String fen = fenField.getText();
            if (fen != null && fen.isBlank()) fen = null;

            int wMoves = parseIntOrDefault(whiteMovesField.getText(), 1);
            int bMoves = parseIntOrDefault(blackMovesField.getText(), 1);

            int height = parseIntOrDefault(heightField.getText(), 8);
            int width  = parseIntOrDefault(widthField.getText(), 8);

            int safeHeight = 0;
            int safeWidth  = 0;
            if (height != 0 && width != 0) {
                safeHeight = Math.max(5, height);
                safeWidth  = Math.max(5, width);
            }
            

            GameConfig cfg = new GameConfig(
                    fen,
                    bureaucratChk.isSelected(),
                    wMoves,
                    bMoves,
                    safeWidth,
                    safeHeight,
                    fillExpandedFilesBox.isSelected(),
                    chess960Chk.isSelected()
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

        // ---------------------------------------------------------------------
        // Root layout
        // ---------------------------------------------------------------------
        VBox root = new VBox(16,
                title,
                presetRow,
                fenBox,
                rulesBox,
                customPiecesBox,
                buttons
        );
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