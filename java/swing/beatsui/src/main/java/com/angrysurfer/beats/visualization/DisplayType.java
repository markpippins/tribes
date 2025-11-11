package com.angrysurfer.beats.visualization;

public enum DisplayType {
    GAME("Game"),
    VISUALIZER("Visualizer"),
    CONTROL("Control"),
    MUSIC("Music"),
    COMPSCI("Compsci");

    private final String label;

    DisplayType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
