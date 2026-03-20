package com.angrysurfer.grid.visualization.handler.matrix;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class RainVisualization implements IVisualizationHandler {
    private final Random random = new Random();

    @Override
    public void update(JButton[][] buttons) {
        // Shift everything down
        for (int row = buttons.length - 1; row > 0; row--) {
            for (int col = 0; col < buttons[0].length; col++) {
                buttons[row][col].setBackground(buttons[row - 1][col].getBackground());
            }
        }
        
        // New drops at top
        for (int col = 0; col < buttons[0].length; col++) {
            if (random.nextInt(100) < 15) {
                buttons[0][col].setBackground(Color.GREEN);
            } else {
                buttons[0][col].setBackground(buttons[0][0].getParent().getBackground());
            }
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
