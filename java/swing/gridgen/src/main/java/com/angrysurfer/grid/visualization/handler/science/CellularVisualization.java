package com.angrysurfer.grid.visualization.handler.science;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class CellularVisualization implements IVisualizationHandler {
    private int[][] cells;
    private final Random random = new Random();
    private final int[][] directions = {
        {-1,-1}, {-1,0}, {-1,1},
        {0,-1},          {0,1},
        {1,-1},  {1,0},  {1,1}
    };
    private double phase = 0;

    @Override
    public void update(JButton[][] buttons) {
        if (cells == null) {
            initializeCells(buttons);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update cells based on rules
        int[][] newCells = new int[cells.length][cells[0].length];
        for (int y = 0; y < cells.length; y++) {
            for (int x = 0; x < cells[0].length; x++) {
                int neighbors = countNeighbors(x, y);
                
                // Rule set inspired by reaction-diffusion systems
                if (cells[y][x] > 0) {
                    if (neighbors < 2 || neighbors > 4) {
                        newCells[y][x] = Math.max(0, cells[y][x] - 1);
                    } else {
                        newCells[y][x] = Math.min(5, cells[y][x] + 1);
                    }
                } else if (neighbors == 3) {
                    newCells[y][x] = 1;
                }
            }
        }
        cells = newCells;

        // Draw cells with color based on age
        for (int y = 0; y < cells.length; y++) {
            for (int x = 0; x < cells[0].length; x++) {
                if (cells[y][x] > 0) {
                    double value = cells[y][x] / 5.0;
                    double hue = (value * 0.5 + phase) % 1.0;
                    buttons[y][x].setBackground(Color.getHSBColor((float)hue, 0.8f, 0.9f));
                }
            }
        }

        // Random cell spawning
        if (random.nextInt(10) == 0) {
            int x = random.nextInt(cells[0].length);
            int y = random.nextInt(cells.length);
            cells[y][x] = 1;
        }

        phase += 0.02;
    }

    private int countNeighbors(int x, int y) {
        int count = 0;
        for (int[] dir : directions) {
            int nx = Math.floorMod(x + dir[0], cells[0].length);
            int ny = Math.floorMod(y + dir[1], cells.length);
            if (cells[ny][nx] > 0) count++;
        }
        return count;
    }

    private void initializeCells(JButton[][] buttons) {
        cells = new int[buttons.length][buttons[0].length];
        for (int i = 0; i < buttons.length * buttons[0].length / 4; i++) {
            int x = random.nextInt(buttons[0].length);
            int y = random.nextInt(buttons.length);
            cells[y][x] = 1;
        }
    }

    @Override
    public String getName() {
        return "Cellular";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.SCIENCE;
    }
}
