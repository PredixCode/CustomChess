package com.predixcode.ui;

import java.util.List;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.FenAdapter;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class GameInfoPanel extends VBox {

    private final Label lblActive = new Label("-");
    private final Label lblCastling = new Label("-");
    private final Label lblEnPassant = new Label("-");
    private final Label lblHalfmove = new Label("-");
    private final Label lblFullmove = new Label("-");

    private final TextArea movesArea = new TextArea();
    private final TextField fenField = new TextField();

    public GameInfoPanel(double width) {
        setSpacing(10);
        setPadding(new Insets(10));
        setPrefWidth(width);
        setMinWidth(width);
        setMaxWidth(width);

        Label title = new Label("Game Info");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Stats grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);

        int r = 0;
        grid.add(new Label("Active:"),      0, r); grid.add(lblActive,    1, r++);
        grid.add(new Label("Castling:"),    0, r); grid.add(lblCastling,  1, r++);
        grid.add(new Label("En Passant:"),  0, r); grid.add(lblEnPassant, 1, r++);
        grid.add(new Label("Halfmove:"),    0, r); grid.add(lblHalfmove,  1, r++);
        grid.add(new Label("Fullmove:"),    0, r); grid.add(lblFullmove,  1, r++);

        // Moves
        Label mvTitle = new Label("Moves");
        mvTitle.setStyle("-fx-font-weight: bold;");
        movesArea.setEditable(false);
        movesArea.setWrapText(false);
        movesArea.setPrefRowCount(12);
        VBox.setVgrow(movesArea, Priority.ALWAYS);

        // FEN
        Label fenTitle = new Label("Current FEN");
        fenTitle.setStyle("-fx-font-weight: bold;");
        fenField.setEditable(false);

        getChildren().addAll(
            title,
            grid,
            new Separator(),
            mvTitle,
            movesArea,
            new Separator(),
            fenTitle,
            fenField
        );
    }

    public void refresh(Board board, List<String> moves) {
        // Stats
        lblActive.setText(board.activeColor != null ? board.activeColor.getName() : "-");
        lblCastling.setText(FenAdapter.getCastlingString(board));
        lblEnPassant.setText(FenAdapter.getEnPassantString(board));
        lblHalfmove.setText(String.valueOf(board.halfmove));
        lblFullmove.setText(String.valueOf(board.fullmove));

        // Moves
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < moves.size(); i++) {
            int moveNum = (i / 2) + 1;
            boolean whiteMove = (i % 2 == 0);
            if (whiteMove) {
                sb.append(moveNum).append(". ").append(moves.get(i));
            } else {
                sb.append("  ").append(moves.get(i));
            }
            sb.append('\n');
        }
        movesArea.setText(sb.toString());
        movesArea.setScrollTop(Double.MAX_VALUE); // scroll to end

        // FEN
        fenField.setText(FenAdapter.toFen(board));
    }
}