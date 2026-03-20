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

public class SpaceInvadersVisualization implements IVisualizationHandler {
    private int playerX = 18;
    private List<Point> invaders = new ArrayList<>();
    private List<Point> shots = new ArrayList<>();
    private List<Point> enemyShots = new ArrayList<>();
    private int moveDirection = 1;
    private int moveDelay = 0;
    private final Random random = new Random();
    private double phase = 0;

    @Override
    public void update(JButton[][] buttons) {
        if (invaders.isEmpty()) {
            initializeGame(buttons);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Move invaders
        if (moveDelay++ % 5 == 0) {
            boolean needsDropDown = false;
            for (Point invader : invaders) {
                if ((invader.x >= buttons[0].length - 2 && moveDirection > 0) ||
                    (invader.x <= 1 && moveDirection < 0)) {
                    needsDropDown = true;
                    break;
                }
            }

            if (needsDropDown) {
                moveDirection = -moveDirection;
                invaders.forEach(invader -> invader.y++);
            } else {
                invaders.forEach(invader -> invader.x += moveDirection);
            }
        }

        // Update shots
        shots.removeIf(shot -> {
            shot.y--;
            return shot.y < 0 || invaders.removeIf(invader -> 
                invader.x == shot.x && invader.y == shot.y);
        });

        // Enemy shooting
        if (random.nextInt(10) == 0 && !invaders.isEmpty()) {
            Point shooter = invaders.get(random.nextInt(invaders.size()));
            enemyShots.add(new Point(shooter.x, shooter.y + 1));
        }

        // Update enemy shots
        enemyShots.removeIf(shot -> ++shot.y >= buttons.length);

        // Draw player
        buttons[buttons.length - 1][playerX].setBackground(Color.GREEN);
        
        // Draw shots
        shots.forEach(shot -> buttons[shot.y][shot.x].setBackground(Color.GREEN));
        enemyShots.forEach(shot -> buttons[shot.y][shot.x].setBackground(Color.RED));

        // Draw invaders (with animation)
        boolean alternate = Math.sin(phase) > 0;
        invaders.forEach(invader -> {
            if (invader.y < buttons.length) {
                Color color = alternate ? Color.WHITE : Color.CYAN;
                buttons[invader.y][invader.x].setBackground(color);
            }
        });

        // Move player AI
        if (!enemyShots.isEmpty()) {
            Point nearestShot = enemyShots.get(0);
            if (nearestShot.x < playerX) playerX--;
            if (nearestShot.x > playerX) playerX++;
        }

        // AI shooting
        if (random.nextInt(5) == 0) {
            shots.add(new Point(playerX, buttons.length - 2));
        }

        phase += 0.2;
    }

    private void initializeGame(JButton[][] buttons) {
        // Create formation of invaders
        for (int row = 1; row < 4; row++) {
            for (int col = 2; col < buttons[0].length - 2; col += 2) {
                invaders.add(new Point(col, row));
            }
        }
        playerX = buttons[0].length / 2;
    }

    @Override
    public String getName() {
        return "Space Invaders";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
