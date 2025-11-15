package com.predixcode.core.board.pieces;

import java.util.LinkedHashSet;
import java.util.Set;

import com.predixcode.core.board.Board;

public class Bureaucrat extends Piece {

    public Bureaucrat() {
        super();
        this.fenSymbol = "c";
    }

    @Override
    public Set<String> getLegalMoves(Board board) {
        Set<String> out = new LinkedHashSet<>();
        for (int col = 0; col < board.width; col++) {
            for (int row = 0; row < board.height; row++) {
                if (board.isEmpty(row, col))
                    out.add(board.toAlg(col, row));
            }
        }
        return out;
    }

    @Override
    public Set<int[]> attackedSquares(Board board) {
        return new LinkedHashSet<>();
    }

    @Override
    public void actionOnCapture(Board board) {
        switchColor();
        Set<String> afterCaptureChoices = getLegalMoves(board);
        String newPos = afterCaptureChoices.iterator().next();
        int[] toXY = board.fromAlg(newPos);
        setPosition(toXY[0], toXY[1]);
    }
}