package com.predixcode.scenarios;

import com.predixcode.core.board.Board;
import com.predixcode.core.rules.BureaucratRule;
import com.predixcode.ui.Gui;

import javafx.stage.Stage;

public class Bureaucrat extends Gui implements Scenario { 

    @Override
    public void start(Stage stage) throws Exception {
        this.board = createBoard();
        super.start(stage);
    }

    @Override
    public Board createBoard() {
        Board b = Board.fromFen("rnbqkbnr/pppppppp/3c4/8/8/4C3/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        return b;
    }

    @Override
    public Board addRules(Board b) {
        b.addRule(new BureaucratRule());
        return b;
    }

}
