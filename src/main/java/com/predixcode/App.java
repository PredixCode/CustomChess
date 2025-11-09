package com.predixcode;

import com.predixcode.core.Game;
import com.predixcode.core.board.Board;

public class App {
    public static void main(String[] args) {
        Board b;
        try {
            b = Board.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            Game g = new Game(b);
            g.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}