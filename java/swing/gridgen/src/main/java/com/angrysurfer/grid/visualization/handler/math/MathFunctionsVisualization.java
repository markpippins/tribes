package com.angrysurfer.grid.visualization.handler.math;

import java.awt.Color;

import com.angrysurfer.grid.visualization.IVisualizationHandler;
import com.angrysurfer.grid.visualization.VisualizationUtils;
import com.angrysurfer.grid.visualization.VisualizationCategory;
import javax.swing.JButton;

public class MathFunctionsVisualization implements IVisualizationHandler {
    private double phase = 0;
    private final int NUM_FUNCTIONS = 4;
    
    @Override
    public void update(JButton[][] buttons) {
        VisualizationUtils.clearDisplay(buttons, buttons[0][0].getParent());
        
        // Draw multiple mathematical functions
        for (int func = 0; func < NUM_FUNCTIONS; func++) {
            float hue = (float)func / NUM_FUNCTIONS;
            Color color = Color.getHSBColor(hue, 0.8f, 1.0f);
            
            for (int x = 0; x < buttons[0].length; x++) {
                double xValue = (x * Math.PI * 2 / buttons[0].length) + phase;
                double yValue = 0;
                
                switch (func) {
                    case 0: // Sin
                        yValue = Math.sin(xValue);
                        break;
                    case 1: // Cos
                        yValue = Math.cos(xValue);
                        break;
                    case 2: // Sin(2x)
                        yValue = Math.sin(xValue * 2) * 0.5;
                        break;
                    case 3: // Sin(x) + Cos(2x)
                        yValue = Math.sin(xValue) * 0.5 + Math.cos(xValue * 2) * 0.5;
                        break;
                }
                
                int y = (int)((yValue + 1) * (buttons.length - 1) / 2);
                if (y >= 0 && y < buttons.length) {
                    buttons[y][x].setBackground(color);
                }
            }
        }
        
        phase += 0.05;
    }

    @Override
    public String getName() {
        return "Math Functions";
    }
   
    @Override
    public VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.MATH;
    }
}
