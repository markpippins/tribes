package com.angrysurfer.grid.visualization.handler.game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class BilliardsVisualizationHandler implements IVisualizationHandler {
    private final List<Ball> balls = new ArrayList<>();
    private final Random random = new Random();
    private final int BALL_COUNT = 8;
    private int gridWidth;
    private int gridHeight;

    private static class Ball {
        double x, y;      // position
        double dx, dy;    // velocity
        Color color;
        
        Ball(double x, double y, double dx, double dy, Color color) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.color = color;
        }

        void update() {
            x += dx;
            y += dy;
        }

        void bounce(int width, int height) {
            if (x < 0 || x >= width) dx = -dx;
            if (y < 0 || y >= height) dy = -dy;
            x = Math.max(0, Math.min(x, width - 1));
            y = Math.max(0, Math.min(y, height - 1));
        }
    }

    public BilliardsVisualizationHandler() {
        // Colors similar to real billiard balls
        Color[] ballColors = {
            Color.WHITE,      // Cue ball
            Color.YELLOW,     // 1
            Color.BLUE,       // 2
            Color.RED,        // 3
            Color.CYAN,     // 4
            Color.ORANGE,     // 5
            Color.GREEN,      // 6
            Color.MAGENTA    // 7
        };

        // Initialize balls with random positions and velocities
        for (int i = 0; i < BALL_COUNT; i++) {
            double dx = random.nextDouble() * 0.4 - 0.2;
            double dy = random.nextDouble() * 0.4 - 0.2;
            balls.add(new Ball(
                random.nextInt(16),  // x
                random.nextInt(16),  // y
                dx,                  // dx
                dy,                  // dy
                ballColors[i]        // color
            ));
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        gridWidth = buttons.length;
        gridHeight = buttons[0].length;

        // Clear grid
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                buttons[x][y].setBackground(Color.BLACK);
                buttons[x][y].setBackground(Color.RED);
            }
        }

        // Update and draw balls
        for (Ball ball : balls) {
            // Update position
            ball.update();
            ball.bounce(gridWidth, gridHeight);

            // Draw ball
            int x = (int) ball.x;
            int y = (int) ball.y;
            if (x >= 0 && x < gridWidth && y >= 0 && y < gridHeight) {
                buttons[x][y].setBackground(ball.color);
                buttons[x][y].repaint();
            }
        }

        // Simple ball collision detection and response
        for (int i = 0; i < balls.size(); i++) {
            for (int j = i + 1; j < balls.size(); j++) {
                Ball b1 = balls.get(i);
                Ball b2 = balls.get(j);
                
                // Check if balls are in the same grid cell
                if ((int)b1.x == (int)b2.x && (int)b1.y == (int)b2.y) {
                    // Swap velocities for simple collision response
                    double tdx = b1.dx;
                    double tdy = b1.dy;
                    b1.dx = b2.dx;
                    b1.dy = b2.dy;
                    b2.dx = tdx;
                    b2.dy = tdy;
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Billiards";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.GAME;
    }
}
