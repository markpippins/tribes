package com.angrysurfer.grid.visualization.handler.science;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class BrownianVisualization implements IVisualizationHandler {
    private List<Particle> particles = new ArrayList<>();
    private Map<Point, TrailPoint> trails = new HashMap<>();
    private final Random random = new Random();
    private double phase = 0;

    private class Particle {
        double x, y;
        double dx, dy;
        float hue;
        
        Particle(int x, int y) {
            this.x = x;
            this.y = y;
            this.hue = random.nextFloat();
            randomizeDirection();
        }
        
        void randomizeDirection() {
            double angle = random.nextDouble() * Math.PI * 2;
            dx = Math.cos(angle) * 0.5;
            dy = Math.sin(angle) * 0.5;
        }
        
        void update(int width, int height) {
            x = Math.floorMod((int)(x + dx), width);
            y = Math.floorMod((int)(y + dy), height);
            if (random.nextInt(10) == 0) randomizeDirection();
        }
    }

    private class TrailPoint {
        int intensity;
        float hue;
        
        TrailPoint(int intensity, float hue) {
            this.intensity = intensity;
            this.hue = hue;
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        if (particles.isEmpty()) {
            initializeParticles(buttons);
        }

        // Fade trails
        trails.entrySet().removeIf(entry -> entry.getValue().intensity <= 1);
        trails.forEach((p, t) -> t.intensity = Math.max(1, t.intensity - 1));

        // Update particles
        for (Particle p : particles) {
            p.update(buttons[0].length, buttons.length);
            Point pos = new Point((int)p.x, (int)p.y);
            trails.put(pos, new TrailPoint(255, p.hue));
        }

        // Draw trails
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        trails.forEach((pos, trail) -> {
            if (pos.x >= 0 && pos.x < buttons[0].length && 
                pos.y >= 0 && pos.y < buttons.length) {
                float brightness = trail.intensity / 255.0f;
                buttons[pos.y][pos.x].setBackground(
                    Color.getHSBColor(trail.hue, 0.8f, brightness)
                );
            }
        });

        // Occasionally add new particles
        if (random.nextInt(30) == 0 && particles.size() < 10) {
            particles.add(new Particle(
                random.nextInt(buttons[0].length),
                random.nextInt(buttons.length)
            ));
        }

        phase += 0.02;
    }

    private void initializeParticles(JButton[][] buttons) {
        particles.clear();
        trails.clear();
        for (int i = 0; i < 5; i++) {
            particles.add(new Particle(
                random.nextInt(buttons[0].length),
                random.nextInt(buttons.length)
            ));
        }
    }

    @Override
    public String getName() {
        return "Brownian Motion";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.SCIENCE;
    }
}
