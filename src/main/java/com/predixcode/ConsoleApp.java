package com.predixcode;

import java.util.Scanner;

import com.predixcode.core.board.Board;

public class ConsoleApp {

    private final Board board;
    
    public ConsoleApp() {
        this.board = Board.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        board.addRule(new com.predixcode.core.rules.StandardRule());
    }

    private String askMove(Scanner sc) {
        System.out.print("Enter your move (e.g., e2 e4): ");
        return sc.nextLine();
    }

    public void start() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            try {
                System.out.println(board);
                String moveInput = askMove(sc);
                String[] parts = moveInput.split(" ");
                board.applyTurn(parts[0], parts[1]);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

        }
    }

    public static void main(String[] args) {
        ConsoleApp app = new ConsoleApp();
        app.start();
    }

}
