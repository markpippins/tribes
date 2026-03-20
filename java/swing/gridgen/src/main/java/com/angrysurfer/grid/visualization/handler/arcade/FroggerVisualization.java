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

public class FroggerVisualization implements IVisualizationHandler {
    private Point frog;
    private List<Row> rows = new ArrayList<>();
    private Random random = new Random();
    private int updateCount = 0;
    private boolean isDead = false;
    private int score = 0;

    private class Row {
        List<Vehicle> vehicles = new ArrayList<>();
        int direction;
        int speed;
        boolean isWater;
        boolean isHome;
        int y;
        
        Row(int y, boolean isWater, int gridWidth) {
            this.y = y;
            this.direction = random.nextBoolean() ? 1 : -1;
            this.speed = random.nextInt(3) + 1;
            this.isWater = isWater;
            this.isHome = y == 0;
            
            // Initialize vehicles/logs
            int spacing = random.nextInt(4) + 4;
            for (int x = 0; x < gridWidth; x += spacing) {
                vehicles.add(new Vehicle(x, 2 + random.nextInt(2)));
            }
        }
        
        void update() {
            // Move vehicles
            for (Vehicle v : vehicles) {
                v.x = Math.floorMod(v.x + direction * speed, 36 + v.length);
            }
        }
        
        boolean collidesWith(int x, int y, int row) {
            if (row != y) return false;
            
            if (isWater) {
                // Check if frog is on a log
                for (Vehicle log : vehicles) {
                    if (x >= log.x && x < log.x + log.length) {
                        return false;
                    }
                }
                return true; // Drowning!
            } else if (!isHome) {
                // Check vehicle collisions
                for (Vehicle v : vehicles) {
                    if (x >= v.x && x < v.x + v.length) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private class Vehicle {
        int x;
        int length;
        
        Vehicle(int x, int length) {
            this.x = x;
            this.length = length;
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        if (rows.isEmpty()) {
            initializeGame(buttons);
        }

        updateCount++;
        if (updateCount % 3 == 0) {
            // Update rows
            for (Row row : rows) {
                row.update();
                
                // Move frog with log if on water
                if (row.isWater && row.y == frog.y) {
                    frog.x = Math.floorMod(frog.x + row.direction * row.speed, buttons[0].length);
                }
            }
            
            // Check collisions
            for (Row row : rows) {
                if (row.collidesWith(frog.x, frog.y, row.y)) {
                    isDead = true;
                }
            }
        }

        // AI movement
        if (updateCount % 5 == 0 && !isDead) {
            moveAI(buttons);
        }

        // Draw everything
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        // Draw rows
        for (int y = 0; y < buttons.length; y++) {
            Row row = rows.get(y);
            
            // Draw water/road
            if (row.isWater) {
                for (int x = 0; x < buttons[0].length; x++) {
                    buttons[y][x].setBackground(new Color(0, 0, 150));
                }
            }
            
            // Draw vehicles/logs
            for (Vehicle v : row.vehicles) {
                for (int i = 0; i < v.length; i++) {
                    int x = Math.floorMod(v.x + i, buttons[0].length);
                    Color color = row.isWater ? new Color(139, 69, 19) : 
                                row.isHome ? Color.GREEN : Color.RED;
                    buttons[y][x].setBackground(color);
                }
            }
        }
        
        // Draw frog
        if (!isDead) {
            buttons[frog.y][frog.x].setBackground(Color.GREEN);
        }

        // Reset if dead
        if (isDead && updateCount % 50 == 0) {
            initializeGame(buttons);
        }
    }

    private void moveAI(JButton[][] buttons) {
        // Simple AI: Try to move up while avoiding obstacles
        Row currentRow = rows.get(frog.y);
        
        // Look for safe spots in row above
        if (frog.y > 0) {
            Row nextRow = rows.get(frog.y - 1);
            
            // Check current position, left, and right
            int[] moves = {0, -1, 1, -2, 2};
            for (int dx : moves) {
                int newX = Math.floorMod(frog.x + dx, buttons[0].length);
                if (!nextRow.collidesWith(newX, frog.y - 1, frog.y - 1)) {
                    frog.x = newX;
                    frog.y--;
                    if (frog.y == 0) score++;
                    return;
                }
            }
        }
        
        // If can't move up, dodge obstacles
        if (currentRow.collidesWith(frog.x, frog.y, frog.y)) {
            frog.x = Math.floorMod(frog.x + 1, buttons[0].length);
        }
    }

    private void initializeGame(JButton[][] buttons) {
        frog = new Point(buttons[0].length / 2, buttons.length - 1);
        isDead = false;
        rows.clear();
        
        // Initialize rows with grid width
        for (int y = 0; y < buttons.length; y++) {
            rows.add(new Row(y, y > 0 && y < 4, buttons[0].length));
        }
    }

    @Override
    public String getName() {
        return "Frogger";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
