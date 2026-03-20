package com.angrysurfer.grid.visualization.handler;

import java.awt.Color;

import javax.swing.JButton;

import com.angrysurfer.grid.visualization.DisplayType;
import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;

public class SlotMachineVisualizer implements IVisualizationHandler {
    private int[] positions = {0, 0, 0};
    private int[] speeds = {3, 4, 5};  // Different speeds for each reel
    private int[] spinTime = {50, 70, 90};  // How long each reel spins
    private int frameCount = 0;
    
    // Slot machine symbols (5x5 patterns)
    private static final int[][][] SYMBOLS = {
        // Cherry
        {{0,0,1,0,0},
         {0,1,1,1,0},
         {0,0,1,0,0},
         {0,1,0,0,0},
         {1,0,0,0,0}},
        // Bell
        {{0,0,1,0,0},
         {0,1,1,1,0},
         {0,1,1,1,0},
         {1,1,1,1,1},
         {0,1,1,1,0}},
        // Seven
        {{1,1,1,1,0},
         {0,0,0,1,0},
         {0,1,1,1,0},
         {0,1,0,0,0},
         {0,1,1,1,0}},
        // Star
        {{0,0,1,0,0},
         {0,1,1,1,0},
         {1,1,1,1,1},
         {0,1,1,1,0},
         {0,0,1,0,0}},
        // BAR
        {{1,1,1,1,1},
         {1,0,1,0,1},
         {1,1,1,1,1},
         {1,0,1,0,1},
         {1,1,1,1,1}}
    };
    
    @Override
    public void update(JButton[][] buttons) {
        // Clear the display
        for (JButton[] row : buttons) {
            for (JButton button : row) {
                button.setBackground(Color.RED);
            }
        }
        
        // Draw border
        for (int i = 0; i < buttons[0].length; i++) {
            buttons[0][i].repaint();  // Top border
            buttons[7][i].repaint();  // Bottom border
        }
        for (int i = 0; i < buttons.length; i++) {
            buttons[i][0].repaint();  // Left border
            buttons[i][15].repaint(); // First divider
            buttons[i][31].repaint(); // Second divider
            buttons[i][47].repaint(); // Right border
        }

        // Update and draw each reel
        for (int reel = 0; reel < 3; reel++) {
            // Update position if still spinning
            if (frameCount < spinTime[reel]) {
                positions[reel] = (positions[reel] + speeds[reel]) % (SYMBOLS.length * 6);
            }
            
            // Calculate which symbol to show
            int symbolIndex = (positions[reel] / 6) % SYMBOLS.length;
            int[][] symbol = SYMBOLS[symbolIndex];
            
            // Calculate where to draw the symbol
            int startX = 2 + (reel * 16);  // 16 columns per reel section
            int startY = 1;  // Start after top border
            
            // Draw the symbol
            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < 5; x++) {
                    if (symbol[y][x] == 1) {
                        // Set the button to "on" for the symbol
                        buttons[startY + y][startX + x].setBackground(Color.GREEN);
                    }
                    else {
                        // Set the button to "off" (background color)
                        buttons[startY + y][startX + x].setBackground(Color.RED);
                    }
                }
            }
        }
        
        // Increment frame counter
        frameCount = (frameCount + 1) % 200;  // Reset after 200 frames
        
        // Reset positions and frame count if all reels have stopped
        if (frameCount == 199) {
            positions = new int[]{0, 0, 0};
            frameCount = 0;
        }
    }

    @Override
    public String getName() {
        return "Slot Machine";
    }

    @Override
    public DisplayType getDisplayType() {
        return DisplayType.GAME;
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.DEFAULT;
    }
}
