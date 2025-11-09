package com.predixcode.core.board.colors;

public class White extends Color {
    
    public White() {
        this.colorName = "white";
        this.colorCode = 1;
    }

    @Override
    public String formatSymbol(String s) {
        return s.toUpperCase();
    }
}
