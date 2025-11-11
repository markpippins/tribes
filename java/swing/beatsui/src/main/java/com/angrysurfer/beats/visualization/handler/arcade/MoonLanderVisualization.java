package com.angrysurfer.beats.visualization.handler.arcade;

import java.awt.Color;
import com.angrysurfer.beats.visualization.*;
import javax.swing.JButton;

public class MoonLanderVisualization implements IVisualizationHandler {
    private double landerX = 24; // Start in middle
    private double landerY = 2;  // Start near top
    private double velocityY = 0.1;
    private double velocityX = 0.05;
    private static final double GRAVITY = 0.05;
    private static final double THRUST = -0.08;
    private boolean thrust = false;
    private int frameCount = 0;

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        int cols = buttons[0].length;
        int rows = buttons.length;

        // Apply physics
        velocityY += GRAVITY;
        if (thrust && frameCount % 2 == 0) {
            velocityY += THRUST;
        }

        // Update position
        landerY += velocityY;
        landerX += velocityX;

        // Bounce off walls
        if (landerX <= 1 || landerX >= cols - 2) {
            velocityX = -velocityX;
        }

        // Ground collision
        if (landerY >= rows - 3) {
            landerY = rows - 3;
            velocityY = -velocityY * 0.5; // Bounce with dampening
        }

        // Keep in bounds
        landerY = Math.max(1, Math.min(landerY, rows - 2));
        landerX = Math.max(1, Math.min(landerX, cols - 2));

        // Draw terrain (moon surface)
        for (int col = 0; col < cols; col++) {
            int height = 2 + (int)(Math.sin(col * 0.3) * 1.5);
            for (int row = rows - height; row < rows; row++) {
                buttons[row][col].setBackground(Color.GRAY);
            }
        }

        // Draw lander
        int x = (int) landerX;
        int y = (int) landerY;

        // Lander body
        buttons[y][x].setBackground(Color.WHITE);
        buttons[y][x-1].setBackground(Color.WHITE);
        buttons[y][x+1].setBackground(Color.WHITE);
        buttons[y-1][x].setBackground(Color.WHITE);

        // Landing legs
        buttons[y+1][x-1].setBackground(Color.WHITE);
        buttons[y+1][x+1].setBackground(Color.WHITE);

        // Thrust animation
        if (thrust && frameCount % 2 == 0) {
            buttons[y+1][x].setBackground(Color.ORANGE);
            buttons[y+2][x].setBackground(Color.RED);
        }

        // Toggle thrust periodically
        if (frameCount % 20 == 0) {
            thrust = !thrust;
        }

        frameCount++;
    }

    @Override
    public String getName() {
        return "Lunar Lander";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
