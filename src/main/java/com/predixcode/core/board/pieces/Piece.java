package com.predixcode.core.board.pieces;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.predixcode.core.colors.Black;
import com.predixcode.core.colors.Color;
import com.predixcode.core.colors.White;

public abstract class Piece {
    public int x;
    public int y;
    protected Color color;
    protected String fenSymbol;

    protected Piece() {}

    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    public int getX() { return x; }
    
    public int getY() { return y; }

    public void setColor(Color color) { this.color = color; }

    public Color getColor() { return this.color; }

    public String symbol() {
        return color == null ? fenSymbol : color.formatSymbol(fenSymbol);
    }

    protected String getFenSymbol() {
        return color.formatSymbol(fenSymbol);
    }

    protected abstract List<Integer[]> getMoves(int matrixX, int matrixY);

    // FEN Conversion
    protected static final Map<Character, Supplier<Piece>> MATRIX = Map.of(
        'p', Pawn::new,
        'r', Rook::new,
        'n', Knight::new,
        'b', Bishop::new,
        'k', King::new,
        'q', Queen::new
    );

    private static final Color WHITE = new White();
    private static final Color BLACK = new Black();

    // Build, color, and position a piece from a FEN character and board coords
    public static Piece initialize(char fenChar, int x, int y) {
        Supplier<Piece> sup = MATRIX.get(Character.toLowerCase(fenChar));
        if (sup == null) return null;

        Piece piece = sup.get();
        piece.setColor(Character.isUpperCase(fenChar) ? WHITE : BLACK);
        piece.setPosition(x, y);
        return piece;
    }
}