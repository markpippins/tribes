package com.angrysurfer.grid.visualization.handler.classic;

import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class ConfettiVisualization implements IVisualizationHandler {
    private final Random random = new Random();

    @Override
    public void update(JButton[][] buttons) {
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                if (random.nextInt(100) < 5) {
                    buttons[row][col].setBackground(VisualizationUtils.RAINBOW_COLORS[
                        random.nextInt(VisualizationUtils.RAINBOW_COLORS.length)]);
                } else if (random.nextInt(100) < 10) {
                    buttons[row][col].setBackground(buttons[0][0].getParent().getBackground());
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Confetti";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.CLASSIC;
    }
}
