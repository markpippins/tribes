package com.angrysurfer.grid.visualization.handler.classic;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class FireworksVisualization implements IVisualizationHandler {
    private final Random random = new Random();

    @Override
    public void update(JButton[][] buttons) {
        // Fade existing colors
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                Color c = buttons[row][col].getBackground();
                if (!c.equals(buttons[0][0].getParent().getBackground())) {
                    buttons[row][col].setBackground(new Color(
                            Math.max(c.getRed() - 20, 0),
                            Math.max(c.getGreen() - 20, 0),
                            Math.max(c.getBlue() - 20, 0)));
                }
            }
        }

        // Random new fireworks
        if (random.nextInt(100) < 10) {
            int centerX = random.nextInt(buttons[0].length);
            Color color = new Color(
                random.nextInt(128) + 127,
                random.nextInt(128) + 127,
                random.nextInt(128) + 127);

            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int x = centerX + i;
                    int y = 2 + j;
                    if (y >= 0 && y < buttons.length && x >= 0 && x < buttons[0].length) {
                        buttons[y][x].setBackground(color);
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Fireworks";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.CLASSIC;
    }
}
