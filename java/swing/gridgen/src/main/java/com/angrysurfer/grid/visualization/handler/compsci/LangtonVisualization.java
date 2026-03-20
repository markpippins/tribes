package com.angrysurfer.grid.visualization.handler.compsci;

import java.awt.Color;
import java.awt.Point;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class LangtonVisualization implements IVisualizationHandler {
    private boolean[][] grid;
    private Ant ant;
    private double phase = 0;

    private class Ant {
        Point pos;
        int direction; // 0=up, 1=right, 2=down, 3=left
        private final int[] dx = { 0, 1, 0, -1 };
        private final int[] dy = { -1, 0, 1, 0 };

        Ant(int x, int y) {
            this.pos = new Point(x, y);
            this.direction = 0;
        }

        void move(boolean turnRight) {
            // Turn
            direction = Math.floorMod(direction + (turnRight ? 1 : -1), 4);

            // Move forward
            pos.x = Math.floorMod(pos.x + dx[direction], grid[0].length);
            pos.y = Math.floorMod(pos.y + dy[direction], grid.length);
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        if (grid == null) {
            initializeGrid(buttons);
        }

        // Update ant multiple times per frame for more interesting patterns
        for (int i = 0; i < 5; i++) {
            // Get current cell state
            boolean currentCell = grid[ant.pos.y][ant.pos.x];

            // Flip cell state
            grid[ant.pos.y][ant.pos.x] = !currentCell;

            // Move ant based on cell state
            ant.move(!currentCell);
        }

        // Draw grid with color variation
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {
                if (grid[y][x]) {
                    double distance = Math.hypot(x - buttons[0].length / 2, y - buttons.length / 2);
                    float hue = (float) ((distance * 0.1 + phase) % 1.0);
                    buttons[y][x].setBackground(Color.getHSBColor(hue, 0.8f, 0.9f));
                }
            }
        }

        // Draw ant
        buttons[ant.pos.y][ant.pos.x].setBackground(Color.WHITE);

        // Occasionally reset if pattern becomes too dense or sparse
        int count = countActiveCells();
        if (count < 10 || count > buttons.length * buttons[0].length * 0.8) {
            initializeGrid(buttons);
        }

        phase += 0.01;
    }

    private int countActiveCells() {
        int count = 0;
        for (boolean[] row : grid) {
            for (boolean cell : row) {
                if (cell)
                    count++;
            }
        }
        return count;
    }

    private void initializeGrid(JButton[][] buttons) {
        grid = new boolean[buttons.length][buttons[0].length];
        ant = new Ant(buttons[0].length / 2, buttons.length / 2);
    }

    @Override
    public String getName() {
        return "Langton's Ant";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.COMPSCI;
    }
}
