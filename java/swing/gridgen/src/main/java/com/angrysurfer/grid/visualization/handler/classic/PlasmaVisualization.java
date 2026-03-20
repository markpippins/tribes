package com.angrysurfer.grid.visualization.handler.classic;

import java.awt.Color;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class PlasmaVisualization implements IVisualizationHandler {
    private double t = 0.0;

    @Override
    public void update(JButton[][] buttons) {
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                double value = Math.sin(col * 0.2 + t) +
                        Math.sin(row * 0.1 + t) +
                        Math.sin((col + row) * 0.15 + t) +
                        Math.sin(Math.sqrt(col * col + row * row) * 0.15);
                value = value * 0.25 + 0.5; // Normalize to 0-1
                int red = (int) (Math.sin(value * Math.PI * 2) * 127 + 128);
                int green = (int) (Math.sin(value * Math.PI * 2 + 2 * Math.PI / 3) * 127 + 128);
                int blue = (int) (Math.sin(value * Math.PI * 2 + 4 * Math.PI / 3) * 127 + 128);
                buttons[row][col].setBackground(new Color(red, green, blue));
            }
        }
        t += 0.1;
    }

    @Override
    public String getName() {
        return "Plasma";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.CLASSIC;
    }
}
