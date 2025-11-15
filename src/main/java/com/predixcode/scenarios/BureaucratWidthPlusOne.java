package com.predixcode.scenarios;

import com.predixcode.core.board.Board;

import javafx.application.Application;

public class BureaucratWidthPlusOne extends Bureaucrat { 

    public static void main(String[] args) {
        Application.launch(Bureaucrat.class, args);
    }

    @Override
    public Board createBoard() {
        Board b = Board.fromFen("rnbqckbnr/ppppppppp/9/9/9/9/PPPPPPPPP/RNBQCKBNR w KQkq - 0 1");
        b.rules.add(new com.predixcode.core.rules.BureaucratRule());
        return b;
    }

}
