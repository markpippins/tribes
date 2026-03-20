package com.angrysurfer.grid.visualization.handler.classic;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class StarfieldVisualization implements IVisualizationHandler {
    private Star[] stars;
    private final Random random = new Random();
    private static final int NUM_STARS = 50;
    private double speed = 0.05;

    private class Star {
        double x, y, z;
        
        Star() {
            reset();
        }
        
        void reset() {
            x = random.nextDouble() * 2 - 1; // -1 to 1
            y = random.nextDouble() * 2 - 1; // -1 to 1
            z = random.nextDouble();         // 0 to 1
        }
        
        void update() {
            z -= speed;
            if (z <= 0) reset();
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        if (stars == null) {
            stars = new Star[NUM_STARS];
            for (int i = 0; i < NUM_STARS; i++) {
                stars[i] = new Star();
            }
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update and draw stars
        for (Star star : stars) {
            star.update();
            
            // Project 3D coordinates to 2D screen space
            int screenX = (int)((star.x / star.z) * buttons[0].length / 4 + buttons[0].length / 2);
            int screenY = (int)((star.y / star.z) * buttons.length / 4 + buttons.length / 2);
            
            // Calculate brightness based on z-position
            int brightness = (int)(255 * (1 - star.z));
            
            // Draw star if it's within bounds
            if (screenX >= 0 && screenX < buttons[0].length &&
                screenY >= 0 && screenY < buttons.length) {
                buttons[screenY][screenX].setBackground(new Color(brightness, brightness, brightness));
                
                // Draw trail
                if (star.z < 0.5) {
                    int trailLength = (int)(3 * (1 - star.z));
                    for (int i = 1; i <= trailLength; i++) {
                        int trailX = screenX - i;
                        if (trailX >= 0 && trailX < buttons[0].length) {
                            int trailBrightness = brightness / (i + 1);
                            buttons[screenY][trailX].setBackground(
                                new Color(trailBrightness, trailBrightness, trailBrightness)
                            );
                        }
                    }
                }
            }
        }

        // Occasionally adjust speed
        if (random.nextInt(100) == 0) {
            speed = 0.03 + random.nextDouble() * 0.04;
        }
    }

    @Override
    public String getName() {
        return "Starfield";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.CLASSIC;
    }
}
