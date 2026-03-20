package com.angrysurfer.grid.visualization.handler.geo;

import java.awt.Color;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import javax.swing.JButton;

public class RainbowVisualization implements IVisualizationHandler {
    private int offset = 0;
    private final Color[] colors = {
        Color.RED, Color.ORANGE, Color.YELLOW,
        Color.GREEN, Color.BLUE, new Color(75, 0, 130), // Indigo
        new Color(238, 130, 238)  // Violet
    };

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Draw rainbow bands with smooth transitions
        for (int col = 0; col < buttons[0].length; col++) {
            for (int row = 0; row < buttons.length; row++) {
                int colorIndex = (offset + col) % (colors.length * 4);
                int baseIndex = colorIndex / 4;
                int nextIndex = (baseIndex + 1) % colors.length;
                float blend = (colorIndex % 4) / 4.0f;

                Color color = blendColors(colors[baseIndex], colors[nextIndex], blend);
                buttons[row][col].setBackground(color);
            }
        }
        offset++;
    }

    private Color blendColors(Color c1, Color c2, float ratio) {
        float r = c1.getRed() * (1 - ratio) + c2.getRed() * ratio;
        float g = c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio;
        float b = c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio;
        return new Color(r/255, g/255, b/255);
    }

    @Override
    public String getName() {
        return "Rainbow";
    }
     
    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.CLASSIC;
    }
}
