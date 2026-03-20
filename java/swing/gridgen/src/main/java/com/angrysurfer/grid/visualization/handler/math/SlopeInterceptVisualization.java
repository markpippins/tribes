package com.angrysurfer.grid.visualization.handler.math;

import java.awt.Color;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class SlopeInterceptVisualization implements IVisualizationHandler {
    private double slope = 1.0;
    private double intercept = 0.0;
    private double time = 0.0;
    private static final double SLOPE_CHANGE_RATE = 0.02;
    private static final double INTERCEPT_CHANGE_RATE = 0.1;

    @Override
    public void update(JButton[][] buttons) {
        // Clear the display
        clearDisplay(buttons);
        
        // Update parameters
        time += 0.05;
        slope = Math.sin(time) * 2.0; // Slope oscillates between -2 and 2
        intercept = Math.cos(time * 0.5) * buttons.length / 2; // Intercept oscillates

        // Draw the line
        drawLine(buttons);
    }

    private void clearDisplay(JButton[][] buttons) {
        for (JButton[] row : buttons) {
            for (JButton button : row) {
                button.setBackground(Color.BLACK);
            }
        }
    }

    private void drawLine(JButton[][] buttons) {
        int height = buttons.length;
        int width = buttons[0].length;
        
        // Calculate points for the line
        for (int x = 0; x < width; x++) {
            // y = mx + b
            int y = (int)(slope * (x - width/2) + intercept + height/2);
            
            // Draw if point is within bounds
            if (y >= 0 && y < height) {
                // Create gradient color based on slope
                float hue = (float)((Math.atan(slope) + Math.PI/2) / Math.PI);
                Color lineColor = Color.getHSBColor(hue, 1.0f, 1.0f);
                buttons[y][x].setBackground(lineColor);
                
                // Add "thickness" to the line
                if (y + 1 < height) {
                    buttons[y + 1][x].setBackground(lineColor.darker());
                }
                if (y - 1 >= 0) {
                    buttons[y - 1][x].setBackground(lineColor.darker());
                }
            }
        }

        // Draw the y-axis
        int midX = width / 2;
        for (int y = 0; y < height; y++) {
            buttons[y][midX].setBackground(Color.DARK_GRAY);
        }

        // Draw the x-axis
        int midY = height / 2;
        for (int x = 0; x < width; x++) {
            buttons[midY][x].setBackground(Color.DARK_GRAY);
        }
    }

    @Override
    public String getName() {
        return "Slope & Intercept";
    }
   
    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MATH;
    }
}
