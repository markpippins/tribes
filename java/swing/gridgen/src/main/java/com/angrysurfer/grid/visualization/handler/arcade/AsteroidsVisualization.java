package com.angrysurfer.grid.visualization.handler.arcade;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class AsteroidsVisualization implements IVisualizationHandler {
    private Point shipPos;
    private double shipAngle = 0;
    private List<Point> shots = new ArrayList<>();
    private List<Asteroid> asteroids = new ArrayList<>();
    private final Random random = new Random();

    private class Asteroid {
        double x, y;
        double dx, dy;
        int size;
        
        Asteroid(double x, double y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 0.1 + random.nextDouble() * 0.2;
            this.dx = Math.cos(angle) * speed;
            this.dy = Math.sin(angle) * speed;
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        if (shipPos == null) {
            initializeGame(buttons);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update ship rotation (AI controlled)
        if (asteroids.size() > 0) {
            Asteroid nearest = asteroids.get(0);
            double targetAngle = Math.atan2(nearest.y - shipPos.y, nearest.x - shipPos.x);
            double angleDiff = targetAngle - shipAngle;
            if (Math.abs(angleDiff) > 0.1) {
                shipAngle += Math.signum(angleDiff) * 0.1;
            }
        }

        // Update shots
        for (Point shot : new ArrayList<>(shots)) {
            shot.x += Math.cos(shipAngle) * 2;
            shot.y += Math.sin(shipAngle) * 2;
            
            // Remove shots that are off screen
            if (shot.x < 0 || shot.x >= buttons[0].length || 
                shot.y < 0 || shot.y >= buttons.length) {
                shots.remove(shot);
            }
        }

        // Update asteroids
        for (Asteroid asteroid : new ArrayList<>(asteroids)) {
            asteroid.x += asteroid.dx;
            asteroid.y += asteroid.dy;
            
            // Wrap around screen
            asteroid.x = Math.floorMod((int)asteroid.x, buttons[0].length);
            asteroid.y = Math.floorMod((int)asteroid.y, buttons.length);

            // Check for collisions with shots
            for (Point shot : new ArrayList<>(shots)) {
                if (Math.abs(shot.x - asteroid.x) < asteroid.size &&
                    Math.abs(shot.y - asteroid.y) < asteroid.size) {
                    shots.remove(shot);
                    asteroids.remove(asteroid);
                    if (asteroid.size > 1) {
                        asteroids.add(new Asteroid(asteroid.x, asteroid.y, asteroid.size - 1));
                        asteroids.add(new Asteroid(asteroid.x, asteroid.y, asteroid.size - 1));
                    }
                    break;
                }
            }
        }

        // AI shooting
        if (random.nextInt(5) == 0 && asteroids.size() > 0) {
            shots.add(new Point(shipPos.x, shipPos.y));
        }

        // Draw shots
        for (Point shot : shots) {
            if (shot.x >= 0 && shot.x < buttons[0].length &&
                shot.y >= 0 && shot.y < buttons.length) {
                buttons[shot.y][shot.x].setBackground(Color.GREEN);
            }
        }

        // Draw asteroids
        for (Asteroid asteroid : asteroids) {
            int x = (int)asteroid.x;
            int y = (int)asteroid.y;
            if (x >= 0 && x < buttons[0].length &&
                y >= 0 && y < buttons.length) {
                buttons[y][x].setBackground(Color.WHITE);
            }
        }

        // Draw ship
        int shipX = (int)(shipPos.x + Math.cos(shipAngle));
        int shipY = (int)(shipPos.y + Math.sin(shipAngle));
        if (shipX >= 0 && shipX < buttons[0].length &&
            shipY >= 0 && shipY < buttons.length) {
            buttons[shipPos.y][shipPos.x].setBackground(Color.CYAN);
            buttons[shipY][shipX].setBackground(Color.BLUE);
        }

        // Respawn asteroids if none left
        if (asteroids.isEmpty()) {
            initializeAsteroids(buttons);
        }
    }

    private void initializeGame(JButton[][] buttons) {
        shipPos = new Point(buttons[0].length / 2, buttons.length / 2);
        shipAngle = 0;
        shots.clear();
        initializeAsteroids(buttons);
    }

    private void initializeAsteroids(JButton[][] buttons) {
        asteroids.clear();
        for (int i = 0; i < 5; i++) {
            asteroids.add(new Asteroid(
                random.nextInt(buttons[0].length),
                random.nextInt(buttons.length),
                3
            ));
        }
    }

    @Override
    public String getName() {
        return "Asteroids";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
