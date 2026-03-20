// package com.angrysurfer.grid.visualization.handler.special;

// import java.awt.Color;
// import java.util.Random;

// import com.angrysurfer.grid.font.FontLoader;
// import com.angrysurfer.grid.font.LedFont;
// import com.angrysurfer.grid.visualization.IVisualizationHandler;
// import com.angrysurfer.grid.visualization.VisualizationCategory;
// import javax.swing.JButton;

// public class BirthdayVisualization implements IVisualizationHandler {
//     private int position = 0;
//     private float hue = 0f;
//     private final FontLoader fontLoader = FontLoader.getInstance();
//     private static final int PADDING = 1;
//     private final Random random = new Random();

//     // Combined message with emoji placeholders
//     private final String[] MESSAGE_PARTS = {
//         "🍺",  // Beer
//         "Happy Birthday, Leroy!!!",
//         "🍷",  // Wine
//         // "You're the best drummer in the house!",
//         // "🥁",  // Drum
//         // "We Love you!",
//         "🎂"   // Cake
//     };

//     // ASCII art for emojis (simplified 8x8 representations)
//     private static final int[][] BEER_ICON = {
//         {0,0,1,1,1,1,0,0},
//         {0,1,1,0,0,1,1,0},
//         {1,1,1,1,1,1,1,1},
//         {1,1,0,0,0,0,1,1},
//         {1,1,1,1,1,1,1,1},
//         {0,1,1,1,1,1,1,0},
//         {0,0,1,1,1,1,0,0},
//         {0,0,0,1,1,0,0,0}
//     };

//     // Add similar patterns for other emojis...

//     @Override
//     public void update(JButton[][] buttons) {
//         // Clear background to black
//         for (JButton[] row : buttons) {
//             for (JButton button : row) {
//                 button.setBackground(Color.BLACK);
//             }
//         }

//         // Add fireworks effect (from FireworksVisualization)
//         if (random.nextInt(100) < 5) {  // Reduced frequency from original
//             addFirework(buttons);
//         }

//         // Draw scrolling text with rainbow effect
//         drawScrollingMessage(buttons);

//         // Update animation states
//         position++;
//         hue = (hue + 0.01f) % 1.0f;
//     }

//     private void addFirework(JButton[][] buttons) {
//         int centerX = random.nextInt(buttons[0].length);
//         int centerY = random.nextInt(buttons.length);
//         Color color = Color.getHSBColor(random.nextFloat(), 0.8f, 1.0f);

//         // Create starburst pattern
//         for (int i = -2; i <= 2; i++) {
//             for (int j = -2; j <= 2; j++) {
//                 if (Math.abs(i) + Math.abs(j) <= 3) {  // Diamond shape
//                     int x = centerX + i;
//                     int y = centerY + j;
//                     if (y >= 0 && y < buttons.length && x >= 0 && x < buttons[0].length) {
//                         buttons[y][x].setBackground(color);
//                     }
//                 }
//             }
//         }
//     }

//     private void drawScrollingMessage(JButton[][] buttons) {
//         int currentX = buttons[0].length - position;
        
//         for (String part : MESSAGE_PARTS) {
//             if (part.startsWith("🍺") || part.startsWith("🍷") || 
//                 part.startsWith("🥁") || part.startsWith("🎂")) {
//                 currentX = drawEmoji(buttons, part, currentX);
//             } else {
//                 currentX = drawText(buttons, part, currentX);
//             }
//             currentX += 4; // Add spacing between parts
//         }

//         // Reset position when message is off screen
//         if (currentX < -100) {  // Arbitrary length to ensure full message is off screen
//             position = 0;
//         }
//     }

//     private int drawEmoji(JButton[][] buttons, String emoji, int startX) {
//         int[][] pattern = getEmojiPattern(emoji);
//         if (pattern == null) return startX + 8;  // Skip if no pattern

//         for (int y = 0; y < pattern.length; y++) {
//             for (int x = 0; x < pattern[0].length; x++) {
//                 int screenX = startX + x;
//                 int screenY = y + PADDING;
                
//                 if (screenX >= 0 && screenX < buttons[0].length && 
//                     screenY >= 0 && screenY < buttons.length) {
//                     if (pattern[y][x] == 1) {
//                         float emojiHue = (hue + (x * 0.1f)) % 1.0f;
//                         buttons[screenY][screenX].setBackground(
//                             Color.getHSBColor(emojiHue, 0.8f, 1.0f));
//                     }
//                 }
//             }
//         }
        
//         return startX + pattern[0].length;
//     }

//     private int[][] getEmojiPattern(String emoji) {
//         return switch (emoji) {
//             case "🍺" -> BEER_ICON;
//             // Add cases for other emojis
//             default -> null;
//         };
//     }

//     private int drawText(JButton[][] buttons, String text, int startX) {
//         for (int i = 0; i < text.length(); i++) {
//             char currentChar = text.charAt(i);
//             LedFont font = fontLoader.getFont(currentChar);
            
//             float charHue = (hue + (i * 0.05f)) % 1.0f;
//             Color color = Color.getHSBColor(charHue, 1.0f, 1.0f);
            
//             for (int y = 0; y < font.getHeight(); y++) {
//                 for (int x = 0; x < font.getWidth(); x++) {
//                     int screenX = startX + x;
//                     int screenY = y + PADDING;
                    
//                     if (screenX >= 0 && screenX < buttons[0].length && 
//                         screenY >= 0 && screenY < buttons.length) {
//                         if (font.getPattern()[y][x] == 1) {
//                             buttons[screenY][screenX].setBackground(color);
//                         }
//                     }
//                 }
//             }
//             startX += font.getWidth() + 1;
//         }
//         return startX;
//     }

//     @Override
//     public String getName() {
//         return "Happy Birthday Leroy!";
//     }

//     @Override
//     public VisualizationCategory getVisualizationCategory() {
//         return VisualizationCategory.DEFAULT;
//     }
// }
