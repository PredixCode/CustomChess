package com.predixcode;


import java.util.List;

import com.predixcode.scenarios.Bureaucrat;
import com.predixcode.scenarios.BureaucratWidthPlusOne;
import com.predixcode.scenarios.DoubleMove;
import com.predixcode.scenarios.Standard;

import javafx.application.Application;

public class App {

    private static final List<Class<? extends Application>> SCENARIOS = List.of(
        Standard.class, DoubleMove.class, Bureaucrat.class, BureaucratWidthPlusOne.class
    );

    public static void main(String[] args) {
        Application.launch(SCENARIOS.get(1), args);
    }
}