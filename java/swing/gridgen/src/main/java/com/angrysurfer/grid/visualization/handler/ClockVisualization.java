package com.angrysurfer.grid.visualization.handler;

import java.awt.Color;
import java.time.LocalTime;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import javax.swing.JButton;

public class ClockVisualization implements IVisualizationHandler {
    private final Color HAND_COLOR = new Color(0, 255, 255);
    private final Color DIGIT_COLOR = new Color(100, 100, 255);
    private double phase = 0;

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        LocalTime now = LocalTime.now();
        int hours = now.getHour();
        int minutes = now.getMinute();
        int seconds = now.getSecond();

        // Draw analog clock hands
        drawClockHand(buttons, hours * 30 + minutes / 2, 2, HAND_COLOR); // Hour hand
        drawClockHand(buttons, minutes * 6, 3, HAND_COLOR);              // Minute hand
        drawClockHand(buttons, seconds * 6, 4, new Color(255, 0, 0));   // Second hand

        // Draw digital time at the bottom
        String timeStr = String.format("%02d:%02d", hours, minutes);
        drawDigits(buttons, timeStr, buttons.length - 2, DIGIT_COLOR);

        phase += 0.1;
    }

    private void drawClockHand(JButton[][] buttons, double angle, int length, Color color) {
        int centerX = buttons[0].length / 2;
        int centerY = buttons.length / 2;
        
        double radians = Math.toRadians(angle - 90);
        int endX = centerX + (int)(Math.cos(radians) * length * 2);
        int endY = centerY + (int)(Math.sin(radians) * length * 2);
        
        drawLine(buttons, centerX, centerY, endX, endY, color);
    }

    private void drawLine(JButton[][] buttons, int x1, int y1, int x2, int y2, Color color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x1 >= 0 && x1 < buttons[0].length && y1 >= 0 && y1 < buttons.length) {
                buttons[y1][x1].setBackground(color);
            }
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
    }

    private void drawDigits(JButton[][] buttons, String text, int row, Color color) {
        int startCol = (buttons[0].length - text.length() * 4) / 2;
        for (int i = 0; i < text.length(); i++) {
            drawDigit(buttons, text.charAt(i), startCol + i * 4, row, color);
        }
    }

    private void drawDigit(JButton[][] buttons, char digit, int x, int y, Color color) {
        switch (digit) {
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                int val = digit - '0';
                for (int i = 0; i < 3; i++) {
                    if (x + i < buttons[0].length && y < buttons.length) {
                        buttons[y][x + i].setBackground(color);
                    }
                }
                break;
            case ':':
                if (Math.sin(phase) > 0) {
                    if (y < buttons.length) {
                        buttons[y][x].setBackground(color);
                    }
                }
                break;
        }
    }

    @Override
    public String getName() {
        return "Clock";
    }
}
