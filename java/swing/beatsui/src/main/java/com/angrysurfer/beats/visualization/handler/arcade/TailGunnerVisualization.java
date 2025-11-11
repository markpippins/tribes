package com.angrysurfer.beats.visualization.handler.arcade;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationUtils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import javax.swing.JButton;

public class TailGunnerVisualization implements IVisualizationHandler {
    private final Random random = new Random();
    private final List<Enemy> enemies = new ArrayList<>();
    private int frame = 0;
    private double crosshairAngle = 0;
    private boolean isFiring = false;
    
    private static final int MAX_ENEMIES = 5;
    private static final double ROTATION_SPEED = 0.1;
    private static final int CROSS_HAIR_SIZE = 3;

    private class Enemy {
        double x, y, z;
        double dx, dy;
        boolean hit;

        Enemy(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dx = (random.nextDouble() - 0.5) * 0.02;
            this.dy = (random.nextDouble() - 0.5) * 0.02;
            this.hit = false;
        }

        void update() {
            x += dx;
            y += dy;
            z -= 0.01; // Move toward viewer
        }

        void draw(JButton[][] buttons, int width, int height) {
            if (hit) return;
            
            // Perspective projection
            double scale = 1.0 / z;
            int screenX = (int)(width/2 + x * scale * width);
            int screenY = (int)(height/2 + y * scale * height);
            
            if (screenX >= 0 && screenX < width && screenY >= 0 && screenY < height) {
                Color enemyColor = frame % 2 == 0 ? Color.RED : Color.RED.darker();
                buttons[screenY][screenX].setBackground(enemyColor);
                
                // Draw enemy "wings"
                for (int dx = -1; dx <= 1; dx++) {
                    int wingX = screenX + dx;
                    if (wingX >= 0 && wingX < width) {
                        buttons[screenY][wingX].setBackground(enemyColor);
                    }
                }
            }
        }
    }
    
    @Override
    public void update(JButton[][] buttons) {
        int width = buttons[0].length;
        int height = buttons.length;
        frame++;

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        // Spawn new enemies
        if (enemies.size() < MAX_ENEMIES) {
            double x = random.nextDouble() - 0.5;
            double y = random.nextDouble() - 0.5;
            enemies.add(new Enemy(x, y, 1.0));
        }

        // Update and draw enemies
        List<Enemy> toRemove = new ArrayList<>();
        for (Enemy enemy : enemies) {
            enemy.update();
            
            // Remove enemies that are too close or too far
            if (enemy.z <= 0.1 || enemy.z > 1.0 || enemy.hit) {
                toRemove.add(enemy);
            } else {
                enemy.draw(buttons, width, height);
            }
        }
        enemies.removeAll(toRemove);

        // Update crosshair rotation
        crosshairAngle += ROTATION_SPEED;
        drawCrosshair(buttons, width, height);
        
        // Random firing
        if (random.nextDouble() < 0.05) {
            isFiring = true;
            checkHits();
        } else {
            isFiring = false;
        }
    }

    private void drawCrosshair(JButton[][] buttons, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;
        
        // Draw rotating crosshair
        for (int i = 0; i < 4; i++) {
            double angle = crosshairAngle + (Math.PI / 2 * i);
            int x = centerX + (int)(Math.cos(angle) * CROSS_HAIR_SIZE);
            int y = centerY + (int)(Math.sin(angle) * CROSS_HAIR_SIZE);
            
            if (x >= 0 && x < width && y >= 0 && y < height) {
                Color color = isFiring ? Color.YELLOW : Color.GREEN;
                buttons[y][x].setBackground(color);
            }
        }
        
        // Draw center dot
        buttons[centerY][centerX].setBackground(Color.GREEN.brighter());
    }

    private void checkHits() {
        for (Enemy enemy : enemies) {
            // Simple hit detection based on z-distance
            if (enemy.z < 0.3 && !enemy.hit) {
                enemy.hit = true;
            }
        }
    }

    @Override
    public String getName() {
        return "Tail Gunner";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
