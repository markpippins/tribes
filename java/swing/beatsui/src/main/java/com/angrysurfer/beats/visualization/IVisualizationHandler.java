package com.angrysurfer.beats.visualization;

import java.awt.Color;

import javax.swing.JButton;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;

public interface IVisualizationHandler {

    void update(JButton[][] buttons);

    String getName();

    default DisplayType getDisplayType() {
        return DisplayType.VISUALIZER;
    }

    default VisualizationCategory getVisualizationCategory() {
        return VisualizationCategory.DEFAULT;
    }

    default void lockDisplay() {
        CommandBus.getInstance().publish(Commands.LOCK_CURRENT_VISUALIZATION);
    }

    default void unlockDisplay() {
        CommandBus.getInstance().publish(Commands.UNLOCK_CURRENT_VISUALIZATION);
    }

    default Color [] getRainbowColors() {
        return new Color[] {
                Color.RED, Color.ORANGE, Color.YELLOW,
                Color.GREEN, Color.BLUE, new Color(75, 0, 130)
        };
    }

    public static Color[] getRainbowColors(int count) {
        Color[] colors = new Color[count];
        for (int i = 0; i < count; i++) {
            colors[i] = Color.getHSBColor((float) i / count, 1, 1);
        }
        return colors;
    }

    public static Color[] getRainbowColors(int count, float saturation, float brightness) {
        Color[] colors = new Color[count];
        for (int i = 0; i < count; i++) {
            colors[i] = Color.getHSBColor((float) i / count, saturation, brightness);
        }
        return colors;
    }

    public static Color[] getRainbowColors(int count, float saturation, float brightness, float alpha) {
        Color[] colors = new Color[count];
        for (int i = 0; i < count; i++) {
            colors[i] = new Color(Color.HSBtoRGB((float) i / count, saturation, brightness));
            colors[i] = new Color(colors[i].getRed(), colors[i].getGreen(), colors[i].getBlue(), (int) (alpha * 255));
        }
        return colors;
    }
}
