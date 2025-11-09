package com.predixcode.core;

import java.util.Scanner;

import com.predixcode.core.board.Board;

public class Game {

    private final Board board;
    
    public Game(Board board) {
        this.board = board;
    }

    private String askMove(Scanner sc) {
        System.out.print("Enter your move (e.g., e2 e4): ");
        return sc.nextLine();
    }

    public void start() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println(board);
            
            String moveInput = askMove(sc);
            String[] parts = moveInput.split(" ");
            board.move(parts[0], parts[1]);
        }
    }

}
