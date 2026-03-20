package com.angrysurfer.grid.visualization.handler.classic;

import java.awt.Color;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class SpiralVisualization implements IVisualizationHandler {
    private double spiralAngle = 0;
    private double spiralRadius = 0;
    private final int centerX;
    private final int centerY;

    public SpiralVisualization() {
        this.centerX = 36 / 2; // GRID_COLS / 2
        this.centerY = 8 / 2;  // GRID_ROWS / 2
    }

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        spiralAngle += 0.2;
        spiralRadius = (spiralAngle % 10) / 2;
        
        int x = (int) (centerX + Math.cos(spiralAngle) * spiralRadius);
        int y = (int) (centerY + Math.sin(spiralAngle) * spiralRadius);
        
        if (y >= 0 && y < buttons.length && x >= 0 && x < buttons[0].length) {
            buttons[y][x].setBackground(Color.MAGENTA);
        }
    }

    @Override
    public String getName() {
        return "Spiral";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.CLASSIC;
    }
}
