package com.angrysurfer.grid.visualization;

import java.awt.Color;
import java.awt.Point;

import javax.swing.JButton;

import java.awt.Container;

public class VisualizationUtils {
    public static final Color[] RAINBOW_COLORS = {
        Color.RED, Color.ORANGE, Color.YELLOW,
        Color.GREEN, Color.BLUE, new Color(75, 0, 130)
    };

    public static Point interpolatePoint(Point start, Point end, double progress) {
        return new Point(
            (int)(start.x + (end.x - start.x) * progress),
            (int)(start.y + (end.y - start.y) * progress)
        );
    }

    public static double distance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    public static void clearDisplay(JButton[][] buttons, Container parent) {
        Color bg = parent.getBackground();
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                buttons[row][col].setBackground(bg);
            }
        }
    }

    public static void fillBackground(JButton[][] buttons, Color color) {
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                buttons[row][col].setBackground(color);
            }
        }
    }
}
