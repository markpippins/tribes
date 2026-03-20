package com.angrysurfer.grid.visualization.handler.arcade;

import java.awt.Color;
import com.angrysurfer.grid.visualization.*;
import javax.swing.JButton;

public class MoonPatrolVisualization implements IVisualizationHandler {
    private double vehicleX = 12;
    private double vehicleY = 8;
    private double velocityY = 0;
    private boolean isJumping = false;
    private int terrainOffset = 0;
    private int[] obstacles = new int[48];
    private static final double GRAVITY = 0.3;
    private static final double JUMP_VELOCITY = -1.2;
    private static final Color VEHICLE_COLOR = Color.CYAN;
    private static final Color GROUND_COLOR = new Color(139, 69, 19); // Brown
    private static final Color MOUNTAIN_COLOR = new Color(100, 100, 100);
    
    public MoonPatrolVisualization() {
        // Initialize random obstacles
        for (int i = 0; i < obstacles.length; i++) {
            if (Math.random() < 0.15) { // 15% chance of obstacle
                obstacles[i] = 1;
            }
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        int rows = buttons.length;
        int cols = buttons[0].length;

        // Physics update
        if (isJumping) {
            velocityY += GRAVITY;
            vehicleY += velocityY;
            
            // Ground collision
            if (vehicleY >= 8) {
                vehicleY = 8;
                velocityY = 0;
                isJumping = false;
            }
        }

        // Auto-jump when approaching obstacle
        int nextObstaclePos = ((int)vehicleX + 2) % cols;
        if (!isJumping && obstacles[nextObstaclePos] == 1) {
            velocityY = JUMP_VELOCITY;
            isJumping = true;
        }

        // Draw scrolling mountains (background)
        for (int col = 0; col < cols; col++) {
            int mountainHeight = 2 + (int)(Math.sin((col + terrainOffset) * 0.2) * 2);
            for (int row = rows - mountainHeight - 3; row < rows - 3; row++) {
                if (row >= 0 && row < rows) {
                    buttons[row][col].setBackground(MOUNTAIN_COLOR);
                }
            }
        }

        // Draw ground with craters
        for (int col = 0; col < cols; col++) {
            int groundRow = rows - 3;
            buttons[groundRow][col].setBackground(GROUND_COLOR);
            
            // Draw obstacles
            if (obstacles[(col + terrainOffset) % cols] == 1) {
                buttons[groundRow - 1][col].setBackground(GROUND_COLOR);
            }
        }

        // Draw vehicle
        int vx = (int)vehicleX;
        int vy = (int)vehicleY;
        
        // Vehicle body
        buttons[vy][vx].setBackground(VEHICLE_COLOR);
        buttons[vy][vx + 1].setBackground(VEHICLE_COLOR);
        
        // Wheels
        if (!isJumping) {
            buttons[vy + 1][vx].setBackground(Color.DARK_GRAY);
            buttons[vy + 1][vx + 1].setBackground(Color.DARK_GRAY);
        }

        // Scroll terrain
        terrainOffset = (terrainOffset + 1) % cols;
    }

    @Override
    public String getName() {
        return "Moon Patrol";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
