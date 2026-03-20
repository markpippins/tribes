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

public class PlatformClimberVisualization implements IVisualizationHandler {
    private Point climber;
    private List<Point> platforms = new ArrayList<>();
    private List<Point> ladders = new ArrayList<>();
    private final Random random = new Random();
    private double verticalVelocity = 0;
    private boolean isJumping = false;
    private int direction = 1;
    private boolean onPlatform = false;

    @Override
    public void update(JButton[][] buttons) {
        if (climber == null) {
            initializeGame(buttons);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Apply gravity if not on ladder
        if (!isOnLadder()) {
            verticalVelocity += 0.2; // Gravity
            climber.y += verticalVelocity;

            // Check platform collisions
            onPlatform = false;
            for (Point platform : platforms) {
                if (climber.y >= platform.y - 1 && climber.y <= platform.y &&
                    Math.abs(climber.x - platform.x) < 2) {
                    climber.y = platform.y - 1;
                    verticalVelocity = 0;
                    onPlatform = true;
                    break;
                }
            }
        }

        // AI movement
        if (random.nextInt(10) == 0) {
            Point target = findNextTarget();
            moveTowardsTarget(target, buttons);
        }

        // Keep climber in bounds
        climber.y = Math.max(0, Math.min(buttons.length - 1, climber.y));
        climber.x = Math.max(0, Math.min(buttons[0].length - 1, climber.x));

        // Draw platforms
        for (Point platform : platforms) {
            for (int i = -1; i <= 1; i++) {
                if (platform.x + i >= 0 && platform.x + i < buttons[0].length) {
                    buttons[platform.y][platform.x + i].setBackground(Color.GRAY);
                }
            }
        }

        // Draw ladders
        for (Point ladder : ladders) {
            for (int i = 0; i < 3; i++) {
                if (ladder.y - i >= 0) {
                    buttons[ladder.y - i][ladder.x].setBackground(Color.ORANGE);
                }
            }
        }

        // Draw climber
        buttons[climber.y][climber.x].setBackground(Color.GREEN);

        // Reset if fallen off bottom
        if (climber.y >= buttons.length - 1) {
            initializeGame(buttons);
        }
    }

    private boolean isOnLadder() {
        for (Point ladder : ladders) {
            if (climber.x == ladder.x && climber.y <= ladder.y && climber.y > ladder.y - 3) {
                return true;
            }
        }
        return false;
    }

    private Point findNextTarget() {
        // Find nearest platform above current position
        Point best = null;
        double bestDist = Double.MAX_VALUE;
        
        for (Point platform : platforms) {
            if (platform.y < climber.y) {
                double dist = Math.hypot(platform.x - climber.x, platform.y - climber.y);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = platform;
                }
            }
        }
        
        // If no platforms above, find nearest ladder
        if (best == null) {
            for (Point ladder : ladders) {
                double dist = Math.hypot(ladder.x - climber.x, ladder.y - climber.y);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = ladder;
                }
            }
        }

        return best != null ? best : new Point(climber.x, 0);
    }

    private void moveTowardsTarget(Point target, JButton[][] buttons) {
        if (isOnLadder()) {
            // Move up ladder
            if (climber.y > target.y) {
                climber.y--;
            }
        } else if (onPlatform) {
            // Move horizontally on platform
            if (Math.abs(target.x - climber.x) > 1) {
                climber.x += Integer.compare(target.x, climber.x);
            } else if (verticalVelocity == 0) {
                // Jump if near edge or target is above
                verticalVelocity = -2.0;
            }
        }
    }

    private void initializeGame(JButton[][] buttons) {
        climber = new Point(2, buttons.length - 2);
        platforms.clear();
        ladders.clear();
        verticalVelocity = 0;
        
        // Generate platforms
        for (int y = buttons.length - 3; y > 2; y -= 2) {
            int x = random.nextInt(buttons[0].length - 4) + 2;
            platforms.add(new Point(x, y));
            
            // Add ladder near platform
            int ladderX = x + (random.nextBoolean() ? 2 : -2);
            ladderX = Math.max(1, Math.min(buttons[0].length - 2, ladderX));
            ladders.add(new Point(ladderX, y));
        }
    }

    @Override
    public String getName() {
        return "Platform Climber";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
