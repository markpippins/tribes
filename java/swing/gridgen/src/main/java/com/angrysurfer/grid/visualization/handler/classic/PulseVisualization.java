package com.angrysurfer.grid.visualization.handler.classic;

import java.awt.Color;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class PulseVisualization implements IVisualizationHandler {
    private double pulseSize = 0;
    private double pulseSpeed = 0.2;
    private final int maxRadius = 10;

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        int centerX = buttons[0].length / 2;
        int centerY = buttons.length / 2;

        // Draw expanding pulse
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                double distance = Math.sqrt(
                    Math.pow(col - centerX, 2) + 
                    Math.pow(row - centerY, 2) * 4); // Multiply by 4 to compensate for grid aspect ratio

                if (distance <= pulseSize && distance > pulseSize - 1) {
                    int intensity = (int)(255 * (1 - (pulseSize - distance)));
                    buttons[row][col].setBackground(new Color(intensity, intensity, intensity));
                }
            }
        }

        pulseSize += pulseSpeed;
        if (pulseSize > maxRadius) {
            pulseSize = 0;
        }
    }

    @Override
    public String getName() {
        return "Pulse";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.CLASSIC;
    }
}
