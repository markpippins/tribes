package com.angrysurfer.grid.visualization.handler.display;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class TimesSquareVisualization implements IVisualizationHandler {
    private List<ScrollingMessage> messages = new ArrayList<>();
    private final Random random = new Random();
    private int tick = 0;
    private static final String[] SAMPLE_MESSAGES = {
        "WELCOME TO TIMES SQUARE",
        "♪ ♫ ♬",
        "NEW YORK CITY",
        "LIVE MUSIC",
        "BROADWAY",
        "42ND STREET",
        "★★★★★"
    };

    private class ScrollingMessage {
        String text;
        int x, y;
        Color color;
        int speed;
        boolean isAnimated;

        ScrollingMessage(String text, int y) {
            this.text = text;
            this.x = 50; // Start off screen
            this.y = y;
            this.color = getRandomBrightColor();
            this.speed = 1 + random.nextInt(2);
            this.isAnimated = random.nextBoolean();
        }

        void update(int maxWidth) {
            x -= speed;
            if (x + text.length() * 6 < 0) { // Reset when message goes off screen
                x = maxWidth;
                color = getRandomBrightColor();
            }
        }
    }

    private Color getRandomBrightColor() {
        float hue = random.nextFloat();
        return Color.getHSBColor(hue, 0.9f, 1.0f);
    }

    @Override
    public void update(JButton[][] buttons) {
        int rows = buttons.length;
        int cols = buttons[0].length;

        // Initialize messages if needed
        if (messages.isEmpty()) {
            for (int i = 0; i < rows; i += 2) {
                if (random.nextDouble() < 0.7) { // 70% chance for a message on each row
                    messages.add(new ScrollingMessage(
                        SAMPLE_MESSAGES[random.nextInt(SAMPLE_MESSAGES.length)],
                        i
                    ));
                }
            }
        }

        // Clear display
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                buttons[row][col].setBackground(Color.BLACK);
            }
        }

        tick++;

        // Update and draw messages
        for (ScrollingMessage msg : messages) {
            msg.update(cols);
            
            // Draw each character of the message
            for (int i = 0; i < msg.text.length(); i++) {
                int charX = msg.x + (i * 6);
                if (charX >= 0 && charX < cols) {
                    Color color = msg.color;
                    
                    // Add animation effects
                    if (msg.isAnimated) {
                        // Pulsing effect
                        float pulse = (float) (0.7 + 0.3 * Math.sin(tick * 0.1 + i * 0.2));
                        color = new Color(
                            Math.min((int)(color.getRed() * pulse), 255),
                            Math.min((int)(color.getGreen() * pulse), 255),
                            Math.min((int)(color.getBlue() * pulse), 255)
                        );
                    }

                    // Draw character using multiple LEDs
                    drawCharacter(buttons, msg.text.charAt(i), charX, msg.y, color);
                }
            }
        }

        // Randomly add new messages
        if (random.nextDouble() < 0.01) { // 1% chance each frame
            int y = random.nextInt(rows - 1);
            messages.add(new ScrollingMessage(
                SAMPLE_MESSAGES[random.nextInt(SAMPLE_MESSAGES.length)],
                y
            ));
        }

        // Remove off-screen messages
        messages.removeIf(msg -> msg.x + msg.text.length() * 6 < 0);
    }

    private void drawCharacter(JButton[][] buttons, char c, int x, int y, Color color) {
        // Simple LED-style character rendering
        switch (c) {
            case '★' -> {
                if (y + 1 < buttons.length) {
                    buttons[y][x].setBackground(color);
                    buttons[y + 1][x].setBackground(color);
                }
            }
            case '♪', '♫', '♬' -> {
                Color fadedColor = new Color(
                    color.getRed(), color.getGreen(), color.getBlue(),
                    128 + random.nextInt(128)
                );
                buttons[y][x].setBackground(fadedColor);
                if (y + 1 < buttons.length) {
                    buttons[y + 1][x].setBackground(color);
                }
            }
            default -> {
                // Standard character
                if (y + 1 < buttons.length) {
                    buttons[y][x].setBackground(color);
                    buttons[y + 1][x].setBackground(color.darker());
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Times Square";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.DEFAULT;
    }
}
