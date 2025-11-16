package com.predixcode.scenarios;

import com.predixcode.core.board.Board;
import com.predixcode.core.rules.StandardRule;
import com.predixcode.ui.Gui;

import javafx.stage.Stage;

public class Standard extends Gui implements Scenario {
    
    @Override
    public void start(Stage stage) throws Exception {
        Board b = createBoard();
        this.board = addRules(b);
        super.start(stage);
    }

    @Override
    public Board createBoard() {
        return Board.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    @Override
    public Board addRules(Board b) {
        b.addRule(new StandardRule());
        return b;
    }
}
