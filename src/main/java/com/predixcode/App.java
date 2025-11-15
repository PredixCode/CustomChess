package com.predixcode;


import java.util.List;

import com.predixcode.scenarios.Bureaucrat;
import com.predixcode.scenarios.Standard;

import javafx.application.Application;

public class App {

    private static final List<Class<? extends Application>> SCENARIOS = List.of(
        Standard.class, Bureaucrat.class
    );

    public static void main(String[] args) {
        Application.launch(SCENARIOS.getLast(), args);
    }
}