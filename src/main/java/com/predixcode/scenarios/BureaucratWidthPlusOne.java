package com.predixcode.scenarios;

import com.predixcode.core.board.Board;

public class BureaucratWidthPlusOne extends Bureaucrat { 

    @Override
    public Board createBoard() {
        Board b = Board.fromFen("rnbqckbnr/ppppppppp/9/9/9/9/PPPPPPPPP/RNBQCKBNR w KQkq - 0 1");
        return b;
    }

}
