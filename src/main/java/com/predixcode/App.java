package com.predixcode;

import com.predixcode.core.Console;
import com.predixcode.core.board.Board;

public class App {
    public static void main(String[] args) {
        Board b;
        try {
            b = Board.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            Console g = new Console(b);
            g.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}