package com.predixcode.core.board;

public class Game {

    private final Board board;
    
    public Game(Board board) {
        this.board = board;
    }

    public void start() {
        System.err.println(this.board.toString());
    }

}
