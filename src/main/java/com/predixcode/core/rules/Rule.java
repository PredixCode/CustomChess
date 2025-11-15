package com.predixcode.core.rules;


import com.predixcode.core.board.Board;
import com.predixcode.core.board.pieces.Piece;

public interface Rule {

    void apply(Board board, Piece movingPiece, String from, String to);
    
}
