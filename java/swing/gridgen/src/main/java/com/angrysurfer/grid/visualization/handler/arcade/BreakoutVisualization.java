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

public class BreakoutVisualization implements IVisualizationHandler {
    private int paddleX;
    private double ballX, ballY;
    private double ballDX = 0.3, ballDY = -0.2;
    private List<Point> blocks = new ArrayList<>();
    private final Random random = new Random();
    private final Color[] blockColors = {
        Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN
    };

    @Override
    public void update(JButton[][] buttons) {
        // Initialize game if needed
        if (blocks.isEmpty()) {
            initializeGame(buttons);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update ball position
        ballX += ballDX;
        ballY += ballDY;

        // Move AI paddle to follow ball
        paddleX = (int)ballX;
        if (paddleX < 1) paddleX = 1;
        if (paddleX > buttons[0].length - 3) paddleX = buttons[0].length - 3;

        // Ball collision with paddle
        if (ballY > buttons.length - 2 && ballX >= paddleX - 1 && ballX < paddleX + 2) {
            ballDY = -ballDY;
            ballDX += (random.nextDouble() - 0.5) * 0.2;
        }

        // Ball collision with walls
        if (ballX < 0 || ballX >= buttons[0].length) ballDX = -ballDX;
        if (ballY < 0) ballDY = -ballDY;

        // Ball collision with blocks
        blocks.removeIf(block -> {
            if (Math.abs(block.x - ballX) < 1 && Math.abs(block.y - ballY) < 1) {
                ballDY = -ballDY;
                return true;
            }
            return false;
        });

        // Reset ball if it goes out
        if (ballY >= buttons.length) {
            ballX = buttons[0].length / 2;
            ballY = buttons.length - 3;
            ballDX = (random.nextBoolean() ? 0.3 : -0.3);
            ballDY = -0.2;
        }

        // Draw blocks
        for (Point block : blocks) {
            Color color = blockColors[(block.y - 1) % blockColors.length];
            buttons[block.y][block.x].setBackground(color);
        }

        // Draw paddle
        for (int i = -1; i <= 1; i++) {
            if (paddleX + i >= 0 && paddleX + i < buttons[0].length) {
                buttons[buttons.length - 1][paddleX + i].setBackground(Color.WHITE);
            }
        }

        // Draw ball
        if (ballX >= 0 && ballX < buttons[0].length && ballY >= 0 && ballY < buttons.length) {
            buttons[(int)ballY][(int)ballX].setBackground(Color.WHITE);
        }
    }

    private void initializeGame(JButton[][] buttons) {
        paddleX = buttons[0].length / 2;
        ballX = paddleX;
        ballY = buttons.length - 2;
        
        // Create blocks
        for (int row = 1; row < 6; row++) {
            for (int col = 2; col < buttons[0].length - 2; col++) {
                blocks.add(new Point(col, row));
            }
        }
    }

    @Override
    public String getName() {
        return "Breakout";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
