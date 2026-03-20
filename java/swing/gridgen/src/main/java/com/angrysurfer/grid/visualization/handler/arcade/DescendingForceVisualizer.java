package com.angrysurfer.grid.visualization.handler.arcade;

import com.angrysurfer.grid.visualization.DisplayType;
import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DescendingForceVisualizer implements IVisualizationHandler {
    private final Random random = new Random();
    private final List<Invader> invaders = new ArrayList<>();
    private int frameCount = 0;
    
    // Classic Space Invader patterns (8x8)
    private static final int[][][] INVADER_PATTERNS = {
        // Squid type
        {
            {0,0,1,1,1,1,0,0},
            {0,1,1,1,1,1,1,0},
            {1,1,1,1,1,1,1,1},
            {1,1,0,1,1,0,1,1},
            {1,1,1,1,1,1,1,1},
            {0,0,1,0,0,1,0,0},
            {0,1,0,1,1,0,1,0},
            {1,0,1,0,0,1,0,1}
        },
        // Crab type
        {
            {0,1,0,0,0,0,1,0},
            {0,0,1,0,0,1,0,0},
            {0,1,1,1,1,1,1,0},
            {1,1,0,1,1,0,1,1},
            {1,1,1,1,1,1,1,1},
            {0,1,1,1,1,1,1,0},
            {1,0,1,0,0,1,0,1},
            {0,1,0,0,0,0,1,0}
        },
        // Octopus type
        {
            {0,0,1,1,1,1,0,0},
            {0,1,1,1,1,1,1,0},
            {1,1,1,0,0,1,1,1},
            {1,1,0,1,1,0,1,1},
            {1,1,1,1,1,1,1,1},
            {0,0,1,1,1,1,0,0},
            {0,1,1,0,0,1,1,0},
            {1,1,0,1,1,0,1,1}
        },
        // UFO type
        {
            {0,0,1,1,1,1,0,0},
            {0,1,1,1,1,1,1,0},
            {1,1,0,1,1,0,1,1},
            {1,1,1,1,1,1,1,1},
            {0,1,1,1,1,1,1,0},
            {0,0,1,0,0,1,0,0},
            {0,1,0,0,0,0,1,0},
            {0,0,1,1,1,1,0,0}
        },
        // Mystery ship
        {
            {0,0,1,1,1,1,0,0},
            {0,1,1,1,1,1,1,0},
            {1,1,1,1,1,1,1,1},
            {0,1,0,1,1,0,1,0},
            {0,1,1,1,1,1,1,0},
            {0,0,1,0,0,1,0,0},
            {0,1,0,1,1,0,1,0},
            {0,0,1,0,0,1,0,0}
        }
    };

    private static final Color[] COLORS = {
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.MAGENTA,
        Color.CYAN,
        Color.ORANGE,
        Color.PINK
    };

    private class Invader {
        double y;           // Using double for smooth movement
        int x;
        double speed;
        Color color;
        boolean isRainbow;
        int patternIndex;  // Added to track which pattern to use
        
        Invader() {
            reset();
        }
        
        void reset() {
            y = -INVADER_PATTERNS[0].length;  // Start above viewport
            x = random.nextInt(40);       // Random horizontal position
            speed = 0.1 + random.nextDouble() * 0.2;  // Random speed
            isRainbow = random.nextDouble() < 0.1; // 10% chance of rainbow
            patternIndex = random.nextInt(INVADER_PATTERNS.length);  // Randomly select pattern
            if (!isRainbow) {
                color = COLORS[random.nextInt(COLORS.length)];
            }
        }
        
        void update() {
            y += speed;
            if (y > 8) {  // Reset when fully past viewport
                reset();
            }
        }
        
        void draw(JButton[][] buttons) {
            int startY = (int)y;
            int[][] pattern = INVADER_PATTERNS[patternIndex];  // Use selected pattern
            // Only draw visible portions of the invader
            for (int row = 0; row < pattern.length; row++) {
                int gridY = startY + row;
                if (gridY >= 0 && gridY < buttons.length) {
                    for (int col = 0; col < pattern[0].length; col++) {
                        int gridX = x + col;
                        if (gridX >= 0 && gridX < buttons[0].length && pattern[row][col] == 1) {
                            JButton button = buttons[gridY][gridX];
                            if (isRainbow) {
                                // For rainbow invaders, use different colors for each row
                                button.setBackground(COLORS[(row + frameCount) % COLORS.length]);
                            } else {
                                button.setBackground(color);
                            }
                            button.repaint();
                        }
                    }
                }
            }
        }
    }

    public DescendingForceVisualizer() {
        // Create initial invaders
        for (int i = 0; i < 3; i++) {
            Invader invader = new Invader();
            // Stagger initial positions
            invader.y = -INVADER_PATTERNS[0].length - (i * (INVADER_PATTERNS[0].length + 4));
            invaders.add(invader);
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        // Paint the entire grid black first
        for (JButton[] row : buttons) {
            for (JButton button : row) {
                button.setBackground(Color.BLACK);
                button.repaint();
            }
        }

        // Update and draw all invaders on top of black background
        for (Invader invader : invaders) {
            invader.update();
            invader.draw(buttons);
        }

        frameCount++;
    }

    @Override
    public String getName() {
        return "Descending Force";
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
