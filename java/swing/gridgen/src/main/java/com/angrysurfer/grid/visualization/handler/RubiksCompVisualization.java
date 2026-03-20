package com.angrysurfer.grid.visualization.handler;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import javax.swing.JButton;

public class RubiksCompVisualization implements IVisualizationHandler {
    private List<RubiksCuber> cubers = new ArrayList<>();
    private long competitionStartTime;
    private boolean hasWinner = false;
    private final Random random = new Random();
    private static final int NUM_COMPETITORS = 4;
    private static final int COMPETITION_TIMEOUT = 10000; // 10 seconds
    private static final int PROGRESS_BAR_WIDTH = 6;
    private static final double MAX_SPEED = 0.03;

    public RubiksCompVisualization() {
        resetCompetition();
    }

    private void resetCompetition() {
        cubers.clear();
        hasWinner = false;
        competitionStartTime = System.currentTimeMillis();
        
        // Create competitors spread evenly across the grid
        for (int i = 0; i < NUM_COMPETITORS; i++) {
            cubers.add(new RubiksCuber(i * 8 + 4, 4, random.nextDouble() * 0.5 + 0.5));
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        if (buttons == null || buttons.length == 0) return;

        clearDisplay(buttons);
        updateAndDrawCompetitors(buttons);

        // Check for win condition or timeout
        if (hasWinner || System.currentTimeMillis() - competitionStartTime > COMPETITION_TIMEOUT) {
            resetCompetition();
        }
    }

    private void clearDisplay(JButton[][] buttons) {
        for (JButton[] row : buttons) {
            for (JButton button : row) {
                button.setBackground(Color.BLACK);
            }
        }
    }

    private void updateAndDrawCompetitors(JButton[][] buttons) {
        for (RubiksCuber cuber : cubers) {
            cuber.update();
            if (cuber.isWinner) {
                hasWinner = true;
            }
            
            drawProgressBar(buttons, cuber);
        }
    }

    private void drawProgressBar(JButton[][] buttons, RubiksCuber cuber) {
        if (cuber.y >= 0 && cuber.y < buttons.length) {
            int progressWidth = (int)(cuber.progress * PROGRESS_BAR_WIDTH);
            for (int i = 0; i < PROGRESS_BAR_WIDTH; i++) {
                int x = cuber.x + i;
                if (x >= 0 && x < buttons[0].length) {
                    Color color = i < progressWidth ? cuber.getCurrentColor() : Color.DARK_GRAY;
                    buttons[cuber.y][x].setBackground(color);
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Rubik's Competition";
    }

    private static class RubiksCuber {
        private final int x, y;
        private double progress;
        private boolean isWinner;
        private final Color[] colors;
        private final double speed;

        RubiksCuber(int x, int y, double speedMultiplier) {
            this.x = x;
            this.y = y;
            this.progress = 0.0;
            this.isWinner = false;
            this.speed = MAX_SPEED * speedMultiplier;
            this.colors = new Color[] {
                Color.RED, Color.ORANGE, Color.YELLOW,
                Color.GREEN, Color.BLUE, Color.WHITE
            };
        }

        void update() {
            if (!isWinner) {
                progress = Math.min(1.0, progress + speed);
                isWinner = progress >= 1.0;
            }
        }

        Color getCurrentColor() {
            int colorIndex = (int)(progress * colors.length);
            return colors[Math.min(colorIndex, colors.length - 1)];
        }
    }
}
