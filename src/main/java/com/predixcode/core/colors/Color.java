package com.predixcode.core.colors;

public abstract class Color {
    protected String colorName;
    protected int colorCode;

    public static final Color WHITE = new White();
    public static final Color BLACK = new Black();

    public String getName() { return colorName; }
    public int getCode() { return colorCode; }

    public abstract String formatSymbol(String s);

    public Color opposite() {
        return this.colorCode == 1 ? BLACK : WHITE;
    }

    public static Color fromChar(char c) {
        return (c == 'w' || c == 'W') ? new White() : new Black();
    }

    @Override
    public String toString() {
        return colorName != null ? colorName : super.toString();
    }
}