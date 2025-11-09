package com.predixcode.core.board.colors;

public abstract class Color {
    protected String colorName;
    protected int colorCode;

    public String getName() { return colorName; }
    public int getCode() { return colorCode; }

    public abstract String formatSymbol(String s);

    public static Color fromChar(char c) {
        return (c == 'w' || c == 'W') ? new White() : new Black();
    }

    @Override
    public String toString() {
        return colorName != null ? colorName : super.toString();
    }
}