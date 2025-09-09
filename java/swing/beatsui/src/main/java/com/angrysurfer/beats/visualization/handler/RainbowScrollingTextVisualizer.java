package com.angrysurfer.beats.visualization.handler;

import java.awt.Color;
import java.util.Random;

import javax.swing.JButton;

import com.angrysurfer.beats.visualization.DisplayType;
import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.visualization.font.FontLoader;
import com.angrysurfer.beats.visualization.font.LedFont;

public class RainbowScrollingTextVisualizer implements IVisualizationHandler {
    private int position = 0;
    private float hue = 0f;
    private final FontLoader fontLoader = FontLoader.getInstance();
    private static final int PADDING = 1;
    private final Random random = new Random();
    private int currentMessageIndex = 0;
    
    // Reuse the same messages array from ScrollingTextVisualizer
    private static final String[] MESSAGES = ScrollingTextVisualizer.MESSAGES;
    private String currentMessage = MESSAGES[0];

    @Override
    public void update(JButton[][] buttons) {
        // Paint background black
        for (JButton[] row : buttons) {
            for (JButton button : row) {
                button.setBackground(Color.BLACK);
                button.repaint();
            }
        }

        // Switch message when current one has completely scrolled off screen
        int messageWidth = currentMessage.length() * (fontLoader.getFont('A').getWidth() + 1);
        if (position > buttons[0].length + messageWidth) {
            position = 0;
            currentMessageIndex = random.nextInt(MESSAGES.length);
            currentMessage = MESSAGES[currentMessageIndex];
        }

        // Draw each character in the current message
        for (int i = 0; i < currentMessage.length(); i++) {
            char currentChar = currentMessage.charAt(i);
            LedFont font = fontLoader.getFont(currentChar);
            
            // Calculate X position for this character
            int startX = buttons[0].length + (i * (font.getWidth() + 1)) - position;
            
            // Skip if character is completely off screen
            if (startX + font.getWidth() <= 0 || startX >= buttons[0].length) continue;
            
            // Calculate color for this character
            float characterHue = (hue + (i * 0.05f)) % 1.0f;
            Color color = Color.getHSBColor(characterHue, 1.0f, 1.0f);
            
            // Draw the character pattern
            for (int y = 0; y < font.getHeight(); y++) {
                for (int x = 0; x < font.getWidth(); x++) {
                    int gridX = startX + x;
                    int gridY = y + PADDING;
                    
                    if (gridX >= 0 && gridX < buttons[0].length && gridY < buttons.length) {
                        if (font.getPattern()[y][x] == 1) {
                            buttons[gridY][gridX].setBackground(color);
                        }
                    }
                }
            }
        }

        // Update position and hue
        position++;
        hue = (hue + 0.01f) % 1.0f;
    }

    @Override
    public String getName() {
        return "Rainbow Scrolling Text";
    }

    @Override
    public DisplayType getDisplayType() {
        return DisplayType.VISUALIZER;
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.DEFAULT;
    }
}
