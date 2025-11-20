package com.predixcode.ui;

import com.predixcode.core.board.Board;

/**
 * Simple Gui that uses a pre-constructed Board.
 * App is responsible for building Board + Rules.
 */
public class ConfigurableGui extends Gui {

    public ConfigurableGui(Board board) {
        this.board = board;
    }
}