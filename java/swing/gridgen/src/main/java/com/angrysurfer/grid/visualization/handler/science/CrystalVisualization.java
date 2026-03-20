package com.angrysurfer.grid.visualization.handler.science;

import java.awt.Color;
import java.awt.Point;
import java.util.*;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class CrystalVisualization implements IVisualizationHandler {
    private Set<Point> crystal = new HashSet<>();
    private Set<Point> growthPoints = new HashSet<>();
    private Random random = new Random();
    private final int[][] DIRECTIONS = {
        {0,1}, {1,0}, {0,-1}, {-1,0},
        {1,1}, {1,-1}, {-1,1}, {-1,-1}
    };
    private double phase = 0;

    @Override
    public void update(JButton[][] buttons) {
        if (crystal.isEmpty()) {
            initializeCrystal(buttons);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Growth step
        Set<Point> newPoints = new HashSet<>();
        for (Point p : growthPoints) {
            for (int[] dir : DIRECTIONS) {
                int newX = p.x + dir[0];
                int newY = p.y + dir[1];
                
                if (newX >= 0 && newX < buttons[0].length &&
                    newY >= 0 && newY < buttons.length &&
                    !crystal.contains(new Point(newX, newY)) &&
                    random.nextDouble() < 0.2) {
                    newPoints.add(new Point(newX, newY));
                }
            }
        }

        crystal.addAll(newPoints);
        growthPoints = newPoints;

        // Draw crystal with color variation based on distance from center
        int centerX = buttons[0].length / 2;
        int centerY = buttons.length / 2;
        
        for (Point p : crystal) {
            double distance = Math.hypot(p.x - centerX, p.y - centerY);
            double angle = Math.atan2(p.y - centerY, p.x - centerX);
            
            // Create shimmering effect
            double shimmer = Math.sin(phase + distance * 0.2 + angle * 2) * 0.5 + 0.5;
            
            int red = (int)(180 + shimmer * 75);
            int green = (int)(180 + shimmer * 75);
            int blue = (int)(220 + shimmer * 35);
            
            buttons[p.y][p.x].setBackground(new Color(
                Math.min(255, red),
                Math.min(255, green),
                Math.min(255, blue)
            ));
        }

        // Occasionally reset crystal
        if (crystal.size() > buttons.length * buttons[0].length * 0.7) {
            initializeCrystal(buttons);
        }

        phase += 0.1;
    }

    private void initializeCrystal(JButton[][] buttons) {
        crystal.clear();
        growthPoints.clear();
        
        // Start with a seed in the center
        Point seed = new Point(buttons[0].length / 2, buttons.length / 2);
        crystal.add(seed);
        growthPoints.add(seed);

        // Add a few random seeds
        for (int i = 0; i < 3; i++) {
            Point randomSeed = new Point(
                random.nextInt(buttons[0].length),
                random.nextInt(buttons.length)
            );
            crystal.add(randomSeed);
            growthPoints.add(randomSeed);
        }
    }

    @Override
    public String getName() {
        return "Crystal Growth";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.SCIENCE;
    }
}
