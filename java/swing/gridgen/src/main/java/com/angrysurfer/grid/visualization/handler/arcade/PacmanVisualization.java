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

public class PacmanVisualization implements IVisualizationHandler {
    private Point pacman = new Point(1, 4);
    private List<Point> dots = new ArrayList<>();
    private List<Point> ghosts = new ArrayList<>();
    private final int[][] directions = {{0,1}, {1,0}, {0,-1}, {-1,0}}; // right, down, left, up
    private int currentDir = 0;
    private double phase = 0;
    private final Random random = new Random();
    private static final Color WALL_COLOR = new Color(0, 0, 255);  // Brighter blue
    private static final Color DOT_COLOR = new Color(255, 255, 255);
    private static final Color PACMAN_COLOR = new Color(255, 255, 0);
    private static final Color[] GHOST_COLORS = {
        new Color(255, 0, 0),     // Red (Blinky)
        new Color(255, 182, 255),  // Pink (Pinky)
        new Color(0, 255, 255),    // Cyan (Inky)
        new Color(255, 182, 85)    // Orange (Clyde)
    };
    private boolean[][] maze;  // Add maze layout storage
    private boolean gameInitialized = false;  // Add this field

    @Override
    public void update(JButton[][] buttons) {
        if (!gameInitialized) {
            initializeGame(buttons);
            gameInitialized = true;
        }

        // Paint everything black first
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                buttons[row][col].setBackground(Color.BLACK);
            }
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Draw maze walls
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                if (maze[row][col]) {
                    buttons[row][col].setBackground(WALL_COLOR);
                }
            }
        }

        // Draw dots (smaller than original)
        for (Point dot : dots) {
            buttons[dot.y][dot.x].setBackground(DOT_COLOR);
        }

        // Update Pacman position with smarter movement
        if (random.nextDouble() < 0.1) {  // 10% chance to change direction randomly
            List<Integer> possibleDirs = new ArrayList<>();
            
            // Check all possible directions
            for (int dir = 0; dir < directions.length; dir++) {
                int newX = Math.floorMod(pacman.x + directions[dir][0], buttons[0].length);
                int newY = Math.floorMod(pacman.y + directions[dir][1], buttons.length);
                if (!maze[newY][newX]) {
                    possibleDirs.add(dir);
                }
            }
            
            // Change direction if there are valid options
            if (!possibleDirs.isEmpty()) {
                currentDir = possibleDirs.get(random.nextInt(possibleDirs.size()));
            }
        }

        // Try to move in current direction
        Point nextPos = new Point(
            Math.floorMod(pacman.x + directions[currentDir][0], buttons[0].length),
            Math.floorMod(pacman.y + directions[currentDir][1], buttons.length)
        );

        // If blocked, try to find a new valid direction
        if (maze[nextPos.y][nextPos.x]) {
            List<Integer> validDirs = new ArrayList<>();
            for (int dir = 0; dir < directions.length; dir++) {
                int newX = Math.floorMod(pacman.x + directions[dir][0], buttons[0].length);
                int newY = Math.floorMod(pacman.y + directions[dir][1], buttons.length);
                if (!maze[newY][newX]) {
                    validDirs.add(dir);
                }
            }
            if (!validDirs.isEmpty()) {
                currentDir = validDirs.get(random.nextInt(validDirs.size()));
                nextPos = new Point(
                    Math.floorMod(pacman.x + directions[currentDir][0], buttons[0].length),
                    Math.floorMod(pacman.y + directions[currentDir][1], buttons.length)
                );
            }
        }

        // Move Pacman if the way is clear
        if (!maze[nextPos.y][nextPos.x]) {
            pacman = nextPos;
        }

        // Update ghost positions - make them move more
        for (int i = 0; i < ghosts.size(); i++) {
            Point ghost = ghosts.get(i);
            int dx = Integer.compare(pacman.x, ghost.x);
            int dy = Integer.compare(pacman.y, ghost.y);
            
            // More random movement with bias toward Pacman
            if (random.nextDouble() < 0.7) {  // 70% chance to chase Pacman
                if (random.nextBoolean()) {
                    if (!maze[ghost.y + dy][ghost.x]) ghost.y += dy;
                } else {
                    if (!maze[ghost.y][ghost.x + dx]) ghost.x += dx;
                }
            } else {  // 30% chance for random movement
                int randomDir = random.nextInt(4);
                int newX = ghost.x + directions[randomDir][0];
                int newY = ghost.y + directions[randomDir][1];
                if (newX >= 0 && newX < buttons[0].length && 
                    newY >= 0 && newY < buttons.length && 
                    !maze[newY][newX]) {
                    ghost.x = newX;
                    ghost.y = newY;
                }
            }
            
            // Draw ghost with its unique color
            buttons[ghost.y][ghost.x].setBackground(GHOST_COLORS[i]);
        }

        // Collect dots
        dots.removeIf(dot -> dot.x == pacman.x && dot.y == pacman.y);

        // Draw animated Pacman (make animation more visible)
        boolean mouthOpen = Math.sin(phase) > 0;
        buttons[pacman.y][pacman.x].setBackground(mouthOpen ? PACMAN_COLOR : Color.BLACK);
        phase += 0.5; // Faster animation

        // Reset if all dots collected
        if (dots.isEmpty()) {
            initializeGame(buttons);
        }

        // Reset game when Pacman gets caught or all dots collected
        if (isGameOver()) {
            gameInitialized = false;  // This will trigger a new maze generation on next update
        }
    }

    private boolean isGameOver() {
        // Check if Pacman collides with any ghost
        for (Point ghost : ghosts) {
            if (ghost.x == pacman.x && ghost.y == pacman.y) {
                return true;
            }
        }
        // Or if all dots are collected (if you're using dots)
        return dots.isEmpty();
    }

    private void initializeGame(JButton[][] buttons) {
        int rows = buttons.length;
        int cols = buttons[0].length;
        maze = new boolean[rows][cols];
        dots.clear();

        // Start with all walls
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                maze[row][col] = true;
            }
        }

        // Create standard Pac-Man maze layout
        // Main horizontal corridors
        for (int col = 1; col < cols-1; col++) {
            maze[2][col] = false;            // Top corridor
            maze[rows/2][col] = false;       // Middle corridor
            maze[rows-3][col] = false;       // Bottom corridor
        }

        // Main vertical corridors - 4 evenly spaced
        int[] verticalPaths = {2, cols/3, (cols*2)/3, cols-3};
        for (int col : verticalPaths) {
            for (int row = 2; row < rows-2; row++) {
                maze[row][col] = false;
            }
        }

        // Add some horizontal connectors
        for (int section = 0; section < 3; section++) {
            int midRow = 2 + ((rows-4) * (section+1))/3;
            for (int col = 1; col < cols-1; col++) {
                maze[midRow][col] = false;
            }
        }

        // Clear corners for ghost areas
        for (int[] corner : new int[][]{{2,2}, {2,rows-3}, {cols-3,2}, {cols-3,rows-3}}) {
            int cx = corner[0], cy = corner[1];
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (cx+dx >= 0 && cx+dx < cols && cy+dy >= 0 && cy+dy < rows) {
                        maze[cy+dy][cx+dx] = false;
                    }
                }
            }
        }

        // Reset positions
        pacman.x = 2;
        pacman.y = rows/2;
        currentDir = 0;

        // Position ghosts in corners
        ghosts.clear();
        ghosts.add(new Point(cols-3, 2));        // Top right
        ghosts.add(new Point(cols-3, rows-3));   // Bottom right
        ghosts.add(new Point(2, 2));             // Top left
        ghosts.add(new Point(2, rows-3));        // Bottom left
    }

    @Override
    public String getName() {
        return "Pac-Man";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
