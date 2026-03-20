package com.angrysurfer.grid.visualization.handler.science;

import java.awt.Color;
import java.util.*;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class KineticsVisualization implements IVisualizationHandler {
    private List<Particle> particles = new ArrayList<>();
    private Random random = new Random();
    private double gravity = 0.1;
    private double friction = 0.98;
    private double phase = 0;

    private class Particle {
        double x, y, vx, vy;
        float hue;
        double mass;
        
        Particle() {
            reset();
        }
        
        void reset() {
            x = random.nextInt(36);
            y = random.nextInt(8);
            vx = random.nextDouble() * 2 - 1;
            vy = random.nextDouble() * 2 - 1;
            hue = random.nextFloat();
            mass = random.nextDouble() * 0.5 + 0.5;
        }
        
        void update(List<Particle> others) {
            // Apply physics
            vy += gravity * mass;
            x += vx;
            y += vy;
            
            // Bounce off walls
            if (x < 0 || x >= 36) {
                vx *= -0.8;
                x = Math.max(0, Math.min(35.9, x));
            }
            if (y < 0 || y >= 8) {
                vy *= -0.8;
                y = Math.max(0, Math.min(7.9, y));
            }
            
            // Apply friction
            vx *= friction;
            vy *= friction;
            
            // Particle collisions
            for (Particle other : others) {
                if (other != this) {
                    double dx = other.x - x;
                    double dy = other.y - y;
                    double dist = Math.hypot(dx, dy);
                    if (dist < 1.5) {
                        // Elastic collision
                        double totalMass = mass + other.mass;
                        // Exchange momentum
                        double newVx = ((mass - other.mass) * vx + 2 * other.mass * other.vx) / totalMass;
                        double newVy = ((mass - other.mass) * vy + 2 * other.mass * other.vy) / totalMass;
                        vx = newVx * 0.8;
                        vy = newVy * 0.8;
                    }
                }
            }
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        if (particles.isEmpty()) {
            for (int i = 0; i < 10; i++) {
                particles.add(new Particle());
            }
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update particles
        for (Particle p : particles) {
            p.update(particles);
            
            // Draw particle with glow effect
            int px = (int)p.x;
            int py = (int)p.y;
            double speed = Math.hypot(p.vx, p.vy);
            float brightness = Math.min(1.0f, (float)(0.5 + speed * 0.5));
            
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int drawX = px + dx;
                    int drawY = py + dy;
                    if (drawX >= 0 && drawX < buttons[0].length &&
                        drawY >= 0 && drawY < buttons.length) {
                        float dist = (float)Math.hypot(dx, dy) / 2;
                        buttons[drawY][drawX].setBackground(
                            Color.getHSBColor(p.hue, 0.8f, brightness * (1 - dist))
                        );
                    }
                }
            }
        }

        // Occasionally reset a random particle
        if (random.nextInt(50) == 0) {
            particles.get(random.nextInt(particles.size())).reset();
        }

        phase += 0.05;
    }

    @Override
    public String getName() {
        return "Kinetics";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.SCIENCE;
    }
}
