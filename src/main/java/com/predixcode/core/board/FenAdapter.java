package com.predixcode.core.board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.predixcode.core.board.pieces.King;
import com.predixcode.core.board.pieces.Piece;
import com.predixcode.core.colors.Color;

public final class FenAdapter {
    private FenAdapter() {}

    public static Board setupBoard(String fen) {
        String[] parts = fen.trim().split("\\s+");
        if (parts.length < 6) throw new IllegalArgumentException("Invalid FEN: " + fen);

        String placement = parts[0];
        char active = parts[1].charAt(0);        // 'w' or 'b'
        String castling = parts[2];              // "KQkq" or "-"
        String epStr = parts[3];                 // "-" or like "e3"
        int halfmove = Integer.parseInt(parts[4]);
        int fullmove = Integer.parseInt(parts[5]);

        Board board = new Board();

        // dimensions (standard chess)
        List<String> rows = Arrays.asList(placement.split("/"));
        board.setDimensions(8, rows.size());

        // pieces
        List<Piece> pieces = buildPieces(rows);
        board.setPieces(pieces);

        // board data
        board.setActiveColor(Color.fromChar(active));
        board.setHalfmove(halfmove);
        board.setFullmove(fullmove);
        board.setEnPassant(parseAlgebraicSquare(epStr));

        // castling rights go straight to the kings
        applyCastlingToKings(pieces, castling);

        return board;
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

    public static int[] parseAlgebraicSquare(String sq) {
        if (sq == null || "-".equals(sq)) return new int[]{-1, -1};
        if (sq.length() != 2) return new int[]{-1, -1};
        char file = sq.charAt(0); // a..h
        char rank = sq.charAt(1); // 1..8
        int x = file - 'a';       // 0..7
        int y = 8 - (rank - '0'); // rank 8 -> y=0, rank 1 -> y=7
        if (x < 0 || x > 7 || y < 0 || y > 7) return new int[]{-1, -1};
        return new int[]{x, y};
    }

    public static void applyCastlingToKings(List<Piece> pieces, String castling) {
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

    public static String toFen(Board board) {
        StringBuilder placement = new StringBuilder();
        for (int row = 0; row < board.getYMatrix(); row++) {
            int empty = 0;
            for (int col = 0; col < board.getXMatrix(); col++) {
                Piece piece = board.getPieceAt(col, row);
                if (piece == null) {
                    empty++;
                } else {
                    if (empty > 0) {
                        placement.append(empty);
                        empty = 0;
                    }
                    char c = piece.symbol().charAt(0);
                    placement.append(c);
                }
            }
            if (empty > 0) placement.append(empty);
            if (row < board.getYMatrix() - 1) placement.append('/');
        }

        String side = (board.getActiveColor() == Color.WHITE) ? "w" :
                      (board.getActiveColor() == Color.BLACK) ? "b" : "w";

        String castling = board.getCastlingString();
        String ep = board.getEnPassantAlgebraic();

        return placement + " " + side + " " + castling + " " + ep + " " + board.getHalfmove() + " " + board.getFullmove();
    }
}