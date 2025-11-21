package com.predixcode.board.pieces;

import java.util.LinkedHashSet;
import java.util.Set;

import com.predixcode.board.Board;

public class Bureaucrat extends Piece {

    public Bureaucrat() {
        super();
        this.fenSymbol = "c";
    }

    @Override
    public Set<String> getLegalMoves(Board board) {
        Set<String> out = new LinkedHashSet<>();
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                // Bureaucrat can move to any unoccupied square
                if (board.isEmpty(x, y)) {
                    out.add(board.toAlg(x, y));
                }
            }
        }
        return out;
    }

    @Override
    public Set<int[]> attackedSquares(Board board) {
        // Bureaucrat doesn't attack/capture
        return new LinkedHashSet<>();
    }

    @Override
    public void actionOnCapture(Board board) {
        // Flip to opponent’s color
        switchColor();

        // Relocate to the first available empty square
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                if (board.isEmpty(x, y)) {
                    setPosition(x, y);
                    return;
                }
            }
        }

        // If no empty squares exist (shouldn’t happen in practice), remove it
        board.getPieces().remove(this);
    }
}