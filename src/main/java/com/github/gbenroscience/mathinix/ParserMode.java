package com.github.gbenroscience.mathinix;

/**
 *
 * @author GBEMIRO
 */
public enum ParserMode {
    STANDARD("Standard Mode"),
    TURBO_ARR("Turbo Mode (Array)"),
    TURBO_WIDE("Turbo Mode (Widened)"),
    TURBO_MATRIX("Turbo Mode (Matrix)");

    private final String displayName;
    ParserMode(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}