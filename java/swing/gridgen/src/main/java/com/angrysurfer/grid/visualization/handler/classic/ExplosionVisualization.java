package com.angrysurfer.grid.visualization.handler.classic;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class ExplosionVisualization implements IVisualizationHandler {
    private final Random random = new Random();

    @Override
    public void update(JButton[][] buttons) {
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                if (random.nextInt(100) < 10) {
                    buttons[row][col].setBackground(new Color(
                            random.nextInt(156) + 100,
                            random.nextInt(50),
                            0));
                } else {
                    buttons[row][col].setBackground(buttons[0][0].getParent().getBackground());
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Explosion";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.CLASSIC;
    }
}
