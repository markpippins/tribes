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

public class TronVisualization implements IVisualizationHandler {
    private List<Point> trail1 = new ArrayList<>();
    private List<Point> trail2 = new ArrayList<>();
    private Point bike1, bike2;
    private int[] dir1 = {1, 0}; // Initial direction for bike 1 (right)
    private int[] dir2 = {-1, 0}; // Initial direction for bike 2 (left)
    private final Random random = new Random();
    private final int[][] directions = {{0,-1}, {1,0}, {0,1}, {-1,0}}; // up, right, down, left

    @Override
    public void update(JButton[][] buttons) {
        if (bike1 == null) {
            initializeGame(buttons);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // AI decision making
        decideDirection(bike1, dir1, trail1, trail2, buttons);
        decideDirection(bike2, dir2, trail2, trail1, buttons);

        // Move bikes
        trail1.add(new Point(bike1.x, bike1.y));
        trail2.add(new Point(bike2.x, bike2.y));
        
        bike1.x = Math.floorMod(bike1.x + dir1[0], buttons[0].length);
        bike1.y = Math.floorMod(bike1.y + dir1[1], buttons.length);
        bike2.x = Math.floorMod(bike2.x + dir2[0], buttons[0].length);
        bike2.y = Math.floorMod(bike2.y + dir2[1], buttons.length);

        // Check for collisions
        boolean collision = false;
        if (containsPoint(trail1, bike1) || containsPoint(trail2, bike1) ||
            containsPoint(trail1, bike2) || containsPoint(trail2, bike2)) {
            collision = true;
        }

        // Draw trails
        for (Point p : trail1) {
            buttons[p.y][p.x].setBackground(Color.CYAN);
        }
        for (Point p : trail2) {
            buttons[p.y][p.x].setBackground(Color.ORANGE);
        }

        // Draw bikes
        buttons[bike1.y][bike1.x].setBackground(Color.BLUE);
        buttons[bike2.y][bike2.x].setBackground(Color.RED);

        // Reset on collision
        if (collision) {
            initializeGame(buttons);
        }
    }

    private void decideDirection(Point bike, int[] currentDir, List<Point> ownTrail, List<Point> enemyTrail, JButton[][] buttons) {
        // Look ahead in current direction
        Point ahead = new Point(
            Math.floorMod(bike.x + currentDir[0], buttons[0].length),
            Math.floorMod(bike.y + currentDir[1], buttons.length)
        );

        // Change direction if about to hit something
        if (containsPoint(ownTrail, ahead) || containsPoint(enemyTrail, ahead)) {
            // Try to find a safe direction
            for (int[] newDir : directions) {
                Point testPoint = new Point(
                    Math.floorMod(bike.x + newDir[0], buttons[0].length),
                    Math.floorMod(bike.y + newDir[1], buttons.length)
                );
                if (!containsPoint(ownTrail, testPoint) && !containsPoint(enemyTrail, testPoint)) {
                    currentDir[0] = newDir[0];
                    currentDir[1] = newDir[1];
                    break;
                }
            }
        } else if (random.nextInt(20) == 0) { // Occasional random turn
            int[] newDir = directions[random.nextInt(directions.length)];
            currentDir[0] = newDir[0];
            currentDir[1] = newDir[1];
        }
    }

    private boolean containsPoint(List<Point> points, Point p) {
        return points.stream().anyMatch(point -> point.x == p.x && point.y == p.y);
    }

    private void initializeGame(JButton[][] buttons) {
        trail1.clear();
        trail2.clear();
        bike1 = new Point(buttons[0].length / 4, buttons.length / 2);
        bike2 = new Point(3 * buttons[0].length / 4, buttons.length / 2);
        dir1[0] = 1; dir1[1] = 0;
        dir2[0] = -1; dir2[1] = 0;
    }

    @Override
    public String getName() {
        return "Tron Light Cycles";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
