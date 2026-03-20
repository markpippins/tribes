package com.angrysurfer.grid.visualization.handler.arcade;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import javax.swing.JButton;

public class RacingVisualization implements IVisualizationHandler {
    private List<Point> track = new ArrayList<>();
    private Point car;
    private double carAngle = 0;
    private double carSpeed = 0;
    private final Random random = new Random();
    private List<Point> obstacles = new ArrayList<>();
    private double trackPhase = 0;

    @Override
    public void update(JButton[][] buttons) {
        if (car == null) {
            initializeGame(buttons);
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Generate track points
        updateTrack(buttons);

        // AI car control
        Point nextTrackPoint = findNextTrackPoint();
        if (nextTrackPoint != null) {
            double targetAngle = Math.atan2(nextTrackPoint.y - car.y, nextTrackPoint.x - car.x);
            double angleDiff = normalizeAngle(targetAngle - carAngle);

            // Steer towards track
            carAngle += Math.signum(angleDiff) * 0.1;

            // Adjust speed based on turn sharpness
            carSpeed = Math.max(0.5, 1.5 - Math.abs(angleDiff));
        }

        // Update car position
        car.x = (int) (car.x + Math.cos(carAngle) * carSpeed);
        car.y = (int) (car.y + Math.sin(carAngle) * carSpeed);

        // Wrap around screen
        car.x = Math.floorMod(car.x, buttons[0].length);
        car.y = Math.floorMod(car.y, buttons.length);

        // Draw track
        for (Point p : track) {
            buttons[p.y][p.x].setBackground(Color.GRAY);
        }

        // Draw obstacles
        for (Point obstacle : obstacles) {
            buttons[obstacle.y][obstacle.x].setBackground(Color.RED);
        }

        // Draw car with direction indicator
        buttons[car.y][car.x].setBackground(Color.YELLOW);
        int frontX = (int) (car.x + Math.cos(carAngle));
        int frontY = (int) (car.y + Math.sin(carAngle));
        if (frontX >= 0 && frontX < buttons[0].length &&
                frontY >= 0 && frontY < buttons.length) {
            buttons[frontY][frontX].setBackground(Color.WHITE);
        }

        trackPhase += 0.1;
    }

    private void updateTrack(JButton[][] buttons) {
        track.clear();

        // Generate sine wave track
        int centerY = buttons.length / 2;
        double amplitude = buttons.length / 3;

        for (int x = 0; x < buttons[0].length; x++) {
            int y = (int) (centerY + Math.sin(x * 0.3 + trackPhase) * amplitude);
            y = Math.max(1, Math.min(buttons.length - 2, y));
            track.add(new Point(x, y));

            // Add track width
            if (y > 0)
                track.add(new Point(x, y - 1));
            if (y < buttons.length - 1)
                track.add(new Point(x, y + 1));
        }

        // Update obstacles
        if (random.nextInt(20) == 0) {
            Point trackPoint = track.get(random.nextInt(track.size()));
            obstacles.add(new Point(buttons[0].length - 1, trackPoint.y));
        }

        // Move obstacles
        for (Point obstacle : new ArrayList<>(obstacles)) {
            obstacle.x--;
            if (obstacle.x < 0) {
                obstacles.remove(obstacle);
            }
        }
    }

    private Point findNextTrackPoint() {
        double minDist = Double.MAX_VALUE;
        Point nearest = null;

        for (Point p : track) {
            double dist = Math.hypot(p.x - car.x, p.y - car.y);
            if (dist < minDist && p.x > car.x) {
                minDist = dist;
                nearest = p;
            }
        }

        return nearest;
    }

    private double normalizeAngle(double angle) {
        while (angle > Math.PI)
            angle -= 2 * Math.PI;
        while (angle < -Math.PI)
            angle += 2 * Math.PI;
        return angle;
    }

    private void initializeGame(JButton[][] buttons) {
        car = new Point(2, buttons.length / 2);
        carAngle = 0;
        carSpeed = 1;
        obstacles.clear();
        trackPhase = 0;
    }

    @Override
    public String getName() {
        return "Racing";

    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.ARCADE;
    }

}
