package com.angrysurfer.grid.visualization.handler.arcade;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class PongVisualization implements IVisualizationHandler {
    private int paddle1Y = 3;
    private int paddle2Y = 3;
    private final int PADDLE_HEIGHT = 3;
    private double ballX = 18.0;
    private double ballY = 4.0;
    private double ballDX = 0.3;
    private double ballDY = 0.2;
    private final Random random = new Random();

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update ball position
        ballX += ballDX;
        ballY += ballDY;

        // AI paddle movement
        if (ballX > buttons[0].length / 2) {
            if (ballY > paddle2Y + PADDLE_HEIGHT/2) paddle2Y++;
            if (ballY < paddle2Y + PADDLE_HEIGHT/2) paddle2Y--;
        }
        
        // Simple AI for paddle 1
        if (ballX < buttons[0].length / 2) {
            if (ballY > paddle1Y + PADDLE_HEIGHT/2) paddle1Y++;
            if (ballY < paddle1Y + PADDLE_HEIGHT/2) paddle1Y--;
        }

        // Ball collision with paddles
        if (ballX < 1 && ballY >= paddle1Y && ballY < paddle1Y + PADDLE_HEIGHT) {
            ballDX = -ballDX;
            ballDY += (random.nextDouble() - 0.5) * 0.2;
        }
        if (ballX > buttons[0].length - 2 && ballY >= paddle2Y && ballY < paddle2Y + PADDLE_HEIGHT) {
            ballDX = -ballDX;
            ballDY += (random.nextDouble() - 0.5) * 0.2;
        }

        // Ball collision with walls
        if (ballY < 0 || ballY >= buttons.length) {
            ballDY = -ballDY;
        }

        // Reset ball if it goes out
        if (ballX < 0 || ballX >= buttons[0].length) {
            ballX = buttons[0].length / 2;
            ballY = buttons.length / 2;
            ballDX = (random.nextBoolean() ? 0.3 : -0.3);
            ballDY = (random.nextDouble() - 0.5) * 0.4;
        }

        // Keep paddles in bounds
        paddle1Y = Math.max(0, Math.min(buttons.length - PADDLE_HEIGHT, paddle1Y));
        paddle2Y = Math.max(0, Math.min(buttons.length - PADDLE_HEIGHT, paddle2Y));

        // Draw paddles
        for (int i = 0; i < PADDLE_HEIGHT; i++) {
            if (paddle1Y + i < buttons.length) buttons[paddle1Y + i][0].setBackground(Color.WHITE);
            if (paddle2Y + i < buttons.length) buttons[paddle2Y + i][buttons[0].length - 1].setBackground(Color.WHITE);
        }

        // Draw ball
        if (ballX >= 0 && ballX < buttons[0].length && ballY >= 0 && ballY < buttons.length) {
            buttons[(int)ballY][(int)ballX].setBackground(Color.WHITE);
        }
    }

    @Override
    public String getName() {
        return "Pong Classic";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
