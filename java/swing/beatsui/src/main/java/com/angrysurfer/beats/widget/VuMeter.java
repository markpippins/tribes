package com.angrysurfer.beats.widget;

import javax.swing.*;
import java.awt.*;

/**
 * A simple VU meter to display audio levels
 */
public class VuMeter extends JPanel {

    private final Orientation orientation;
    private int level = 0;
    public VuMeter(Orientation orientation) {
        this.orientation = orientation;

        if (orientation == Orientation.HORIZONTAL) {
            setPreferredSize(new Dimension(50, 18));
        } else {
            setPreferredSize(new Dimension(8, 50));
        }

        setOpaque(false);
    }

    public void setLevel(int level) {
        this.level = Math.max(0, Math.min(100, level));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Draw background
        g2d.setColor(new Color(30, 30, 30));
        g2d.fillRect(0, 0, width, height);

        // Calculate level size
        int levelSize;
        if (orientation == Orientation.HORIZONTAL) {
            levelSize = (int) (width * (level / 100.0));
        } else {
            levelSize = (int) (height * (level / 100.0));
        }

        // Get level color (green to yellow to red based on level)
        Color levelColor;
        if (level < 70) {
            levelColor = new Color(0, 180, 0); // Green
        } else if (level < 90) {
            levelColor = new Color(200, 180, 0); // Yellow
        } else {
            levelColor = new Color(200, 0, 0); // Red
        }

        // Draw level
        g2d.setColor(levelColor);
        if (orientation == Orientation.HORIZONTAL) {
            g2d.fillRect(0, 0, levelSize, height);

            // Draw segments
            for (int i = 1; i < width; i += 3) {
                g2d.setColor(new Color(20, 20, 20));
                g2d.drawLine(i, 0, i, height);
            }
        } else {
            int y = height - levelSize;
            g2d.fillRect(0, y, width, levelSize);

            // Draw segments
            for (int i = 1; i < height; i += 3) {
                g2d.setColor(new Color(20, 20, 20));
                g2d.drawLine(0, i, width, i);
            }
        }

        // Draw border
        g2d.setColor(new Color(100, 100, 100));
        g2d.drawRect(0, 0, width - 1, height - 1);

        g2d.dispose();
    }

    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }
}
