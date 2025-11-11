package com.angrysurfer.beats.visualization.handler.geo;

import java.awt.Color;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import javax.swing.JButton;

public class JuliaSetVisualization implements IVisualizationHandler {
    private double t = 0.0;

    @Override
    public void update(JButton[][] buttons) {
        double cX = Math.sin(t * 0.1) * 0.7;
        double cY = Math.cos(t * 0.1) * 0.3;

        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                double x = 1.5 * (col - buttons[0].length / 2.0) / (0.5 * buttons[0].length);
                double y = (row - buttons.length / 2.0) / (0.5 * buttons.length);

                int iteration = 0;
                while (x * x + y * y < 4 && iteration < 20) {
                    double xtemp = x * x - y * y + cX;
                    y = 2 * x * y + cY;
                    x = xtemp;
                    iteration++;
                }

                int hue = (iteration * 15) % 360;
                buttons[row][col].setBackground(
                        Color.getHSBColor(hue / 360f, 0.9f, iteration < 20 ? 1f : 0f));
            }
        }
        t += 0.05;
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.GEO;
    }

    @Override
    public String getName() {
        return "Julia Set";
    }

}
