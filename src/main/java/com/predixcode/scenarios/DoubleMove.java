package com.predixcode.scenarios;

import com.predixcode.core.board.Board;
import com.predixcode.core.rules.MultipleMoveRule;

public class DoubleMove extends Standard {

    @Override
    public Board addRules(Board b) {
        b.addRule(new MultipleMoveRule(2));
        return b;
    }
    
}
