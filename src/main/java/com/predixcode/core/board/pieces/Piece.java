package com.predixcode.core.board.pieces;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.predixcode.core.board.Board;
import com.predixcode.core.colors.Color;

public abstract class Piece {
    public int x;
    public int y;
    protected Color color;
    protected String fenSymbol;

    private static final List<Supplier<Piece>> TYPES = List.of(
        Pawn::new, Knight::new, Bishop::new, Rook::new, Queen::new, King::new, Bureaucrat::new
    );

    protected Piece() {}

    public abstract Set<String> getLegalMoves(Board board);  // algebraic like "e4"
    public abstract Set<int[]> attackedSquares(Board board);      // int[]{x,y}

    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    public int getX() { return x; }
    public int getY() { return y; }

    public void setColor(Color color) { this.color = color; }
    public Color getColor() { return this.color; }

    public String getSymbol() {
        return color == null ? fenSymbol : color.formatSymbol(fenSymbol);
    }

    public String getImagePath(String theme) {
        String name = getColor().getSymbol() + getSymbol().toUpperCase();
        return "/pieces/" + theme + "/" + name + ".png";
    }

    protected String getFenSymbol() {
        return color != null ? color.formatSymbol(fenSymbol) : fenSymbol;
    }

    public static Piece initFromFen(char fenChar, int x, int y) {
        for (Supplier<Piece> sup : TYPES) {
            Piece probe = sup.get();
            String base = probe.getFenSymbol();
            if (base.equalsIgnoreCase(String.valueOf(fenChar))) {
                Piece piece = sup.get();
                return buildPiece(piece, fenChar, x, y);
            }
        }
        return null;
    }

    private static Piece buildPiece(Piece piece, char fenChar, int x, int y) {
        piece.setColor(Character.isUpperCase(fenChar) ? Color.WHITE : Color.BLACK);
        piece.setPosition(x, y);
        return piece;
    };
}