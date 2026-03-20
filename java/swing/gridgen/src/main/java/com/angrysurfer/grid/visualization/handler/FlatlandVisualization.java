package com.angrysurfer.grid.visualization.handler;

import java.awt.Color;
import java.awt.Point;
import java.util.*;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import javax.swing.JButton;

public class FlatlandVisualization implements IVisualizationHandler {
    private List<Shape> shapes = new ArrayList<>();
    private Random random = new Random();
    private double phase = 0;
    
    private class Shape {
        Point center;
        int sides;
        double angle;
        double size;
        float hue;
        
        Shape() {
            sides = random.nextInt(4) + 3; // 3 to 6 sides
            size = random.nextDouble() * 2 + 1;
            hue = random.nextFloat();
            angle = random.nextDouble() * Math.PI * 2;
        }
        
        void update() {
            angle += 0.02;
            if (center == null) {
                respawn();
            }
            center.x--;
            if (center.x + size < 0) {
                respawn();
            }
        }
        
        void respawn() {
            center = new Point(38, random.nextInt(8));
            sides = random.nextInt(4) + 3;
            size = random.nextDouble() * 2 + 1;
            hue = random.nextFloat();
        }
        
        void draw(JButton[][] buttons) {
            // Project 2D polygon into 1D line with varying intensity
            double projection = Math.abs(Math.cos(angle)) * size;
            int width = (int)projection;
            
            for (int i = -width; i <= width; i++) {
                int x = center.x + i;
                if (x >= 0 && x < buttons[0].length) {
                    float brightness = 1.0f - Math.abs(i) / (float)width;
                    buttons[center.y][x].setBackground(
                        Color.getHSBColor(hue, 0.8f, brightness)
                    );
                }
            }
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        // Add new shapes
        if (shapes.size() < 5 && random.nextInt(20) == 0) {
            shapes.add(new Shape());
        }
        
        // Update and draw shapes
        for (Shape shape : new ArrayList<>(shapes)) {
            shape.update();
            if (shape.center.x < -5) {
                shapes.remove(shape);
            } else {
                shape.draw(buttons);
            }
        }
        
        phase += 0.05;
    }

    @Override
    public String getName() {
        return "Flatland";
    }
}
