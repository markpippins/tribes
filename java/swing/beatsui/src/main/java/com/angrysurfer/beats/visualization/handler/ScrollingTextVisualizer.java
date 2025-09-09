package com.angrysurfer.beats.visualization.handler;

import java.awt.Color;
import java.util.Random;

import javax.swing.JButton;

import com.angrysurfer.beats.visualization.DisplayType;
import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.beats.visualization.font.FontLoader;
import com.angrysurfer.beats.visualization.font.LedFont;

public class ScrollingTextVisualizer implements IVisualizationHandler {
    private int position = 0;
    private final FontLoader fontLoader = FontLoader.getInstance();
    private static final int PADDING = 1;
    private final Random random = new Random();
    private int currentMessageIndex = 0;
    private Color currentMessageColor = Color.GREEN;

    public static final String[] MESSAGES = {
        "Just Do It",
        "Think Different",
        "Got Milk?",
        "I'm Lovin' It",
        "Finger Lickin' Good",
        "Have It Your Way",
        "The Best A Man Can Get",
        "Melts In Your Mouth",
        "Taste The Rainbow",
        "Red Bull Gives You Wings",
        "The Happiest Place On Earth",
        "Snap Crackle Pop",
        "It's Finger Clickin' Good",
        "Because You're Worth It",
        "Diamonds Are Forever",
        "The Breakfast of Champions",
        "America Runs On Dunkin",
        "Maybe She's Born With It",
        "Eat Fresh",
        "What's In Your Wallet?",
        "Can You Hear Me Now?",
        "The King Of Beers",
        "Good To The Last Drop",
        "Betcha Can't Eat Just One"
    };

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
            // Generate new random color for the new message
            float hue = random.nextFloat();
            currentMessageColor = Color.getHSBColor(hue, 0.8f, 1.0f);
        }

        // Draw each character in the current message
        for (int i = 0; i < currentMessage.length(); i++) {
            char currentChar = currentMessage.charAt(i);
            LedFont font = fontLoader.getFont(currentChar);
            
            // Calculate X position for this character
            int startX = buttons[0].length + (i * (font.getWidth() + 1)) - position;
            
            // Skip if character is completely off screen
            if (startX + font.getWidth() <= 0 || startX >= buttons[0].length) continue;
            
            // Draw the character pattern
            for (int y = 0; y < font.getHeight(); y++) {
                for (int x = 0; x < font.getWidth(); x++) {
                    int gridX = startX + x;
                    int gridY = y + PADDING;
                    
                    if (gridX >= 0 && gridX < buttons[0].length && gridY < buttons.length) {
                        if (font.getPattern()[y][x] == 1) {
                            buttons[gridY][gridX].setBackground(currentMessageColor);
                        }
                    }
                }
            }
        }

        // Update position
        position++;
    }

    @Override
    public String getName() {
        return "Scrolling Text";
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
