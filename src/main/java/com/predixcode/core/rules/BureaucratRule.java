package com.predixcode.core.rules;

import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Piece;

public class BureaucratRule extends Standard {

    @Override
    public void apply(Board  board, Piece movingPiece, String from, String to) {
        super.apply(board, movingPiece, from, to);
    }
    
}
