package com.angrysurfer.grid.visualization.handler;

import java.awt.Color;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import javax.swing.JButton;

public class XYPadVisualization implements IVisualizationHandler {
    private double phase = 0.0;
    private double[] modulators = {1.0, 1.5, 2.0, 0.5};
    private int currentModulator = 0;

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Calculate modulated XY position
        double xMod = Math.sin(phase * modulators[currentModulator]);
        double yMod = Math.cos(phase * modulators[(currentModulator + 1) % modulators.length]);
        
        int centerX = (int)(buttons[0].length / 2 + xMod * buttons[0].length / 3);
        int centerY = (int)(buttons.length / 2 + yMod * buttons.length / 3);

        // Draw XY pad visualization
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                // Calculate distance from center
                double distance = Math.sqrt(
                    Math.pow(col - centerX, 2) + 
                    Math.pow(row - centerY, 2));

                // Draw crosshair lines
                if (row == centerY || col == centerX) {
                    buttons[row][col].setBackground(Color.RED);
                } 
                // Draw intensity field around center point
                else if (distance < 4) {
                    int intensity = (int)(255 * (4 - distance) / 4);
                    buttons[row][col].setBackground(new Color(intensity, 0, intensity));
                }
            }
        }

        // Update movement
        phase += 0.05;
        if (phase > Math.PI * 2) {
            phase = 0;
            currentModulator = (currentModulator + 1) % modulators.length;
        }
    }

    @Override
    public String getName() {
        return "XY Pad";
    }
}
