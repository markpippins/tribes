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

public class DigDugVisualization implements IVisualizationHandler {
    private Point digger;
    private List<Point> tunnels = new ArrayList<>();
    private List<Monster> monsters = new ArrayList<>();
    private final Random random = new Random();
    private int direction = 0; // 0=right, 1=down, 2=left, 3=up
    private final int[][] directions = {{1,0}, {0,1}, {-1,0}, {0,-1}};

    private class Monster {
        Point pos;
        boolean inflated;
        int pumpCount;

        Monster(Point pos) {
            this.pos = pos;
            this.inflated = false;
            this.pumpCount = 0;
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        if (digger == null) {
            initializeGame(buttons);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // AI movement - try to reach nearest monster or unexplored area
        if (monsters.size() > 0) {
            Monster nearest = findNearestMonster();
            moveTowardsTarget(nearest.pos, buttons);
        } else {
            moveTowardsUnexplored(buttons);
        }

        // Add tunnel at current position
        tunnels.add(new Point(digger.x, digger.y));

        // Update monsters
        for (Monster monster : new ArrayList<>(monsters)) {
            // Move monsters towards digger
            if (!monster.inflated && random.nextInt(3) == 0) {
                Point nextPos = getNextMonsterPosition(monster.pos);
                if (canMoveTo(nextPos, buttons)) {
                    monster.pos = nextPos;
                }
            }

            // Random chance to deflate
            if (monster.inflated && random.nextInt(5) == 0) {
                monster.inflated = false;
                monster.pumpCount = Math.max(0, monster.pumpCount - 1);
            }

            // Remove monster if fully pumped
            if (monster.pumpCount >= 3) {
                monsters.remove(monster);
            }
        }

        // Draw tunnels
        for (Point tunnel : tunnels) {
            buttons[tunnel.y][tunnel.x].setBackground(Color.DARK_GRAY);
        }

        // Draw monsters
        for (Monster monster : monsters) {
            Color monsterColor = monster.inflated ? Color.RED : Color.ORANGE;
            buttons[monster.pos.y][monster.pos.x].setBackground(monsterColor);
        }

        // Draw digger
        buttons[digger.y][digger.x].setBackground(Color.GREEN);

        // Spawn new monsters occasionally
        if (monsters.isEmpty() || (random.nextInt(50) == 0 && monsters.size() < 3)) {
            spawnMonster(buttons);
        }
    }

    private Monster findNearestMonster() {
        Monster nearest = monsters.get(0);
        double minDist = Double.MAX_VALUE;
        for (Monster m : monsters) {
            double dist = Math.hypot(m.pos.x - digger.x, m.pos.y - digger.y);
            if (dist < minDist) {
                minDist = dist;
                nearest = m;
            }
        }
        return nearest;
    }

    private void moveTowardsTarget(Point target, JButton[][] buttons) {
        int dx = Integer.compare(target.x, digger.x);
        int dy = Integer.compare(target.y, digger.y);
        
        // Try horizontal movement first
        if (dx != 0 && canMoveTo(new Point(digger.x + dx, digger.y), buttons)) {
            digger.x += dx;
        }
        // Then try vertical movement
        else if (dy != 0 && canMoveTo(new Point(digger.x, digger.y + dy), buttons)) {
            digger.y += dy;
        }
        // If stuck, try random direction
        else {
            int[] dir = directions[random.nextInt(directions.length)];
            Point newPos = new Point(digger.x + dir[0], digger.y + dir[1]);
            if (canMoveTo(newPos, buttons)) {
                digger.x = newPos.x;
                digger.y = newPos.y;
            }
        }
    }

    private void moveTowardsUnexplored(JButton[][] buttons) {
        // Find direction with fewest tunnels
        int bestDir = 0;
        int minTunnels = Integer.MAX_VALUE;
        for (int i = 0; i < directions.length; i++) {
            int count = countTunnelsInDirection(directions[i]);
            if (count < minTunnels) {
                minTunnels = count;
                bestDir = i;
            }
        }
        
        Point newPos = new Point(
            digger.x + directions[bestDir][0],
            digger.y + directions[bestDir][1]
        );
        if (canMoveTo(newPos, buttons)) {
            digger.x = newPos.x;
            digger.y = newPos.y;
        }
    }

    private int countTunnelsInDirection(int[] dir) {
        int count = 0;
        for (Point tunnel : tunnels) {
            if (Math.signum(tunnel.x - digger.x) == dir[0] &&
                Math.signum(tunnel.y - digger.y) == dir[1]) {
                count++;
            }
        }
        return count;
    }

    private Point getNextMonsterPosition(Point pos) {
        int dx = Integer.compare(digger.x, pos.x);
        int dy = Integer.compare(digger.y, pos.y);
        return new Point(pos.x + dx, pos.y + dy);
    }

    private boolean canMoveTo(Point pos, JButton[][] buttons) {
        return pos.x >= 0 && pos.x < buttons[0].length &&
               pos.y >= 0 && pos.y < buttons.length;
    }

    private void spawnMonster(JButton[][] buttons) {
        int x = random.nextInt(buttons[0].length);
        int y = random.nextInt(buttons.length);
        monsters.add(new Monster(new Point(x, y)));
    }

    private void initializeGame(JButton[][] buttons) {
        digger = new Point(0, buttons.length - 1);
        tunnels.clear();
        monsters.clear();
        spawnMonster(buttons);
    }

    @Override
    public String getName() {
        return "DigDug";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }
}
