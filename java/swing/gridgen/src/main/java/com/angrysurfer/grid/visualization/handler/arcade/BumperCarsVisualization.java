package com.angrysurfer.grid.visualization.handler.arcade;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class BumperCarsVisualization implements IVisualizationHandler {
    private final Random random = new Random();
    private List<BumperCar> cars = new ArrayList<>();
    private static final int NUM_CARS = 4;
    private static final Color[] CAR_COLORS = {
        new Color(255, 50, 50),   // Red
        new Color(50, 255, 50),   // Green
        new Color(50, 50, 255),   // Blue
        new Color(255, 255, 50)   // Yellow
    };

    private class BumperCar {
        Point position;
        Point velocity;
        Color color;
        
        BumperCar(int x, int y, Color color) {
            this.position = new Point(x, y);
            this.velocity = new Point(random.nextInt(3)-1, random.nextInt(3)-1);
            this.color = color;
        }

        void update(int maxX, int maxY) {
            // Move car
            position.x = Math.floorMod(position.x + velocity.x, maxX);
            position.y = Math.floorMod(position.y + velocity.y, maxY);

            // Random direction change (10% chance)
            if (random.nextInt(100) < 10) {
                velocity.x = random.nextInt(3) - 1;
                velocity.y = random.nextInt(3) - 1;
            }
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        int rows = buttons.length;
        int cols = buttons[0].length;

        // Initialize cars if needed
        if (cars.isEmpty()) {
            for (int i = 0; i < NUM_CARS; i++) {
                cars.add(new BumperCar(
                    random.nextInt(cols),
                    random.nextInt(rows),
                    CAR_COLORS[i % CAR_COLORS.length]
                ));
            }
        }

        // Clear grid
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                buttons[row][col].setBackground(buttons[0][0].getParent().getBackground());
            }
        }

        // Update and draw cars
        for (BumperCar car : cars) {
            car.update(cols, rows);
            
            // Draw car and "headlights"
            buttons[car.position.y][car.position.x].setBackground(car.color);
            
            // Draw trail/glow effect in movement direction
            int trailX = Math.floorMod(car.position.x - car.velocity.x, cols);
            int trailY = Math.floorMod(car.position.y - car.velocity.y, rows);
            
            Color fadeColor = new Color(
                car.color.getRed(),
                car.color.getGreen(),
                car.color.getBlue(),
                128  // Semi-transparent
            );
            
            buttons[trailY][trailX].setBackground(fadeColor);
        }

        // Check for collisions
        for (int i = 0; i < cars.size(); i++) {
            for (int j = i + 1; j < cars.size(); j++) {
                BumperCar car1 = cars.get(i);
                BumperCar car2 = cars.get(j);

                if (car1.position.equals(car2.position)) {
                    // Collision! Swap velocities
                    Point tempVel = new Point(car1.velocity);
                    car1.velocity = car2.velocity;
                    car2.velocity = tempVel;

                    // Add collision effect
                    Color sparkColor = Color.WHITE;
                    buttons[car1.position.y][car1.position.x].setBackground(sparkColor);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Bumper Cars";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
