package com.angrysurfer.grid.visualization.handler;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class RainbowStarVisualization implements IVisualizationHandler {
    private double angle = 0.0;
    private double scale = 1.0;
    private boolean growing = true;
    private final List<Point> starPoints = new ArrayList<>();
    private final Color[] rainbowColors = getRainbowColors();
    private int colorIndex = 0;

    private static class Point {
        double x, y;
        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public RainbowStarVisualization() {
        // Create star points
        createStarPoints(5, 0.5, 1.0); // 5-pointed star
    }

    private void createStarPoints(int points, double innerRadius, double outerRadius) {
        starPoints.clear();
        double angleStep = Math.PI / points;
        
        for (int i = 0; i < points * 2; i++) {
            double r = (i % 2 == 0) ? outerRadius : innerRadius;
            double x = Math.cos(i * angleStep) * r;
            double y = Math.sin(i * angleStep) * r;
            starPoints.add(new Point(x, y));
        }
    }

    @Override
    public void update(JButton[][] buttons) {
        int centerX = buttons.length / 2;
        int centerY = buttons[0].length / 2;
        
        // Clear grid
        for (int x = 0; x < buttons.length; x++) {
            for (int y = 0; y < buttons[0].length; y++) {
                buttons[x][y].setBackground(Color.BLACK);
            }
        }

        // Update animation parameters
        angle += 0.1;
        if (growing) {
            scale += 0.05;
            if (scale > 1.2) growing = false;
        } else {
            scale -= 0.05;
            if (scale < 0.8) growing = true;
        }

        // Draw star
        for (Point p : starPoints) {
            // Rotate and scale point
            double rotX = p.x * Math.cos(angle) - p.y * Math.sin(angle);
            double rotY = p.x * Math.sin(angle) + p.y * Math.cos(angle);
            
            // Scale and translate to center
            int gridX = centerX + (int)(rotX * scale * centerX);
            int gridY = centerY + (int)(rotY * scale * centerY);
            
            // Draw point if in bounds
            if (gridX >= 0 && gridX < buttons.length && 
                gridY >= 0 && gridY < buttons[0].length) {
                buttons[gridX][gridY].setBackground(rainbowColors[colorIndex]);
                
                // Draw connecting lines using Bresenham's algorithm
                int prevIndex = starPoints.indexOf(p) - 1;
                if (prevIndex < 0) prevIndex = starPoints.size() - 1;
                Point prevPoint = starPoints.get(prevIndex);
                
                double prevRotX = prevPoint.x * Math.cos(angle) - prevPoint.y * Math.sin(angle);
                double prevRotY = prevPoint.x * Math.sin(angle) + prevPoint.y * Math.cos(angle);
                
                int prevGridX = centerX + (int)(prevRotX * scale * centerX);
                int prevGridY = centerY + (int)(prevRotY * scale * centerY);
                
                drawLine(buttons, prevGridX, prevGridY, gridX, gridY, rainbowColors[colorIndex]);
            }
        }

        // Cycle colors
        colorIndex = (colorIndex + 1) % rainbowColors.length;
    }

    private void drawLine(JButton[][] buttons, int x1, int y1, int x2, int y2, Color color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        
        while (true) {
            if (x1 >= 0 && x1 < buttons.length && y1 >= 0 && y1 < buttons[0].length) {
                buttons[x1][y1].setBackground(color);
            }
            
            if (x1 == x2 && y1 == y2) break;
            
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    @Override
    public String getName() {
        return "Rainbow Star";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.DEFAULT;
    }
}
