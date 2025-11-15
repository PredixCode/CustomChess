package com.predixcode.scenarios;

import com.predixcode.core.board.Board;
import com.predixcode.ui.GUI;

import javafx.application.Application;
import javafx.stage.Stage;

public class Standard extends GUI implements Scenario { 

    public static void main(String[] args) {
        Application.launch(Standard.class, args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        this.board = createBoard();
        super.start(stage);
    }

    @Override
    public Board createBoard() {
        Board b = Board.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        b.rules.add(new com.predixcode.core.rules.Standard());
        return b;
    }

}
