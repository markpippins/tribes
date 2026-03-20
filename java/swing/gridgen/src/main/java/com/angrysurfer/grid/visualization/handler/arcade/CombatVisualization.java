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

public class CombatVisualization implements IVisualizationHandler {
    private Tank tank1, tank2;
    private List<Shell> shells = new ArrayList<>();
    private List<Point> walls = new ArrayList<>();
    private Random random = new Random();
    private List<Explosion> explosions = new ArrayList<>();

    private class Tank {
        Point pos;
        double angle;
        Color color;
        int cooldown = 0;
        
        Tank(Point pos, double angle, Color color) {
            this.pos = pos;
            this.angle = angle;
            this.color = color;
        }
        
        void moveForward(List<Point> obstacles) {
            Point newPos = new Point(
                pos.x + (int)Math.round(Math.cos(angle)),
                pos.y + (int)Math.round(Math.sin(angle))
            );
            if (!obstacles.contains(newPos)) pos = newPos;
        }
        
        void rotate(double delta) {
            angle = (angle + delta + Math.PI * 2) % (Math.PI * 2);
        }
        
        void shoot() {
            if (cooldown <= 0) {
                shells.add(new Shell(
                    new Point(pos.x, pos.y),
                    angle,
                    color
                ));
                cooldown = 10;
            }
        }
    }

    private class Shell {
        Point pos;
        double angle;
        Color color;
        
        Shell(Point pos, double angle, Color color) {
            this.pos = pos;
            this.angle = angle;
            this.color = color;
        }
        
        void move() {
            pos.x += Math.round(Math.cos(angle) * 2);
            pos.y += Math.round(Math.sin(angle) * 2);
        }
    }

    private class Explosion {
        Point pos;
        int radius;
        int maxRadius;
        int life;
        
        Explosion(Point pos) {
            this.pos = pos;
            this.radius = 0;
            this.maxRadius = 3;
            this.life = 10;
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        if (tank1 == null) {
            initializeGame(buttons);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update tanks
        updateTank(tank1, tank2, buttons);
        updateTank(tank2, tank1, buttons);

        // Update shells
        for (Shell shell : new ArrayList<>(shells)) {
            shell.move();
            
            // Check for collisions
            if (walls.contains(shell.pos) ||
                shell.pos.equals(tank1.pos) ||
                shell.pos.equals(tank2.pos)) {
                shells.remove(shell);
                explosions.add(new Explosion(new Point(shell.pos.x, shell.pos.y)));
                continue;
            }
            
            // Remove if off screen
            if (shell.pos.x < 0 || shell.pos.x >= buttons[0].length ||
                shell.pos.y < 0 || shell.pos.y >= buttons.length) {
                shells.remove(shell);
            }
        }

        // Update explosions
        for (Explosion exp : new ArrayList<>(explosions)) {
            if (exp.life > 0) {
                exp.radius = Math.min(exp.maxRadius, exp.radius + 1);
                exp.life--;
            } else {
                explosions.remove(exp);
            }
        }

        // Draw everything
        drawGame(buttons);
    }

    private void updateTank(Tank tank, Tank enemy, JButton[][] buttons) {
        tank.cooldown--;
        
        // AI control
        double angleToEnemy = Math.atan2(
            enemy.pos.y - tank.pos.y,
            enemy.pos.x - tank.pos.x
        );
        
        // Rotate towards enemy
        double angleDiff = normalizeAngle(angleToEnemy - tank.angle);
        if (Math.abs(angleDiff) > 0.1) {
            tank.rotate(Math.signum(angleDiff) * 0.1);
        }
        
        // Move and shoot based on distance
        double dist = Point.distance(tank.pos.x, tank.pos.y, enemy.pos.x, enemy.pos.y);
        if (dist > 10) {
            tank.moveForward(walls);
        }
        if (Math.abs(angleDiff) < 0.5 && random.nextInt(10) == 0) {
            tank.shoot();
        }
    }

    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= Math.PI * 2;
        while (angle < -Math.PI) angle += Math.PI * 2;
        return angle;
    }

    private void drawGame(JButton[][] buttons) {
        // Draw walls
        for (Point wall : walls) {
            buttons[wall.y][wall.x].setBackground(Color.GRAY);
        }
        
        // Draw tanks
        drawTank(buttons, tank1);
        drawTank(buttons, tank2);
        
        // Draw shells
        for (Shell shell : shells) {
            if (shell.pos.x >= 0 && shell.pos.x < buttons[0].length &&
                shell.pos.y >= 0 && shell.pos.y < buttons.length) {
                buttons[shell.pos.y][shell.pos.x].setBackground(shell.color);
            }
        }
        
        // Draw explosions
        for (Explosion exp : explosions) {
            drawExplosion(buttons, exp);
        }
    }

    private void drawTank(JButton[][] buttons, Tank tank) {
        buttons[tank.pos.y][tank.pos.x].setBackground(tank.color);
        int barrelX = tank.pos.x + (int)Math.round(Math.cos(tank.angle));
        int barrelY = tank.pos.y + (int)Math.round(Math.sin(tank.angle));
        if (barrelX >= 0 && barrelX < buttons[0].length &&
            barrelY >= 0 && barrelY < buttons.length) {
            buttons[barrelY][barrelX].setBackground(tank.color.darker());
        }
    }

    private void drawExplosion(JButton[][] buttons, Explosion exp) {
        for (int y = -exp.radius; y <= exp.radius; y++) {
            for (int x = -exp.radius; x <= exp.radius; x++) {
                if (x*x + y*y <= exp.radius*exp.radius) {
                    int drawX = exp.pos.x + x;
                    int drawY = exp.pos.y + y;
                    if (drawX >= 0 && drawX < buttons[0].length &&
                        drawY >= 0 && drawY < buttons.length) {
                        buttons[drawY][drawX].setBackground(Color.ORANGE);
                    }
                }
            }
        }
    }

    private void initializeGame(JButton[][] buttons) {
        // Create tanks
        tank1 = new Tank(new Point(2, 2), 0, Color.BLUE);
        tank2 = new Tank(new Point(buttons[0].length-3, buttons.length-3), Math.PI, Color.RED);
        
        // Create walls
        walls.clear();
        for (int i = 0; i < 20; i++) {
            Point wall = new Point(
                random.nextInt(buttons[0].length),
                random.nextInt(buttons.length)
            );
            if (!wall.equals(tank1.pos) && !wall.equals(tank2.pos)) {
                walls.add(wall);
            }
        }
    }

    @Override
    public String getName() {
        return "Combat";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
