package com.predixcode.core.colors;

public class Black extends Color {

    public Black() {
        this.colorName = "black";
        this.colorCode = 0;
    }

    @Override
    public String formatSymbol(String s) {
        return s;
    }
}
