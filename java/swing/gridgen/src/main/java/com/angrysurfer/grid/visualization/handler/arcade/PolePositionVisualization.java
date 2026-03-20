package com.angrysurfer.grid.visualization.handler.arcade;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class PolePositionVisualization implements IVisualizationHandler {
    private double roadX = 0;
    private double curvePhase = 0;
    private double speed = 1.0;
    private int carPosition = 18;
    private final Random random = new Random();
    private final List<int[]> obstacles = new ArrayList<>();
    private int score = 0;
    private int distance = 0;
    private int frames = 0;
    
    private static final Color ROAD_COLOR = new Color(40, 40, 40);
    private static final Color GRASS_COLOR = new Color(0, 100, 0);
    private static final Color STRIPE_COLOR = Color.YELLOW;
    private static final Color CAR_COLOR = Color.RED;
    private static final Color OBSTACLE_COLOR = Color.BLUE;
    private static final Color HORIZON_COLOR = new Color(100, 150, 255);
    
    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        frames++;

        // Draw sky/horizon
        for (int x = 0; x < buttons[0].length; x++) {
            buttons[0][x].setBackground(HORIZON_COLOR);
        }

        // Update road position and game state
        roadX += Math.sin(curvePhase) * 0.1 * speed;
        curvePhase += 0.02 * speed;
        distance += (int)(speed * 10);

        // Spawn new obstacles
        if (frames % 50 == 0 && random.nextDouble() < 0.3) {
            int lanePosition = random.nextInt(buttons[0].length);
            obstacles.add(new int[]{0, lanePosition});
        }

        // Draw road with banking effect
        for (int y = 1; y < buttons.length; y++) {
            double perspective = (y + 1.0) / buttons.length;
            double roadWidth = 20 * perspective;
            double banking = Math.sin(curvePhase) * 5 * (1 - perspective);
            double xOffset = (roadX + banking) * (1 - perspective) * 10;
            
            int centerX = buttons[0].length / 2 + (int)xOffset;
            int leftEdge = centerX - (int)(roadWidth / 2);
            int rightEdge = centerX + (int)(roadWidth / 2);
            
            // Draw road surface and details
            for (int x = 0; x < buttons[0].length; x++) {
                if (x >= leftEdge && x <= rightEdge) {
                    buttons[y][x].setBackground(ROAD_COLOR);
                    
                    // Draw center lines with perspective
                    if (Math.abs(x - centerX) < 1 && 
                        (y + (int)(distance * perspective)) % 3 == 0) {
                        buttons[y][x].setBackground(STRIPE_COLOR);
                    }
                    
                    // Draw side markers
                    if (x == leftEdge || x == rightEdge) {
                        if ((y + (int)(distance * perspective)) % 2 == 0) {
                            buttons[y][x].setBackground(Color.WHITE);
                        }
                    }
                } else {
                    // Draw grass with checker pattern
                    if ((x + y + (int)(distance * perspective)) % 2 == 0) {
                        buttons[y][x].setBackground(GRASS_COLOR);
                    } else {
                        buttons[y][x].setBackground(GRASS_COLOR.darker());
                    }
                }
            }
        }

        // Update and draw obstacles
        Iterator<int[]> iter = obstacles.iterator();
        while (iter.hasNext()) {
            int[] obstacle = iter.next();
            obstacle[0]++; // Move down
            
            // Remove if off screen
            if (obstacle[0] >= buttons.length) {
                iter.remove();
                score += 10;
                continue;
            }

            // Draw obstacle
            int y = obstacle[0];
            int x = obstacle[1];
            if (y < buttons.length && x >= 0 && x < buttons[0].length) {
                buttons[y][x].setBackground(OBSTACLE_COLOR);
                
                // Collision detection with car
                if (y == buttons.length - 2 && 
                    Math.abs(x - carPosition) < 2) {
                    speed *= 0.8; // Slow down on collision
                    score -= 5;
                }
            }
        }

        // AI steering with obstacle avoidance
        double targetX = -Math.sin(curvePhase + 0.5) * 5;
        // Look for nearby obstacles
        for (int[] obstacle : obstacles) {
            if (obstacle[0] > buttons.length - 4) {
                if (Math.abs(obstacle[1] - carPosition) < 3) {
                    targetX += (carPosition - obstacle[1]) * 2;
                }
            }
        }
        
        double diff = targetX - roadX;
        if (Math.abs(diff) > 0.1) {
            carPosition += Math.signum(diff);
            carPosition = Math.max(0, Math.min(buttons[0].length - 1, carPosition));
        }

        // Draw car
        int carY = buttons.length - 2;
        for (int dx = -1; dx <= 1; dx++) {
            int x = carPosition + dx;
            if (x >= 0 && x < buttons[0].length) {
                buttons[carY][x].setBackground(CAR_COLOR);
                if (dx == 0) {
                    buttons[carY - 1][x].setBackground(CAR_COLOR);
                }
            }
        }

        // Vary speed with acceleration
        speed = Math.min(2.0, speed + 0.001);
        if (random.nextInt(100) == 0) {
            speed *= 0.9;
        }
    }

    @Override
    public String getName() {
        return "Pole Position";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
