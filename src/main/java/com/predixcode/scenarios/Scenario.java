package com.predixcode.scenarios;

import com.predixcode.core.board.Board;

import javafx.stage.Stage;

public interface Scenario {

    void start(Stage stage) throws Exception;

    Board createBoard();

    Board addRules(Board board);
}
