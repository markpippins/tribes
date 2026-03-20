package com.angrysurfer.grid.visualization.handler.arcade;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import javax.swing.JButton;

public class StarTrekVisualization implements IVisualizationHandler {
    private List<Star> stars = new ArrayList<>();
    private Ship enterprise;
    private Random random = new Random();
    private double phase = 0;

    private class Star {
        double x, y, z;
        double speed;

        Star() {
            respawn();
        }

        void respawn() {
            x = random.nextDouble() * 2 - 1;
            y = random.nextDouble() * 2 - 1;
            z = 1.0;
            speed = random.nextDouble() * 0.02 + 0.01;
        }

        void update() {
            z -= speed;
            if (z <= 0)
                respawn();
        }
    }

    private class Ship {
        int x, y;
        boolean warping;
        int warpPhase;

        Ship() {
            x = 18;
            y = 4;
            warping = false;
            warpPhase = 0;
        }

        void draw(JButton[][] buttons) {
            // Draw Enterprise
            if (!warping || warpPhase % 2 == 0) {
                // Main hull
                buttons[y][x].setBackground(Color.WHITE);
                buttons[y][x + 1].setBackground(Color.WHITE);

                // Nacelles
                if (y > 0 && y < buttons.length - 1) {
                    buttons[y - 1][x + 1].setBackground(Color.RED);
                    buttons[y + 1][x + 1].setBackground(Color.RED);
                }
            }

            if (warping) {
                warpPhase++;
                if (warpPhase > 10) {
                    warping = false;
                    warpPhase = 0;
                }
            } else if (random.nextInt(100) == 0) {
                warping = true;
            }
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        if (stars.isEmpty()) {
            for (int i = 0; i < 30; i++) {
                stars.add(new Star());
            }
            enterprise = new Ship();
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update and draw stars
        for (Star star : stars) {
            star.update();

            // Project 3D to 2D
            int projX = (int) ((star.x / star.z) * buttons[0].length / 2 + buttons[0].length / 2);
            int projY = (int) ((star.y / star.z) * buttons.length / 2 + buttons.length / 2);

            if (projX >= 0 && projX < buttons[0].length &&
                    projY >= 0 && projY < buttons.length) {
                float brightness = (float) (1.0 - star.z);
                buttons[projY][projX].setBackground(
                        Color.getHSBColor(0.6f, 0.2f, brightness));
            }
        }

        // Draw Enterprise
        enterprise.draw(buttons);

        // Draw warp effect when active
        if (enterprise.warping) {
            for (int y = 0; y < buttons.length; y++) {
                for (int x = enterprise.x + 2; x < buttons[0].length; x++) {
                    if (random.nextDouble() < 0.3) {
                        float hue = (float) x / buttons[0].length;
                        buttons[y][x].setBackground(
                                Color.getHSBColor(hue, 0.8f, 1.0f));
                    }
                }
            }
        }

        phase += 0.05;
    }

    @Override
    public String getName() {
        return "Star Trek";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }

}
