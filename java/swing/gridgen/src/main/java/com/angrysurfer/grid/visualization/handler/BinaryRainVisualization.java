package com.angrysurfer.grid.visualization.handler;

import java.awt.Color;
import java.util.*;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import javax.swing.JButton;

public class BinaryRainVisualization implements IVisualizationHandler {
    private List<Raindrop> raindrops = new ArrayList<>();
    private Random random = new Random();
    private static final int MAX_DROPS = 15;
    private double phase = 0;

    private class Raindrop {
        int x, y;
        char[] symbols;
        int length;
        float hue;
        
        Raindrop(int x, int width, int height) {
            this.x = x;
            this.y = -random.nextInt(5);
            this.length = 3 + random.nextInt(5);
            this.symbols = new char[length];
            this.hue = random.nextFloat() * 0.3f + 0.4f; // Green-ish hues
            
            for (int i = 0; i < length; i++) {
                symbols[i] = (random.nextBoolean() ? '1' : '0');
            }
        }
        
        void fall() {
            y++;
            if (random.nextInt(4) == 0) {
                symbols[random.nextInt(length)] = (random.nextBoolean() ? '1' : '0');
            }
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Add new raindrops
        if (raindrops.size() < MAX_DROPS && random.nextInt(3) == 0) {
            raindrops.add(new Raindrop(
                random.nextInt(buttons[0].length),
                buttons[0].length,
                buttons.length
            ));
        }

        // Update and draw raindrops
        for (Raindrop drop : new ArrayList<>(raindrops)) {
            drop.fall();
            
            // Draw raindrop trail
            for (int i = 0; i < drop.length; i++) {
                int y = drop.y - i;
                if (y >= 0 && y < buttons.length && drop.x >= 0 && drop.x < buttons[0].length) {
                    float brightness = 1.0f - (float)i / drop.length;
                    buttons[y][drop.x].setBackground(
                        Color.getHSBColor(drop.hue, 0.8f, brightness)
                    );
                }
            }
            
            // Remove if off screen
            if (drop.y - drop.length > buttons.length) {
                raindrops.remove(drop);
            }
        }

        phase += 0.05;
    }

    @Override
    public String getName() {
        return "Binary Rain";
    }
}
