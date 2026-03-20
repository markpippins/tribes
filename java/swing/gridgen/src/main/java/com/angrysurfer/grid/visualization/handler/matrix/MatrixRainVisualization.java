package com.angrysurfer.grid.visualization.handler.matrix;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class MatrixRainVisualization implements IVisualizationHandler {
    private final Random random = new Random();
    private int[] dropPositions;
    private int[] dropSpeeds;

    @Override
    public void update(JButton[][] buttons) {
        if (dropPositions == null) {
            initializeDrops(buttons[0].length);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update and draw drops
        for (int col = 0; col < buttons[0].length; col++) {
            dropPositions[col] += dropSpeeds[col];

            // Reset drop if it reached bottom
            if (dropPositions[col] >= buttons.length * 16) {
                dropPositions[col] = 0;
                dropSpeeds[col] = random.nextInt(2) + 1;
            }

            // Draw drop with trail
            int headPos = dropPositions[col] / 16;
            if (headPos < buttons.length) {
                // Draw bright head
                buttons[headPos][col].setBackground(Color.WHITE);

                // Draw fading trail
                for (int i = 1; i <= 3 && headPos - i >= 0; i++) {
                    int trailIntensity = 255 - (i * 60);
                    buttons[headPos - i][col].setBackground(new Color(0, trailIntensity, 0));
                }
            }
        }

        // Randomly start new drops
        if (random.nextInt(3) == 0) {
            int col = random.nextInt(buttons[0].length);
            if (dropPositions[col] > buttons.length * 16) {
                dropPositions[col] = 0;
            }
        }
    }

    private void initializeDrops(int cols) {
        dropPositions = new int[cols];
        dropSpeeds = new int[cols];
        for (int i = 0; i < cols; i++) {
            dropPositions[i] = random.nextInt(cols * 16);
            dropSpeeds[i] = random.nextInt(2) + 1;
        }
    }

    @Override
    public String getName() {
        return "Matrix Rain";
    }
   
    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MATRIX;
    }
}
