package com.angrysurfer.grid.visualization.handler.classic;

import java.awt.Color;
import java.awt.Point;
import java.util.*;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class MazeVisualization implements IVisualizationHandler {
    private boolean[][] maze;
    private Stack<Point> stack = new Stack<>();
    private Set<Point> visited = new HashSet<>();
    private Random random = new Random();
    private int currentX, currentY;
    private final int[][] DIRECTIONS = {{0,1}, {1,0}, {0,-1}, {-1,0}}; // right, down, left, up
    private Color pathColor = new Color(100, 200, 255);
    private Color currentColor = Color.WHITE;

    @Override
    public void update(JButton[][] buttons) {
        if (maze == null) {
            initializeMaze(buttons);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Draw existing maze
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {
                if (maze[y][x]) {
                    buttons[y][x].setBackground(pathColor);
                }
            }
        }

        // Generate next step of maze
        if (!stack.isEmpty()) {
            Point current = new Point(currentX, currentY);
            buttons[currentY][currentX].setBackground(currentColor);

            List<Point> unvisitedNeighbors = getUnvisitedNeighbors();
            
            if (!unvisitedNeighbors.isEmpty()) {
                stack.push(current);
                Point next = unvisitedNeighbors.get(random.nextInt(unvisitedNeighbors.size()));
                maze[next.y][next.x] = true;
                maze[(currentY + next.y) / 2][(currentX + next.x) / 2] = true; // Create path
                currentX = next.x;
                currentY = next.y;
                visited.add(next);
            } else if (!stack.isEmpty()) {
                Point backtrack = stack.pop();
                currentX = backtrack.x;
                currentY = backtrack.y;
            }
        } else if (random.nextInt(100) == 0) {
            // Occasionally restart maze generation
            initializeMaze(buttons);
        }
    }

    private List<Point> getUnvisitedNeighbors() {
        List<Point> neighbors = new ArrayList<>();
        for (int[] dir : DIRECTIONS) {
            int newX = currentX + dir[0] * 2;
            int newY = currentY + dir[1] * 2;
            Point p = new Point(newX, newY);
            if (newX >= 0 && newX < maze[0].length && 
                newY >= 0 && newY < maze.length && 
                !visited.contains(p)) {
                neighbors.add(p);
            }
        }
        return neighbors;
    }

    private void initializeMaze(JButton[][] buttons) {
        maze = new boolean[buttons.length][buttons[0].length];
        stack.clear();
        visited.clear();
        currentX = 1;
        currentY = 1;
        maze[currentY][currentX] = true;
        visited.add(new Point(currentX, currentY));
    }

    @Override
    public String getName() {
        return "Maze Generator";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.CLASSIC;
    }
}
