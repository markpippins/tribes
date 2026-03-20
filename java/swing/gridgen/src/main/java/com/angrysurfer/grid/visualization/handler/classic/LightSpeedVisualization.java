package com.angrysurfer.grid.visualization.handler.classic;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import javax.swing.JButton;

public class LightSpeedVisualization implements IVisualizationHandler {
    private List<LightBeam> beams = new ArrayList<>();
    private Random random = new Random();
    private double speed = 1.0;
    private double phase = 0;

    private class LightBeam {
        double x, y;
        double vx, vy;
        float hue;
        int length;

        LightBeam(int width, int height) {
            reset(width, height);
        }

        void reset(int width, int height) {
            x = width / 2.0;
            y = height / 2.0;
            double angle = random.nextDouble() * Math.PI * 2;
            double velocity = random.nextDouble() * 0.5 + 0.5;
            vx = Math.cos(angle) * velocity;
            vy = Math.sin(angle) * velocity;
            hue = random.nextFloat();
            length = random.nextInt(5) + 3;
        }

        void update(int width, int height) {
            x += vx * speed;
            y += vy * speed;
            vx *= 1.05;
            vy *= 1.05;

            if (Math.abs(x - width / 2) > width / 2 ||
                    Math.abs(y - height / 2) > height / 2) {
                reset(width, height);
            }
        }

        void draw(JButton[][] buttons) {
            int px = (int) x;
            int py = (int) y;

            for (int i = 0; i < length; i++) {
                int drawX = px - (int) (vx * i);
                int drawY = py - (int) (vy * i);

                if (drawX >= 0 && drawX < buttons[0].length &&
                        drawY >= 0 && drawY < buttons.length) {
                    float brightness = 1.0f - (float) i / length;
                    buttons[drawY][drawX].setBackground(
                            Color.getHSBColor(hue, 0.5f, brightness));
                }
            }
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        if (beams.isEmpty()) {
            for (int i = 0; i < 15; i++) {
                beams.add(new LightBeam(buttons[0].length, buttons.length));
            }
        }

        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Update and draw light beams
        for (LightBeam beam : beams) {
            beam.update(buttons[0].length, buttons.length);
            beam.draw(buttons);
        }

        // Occasionally add new beams
        if (random.nextInt(10) == 0 && beams.size() < 20) {
            beams.add(new LightBeam(buttons[0].length, buttons.length));
        }

        // Vary speed
        speed = 1.0 + Math.sin(phase) * 0.2;
        phase += 0.05;
    }

    @Override
    public String getName() {
        return "Light Speed";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.CLASSIC;
    }
}
