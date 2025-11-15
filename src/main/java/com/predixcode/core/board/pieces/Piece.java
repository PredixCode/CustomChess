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
    public void actionOnCapture(Board board) {}

    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int[] getXY () { return new int[] { this.x, this.y }; }

    public void setColor(Color color) { this.color = color; }
    public Color getColor() { return this.color; }
    public void switchColor() { this.color = this.color.opposite();}

    public String getSymbol() {
        return color == null ? fenSymbol : color.formatSymbol(fenSymbol);
    }

    public String getImagePath(String theme) {
        String name = getColor().getSymbol() + getSymbol().toUpperCase();
        return "/pieces/" + theme + "/" + name + ".png";
    }

    public static Piece initFromFen(char fenChar, int x, int y) {
        for (Supplier<Piece> sup : TYPES) {
            Piece probe = sup.get();
            String base = probe.getSymbol();
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