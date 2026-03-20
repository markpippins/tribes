package com.angrysurfer.grid.visualization.handler.science;

import java.awt.Color;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class DNAVisualization implements IVisualizationHandler {
    private double angle = 0;
    private static final double AMPLITUDE_SCALE = 4.5; // Increased from 1.2
    private static final double WAVE_FREQUENCY = 0.2; // Adjusted for smoother waves
    private static final double VERTICAL_OFFSET = 5.5; // Center the helix vertically

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        for (int col = 0; col < buttons[0].length; col++) {
            double offset = angle + col * WAVE_FREQUENCY;
            int row1 = (int) (VERTICAL_OFFSET + Math.sin(offset) * AMPLITUDE_SCALE);
            int row2 = (int) (VERTICAL_OFFSET + Math.sin(offset + Math.PI) * AMPLITUDE_SCALE);

            if (row1 >= 0 && row1 < buttons.length) {
                buttons[row1][col].setBackground(Color.BLUE);
            }
            if (row2 >= 0 && row2 < buttons.length) {
                buttons[row2][col].setBackground(Color.RED);
            }
            
            // Draw connecting bars every 4 columns
            if (col % 4 == 0) {
                for (int row = Math.min(row1, row2); row <= Math.max(row1, row2); row++) {
                    if (row >= 0 && row < buttons.length) {
                        buttons[row][col].setBackground(Color.GREEN);
                    }
                }
            }
        }
        angle += 0.1;
    }

    @Override
    public String getName() {
        return "DNA Helix";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.SCIENCE;
    }
}
