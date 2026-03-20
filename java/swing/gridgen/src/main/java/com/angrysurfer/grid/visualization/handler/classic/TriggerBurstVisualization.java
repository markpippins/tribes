package com.angrysurfer.grid.visualization.handler.classic;

import java.awt.Color;
import java.util.Random;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class TriggerBurstVisualization implements IVisualizationHandler {
    private final Random random = new Random();
    private int burstX = 0, burstY = 0;
    private int burstCount = 0;
    private final int maxBursts = 5;
    private final int[][] directions = {{-1,-1}, {-1,0}, {-1,1}, {0,-1}, {0,1}, {1,-1}, {1,0}, {1,1}};

    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());

        // Create new burst
        if (burstCount == 0 || random.nextInt(100) < 10) {
            burstX = random.nextInt(buttons[0].length);
            burstY = random.nextInt(buttons.length);
            burstCount = maxBursts;
        }

        // Draw expanding burst
        if (burstCount > 0) {
            int radius = maxBursts - burstCount + 1;
            for (int[] dir : directions) {
                for (int r = 1; r <= radius; r++) {
                    int x = burstX + dir[0] * r;
                    int y = burstY + dir[1] * r;
                    
                    if (x >= 0 && x < buttons[0].length && y >= 0 && y < buttons.length) {
                        int brightness = 255 - (255 * r / radius);
                        buttons[y][x].setBackground(new Color(brightness, 0, brightness));
                    }
                }
            }
            burstCount--;
        }

        // Draw trigger point
        if (burstX >= 0 && burstX < buttons[0].length && 
            burstY >= 0 && burstY < buttons.length) {
            buttons[burstY][burstX].setBackground(Color.WHITE);
        }
    }

    @Override
    public String getName() {
        return "Trigger Burst";
    }

    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.CLASSIC;
    }
}
