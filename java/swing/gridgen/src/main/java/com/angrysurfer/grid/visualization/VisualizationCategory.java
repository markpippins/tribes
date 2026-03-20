package com.angrysurfer.grid.visualization;

public enum VisualizationCategory {
    DEFAULT("Default"),
    ARCADE("Arcade Games"),
    CLASSIC("Classic Visualizations"),
    COMPSCI("Computer Science"),
    GAME("Games"),
    GEO("Geometric"),
    MATH("Math"),
    MATRIX("Matrix"),
    MUSIC("Music"),
    SCIENCE("Science");

    private final String label;

    VisualizationCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
