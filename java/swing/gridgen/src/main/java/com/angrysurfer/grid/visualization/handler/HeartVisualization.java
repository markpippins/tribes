package com.angrysurfer.grid.visualization.handler;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import javax.swing.JButton;

public class HeartVisualization implements IVisualizationHandler {
    private double heartBeat = 0;
    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private double beatRate = 0.1;
    private int heartCount = 3;
    private Color currentColor = Color.RED;
    
    private class Particle {
        double x, y;
        double dx, dy;
        int life;
        
        Particle(double x, double y) {
            this.x = x;
            this.y = y;
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 0.2 + random.nextDouble() * 0.3;
            this.dx = Math.cos(angle) * speed;
            this.dy = Math.sin(angle) * speed;
            this.life = 10 + random.nextInt(20);
        }
        
        void update() {
            x += dx;
            y += dy;
            life--;
        }
    }

    private boolean isHeart(double x, double y) {
        x = Math.abs(x);
        // Negate y to flip the heart right-side up
        y = -y;
        return Math.pow((x * x + y * y - 1), 3) <= x * x * y * y * y;
    }

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        // Update heartbeat and color
        heartBeat += beatRate;
        double pulse = Math.sin(heartBeat);
        beatRate = 0.1 + Math.abs(Math.sin(heartBeat * 0.1)) * 0.05; // Variable heart rate
        
        // Color transition
        float hue = (float)((Math.sin(heartBeat * 0.1) + 1) * 0.15); // Varies between red and pink
        currentColor = Color.getHSBColor(hue, 0.8f, 0.9f);
        
        // Draw multiple hearts with adjusted center position
        for (int h = 0; h < heartCount; h++) {
            int centerX = buttons[0].length / 2 + (h - heartCount/2) * (buttons[0].length/3);
            // Position hearts at about the middle of the grid (row 6 of 12)
            int centerY = buttons.length / 2;
            double size = 1.5 + pulse * 0.5; // Pulsing effect
            
            // Draw heart
            for (int row = 0; row < buttons.length; row++) {
                for (int col = 0; col < buttons[0].length; col++) {
                    double dx = (col - centerX) / (size * 2);
                    double dy = (row - centerY) / (size * 2);
                    if (isHeart(dx, dy)) {
                        buttons[row][col].setBackground(currentColor);
                        
                        // Spawn particles during expansion
                        if (pulse > 0.8 && random.nextDouble() < 0.1) {
                            particles.add(new Particle(col, row));
                        }
                    }
                }
            }
        }
        
        // Update and draw particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            
            int px = (int)p.x;
            int py = (int)p.y;
            
            if (p.life <= 0 || px < 0 || px >= buttons[0].length || py < 0 || py >= buttons.length) {
                particles.remove(i);
                continue;
            }
            
            // Particle color fades with life
            float alpha = p.life / 30f;
            Color particleColor = new Color(
                currentColor.getRed(),
                currentColor.getGreen(),
                currentColor.getBlue(),
                (int)(255 * alpha)
            );
            buttons[py][px].setBackground(particleColor);
        }
        
        // Occasionally change number of hearts
        if (random.nextDouble() < 0.005) {
            heartCount = 1 + random.nextInt(3);
        }
    }

    @Override
    public String getName() {
        return "Heart Beat";
    }
}
