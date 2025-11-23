package com.predixcode.core.colors;

public abstract class Color {
    protected String colorName;
    protected String colorSymbol;
    protected int colorCode;
    

    public static final Color WHITE = new White();
    public static final Color BLACK = new Black();

    public String getName() { return colorName; }
    public String getSymbol() { return colorSymbol; }
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

    @Override
    public boolean equals(Object Color) {
        if (this == Color) return true;
        if (Color == null || getClass() != Color.getClass()) return false;
        Color other = (Color) Color;

        return this.colorCode == other.colorCode || (this.colorName.equals(other.colorName) && this.colorSymbol == other.colorSymbol);
    }
}