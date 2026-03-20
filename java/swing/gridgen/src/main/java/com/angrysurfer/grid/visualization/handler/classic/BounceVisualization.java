package com.angrysurfer.grid.visualization.handler.classic;

import java.awt.Color;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class BounceVisualization implements IVisualizationHandler {
    private int bouncePos = 1;
    private boolean bounceUp = true;

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        for (int col = 0; col < buttons[0].length; col++) {
            buttons[bouncePos][col].setBackground(Color.YELLOW);
        }

        if (bounceUp) {
            bouncePos--;
            if (bouncePos < 0) {
                bouncePos = 0;
                bounceUp = false;
            }
        } else {
            bouncePos++;
            if (bouncePos >= buttons.length) {
                bouncePos = buttons.length - 1;
                bounceUp = true;
            }
        }
    }

    @Override
    public String getName() {
        return "Bounce";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.CLASSIC;
    }
}
