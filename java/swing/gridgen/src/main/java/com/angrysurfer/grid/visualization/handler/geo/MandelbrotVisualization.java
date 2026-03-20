package com.angrysurfer.grid.visualization.handler.geo;

import java.awt.Color;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class MandelbrotVisualization implements IVisualizationHandler {
    private double t = 0.0;

    @Override
    public void update(JButton[][] buttons) {
        double zoom = 1.5 + Math.sin(t * 0.1) * 0.5;
        double centerX = -0.5 + Math.sin(t * 0.05) * 0.2;
        double centerY = Math.cos(t * 0.05) * 0.2;

        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                double x0 = (col - buttons[0].length / 2.0) * zoom / buttons[0].length + centerX;
                double y0 = (row - buttons.length / 2.0) * zoom / buttons.length + centerY;

                double x = 0, y = 0;
                int iteration = 0;
                while (x * x + y * y < 4 && iteration < 20) {
                    double xtemp = x * x - y * y + x0;
                    y = 2 * x * y + y0;
                    x = xtemp;
                    iteration++;
                }

                int hue = (iteration * 13) % 360;
                buttons[row][col].setBackground(
                    Color.getHSBColor(hue / 360f, 0.8f, iteration < 20 ? 1f : 0f));
            }
        }
        t += 0.1;
    }

    @Override
    public String getName() {
        return "Mandelbrot";
    }

        @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.GEO;
    }
}
