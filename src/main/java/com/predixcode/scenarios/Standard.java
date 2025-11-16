package com.predixcode.scenarios;

import com.predixcode.core.board.Board;
import com.predixcode.core.rules.StandardRule;
import com.predixcode.ui.Gui;

import javafx.stage.Stage;

public class Standard extends Gui implements Scenario {
    
    @Override
    public void start(Stage stage) throws Exception {
        this.board = createBoard();
        super.start(stage);
    }

    @Override
    public Board createBoard() {
        Board b = Board.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        return addRules(b);
    }

    @Override
    public Board addRules(Board b) {
        b.addRule(new StandardRule());
        return b;
    }
}
