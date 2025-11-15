package com.predixcode.scenarios;

import com.predixcode.core.board.Board;
import com.predixcode.ui.Gui;

import javafx.application.Application;
import javafx.stage.Stage;

public class Bureaucrat extends Gui implements Scenario { 

    public static void main(String[] args) {
        Application.launch(Bureaucrat.class, args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        this.board = createBoard();
        super.start(stage);
    }

    @Override
    public Board createBoard() {
        Board b = Board.fromFen("rnbqkbnr/pppppppp/3c4/8/8/4C3/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        b.rules.add(new com.predixcode.core.rules.BureaucratRule());
        return b;
    }

}
