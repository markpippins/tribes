package com.angrysurfer.grid.visualization.handler.classic;

import java.awt.Color;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class SnakeVisualization implements IVisualizationHandler {
    private double angle = 0;

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        int row = 2 + (int) (Math.sin(angle) * 1.5);
        int col = (int) (angle * 2) % buttons[0].length;
        if (row >= 0 && row < buttons.length && col >= 0 && col < buttons[0].length) {
            buttons[row][col].setBackground(Color.GREEN);
        }
        angle += 0.2;
    }

    @Override
    public String getName() {
        return "Snake";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.CLASSIC;
    }
}
