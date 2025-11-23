package com.predixcode.core.colors;

public class Black extends Color {

    public Black() {
        this.colorName = "black";
        this.colorSymbol = "b";
        this.colorCode = 0;
    }

    @Override
    public String formatSymbol(String s) {
        return s;
    }
}
