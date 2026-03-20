package com.angrysurfer.grid.visualization.handler.geo;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

import java.awt.Color;

public class LissajousVisualization implements IVisualizationHandler {
    
    private double t = 0.0;
    private final double frequency1 = 3;
    private final double frequency2 = 2;

    @Override
    public void update(JButton[][] buttons) {
        // Clear grid
        for (JButton[] row : buttons) {
            for (JButton button : row) {
                button.setBackground(Color.BLACK);
            }
        }

        // Draw Lissajous curve
        for (double i = 0; i < 2 * Math.PI; i += 0.1) {
            int x = (int) ((Math.sin(frequency1 * i + t) + 1) * buttons[0].length / 2);
            int y = (int) ((Math.sin(frequency2 * i) + 1) * buttons.length / 2);
            
            if (x >= 0 && x < buttons[0].length && y >= 0 && y < buttons.length) {
                int hue = (int) (i * 30) % 360;
                buttons[y][x].setBackground(Color.getHSBColor(hue / 360f, 1f, 1f));
            }
        }
        t += 0.05;
    }

    @Override
    public String getName() {
        return "Lissajous";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.GEO;
    }

}
