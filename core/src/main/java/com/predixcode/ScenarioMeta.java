package com.predixcode;

/**
 * A named preset: default FEN + default rule flags.
 */
public final class ScenarioMeta {
    private final String name;
    private final String defaultFen;
    private final boolean defaultBureaucratRule;
    private final int defaultWhiteMovesPerTurn;
    private final int defaultBlackMovesPerTurn;

    public ScenarioMeta(String name,
                        String defaultFen,
                        boolean defaultBureaucratRule,
                        int defaultWhiteMovesPerTurn,
                        int defaultBlackMovesPerTurn) {
        this.name = name;
        this.defaultFen = defaultFen;
        this.defaultBureaucratRule = defaultBureaucratRule;
        this.defaultWhiteMovesPerTurn = defaultWhiteMovesPerTurn;
        this.defaultBlackMovesPerTurn = defaultBlackMovesPerTurn;
    }

    public String getName() { return name; }
    public String getDefaultFen() { return defaultFen; }
    public boolean isDefaultBureaucratRule() { return defaultBureaucratRule; }
    public int getDefaultWhiteMovesPerTurn() { return defaultWhiteMovesPerTurn; }
    public int getDefaultBlackMovesPerTurn() { return defaultBlackMovesPerTurn; }

    @Override
    public String toString() {
        return name;
    }
}