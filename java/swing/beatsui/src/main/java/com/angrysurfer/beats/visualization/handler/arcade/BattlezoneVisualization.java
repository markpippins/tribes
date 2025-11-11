package com.angrysurfer.beats.visualization.handler.arcade;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationUtils;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import javax.swing.JButton;

public class BattlezoneVisualization implements IVisualizationHandler {
    private double playerAngle = 0;
    private double playerX = 0.5;
    private double playerY = 0.5;
    private final List<Point> enemies = new ArrayList<>();
    private final Random random = new Random();
    private int frame = 0;
    
    private static final int MAX_ENEMIES = 3;
    private static final double MOVEMENT_SPEED = 0.01;
    private static final double ROTATION_SPEED = 0.1;

    private void spawnEnemies(int width, int height) {
        while (enemies.size() < MAX_ENEMIES) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            // Don't spawn too close to player
            if (Math.abs(x - playerX * width) > width/4 || Math.abs(y - playerY * height) > height/4) {
                enemies.add(new Point(x, y));
            }
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        int width = buttons[0].length;
        int height = buttons.length;
        frame++;

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        spawnEnemies(width, height);

        // Move player
        playerAngle += ROTATION_SPEED;
        if (random.nextDouble() < 0.1) {
            playerX += Math.cos(playerAngle) * MOVEMENT_SPEED;
            playerY += Math.sin(playerAngle) * MOVEMENT_SPEED;
        }

        // Keep player in bounds
        playerX = Math.max(0.1, Math.min(0.9, playerX));
        playerY = Math.max(0.1, Math.min(0.9, playerY));

        // Draw perspective grid
        drawGrid(buttons, width, height);

        // Draw and update enemies
        updateEnemies(buttons, width, height);

        // Draw player's tank
        drawTank(buttons, width, height);
    }

    private void drawGrid(JButton[][] buttons, int width, int height) {
        // Draw horizontal lines
        for (int z = 1; z <= 4; z++) {
            int y = height/2 + (z * height/8);
            int startX = width/2 - (z * width/8);
            int endX = width/2 + (z * width/8);
            
            for (int x = startX; x <= endX; x++) {
                if (y >= 0 && y < height && x >= 0 && x < width) {
                    buttons[y][x].setBackground(Color.GREEN.darker());
                }
            }
        }

        // Draw vertical perspective lines
        for (int x = -4; x <= 4; x += 2) {
            int startY = height/2;
            int endY = height;
            int baseX = width/2 + (x * width/16);
            
            for (int y = startY; y <= endY; y++) {
                int xOffset = (int)((y - startY) * (x/8.0));
                int drawX = baseX + xOffset;
                if (y >= 0 && y < height && drawX >= 0 && drawX < width) {
                    buttons[y][drawX].setBackground(Color.GREEN.darker());
                }
            }
        }
    }

    private void updateEnemies(JButton[][] buttons, int width, int height) {
        List<Point> toRemove = new ArrayList<>();
        
        for (Point enemy : enemies) {
            // Move enemies randomly
            if (random.nextDouble() < 0.1) {
                enemy.x += random.nextInt(3) - 1;
                enemy.y += random.nextInt(3) - 1;
            }

            // Keep enemies in bounds
            enemy.x = Math.max(0, Math.min(width-1, enemy.x));
            enemy.y = Math.max(height/2, Math.min(height-1, enemy.y));

            // Draw enemy tank
            drawEnemyTank(buttons, enemy.x, enemy.y, width, height);

            // Remove enemies that are too far or too close
            if (enemy.y >= height-1 || enemy.y <= height/2) {
                toRemove.add(enemy);
            }
        }
        
        enemies.removeAll(toRemove);
    }

    private void drawTank(JButton[][] buttons, int width, int height) {
        int x = (int)(playerX * width);
        int y = (int)(playerY * height);
        
        // Draw tank body
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int drawX = x + dx;
                int drawY = y + dy;
                if (drawY >= 0 && drawY < height && drawX >= 0 && drawX < width) {
                    buttons[drawY][drawX].setBackground(Color.GREEN);
                }
            }
        }
        
        // Draw tank turret
        int turretX = x + (int)(Math.cos(playerAngle) * 2);
        int turretY = y + (int)(Math.sin(playerAngle) * 2);
        if (turretY >= 0 && turretY < height && turretX >= 0 && turretX < width) {
            buttons[turretY][turretX].setBackground(Color.GREEN.brighter());
        }
    }

    private void drawEnemyTank(JButton[][] buttons, int x, int y, int width, int height) {
        Color enemyColor = frame % 2 == 0 ? Color.RED : Color.RED.darker();
        
        // Draw enemy tank body
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int drawX = x + dx;
                int drawY = y + dy;
                if (drawY >= 0 && drawY < height && drawX >= 0 && drawX < width) {
                    buttons[drawY][drawX].setBackground(enemyColor);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Battlezone";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
