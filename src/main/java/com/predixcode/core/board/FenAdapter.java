package com.predixcode.core.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.predixcode.core.board.pieces.King;
import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.colors.Color;

public final class FenAdapter {
    private FenAdapter() {}

    public static Board boardFromFen(String fen) {
        String[] parts = fen.trim().split("\\s+");
        System.out.println("FEN parts: " + Arrays.toString(parts));
        if (parts.length < 6) throw new IllegalArgumentException("Invalid FEN: " + fen);

        String placement = parts[0];
        char active = parts[1].trim().charAt(0);        // 'w' or 'b'
        String castling = parts[2];              // "KQkq" or "-"
        String epStr = parts[3];                 // "-" or like "e3"
        int halfmove = Integer.parseInt(parts[4]);
        int fullmove = Integer.parseInt(parts[5]);

        Board board = new Board();

        // dimensions
        List<String> rows = Arrays.asList(placement.split("/"));
        board.setWidth(rows.getFirst().length());
        board.setHeight(rows.size()); 

        // pieces
        List<Piece> pieces = buildPieces(rows);
        board.setPieces(pieces);

        // board data
        board.setActiveColor(Color.fromChar(active));
        board.setHalfmove(halfmove);
        board.setFullmove(fullmove);
        if (!"-".equals(epStr)) {
            int[] epXY = board.fromAlg(epStr);
            board.setEnPassant(epXY);
        } else {
            board.clearEnPassant();
        }

        // castling rights
        applyCastlingToKings(pieces, castling);
        return board;
    }

    public static String toFen(Board board) {
        StringBuilder placement = new StringBuilder();
        for (int row = 0; row < board.getHeight(); row++) {
            int empty = 0;
            for (int col = 0; col < board.getWidth(); col++) {
                Piece piece = board.getPieceAt(col, row);
                if (piece == null) {
                    empty++;
                } else {
                    if (empty > 0) {
                        placement.append(empty);
                        empty = 0;
                    }
                    char c = piece.getSymbol().charAt(0);
                    placement.append(c);
                }
            }
            if (empty > 0) placement.append(empty);
            if (row < board.getHeight() - 1) placement.append('/');
        }

        String side = (board.getActiveColor() == Color.WHITE) ? "w" :
                      (board.getActiveColor() == Color.BLACK) ? "b" : "w";

        String castling = FenAdapter.getCastlingString(board);
        String ep = getEnPassantString(board);

        return placement + " " + side + " " + castling + " " + ep + " " + board.getHalfmove() + " " + board.getFullmove();
    }

    public static String getCastlingString(Board board) {
        boolean K = false, Q = false, k = false, q = false;
        for (Piece p : board.getPieces()) {
            if (p instanceof King king) {
                boolean isWhite = p.getColor() == Color.WHITE;
                if (isWhite) {
                    if (king.canCastleKingSide()) K = true;
                    if (king.canCastleQueenSide()) Q = true;
                } else {
                    if (king.canCastleKingSide()) k = true;
                    if (king.canCastleQueenSide()) q = true;
                }
            }
        }
        String s = (K ? "K" : "") + (Q ? "Q" : "") + (k ? "k" : "") + (q ? "q" : "");
        return s.isEmpty() ? "-" : s;
    }

    public static String getEnPassantString(Board board) {
        if (board.getEnPassantXY()[0] < 0 || board.getEnPassantXY()[1] < 0) return "-";
        return board.toAlg(board.getEnPassantXY()[0], board.getEnPassantXY()[1]);
    }

    private static List<Piece> buildPieces(List<String> rows) {
        List<Piece> pieces = new ArrayList<>();
        for (int y = 0; y < rows.size(); y++) {
            String row = rows.get(y);
            int x = 0;
            for (int i = 0; i < row.length(); i++) {
                char ch = row.charAt(i);
                if (Character.isDigit(ch)) {
                    x += Character.getNumericValue(ch);
                } else {
                    Piece piece = Piece.initFromFen(ch, x, y);
                    if (piece != null) pieces.add(piece);
                    x++;
                }
            }
        }
        return pieces;
    }

    private static void applyCastlingToKings(List<Piece> pieces, String castling) {
        King whiteKing = null;
        King blackKing = null;

        for (Piece p : pieces) {
            if (p instanceof King k) {
                boolean isWhite = p.getColor() != null && p.getColor().getCode() == 1;
                if (isWhite) whiteKing = k; else blackKing = k;
            }
        }

        if (whiteKing != null) {
            whiteKing.setCastleKingSide(castling.contains("K"));
            whiteKing.setCastleQueenSide(castling.contains("Q"));
        }
        if (blackKing != null) {
            blackKing.setCastleKingSide(castling.contains("k"));
            blackKing.setCastleQueenSide(castling.contains("q"));
        }
    }
}