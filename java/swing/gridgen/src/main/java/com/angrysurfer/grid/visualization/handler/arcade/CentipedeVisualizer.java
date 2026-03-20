package com.angrysurfer.grid.visualization.handler.arcade;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.angrysurfer.grid.visualization.DisplayType;
import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class CentipedeVisualizer implements IVisualizationHandler {
    private final Random random = new Random();
    private final List<Segment> segments = new ArrayList<>();
    private static final int CENTIPEDE_LENGTH = 12;
    private static final Color HEAD_COLOR = new Color(0, 0, 255);  // Bright blue
    private static final Color BODY_COLOR = new Color(100, 100, 255);  // Light blue
    private static final Color MUSHROOM_COLOR = new Color(139, 69, 19);  // Brown
    private static final Color GRASS_COLOR = new Color(34, 139, 34);  // Forest green
    private final boolean[][] mushrooms;
    private int frameCount = 0;

    private class Segment {
        double x, y;
        boolean movingRight;
        
        Segment(double x, double y, boolean movingRight) {
            this.x = x;
            this.y = y;
            this.movingRight = movingRight;
        }
        
        void update() {
            // Move horizontally
            if (movingRight) {
                x += 0.2;
                if (x >= 47 || isMushroom((int)x + 1, (int)y)) {
                    movingRight = false;
                    y += 1;  // Move down one row
                }
            } else {
                x -= 0.2;
                if (x <= 0 || isMushroom((int)x - 1, (int)y)) {
                    movingRight = true;
                    y += 1;  // Move down one row
                }
            }
            
            // Reset to top if reached bottom
            if (y >= 7) {
                y = 0;
                x = random.nextInt(48);
            }
        }
    }

    public CentipedeVisualizer() {
        // Initialize mushroom field
        mushrooms = new boolean[8][48];
        for (int i = 0; i < 20; i++) {  // Place 20 random mushrooms
            int x = random.nextInt(48);
            int y = random.nextInt(7) + 1;  // Keep top row clear
            mushrooms[y][x] = true;
        }
        
        // Create centipede segments
        for (int i = 0; i < CENTIPEDE_LENGTH; i++) {
            segments.add(new Segment(i * 1.2, 0, true));
        }
    }

    private boolean isMushroom(int x, int y) {
        return y >= 0 && y < 8 && x >= 0 && x < 48 && mushrooms[y][x];
    }

    @Override
    public void update(JButton[][] buttons) {
        // Paint background green like grass
        for (JButton[] row : buttons) {
            for (JButton button : row) {
                button.setBackground(GRASS_COLOR);
                button.repaint();
            }
        }

        // Draw mushrooms
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 48; x++) {
                if (mushrooms[y][x]) {
                    buttons[y][x].setBackground(MUSHROOM_COLOR);
                }
            }
        }

        // Update and draw centipede segments
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            segment.update();
            
            // Only draw if within bounds
            int x = (int)segment.x;
            int y = (int)segment.y;
            if (x >= 0 && x < 48 && y >= 0 && y < 8) {
                buttons[y][x].setBackground(i == 0 ? HEAD_COLOR : BODY_COLOR);
                buttons[y][x].repaint();
            }
        }

        frameCount++;
    }

    @Override
    public String getName() {
        return "Centipede";
    }

    @Override
    public DisplayType getDisplayType() {
        return DisplayType.GAME;
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
