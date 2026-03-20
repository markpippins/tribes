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

public class MissileCommandVisualization implements IVisualizationHandler {
    private List<Missile> playerMissiles = new ArrayList<>();
    private List<Missile> enemyMissiles = new ArrayList<>();
    private List<Explosion> explosions = new ArrayList<>();
    private List<City> cities = new ArrayList<>();
    private final Random random = new Random();

    private class Missile {
        Point start, end;
        double progress;
        
        Missile(Point start, Point end) {
            this.start = start;
            this.end = end;
            this.progress = 0;
        }
    }

    private class Explosion {
        Point pos;
        int radius;
        int maxRadius;
        
        Explosion(Point pos) {
            this.pos = pos;
            this.radius = 0;
            this.maxRadius = 2;
        }
    }

    private class City {
        int x;
        boolean alive;
        
        City(int x) {
            this.x = x;
            this.alive = true;
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        if (cities.isEmpty()) {
            initializeGame(buttons);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update player missiles
        updateMissiles(playerMissiles, Color.GREEN, buttons);

        // Update enemy missiles
        updateMissiles(enemyMissiles, Color.RED, buttons);

        // Update explosions
        for (Explosion exp : new ArrayList<>(explosions)) {
            if (exp.radius < exp.maxRadius) {
                exp.radius++;
                drawExplosion(exp, buttons);
            } else {
                explosions.remove(exp);
            }
        }

        // Create new enemy missiles
        if (random.nextInt(10) == 0 && cities.stream().anyMatch(c -> c.alive)) {
            City target = cities.stream()
                              .filter(c -> c.alive)
                              .skip(random.nextInt((int)cities.stream().filter(c -> c.alive).count()))
                              .findFirst()
                              .get();
            
            enemyMissiles.add(new Missile(
                new Point(random.nextInt(buttons[0].length), 0),
                new Point(target.x, buttons.length - 1)
            ));
        }

        // AI defense
        if (random.nextInt(5) == 0 && !enemyMissiles.isEmpty()) {
            Missile target = enemyMissiles.get(random.nextInt(enemyMissiles.size()));
            Point intercept = new Point(
                (int)(target.start.x + (target.end.x - target.start.x) * target.progress),
                (int)(target.start.y + (target.end.y - target.start.y) * target.progress)
            );
            playerMissiles.add(new Missile(
                new Point(buttons[0].length / 2, buttons.length - 1),
                intercept
            ));
        }

        // Draw cities
        for (City city : cities) {
            if (city.alive) {
                buttons[buttons.length - 1][city.x].setBackground(Color.CYAN);
            }
        }
    }

    private void updateMissiles(List<Missile> missiles, Color color, JButton[][] buttons) {
        for (Missile missile : new ArrayList<>(missiles)) {
            missile.progress += 0.05;
            if (missile.progress >= 1.0) {
                missiles.remove(missile);
                explosions.add(new Explosion(missile.end));
            } else {
                int x = (int)(missile.start.x + (missile.end.x - missile.start.x) * missile.progress);
                int y = (int)(missile.start.y + (missile.end.y - missile.start.y) * missile.progress);
                if (x >= 0 && x < buttons[0].length && y >= 0 && y < buttons.length) {
                    buttons[y][x].setBackground(color);
                }
            }
        }
    }

    private void drawExplosion(Explosion exp, JButton[][] buttons) {
        for (int y = -exp.radius; y <= exp.radius; y++) {
            for (int x = -exp.radius; x <= exp.radius; x++) {
                if (x*x + y*y <= exp.radius*exp.radius) {
                    int drawX = exp.pos.x + x;
                    int drawY = exp.pos.y + y;
                    if (drawX >= 0 && drawX < buttons[0].length &&
                        drawY >= 0 && drawY < buttons.length) {
                        buttons[drawY][drawX].setBackground(Color.YELLOW);
                    }
                }
            }
        }
    }

    private void initializeGame(JButton[][] buttons) {
        cities.clear();
        for (int i = 0; i < 6; i++) {
            cities.add(new City(4 + i * 6));
        }
        playerMissiles.clear();
        enemyMissiles.clear();
        explosions.clear();
    }

    @Override
    public String getName() {
        return "Missile Command";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
